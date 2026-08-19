# Data Model: Coin Flip Decisions

**Feature**: `001-coin-flip-decisions` | **Date**: 2026-08-18

Derived from the spec's Key Entities section and Functional Requirements. Persisted via Room
(see [research.md](./research.md) §5). All persisted entities live in a local SQLite database with
no network sync.

## Entities

### Coin

A reusable decision context created and named by the user.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` (PK, autogenerate) | Room-generated identity. |
| `name` | `String` | Required, non-blank, max 40 characters (FR-015, FR-018). Not required to be unique (spec Assumptions). |
| `createdAt` | `Instant` | Set once at creation; used only for tie-breaking/ordering the full coin list, not for Quick Access scoring. |
| `interactionCount` | `Int`, default `0` | Incremented by 1 each time the coin is opened or flipped. Never decremented. Used in Quick Access scoring (research.md §7). |
| `lastInteractionAt` | `Instant?`, default `null` | Updated to "now" each time the coin is opened or flipped. `null` until first interaction (coin excluded from Quick Access — see below). |

**Validation rules**:
- `name` MUST NOT be blank (after trim) — FR-015.
- `name` length MUST NOT exceed 40 characters — FR-018.
- A `Coin` MUST NOT be persisted (insert or update) with fewer than 2 associated `Choice` rows —
  FR-002. Enforced in the repository's save operation as a single transaction covering the coin
  and its choices.

**Derived (not stored) value**:
- `quickAccessScore: Double` — computed at read time from `interactionCount` and
  `lastInteractionAt` per the formula in research.md §7. `null` `lastInteractionAt` ⇒ score `0`
  and the coin is excluded from the Quick Access section (it still appears in the full list).

### Choice

A single option belonging to a coin.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` (PK, autogenerate) | Room-generated identity. |
| `coinId` | `Long` (FK → `Coin.id`, `CASCADE` delete) | Deleting a coin deletes all its choices (FR-007). |
| `text` | `String` | Required, non-blank, max 60 characters (FR-015, FR-018). Duplicate text within the same coin is explicitly allowed (spec Edge Cases) — each row is independently selectable. |
| `position` | `Int` | 0-based order within the coin; preserves the user's editing order for display and for stable flip-result identification. |

**Validation rules**:
- `text` MUST NOT be blank (after trim) — FR-015.
- `text` length MUST NOT exceed 60 characters — FR-018.
- A `Choice` MUST NOT be removed if doing so would drop its parent coin below 2 remaining choices —
  FR-006. Enforced in the ViewModel/repository before the removal is committed.

### Decision Outcome (transient, not persisted)

The choice selected the last time a given coin was flipped.

| Field | Type | Notes |
|---|---|---|
| `coinId` | `Long` | Which coin this outcome belongs to. |
| `choice` | `Choice` | The selected choice (full row, so its text renders even if the underlying list is later edited within the same app session). |
| `flippedAt` | `Instant` | Timestamp of the flip, in-memory only. |

- Held in the Coin Flip screen's `ViewModel` state (`StateFlow`), never written to Room.
- Explicitly reset (cleared) on process death / app restart per spec Assumptions and the
  clarification "Reset on restart" — there is no persistence path for this entity by design.
- Not a historical log: only the single most recent outcome per open coin screen is kept; no list
  of past outcomes exists in the MVP.

## Relationships

```text
Coin (1) ──────< (N) Choice        [FK: Choice.coinId → Coin.id, ON DELETE CASCADE]
Coin (1) ──────  (0..1) Decision Outcome   [in-memory only, not a DB relationship]
```

- One `Coin` has many `Choice` rows (minimum 2, no maximum).
- A `Choice` belongs to exactly one `Coin`.
- `Decision Outcome` is not a stored relationship; it is transient UI state scoped to a single
  coin-flip screen session.

## State Transitions

Neither `Coin` nor `Choice` has a lifecycle status field — both simply exist (post-save) or are
deleted. The only relevant transitions are interaction-driven field updates:

```text
Coin created (save) ──> interactionCount=0, lastInteractionAt=null
        │
        ├─ opened or flipped ──> interactionCount += 1, lastInteractionAt = now
        │        (repeatable; no upper bound)
        │
        ├─ edited (name/choices changed, save) ──> fields updated in place;
        │        interactionCount / lastInteractionAt unaffected by editing itself
        │
        └─ deleted (confirmed) ──> Coin row and all Choice rows removed (cascade), terminal
```

## Consistency & Integrity Rules Summary

| Rule | Enforced by | Source |
|---|---|---|
| Coin name non-blank, ≤40 chars | UI input filter + save-time validation | FR-015, FR-018 |
| Choice text non-blank, ≤60 chars | UI input filter + save-time validation | FR-015, FR-018 |
| Coin has ≥2 choices at all times | Repository save transaction; UI blocks the removal that would violate it | FR-002, FR-006 |
| Deleting a coin removes its choices | Room `CASCADE` foreign key | FR-007 |
| Flip picks uniformly among current choices | Domain-layer flip function (research.md §6) | FR-011 |
| Decision Outcome not persisted across restarts | Kept only in ViewModel `StateFlow`, never written to Room | Clarification, Assumptions |
