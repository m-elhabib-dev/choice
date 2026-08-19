# Data Model: Coin Decision Suite

**Feature**: `002-coin-decision-suite` | **Date**: 2026-08-19

Derived from the spec's Key Entities section and Functional Requirements, extending
`001-coin-flip-decisions/data-model.md`. All persisted entities live in the existing local SQLite
database (Room) with no network sync. Fields inherited unchanged from `001` are listed but not
re-justified; see `001`'s data-model.md for their original rationale.

## Entities

### Coin (extended)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` (PK, autogenerate) | Unchanged from `001`. |
| `name` | `String` | Unchanged from `001` (non-blank, ≤40 chars, not required to be unique). |
| `createdAt` | `Instant` | Unchanged from `001`. |
| `interactionCount` | `Int`, default `0` | Unchanged column; no longer read for any ranking (research.md §9) but still incremented, retained as harmless historical metadata. |
| `lastInteractionAt` | `Instant?`, default `null` | Unchanged column; now the sole driver of Quick Coins ordering among favorites (research.md §9). |
| `isFavorite` | `Boolean`, default `false` | **New.** Toggled by the user (FR-002); drives inclusion in the Quick Coins section (FR-004). |
| `weightedEnabled` | `Boolean`, default `false` | **New.** When `true`, flips use each choice's `weight`; when `false`, flips are uniform regardless of any stored weights (FR-011, FR-012). |
| `avoidLastResultEnabled` | `Boolean`, default `false` | **New.** When `true`, flips exclude the coin's most recent `Decision` from the selection pool, with fallback (FR-016–FR-018). |

**Validation rules** (all unchanged from `001` plus):
- `name` non-blank, ≤40 chars; a `Coin` MUST NOT be persisted with fewer than 2 `Choice` rows —
  unchanged (FR from `001`).
