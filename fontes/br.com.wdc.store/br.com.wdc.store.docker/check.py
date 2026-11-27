#!/usr/bin/env python3
"""
Checks the compose setup end to end, from outside, with nothing but Python's standard library: both instances ready
and connected on their mesh, a sign-in through nginx whose token works on both instances, a session ended on one
instance refused by the other, nginx's limit on /api/auth, and Prometheus scraping both instances.
Run it after `docker compose up -d --wait`. Each check prints PASS or FAIL; the exit code is 1 when any failed.
"""
import base64
import hashlib
import hmac
import json
import os
import sys
import time
import urllib.error
import urllib.request

NGINX = os.environ.get("STORE_NGINX", "http://localhost:18080")
STORE_A = os.environ.get("STORE_A", "http://localhost:18081")
STORE_B = os.environ.get("STORE_B", "http://localhost:18082")
PROMETHEUS = os.environ.get("STORE_PROMETHEUS", "http://localhost:19090")
USER, PASSWORD = "jdoe", "jdoe"

failures = []


def call(base, path, body=None, token=None, method=None, raw=None):
    """A request; `body` goes as JSON, `raw` as bytes. Answers the status and the body (0 when unreachable)."""
    data = raw if raw is not None else json.dumps(body).encode() if body is not None else None
    content_type = "application/octet-stream" if raw is not None else "application/json"
    request = urllib.request.Request(base + path, data=data, method=method or ("POST" if data is not None else "GET"),
                                     headers={"Content-Type": content_type})
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            return response.status, response.read()
    except urllib.error.HTTPError as e:
        return e.code, e.read()
    except OSError:
        return 0, b""


def check(what, ok):
    print(("PASS " if ok else "FAIL ") + what)
    if not ok:
        failures.append(what)
    return ok


def wait_for(condition, seconds=120):
    until = time.time() + seconds
    while time.time() < until:
        try:
            if condition():
                return True
        except (OSError, ValueError):
            pass
        time.sleep(2)
    return False


def password_digest(password):
    """The password's digest the store's clients put into PBKDF2: MD5 as a signed big integer, in base 36."""
    n = int.from_bytes(hashlib.md5(password.encode()).digest(), "big", signed=True)
    sign, n = ("-", -n) if n < 0 else ("", n)
    digits, text = "0123456789abcdefghijklmnopqrstuvwxyz", ""
    while n:
        n, r = divmod(n, 36)
        text = digits[r] + text
    return sign + (text or "0")


def sign_in(base, user, password, login_base=None):
    """
    SCRAM-SHA-256 as the store's clients do it, the challenge asked of `base` and the login sent to
    `login_base` (the same by default); None when refused. Waits out nginx's limit (a run just before).
    """
    until = time.time() + 70
    status, body = call(base, "/api/auth/challenge", {"userName": user})
    while status == 429 and time.time() < until:
        time.sleep(5)
        status, body = call(base, "/api/auth/challenge", {"userName": user})
    if status != 200:
        return None
    challenge = json.loads(body)
    salted = hashlib.pbkdf2_hmac("sha256", password_digest(password).encode(), base64.b64decode(challenge["salt"]),
                                 challenge["iterations"], 32)
    client_key = hmac.new(salted, b"Client Key", "sha256").digest()
    message = f"n={user},r={challenge['nonce']}".encode()
    signature = hmac.new(hashlib.sha256(client_key).digest(), message, "sha256").digest()
    proof = base64.b64encode(bytes(a ^ b for a, b in zip(client_key, signature))).decode()
    status, body = call(login_base or base, "/api/auth/login",
                        {"userName": user, "proof": proof, "nonce": challenge["nonce"]})
    return json.loads(body) if status == 200 else None


def ready(base):
    status, body = call(base, "/health/ready")
    return json.loads(body) if status == 200 else None


def purchases(base, token):
    """An authenticated call: the status of counting the user's purchases."""
    return call(base, "/api/repo/purchase/count", {}, token)[0]


def mesh_connected():
    return all((ready(b) or {}).get("cluster", {}).get("peers") == 1 for b in (STORE_A, STORE_B))


def targets_up():
    _, body = call(PROMETHEUS, "/api/v1/targets")
    targets = json.loads(body)["data"]["activeTargets"]
    return len(targets) == 2 and all(t["health"] == "up" for t in targets)


check("nginx answers, an instance behind it is ready", wait_for(lambda: ready(NGINX) is not None))
check("instance A is ready", ready(STORE_A) is not None)
check("instance B is ready", ready(STORE_B) is not None)
check("each instance sees the other on the mesh", wait_for(mesh_connected, 60))

session = sign_in(NGINX, USER, PASSWORD)
if check(f"{USER} signs in through nginx", session is not None and bool(session.get("accessToken"))):
    token = session["accessToken"]
    check("the token works on instance A", purchases(STORE_A, token) == 200)
    check("and on instance B", purchases(STORE_B, token) == 200)
    call(STORE_A, "/api/auth/logout", {"refreshToken": session["refreshToken"]})
    time.sleep(1)
    check("signed out on A, the token is refused by B (the mesh told it)", purchases(STORE_B, token) == 401)
    check("and by A", purchases(STORE_A, token) == 401)

check("a challenge asked of A is answered on B", sign_in(STORE_A, USER, PASSWORD, login_base=STORE_B) is not None)

admin = sign_in(STORE_A, "admin", "admin")
if check("admin signs in on A", admin is not None):
    image_path = "/api/repo/product/0/image"
    _, original = call(STORE_B, image_path)  # B now holds it
    changed = original + b"changed"
    status, _ = call(STORE_A, image_path, token=admin["accessToken"], method="PUT", raw=changed)
    check("an image changed on A", status == 200)
    time.sleep(1)
    check("is served changed by B (the mesh told it)", call(STORE_B, image_path)[1] == changed)
    call(STORE_A, image_path, token=admin["accessToken"], method="PUT", raw=original)

statuses = [call(NGINX, "/api/auth/challenge", {"userName": "nobody"})[0] for _ in range(100)]
check(f"nginx limits /api/auth per address ({statuses.count(429)} of 100 answered 429)", 429 in statuses)

check("Prometheus scrapes both instances", wait_for(targets_up, 60))

print("all checks passed" if not failures else f"{len(failures)} check(s) failed")
sys.exit(1 if failures else 0)
