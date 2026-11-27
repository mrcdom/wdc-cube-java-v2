# Architecture decision records

Each record states one decision that shapes this reference architecture: the problem it answered, what was decided, and what follows from it, including what it costs. They are written for engineers who adopt the architecture, or who wonder why the code is the way it is before changing it. The architecture documents in [docs/](../) describe how the code works; these records say why.

A record is not edited once accepted, except to fix a mistake or a link. A decision that changes gets a new record, and the old one's status says which record supersedes it.

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-the-cube-knows-only-scopes.md) | The Cube knows only scopes; each rendering technology owns and releases its views | Accepted |
| [0003](0003-places-declared-by-keys-classes.md) | Places are declared by keys classes, in a tree | Accepted |
| [0004](0004-actions-are-guarded-scope-fields.md) | Actions are guarded fields of the scope | Accepted; amended by 0021 |
| [0005](0005-computed-state-once-per-paint.md) | Computed state is committed once per paint cycle | Superseded by 0022 |
| [0006](0006-access-control-at-the-rest-boundary.md) | Access control lives at the REST boundary, as a data policy per entity | Accepted |
| [0007](0007-resource-calls-named-by-http-verbs.md) | Resource calls are named by HTTP-like verbs; writes open the transaction | Accepted |
| [0008](0008-read-only-and-read-write-repositories.md) | Read-only and read-write repository contracts | Accepted |
| [0009](0009-programmatic-transactions-per-module.md) | Programmatic CMT-style transactions, dual JTA/JDBC, one TransactionService per module | Accepted |
| [0010](0010-client-driven-remote-transactions.md) | Client-driven remote transactions over REST | Accepted |
| [0011](0011-persistence-with-jooq.md) | Persistence with jOOQ's type-safe DSL, no SQL strings | Accepted |
| [0012](0012-criteria-are-fields-of-criterion.md) | Criteria are fields of `Criterion`; requests accumulate with AND; alternative groups are server-side only; an empty `in` matches no row | Accepted |
| [0013](0013-dependency-injection-without-a-framework.md) | Dependency injection without a framework: `BEAN` holders filled by composition roots | Accepted |
| [0014](0014-server-driven-remote-views.md) | Server-driven remote views over the bridge/skeleton protocol | Accepted; amended by 0023 |
| [0015](0015-the-server-prices-items.md) | The server decides the price of an item | Superseded by 0020 |
| [0016](0016-framework-published-apart-from-the-demo.md) | The framework is published apart from the demo | Accepted |
| [0017](0017-fluent-entities-public-structs.md) | Entities have a fluent API; presentation structs keep public fields | Accepted |
| [0018](0018-five-view-technologies-share-the-presenters.md) | Five view technologies share the same presenters and scopes | Accepted |
| [0019](0019-api-response-headers.md) | Security headers, cache and Server-Timing on every API response | Accepted |
| [0020](0020-the-price-of-an-item-is-its-products.md) | The price of an item is always its product's, for every role | Accepted |
| [0021](0021-actions-run-one-at-a-time.md) | The actions of one application run one at a time | Accepted |
| [0022](0022-computed-state-in-every-host.md) | Computed state is committed once per paint cycle, in every host | Accepted |
| [0023](0023-remote-channel-key-per-connection.md) | The remote channel: a key agreed per connection, HMAC signatures, a versioned protocol | Accepted |
| [0024](0024-scram-sign-in.md) | Sign-in with SCRAM-SHA-256; failed sign-ins slow the account down | Accepted |
| [0025](0025-explicit-work-directory.md) | Every host runs from an explicit work directory | Accepted |
| [0026](0026-metrics-and-health.md) | Metrics with Micrometer in Prometheus format, behind a token; live and ready health checks | Accepted |
| [0027](0027-design-tokens.md) | One set of design tokens, generated for every view technology | Accepted |
| [0028](0028-ui-texts-in-one-catalog.md) | The UI texts in one catalog per language, translated by the views from keys | Accepted |
| [0029](0029-sessions-in-the-database-and-a-mesh-between-instances.md) | Sessions in the database, held in memory once read; the instances tell each other over a WebSocket mesh | Accepted |
| [0030](0030-compose-setup-with-two-instances.md) | A compose setup with two instances, its image made from what Maven built | Accepted |
| [0031](0031-remote-transaction-owned-by-user-and-client.md) | A remote transaction belongs to the user's client context, not to the user alone | Accepted |

New record: copy the shape of an existing one (`# NNNN. <decision as a sentence>`, Date, Status, Context, Decision, Consequences, and Alternatives considered when there were real ones), take the next number, and add it to this table.