- `isFavorite`, `weightedEnabled`, `avoidLastResultEnabled` are independent booleans — any
  combination is valid; none is required for the others (spec: "weighted selection is optional and
  should not complicate the normal coin creation experience").

### Choice (extended)

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` (PK, autogenerate) | Unchanged from `001`. |
| `coinId` | `Long` (FK → `Coin.id`, `CASCADE` delete) | Unchanged from `001`. |
| `text` | `String` | Unchanged from `001` (non-blank, ≤60 chars). |
| `position` | `Int` | Unchanged column; now also the target of user-driven reordering (FR-001), not just original entry order. |
| `weight` | `Int?`, default `null` | **New.** Positive whole number, no fixed upper bound (Clarifications). `null` means "use the baseline weight of `1`" (spec Assumptions) — every choice is always well-defined for weighted selection even if the user hasn't set a weight yet. |

**Validation rules** (all unchanged from `001` plus):
- `weight`, when not `null`, MUST be a positive whole number (`>= 1`) — FR-013, FR-014. Blank,
  zero, negative, non-numeric, or non-whole-number input is rejected before it reaches this field.
- `weight` is stored regardless of the parent coin's `weightedEnabled` value (so a value the user
  entered isn't lost if they temporarily disable weighting), but is only *read* for selection when
  `weightedEnabled == true` (research.md §3).

### Decision

**New entity.** A single completed, accepted decision — the coin's history and the source data for
its statistics.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` (PK, autogenerate) | Room-generated identity. |
| `coinId` | `Long` (FK → `Coin.id`, `CASCADE` delete) | Deleting a coin deletes its decision history with it (FR-023, Assumptions). |
| `choiceId` | `Long?` (FK → `Choice.id`, `SET NULL` delete) | The choice selected, while it still exists. Becomes `null` if that choice is later removed — the row is not deleted (FR-024). |
| `choiceTextSnapshot` | `String` | The choice's text *as it was at decision time*, captured on insert. Always used for display/statistics grouping instead of re-reading the live `Choice` row, so edits/removals never blank out or change past entries (FR-024, FR-026). |
| `decidedAt` | `Instant` | When the user accepted this decision (FR-020). |

**Validation rules**:
- A `Decision` is only ever inserted when the user explicitly accepts a flip result (FR-010) — an
  overridden/rejected flip never reaches this table.
- `choiceTextSnapshot` is set once at insert time and never updated afterward, even if the live
  `Choice.text` later changes (FR-024).

**Derived (not stored) values**:
- "Last result" for `avoidLastResultEnabled` filtering (research.md §4): the most recent
  `Decision.choiceId` for a coin, read at flip time — not a separate stored field (avoids a second
  source of truth for "the previous decision").
- `CoinStatistics` (research.md §5): total count, per-choice counts, most/least frequent choice
  (tie-broken by the tied choice's current `position`), and most recent decision — computed on
  demand from a coin's `Decision` list, never cached in the database.

### Coin Template (not persisted)

A bundled, read-only starting point used only to pre-fill the coin-creation flow.

| Field | Type | Notes |
|---|---|---|
| `name` | `String` | Suggested coin name (e.g., "Breakfast"). |
| `choices` | `List<String>` | Suggested starter choice texts. |

- Compiled into the app as a static list (research.md §8) — no table, no ID, nothing to migrate.
- Once used to create a coin, the resulting `Coin`/`Choice` rows are fully independent; there is no
  stored link back to the template (spec Assumptions).

### Shared Coin Payload (not persisted, transport-only)

A versioned, portable representation of one coin, used only in transit via the Android share
sheet or a pasted string; see contracts/share-payload-contract.md for the exact JSON shape.

| Field | Type | Notes |
|---|---|---|
| `schemaVersion` | `Int` | Currently `1`. Any other value is rejected at import (FR-037, FR-039). |
| `name` | `String` | The coin's name at share time. |
| `choices` | `List<{text: String, weight: Int?}>` | The coin's choices, in order, at share time. |
| `weightedEnabled` | `Boolean` | Carried so the imported coin reproduces weighted behavior (FR-036). |
| `avoidLastResultEnabled` | `Boolean` | Carried so the imported coin reproduces avoid-last behavior (FR-036). |

- Deliberately excludes `isFavorite`, `interactionCount`, `lastInteractionAt`, and all `Decision`
  rows — personal/local-only state per FR-036 and spec Assumptions.
- Importing always inserts a brand-new `Coin` (+ `Choice` rows); it never matches against or
  merges with an existing coin (FR-040).

## Relationships

```text
Coin (1) ──────< (N) Choice           [FK: Choice.coinId → Coin.id, ON DELETE CASCADE]
Coin (1) ──────< (N) Decision         [FK: Decision.coinId → Coin.id, ON DELETE CASCADE]  — new
Choice (1) ─────< (0..N) Decision     [FK: Decision.choiceId → Choice.id, ON DELETE SET NULL]  — new
```

- One `Coin` has many `Choice` rows (minimum 2, no maximum) — unchanged from `001`.
- One `Coin` has many `Decision` rows (zero to many) — all deleted if the coin is deleted.
- One `Choice` may be referenced by many `Decision` rows; removing the `Choice` nulls the
  reference on those rows rather than deleting them, so history survives the choice's removal.
- `Coin Template` and `Shared Coin Payload` have no database relationship to anything — they only
  ever produce a brand-new, independent `Coin`/`Choice` row set.

## Migration (`v1 → v2`)

Non-destructive `Migration(1, 2)`, preserving all existing `coins`/`choices` rows (FR-043, SC-005):

```sql
ALTER TABLE coins ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0;
ALTER TABLE coins ADD COLUMN weightedEnabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE coins ADD COLUMN avoidLastResultEnabled INTEGER NOT NULL DEFAULT 0;

ALTER TABLE choices ADD COLUMN weight INTEGER DEFAULT NULL;

CREATE TABLE decisions (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    coinId INTEGER NOT NULL,
    choiceId INTEGER,
    choiceTextSnapshot TEXT NOT NULL,
    decidedAt INTEGER NOT NULL,
    FOREIGN KEY (coinId) REFERENCES coins(id) ON DELETE CASCADE,
    FOREIGN KEY (choiceId) REFERENCES choices(id) ON DELETE SET NULL
);
CREATE INDEX index_decisions_coinId ON decisions(coinId);
CREATE INDEX index_decisions_choiceId ON decisions(choiceId);
```

Every pre-existing `coins`/`choices` row is unaffected except for gaining the three new `coins`
columns at their default (`false`) values and the new `choices.weight` column at `null` — matching
each new column's documented default above, so no existing coin silently becomes a favorite,
weighted, or avoid-last coin after the update.

## State Transitions

```text
Coin created (save) ──> isFavorite=false, weightedEnabled=false, avoidLastResultEnabled=false
        │
        ├─ favorited/unfavorited ──> isFavorite toggled; independent of every other field
        ├─ weighting enabled/disabled ──> weightedEnabled toggled; per-choice weight values
        │        are retained either way, only *read* when true (research.md §3)
        ├─ avoid-last-result enabled/disabled ──> avoidLastResultEnabled toggled independently
        ├─ flipped + accepted ──> one new Decision row inserted (coinId, choiceId,
        │        choiceTextSnapshot, decidedAt); does not touch Coin/Choice rows
        ├─ flipped + overridden (flip again) ──> no Decision row inserted; no persisted state
        │        change at all beyond the transient in-progress result in ViewModel state
        ├─ choice removed/edited ──> any Decision rows referencing it keep
        │        choiceTextSnapshot; choiceId becomes null if the choice was removed
        └─ deleted (confirmed) ──> Coin, its Choice rows, and its Decision rows all removed
                 (cascade), terminal
```

## Consistency & Integrity Rules Summary

| Rule | Enforced by | Source |
|---|---|---|
| Coin name/choice text limits, ≥2 choices | Unchanged from `001` | `001` FR-002/015/018 |
| Deleting a coin removes its choices and its decisions | Room `CASCADE` foreign keys (both) | FR-023, Assumptions |
| Removing a choice keeps its past decisions, choice reference nulled | Room `SET NULL` foreign key | FR-024 |
| A decision is only recorded on explicit accept | Repository method only called from the accept action, never on flip/override | FR-010 |
| `weight` must be a positive whole number when set | UI input filter + save-time validation | FR-013, FR-014 |
| Weighted selection only applies when `weightedEnabled` | Domain `selectChoice` orchestrator branches on the flag | FR-011, FR-012 |
| Avoid-last-result falls back to the full pool if exclusion would empty it | `applyAvoidLastResult` pure function | FR-018 |
| Statistics ties broken by current choice `position` | `computeStatistics` pure function | FR-025, Clarifications |
| Quick Coins = favorites ordered by `lastInteractionAt` desc | Repository query (research.md §9) | FR-004, Clarifications |
| Full coin list ordered alphabetically | Existing `observeCoins()` query (unchanged) | FR-006, Clarifications |
| Existing rows survive the `v1 → v2` schema change | Explicit `Migration(1, 2)`, no destructive fallback | FR-043, SC-005 |
