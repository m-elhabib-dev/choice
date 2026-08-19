# Contract: Shared coin payload format (v1)

**Feature**: `002-coin-decision-suite`

This is the one genuinely external interface this feature introduces: a portable representation of
a coin that crosses from one installation of Choice to another via Android's native share sheet or
a pasted string (FR-035–FR-041). It has no backend and no account — the payload itself is the
entire "protocol."

## Transport

- **Outbound**: `Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, json) }`,
  launched via `Intent.createChooser` (the standard Android share sheet).
- **Inbound**: either (a) `MainActivity` receives an `ACTION_SEND` `text/plain` intent directly
  (its manifest intent filter makes "Choice" a share-sheet destination) and routes the `EXTRA_TEXT`
  string straight to the Import screen, or (b) the user opens the Import screen directly and pastes
  the text manually.
- No network request is ever made by either path (Principle III).

## JSON shape (`schemaVersion: 1`)

```json
{
  "schemaVersion": 1,
  "name": "Breakfast",
  "choices": [
    { "text": "Ful", "weight": null },
    { "text": "Eggs", "weight": 2 },
    { "text": "Falafel", "weight": null },
    { "text": "Cheese", "weight": null }
  ],
  "weightedEnabled": true,
  "avoidLastResultEnabled": false
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| `schemaVersion` | integer | Yes | Must equal a version this app's `decodeSharedCoin` understands (currently only `1`). Any other value — including a future, higher version — is rejected (FR-037, FR-039), not guessed at. |
| `name` | string | Yes | Non-blank, ≤40 characters — the same limit enforced everywhere else (spec Assumptions). |
| `choices` | array of object | Yes | At least 2 entries (the coin's own minimum). |
| `choices[].text` | string | Yes | Non-blank, ≤60 characters. |
| `choices[].weight` | integer or `null` | Yes (key present, value may be `null`) | When present and non-null, MUST be a positive whole number. `null` means "use the baseline weight" on import, same as an unset local weight. |
| `weightedEnabled` | boolean | Yes | Carried as-is. |
| `avoidLastResultEnabled` | boolean | Yes | Carried as-is. |

**Deliberately excluded** (personal/local-only state, per FR-036 and spec Assumptions):
`isFavorite`, `interactionCount`, `lastInteractionAt`, decision history, choice/coin IDs.

## Validation rules on import (`decodeSharedCoin`)

Any failure below throws `InvalidSharePayloadException` and aborts the import with no partial
coin created (FR-039, SC-009):

1. The text is valid JSON and its root is an object.
2. `schemaVersion` is present, is an integer, and equals a version this app supports (`1`).
3. `name` is present, is a string, and is non-blank and ≤40 characters after trimming.
4. `choices` is present, is an array, and has at least 2 entries.
5. Every entry in `choices` has a non-blank, ≤60-character `text`.
6. Every entry's `weight`, if not `null`, is a positive whole number.
7. `weightedEnabled` and `avoidLastResultEnabled` are present and are booleans.

On success, `importSharedCoin` (contracts/repository-contract.md) always creates a brand-new
`Coin` + `Choice` rows — it never checks for or merges with an existing coin of the same name/
choices (FR-040); `isFavorite` starts `false` and no `Decision` rows are created.

## Versioning policy

- `schemaVersion` exists specifically so a future release can change this shape without breaking
  payloads shared between an old and a new install (FR-037).
- This contract only defines version `1`. A future version bump MUST add a new decode path for the
  new version (and, ideally, continue accepting `1` for backward compatibility) rather than
  mutating this document's v1 shape in place.
- An unrecognized `schemaVersion` (e.g., a payload from a future app version received on an older
  install) is always rejected with a clear message (FR-039) — the app never guesses at an unknown
  shape.
