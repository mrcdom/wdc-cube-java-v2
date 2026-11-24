/**
 * The session: challenge-response authentication and the HTTP transport that uses it.
 *
 * Emits events (`change`) so the interface can react without this class knowing about the screen.
 */

import { hmacSha256, passwordDigest } from './crypto.js';

/** Request and response of a call, so the interface can show them. */
export class Exchange {
  constructor({ method, url, requestBody, status, responseBody, elapsedMs, error = null }) {
    Object.assign(this, { method, url, requestBody, status, responseBody, elapsedMs, error });
  }

  get ok() {
    return this.error === null && this.status >= 200 && this.status < 300;
  }
}

export class Session extends EventTarget {
  #accessToken = null;
  #refreshToken = null;
  #userName = null;
  #userId = null;

  get authenticated() {
    return this.#accessToken !== null;
  }

  get userName() {
    return this.#userName;
  }

  get userId() {
    return this.#userId;
  }

  get accessToken() {
    return this.#accessToken;
  }

  #announce() {
    this.dispatchEvent(new CustomEvent('change'));
  }

  /**
   * Challenge-response: asks for a nonce, replies with the HMAC of the password digest.
   *
   * The password never travels over the wire — not at login, not afterwards.
   */
  async login(userName, password) {
    const challenge = await this.send('GET', '/api/auth/challenge');
    if (!challenge.ok) throw new Error(`Could not get the challenge (HTTP ${challenge.status})`);

    const { nonce } = challenge.responseBody;
    const digest = await hmacSha256(passwordDigest(password), userName + nonce);

    const login = await this.send('POST', '/api/auth/login', { userName, digest, nonce });
    if (!login.ok) throw new Error(`Authentication failed (HTTP ${login.status})`);

    this.#accessToken = login.responseBody.accessToken;
    this.#refreshToken = login.responseBody.refreshToken;
    this.#userId = login.responseBody.userId;
    this.#userName = userName;
    this.#announce();
    return login;
  }

  async refresh() {
    if (!this.#refreshToken) throw new Error('No refresh token');
    const response = await this.send('POST', '/api/auth/refresh', { refreshToken: this.#refreshToken });
    if (response.ok) {
      this.#accessToken = response.responseBody.accessToken;
      this.#refreshToken = response.responseBody.refreshToken ?? this.#refreshToken;
      this.#announce();
    }
    return response;
  }

  async logout() {
    const response = this.#accessToken ? await this.send('POST', '/api/auth/logout', {}) : null;
    this.#accessToken = null;
    this.#refreshToken = null;
    this.#userName = null;
    this.#userId = null;
    this.#announce();
    return response;
  }

  /** A call to the API, returning everything the interface needs to show — including the error. */
  async send(method, url, body = null) {
    const headers = { Accept: 'application/json' };
    if (body !== null) headers['Content-Type'] = 'application/json';
    if (this.#accessToken) headers.Authorization = `Bearer ${this.#accessToken}`;

    const started = performance.now();
    try {
      const response = await fetch(url, {
        method,
        headers,
        body: body === null ? undefined : JSON.stringify(body),
      });

      const text = await response.text();
      let parsed = text;
      try {
        parsed = text ? JSON.parse(text) : null;
      } catch {
        // A response that is not JSON (an image, a plain-text server error) is kept as it came.
      }

      return new Exchange({
        method,
        url,
        requestBody: body,
        status: response.status,
        responseBody: parsed,
        elapsedMs: Math.round(performance.now() - started),
      });
    } catch (cause) {
      return new Exchange({
        method,
        url,
        requestBody: body,
        status: 0,
        responseBody: null,
        elapsedMs: Math.round(performance.now() - started),
        error: cause.message,
      });
    }
  }
}
