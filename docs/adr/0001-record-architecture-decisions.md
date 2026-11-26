# 0001. Record architecture decisions

- **Date:** 2026-11-26
- **Status:** Accepted

## Context

This project is a reference architecture: people read it to build their own applications on the Cube MVP pattern and its framework. The documents in [docs/](../) describe how the code works today — the presentation architecture, the data layer, the security model, the bridge protocol — but not why it is that way, nor what it replaced. That reasoning lives in commit messages spread over several months, out of reach for someone who only reads the code.

Several choices look odd without their reason: no DI framework, no secured repositories, presenters that never touch a view, a server that ignores the price a client sends. Each is at risk of being "fixed" back by someone who does not know what it answered.

## Decision

Significant decisions are recorded in `docs/adr/`, one per file, numbered, in the shape of Michael Nygard's architecture decision records: context, decision, consequences, and the alternatives that were considered when there were real ones. The records are in US English, like the rest of the repository, and link to the architecture documents for details instead of repeating them.

A decision is significant when it shapes the code that adopters write, when it costs something that has to be accepted knowingly, or when it departs from what a reader would expect.

## Consequences

- The records 0002 to 0019 were written after the fact, from the code, the documents and the git history; the date of each is when its decision was made, and a decision that grew in steps gives the date of each step.
- A record is not edited once accepted, except to fix a mistake or a link. A change that goes against a record supersedes it with a new one, and the old record's status names its successor, so the reasoning stays visible.
- The architecture documents stay the description of the present; the records are the history of why.
