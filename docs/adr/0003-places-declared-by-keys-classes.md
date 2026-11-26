# 0003. Places are declared by keys classes, in a tree

- **Date:** 2026-11-23
- **Status:** Accepted

## Context

Places used to be an enum, with a route builder that repeated each place's path (`step(...)`) and a `GoAction` registered from the enum's constructor; some hosts called `Routes.Place.values()` only to force that registration. The names a place read and wrote in the address lived in `PlaceParameters` and `PlaceAttributes`, as strings any presenter could use, and an unusable value in the address (`productId=abc`) ended in an `AssertionError`.

## Decision

`CubePlace` is a tree node: `CubePlace.root(name, factory)` and `parent.child(name, factory)`. A place's path comes from the tree, so navigation is `app.navigate(place, intent)`, and an address is the deepest place's name plus its query string.

Each place has a **keys class** (`XxxKeys`, extending the framework's `CubeKeys`) that declares it in a static field, from the place of the keys class it extends:

```java
public static final CubePlace PLACE = HomeKeys.PLACE.child("product", ProductPresenter::new);
```

The class tree is the place tree. The keys class is the only one that knows the names its place reads and writes in the intent; each name starts with the class's **two-letter prefix** (`pr-id`, `rc-id`), unique in the application. Getters never throw (the address comes from outside); a setter with the default value removes the name from the address.

Without an intent, keys start from the current state (`app.newIntent()`) and `flip()` navigates: `new ProductKeys(app).withProductId(id).flip()`. Inside `applyParameters`, a redirect uses the intent in transit: `new HomeKeys(app, intent).flip()`.

`StoreApplication.PLACES` lists every place, which loads the keys classes before an address is resolved by name; `CubeApplication.go` resolves the name in the tree and falls back to the root.

## Consequences

- Navigation is typed and has one entry per place; renaming an address parameter touches one class.
- A keys class not listed in `PLACES` is not in the tree until something loads it. `PlaceKeysTest` checks unique prefixes, names carrying their class's prefix, class tree = place tree, a complete `PLACES`, and getters that do not trust the address.
- Addresses saved before the change (`productId=`) stopped working.

## Alternatives considered

- **An enum of places with `Routes`, `PlaceParameters` and `PlaceAttributes`.** Replaced: paths repeated per route, untyped names shared by everyone, and registration that depended on class loading tricks.

See [cube-mvp-multiplatform-architecture.md → State View](../cube-mvp-multiplatform-architecture.md#state-view).
