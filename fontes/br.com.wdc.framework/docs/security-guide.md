# Securing an Application with the WDC Framework

A practical guide for developers who build on the framework: how to authenticate users, how to decide what each role may do with the data your REST API exposes, and what to take care of where the presenters run on the server. You need no knowledge of the framework's internals; the examples come from **WDC Store**, the sample application of this repository.

> Looking for the reasoning behind the design, the threat model and the known limitations? See [Security Architecture](../../../docs/security-architecture.md).

## Contents

1. [What the framework gives you, and what you write](#1-what-the-framework-gives-you-and-what-you-write)
2. [Authentication](#2-authentication)
3. [Authorizing a REST API with data access policies](#3-authorizing-a-rest-api-with-data-access-policies)
4. [Criteria: alternative groups and searches](#4-criteria-alternative-groups-and-searches)
5. [Hosts where the presenters run on the server](#5-hosts-where-the-presenters-run-on-the-server)
6. [Checklist](#6-checklist)

---

## 1. What the framework gives you, and what you write

| Concern | The framework gives you (`framework-domain`, `framework-jooq`, `framework-cube-remote`) | You write |
|---------|--------------------------------------------------------------------------------------|-----------|
| Who the requester is | `SecurityContext` (user id, name, **role names**) and its per-thread holder `SecurityContext.CURRENT` | The filter of your HTTP layer that sets it for each request |
| Logging in | The `AuthenticationService` contract, `Scram`/`ScramVerifier` (SCRAM-SHA-256), `PasswordVerifiers`, `PasswordUtil`, `CryptoProvider` | The implementation: users, sessions, tokens, limits on guessing |
| What each role may do with the data | `DataPolicy`, `DataGrant`, `RowScope`, `Mask`, `EntityDescriptor`, `DataAccess` | One policy per entity your REST API exposes, and one call to it per endpoint |
| Disjunctions in queries | `Criteria.anyOf`, `CriterionTranslator.anyOf` / `translateAny`, `CriterionCodec.refuseAnyOf` | One line in each repository and each codec |
| Remote presentation channel | Signed session ids, handshake, encryption of sensitive values, signed navigation | Production keys, TLS, and scoping the ids your presenters receive |

---

## 2. Authentication

### The requester

Every layer finds the requester in `SecurityContext.CURRENT`. Your HTTP layer sets it at the start of each request — from the bearer token, through your `AuthenticationService.resolveToken(...)` — and clears it at the end. The one thing the data policies need from it is `roles()`: **the names of the user's roles**, which is what the policies grant by.

```java
public interface SecurityContext {
    ThreadLocal<SecurityContext> CURRENT = new ThreadLocal<>();
    Long userId();
    String userName();
    Set<String> roles();          // "ADMIN", "CUSTOMER", ...
    PrivateKey privateKey();      // the session's key, to decrypt passwords sent by the client
    String publicKeyBase64();
}
```

### The authentication service

Implement `AuthenticationService` and register it in `AuthenticationService.BEAN` at startup. Its contract is a **SCRAM-SHA-256** sign-in (RFC 5802/7677): the password never travels, and what the server stores — a `ScramVerifier` — signs no one in:

| Method | Does |
|--------|------|
| `challenge(userName)` | A single-use nonce for that user, and the salt and iterations of its verifier (made-up but stable for an unknown user) |
| `login(userName, proof, nonce)` | Checks the client's proof against the verifier (`Scram.verifyProof`) and opens a session: access token, refresh token, the session's public key and the server's signature, which the client checks (`Scram.ClientResponse.serverIsAuthentic`). May throw `TooManyRequestsException` when the account must wait |
| `refresh(refreshToken)` | Renews the tokens |
| `logout(refreshToken)` | Ends the session of that device |
| `resolveToken(jwt)` | The `SecurityContext` of a bearer token, or `null` |
| `createPersistentToken(...)` / `loginWithPersistentToken(...)` | "Remember me" for the remote hosts |

The client's side is `Scram.respond(userName, password, challenge)`: the proof to send and the signature to expect. `PasswordVerifiers.fromPassword` makes the verifier to store, with a fresh salt and `ScramVerifier.DEFAULT_ITERATIONS` (4096); `PasswordVerifiers.fromDigest` converts a stored `PasswordUtil.hashPassword` digest into the verifier of the same password.

When no `AuthenticationService` is registered, there is no requester, and the data policies let everything through — that is what local and test modes do. WDC Store's implementation, `AuthenticationServiceImpl`, is a complete example.

---

## 3. Authorizing a REST API with data access policies

A REST API that accepts the client's criteria and projection is convenient — no transfer object per screen — and risky: a modified client can ask for anything. A **data access policy** states, once per entity and per role, which rows a request reaches, which fields it reads (as they are or masked), and which fields it writes. Your endpoints apply it to every request.

### 3.1 Describe the entity

An `EntityDescriptor` names each field as it travels on the wire and tells the policy how to read and write it. The last argument of a field is the value that marks it in a projection (`ProjectionValues`).

```java
private static final ProjectionValues PV = ProjectionValues.INSTANCE;

public static final EntityDescriptor<Product, ProductCriteria> PRODUCT = EntityDescriptor.<Product, ProductCriteria>builder("product")
        .entity(Product::new)
        .criteria(ProductCriteria::new)
        .projection(ProductCriteria::projection, ProductCriteria::withProjection)
        .anyOf(ProductCriteria::anyOf, ProductCriteria::withAnyOf)
        .key("id", Product::id, Product::withId, PV.i64)
        .field("name", Product::name, Product::withName, PV.str)
        .field("price", Product::price, Product::withPrice, PV.f64)
        .field("description", Product::description, Product::withDescription, PV.str)
        .criterion("productId", "id")                       // a criteria field named differently from its entity field
        .orderings(ProductDataPolicy::orderingFields)       // the fields each provisioned ordering sorts by
        .build();
```

`build()` fails if a criteria field maps to no entity field, so a field added to the criteria without a mapping is caught at startup.

**Relations.** A field that points to another entity is a `relation`; a list of rows of another entity is a `collection`. Each names the policy of the other entity — as a `Supplier`, because entities refer to each other:

```java
.relation("user", Purchase::user, Purchase::withUser, () -> UserDataPolicy.POLICY)
.collection("items", Purchase::items, Purchase::withItems, () -> PurchaseItemDataPolicy.POLICY, "purchase")
```

The last argument of a collection is the field of each child that points back to the owner (`item.purchase`).

**Default shape.** What a read without a projection returns. Without a declaration, it is every field but the collections, relations by key. Declare one when the natural answer goes into relations:

```java
.defaultShape(() -> new Purchase()
        .withBuyDate(PV.offsetDateTime)
        .withUser(new User()
                .withName(PV.str))
        .withItems(PV.singletonList(itemShape(), new PurchaseItemCriteria())))
```

### 3.2 Say which rows a grant reaches

A `RowScope` selects rows twice, in two languages that must agree: as criteria, for the database, and in memory, for each returned row. Declare each scope once, as a constant.

```java
/** The requester's own purchases. */
public static final RowScope<Purchase, PurchaseCriteria> OWN = new RowScope<>() {

    @Override
    public void restrict(PurchaseCriteria group, SecurityContext sc) {
        group.withUserId(sc.userId());                     // in the database
    }

    @Override
    public boolean covers(Purchase row, SecurityContext sc) {
        return row.user() != null && sc.userId().equals(row.user().id());   // in memory
    }

    @Override
    public Purchase projection() {                         // what covers() reads besides the key
        return new Purchase()
                .withUser(new User()
                        .withId(PV.i64));
    }
};
```

What `projection()` names is fetched even when the client does not ask for it, and removed before the response. Without `rows(...)`, a grant reaches every row (`RowScope.all()`).

### 3.3 Declare the policy

Grants are declared per role name, with **projections** — the same objects a query uses — so a renamed field breaks the policy at compile time. A role may have several grants, one per set of rows.

```java
public static final DataPolicy<User, UserCriteria> POLICY = DataPolicy.on(USER)
        .grant("ADMIN", g -> g
                .read(new User()
                        .withUserName(PV.str)
                        .withName(PV.str)
                        .withRoles(PV.str))
                .create(new User()
                        .withUserName(PV.str)
                        .withPassword(PV.str)
                        .withName(PV.str)
                        .withRoles(PV.str))
                .update(...)
                .delete())
        .grant("CUSTOMER", g -> g
                .rows(OWN)
                .read(ownUser()))
        .grant("MANAGER", g -> g                          // its own user, in full
                .rows(OWN)
                .read(ownUser()))
        .grant("MANAGER", g -> g                          // everyone else: name, and the login masked
                .read(new User()
                        .withName(PV.str))
                .mask(new User()
                        .withUserName(PV.str), Mask.keepFirst(1)))
        .build();
```

| Part | Means |
|------|-------|
| `rows(scope)` | The rows the grant reaches (all, when omitted) |
| `read(projection)` | Fields read as they are — the only ones usable in filters and orderings |
| `mask(projection, mask)` | Fields read through a mask (`Mask.keepFirst(n)`, `Mask.keepLast(n)`, or your own); textual fields only |
| `create(projection)` / `update(projection)` | Fields that may be set when creating / changed when updating |
| `delete()` | The reachable rows may be deleted |

You never declare the key: whoever reads a row reads its key, and nobody writes it. A field in no `read` grant — the password above — is never returned.

### 3.4 Apply it in your endpoints

Get the requester's access once per request, then make **one call per operation**. `accessFor(null)` means "no security" and lets everything through, so a missing requester must be refused **before** — when security is on, an unauthenticated request never reaches the policy:

```java
private static DataAccess<Purchase, PurchaseCriteria> access() {
    return PurchaseDataPolicy.POLICY.accessFor(requester());
}

/** The requester; null only when no authentication service is registered (local and test modes). */
private static SecurityContext requester() {
    if (AuthenticationService.BEAN.get() == null) {
        return null;
    }
    var sc = SecurityContext.CURRENT.get();
    if (sc == null) {
        throw new AccessDeniedException("Authentication required");
    }
    return sc;
}
```

WDC Store keeps this helper in `SecurityEnforcer.requireAuthenticated()`.

| Endpoint | Call |
|----------|------|
| fetch, fetch-page, fetch by id | `var plan = access.prepareRead(criteria);` before the repository, then `plan.apply(rows)` on what it returns |
| count | `access.restrictCount(criteria);` |
| insert | `access.checkCreate(entity);` |
| update | `access.checkUpdate(entity, changes, (row, projection) -> repo.fetchById(row.id(), projection));` |
| delete | `access.restrictDelete(criteria);` |

```java
private void fetch(Context ctx) {
    var access = access();
    var criteria = readCriteria(ctx);                       // criteria and projection, as the client sent them
    var plan = access.prepareRead(criteria);                // checks; restricts the rows; completes the projection
    var items = plan.apply(repo().fetch(criteria, offset, limit));   // each row: fields as they are, masked or removed
    writeItems(ctx, items);
}
```

Fetch by id as a fetch with the key in the criteria, so it follows the same rules:

```java
var criteria = new PurchaseCriteria()
        .withPurchaseId(id)
        .withProjection(projection);
var plan = access.prepareRead(criteria);
var found = repo().fetch(criteria, 0, 1);
return found.isEmpty() ? null : plan.apply(found.get(0));
```

Map `AccessDeniedException` to **403** in your HTTP layer, with its message in the body.

### 3.5 What your clients see

| The client… | It gets |
|-------------|---------|
| Asks for a row its roles do not reach | Nothing — not found, not counted |
| Projects a field no grant of its roles reads | **403** naming the field: `not allowed to read user fields [password]` |
| Filters or sorts by a field it may only read masked, or not at all | Only the rows where some grant reads it as it is; **403** when none does |
| Sends no projection | The entity's default shape, reduced to what it may read — never a 403 |
| Writes a field its grant does not allow, or a row its grant does not reach | **403** |
| Sends a relation with more than its key (`user: {id, name}`) | **403** — relations are written by key |

The policy is not sent to the client. Build your clients for their roles, and treat a 403 as an unexpected error.

### 3.6 Rules worth knowing

- **Several roles combine row by row.** Each returned row shows what the grants that reach that row allow — never "the columns of one role over the rows of another".
- **Relations follow their own policy.** `purchase.user.name` is checked by the user policy, and a purchase's items are restricted by the item policy, wherever they appear.
- **A field your server decides is not writable.** Leave it out of the client's grant and fill it on the server — WDC Store records an item's price from its product, in the `INSERT`, and never takes one from a customer.
- **A grant cannot write what it masks** — `build()` refuses it — so a masked value sent back is a 403, not an overwrite.
- **Updates cannot move a row out of reach.** A grant over one's own rows cannot hand a row to someone else by changing its owner.

### 3.7 Test with authentication on

Policies are only exercised when a requester exists. Run your REST tests with the authentication service registered and a real login per test user — WDC Store does it with `TestEnvironment.withSecurity()` and `RestSession.signIn(env, "jdoe")` — and assert both what a role reaches and what it is refused.

---

## 4. Criteria: alternative groups and searches

Every `Criteria` has **alternative groups**, `anyOf()`: the criteria matches when its own fields match **and** at least one group does. The policies use them to restrict rows, so your code must support them in two places:

```java
// In each repository's ApplyConditions — translate the groups with the same apply
return CriterionTranslator.and(Arrays.asList(
        CriterionTranslator.translate(enProduct.ID, criteria.productId()),
        CriterionTranslator.translate(enProduct.NAME, criteria.name()),
        CriterionTranslator.anyOf(criteria.anyOf(), this::apply)));

// In each codec — the groups are server-side only, and refused on the wire
public void writeCriteriaFields(ExtensibleObjectOutput out, ProductCriteria criteria) {
    CriterionCodec.refuseAnyOf(criteria);
    ...
}
public boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, ProductCriteria criteria) {
    switch (fieldName) {
        case CriterionCodec.ANY_OF -> throw CriterionCodec.anyOfDoesNotTravel();
        ...
    }
}
```

Groups do not travel because a client free to send any tree of `OR`s over any columns eventually sends the query no index supports. When a client needs a **search** — a term in the name, the code or the document — add a criteria field for it, and let the repository choose the columns:

```java
// a "term" criteria field, searched in the name and the description
CriterionTranslator.translateAny(List.of(enProduct.NAME, enProduct.DESCRIPTION), criteria.term())
```

---

## 5. Hosts where the presenters run on the server

In the remote hosts (React, Flutter, TeaVM shell, Codename One) and in Vaadin, the presenters run on the server and query the repositories directly; the data policies do not apply there. The framework protects the channel; the presenter owns two duties.

**Automatic** (`framework-cube-remote`): session ids are signed and cannot be forged; the client's sensitive values (its storage, the access token) are encrypted with AES-GCM under a key agreed per connection in the handshake (ECDH P-256 + HKDF-SHA256, a new key on every reconnection); a navigation URL the server did not sign is replaced by the current state.

**Yours:**

- **Scope every id that comes from an event.** An event's form data is chosen by the client, and the navigation the server builds from it is signed by the server itself — the signature does not vouch for the id. Look up by id **and** owner:

  ```java
  var found = PurchaseRepository.BEAN.get().fetch(new PurchaseCriteria()
          .withPurchaseId(purchaseId)
          .withUserId(app.getSubject().getId())          // another user's purchase is not found
          .withProjection(projection()), 0, 1);
  ```

- **Keep the scope to what the screen shows.** Everything in a scope reaches the client; mask on the server what the screen shows masked.

- **Deploy with production keys and TLS.** The protocol's own encryption covers only sensitive values: serve everything over HTTPS/WSS, and set your own secrets — the key that signs the session ids and addresses (`[remote] signingKey` in the WDC Store), and the JWT secret.

---

## 6. Checklist

- [ ] An `AuthenticationService` is registered, and your HTTP filter sets and clears `SecurityContext.CURRENT`, with the role names; an unauthenticated request is refused before any policy is asked.
- [ ] Every entity the REST API exposes has an `EntityDescriptor` and a `DataPolicy`; every relation and collection names the related policy.
- [ ] Every endpoint calls its `DataAccess` method; reads apply the plan to what they return; fetch by id goes through `prepareRead`.
- [ ] `AccessDeniedException` becomes a 403 with its message.
- [ ] No grant reads a secret (passwords, tokens); fields the server decides are in no client grant.
- [ ] Each repository translates `anyOf`; each codec refuses it.
- [ ] REST tests run with authentication on, per role.
- [ ] Presenters scope every id received in an event to the user.
- [ ] Production: TLS everywhere, production keys, and the hardening items in [Security Architecture → Known Limitations](../../../docs/security-architecture.md#known-limitations-and-hardening).
