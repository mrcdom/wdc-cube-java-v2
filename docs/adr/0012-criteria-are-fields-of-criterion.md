# 0012. Criteria are fields of `Criterion`; requests accumulate with AND; alternative groups are server-side only; an empty `in` matches no row

- **Date:** 2026-09-08 (`Criterion` fields); 2026-11-24 (alternative groups); 2026-11-26 (empty `in`)
- **Status:** Accepted

## Context

A field of an `XxxCriteria` used to be a bare value, which could only mean equality: no range, no `in`, no "starts with", no disjunction, and every repository translated its fields by hand. On the wire the field was the bare value too, so anything richer would have been dropped silently.

Two later needs followed. The data policies must restrict rows by several scopes ("my rows, or my store's"), a disjunction across fields that per-field `or()` cannot express ([0006](0006-access-control-at-the-rest-boundary.md)). And an `in` with an empty list dropped the filter: a list of ids that came out empty fetched the whole table, and a delete with nothing selected wiped it.

## Decision

- **Each field is a `Criterion`** (`framework.domain.criteria`), and its class decides what can be asked: `Criterion` (`eq`, `ne`, `in`, `isNull`, `isNotNull`), `ComparableCriterion` (`gt`, `ge`, `lt`, `le`, `between`), `TextCriterion` (`like`, `ilike`, `containing`, `startingWith`). A `between` on a field with no useful order does not compile.
- **Requests accumulate with `AND`**, so `ge(start)` then `le(end)` is a range and `ne(1)` then `ne(2)` excludes both. `or()` makes the field disjunctive, within that field only. A `null` value adds no request; `clear()` erases, `isNull()` compares with null.
- **The field is never `null`**: `criteria.xxx() == null` compiles and is always false; `hasXxx()` tells whether it is set.
- **Alternative groups** (`anyOf()` / `withAnyOf(group, ...)`, each group a criteria of the same entity) express a disjunction across fields — **on the server only**. `CriterionCodec.refuseAnyOf` refuses them on both sides of the wire (400 on the server). A client's search of one term over several columns is a criteria field of its own, translated with `CriterionTranslator.translateAny`.
- **An empty `in` matches no row**: a search finds nothing, a count is 0, a delete deletes nothing (`CriterionTranslator` emits a false condition). Only `null` leaves the field out.
- **The translation to jOOQ is written once**, in `CriterionTranslator`; each `ApplyConditions` only maps field → column, with a value converter where the types differ.
- **On the wire** a field is an object with its requests (`{"or": true, "p": [{"o": "GE", "v": [10.0]}]}`); a bare value is still read as equality.

## Consequences

- Filters built from screen fields behave as expected, and the dangerous mistakes (an `OR` default, an empty list meaning "all") are ruled out by the defaults.
- The REST client cannot send an arbitrary tree of `OR`s, so it cannot ask for a query no index supports or widen what a policy grants.
- Code migrated from bare fields must replace `== null` tests with `hasXxx()`; two delete guards silently broke that way during the migration.

See [data-layer.md → Criterion](../data-layer.md#criterion-one-field-several-requests) and [security-architecture.md → Alternative groups](../security-architecture.md#alternative-groups-in-the-criteria-server-side-only).
