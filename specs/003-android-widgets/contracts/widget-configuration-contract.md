# Contract: Widget Configuration Activity Handshake

Both `SingleCoinWidgetConfigActivity` and `QuickCoinsWidgetConfigActivity` implement the standard
Android App Widget configuration handshake, plus one additional, app-defined "reconfigure" entry
point used from inside an already-placed widget's Unavailable state.

## 1. System-initiated configuration (widget just added to home screen)

**Trigger**: User drags the widget from the picker onto the home screen; the system starts the
Activity declared in `android:configure` with:

- `Intent.EXTRA` `AppWidgetManager.EXTRA_APPWIDGET_ID: Int` — always present, always valid, always
  belongs to the widget instance just added.

**Activity responsibilities**:
1. Read `EXTRA_APPWIDGET_ID`. If missing, `finish()` immediately (defensive; should not happen).
2. `setResult(RESULT_CANCELED)` immediately as the default, per platform convention — updated to
   `RESULT_OK` only on successful save (so back-press / process death before save cleanly removes
   the never-configured widget, matching FR-015's "let the user reconfigure rather than leaving it
   broken" for the setup path itself).
3. Load the current coin list from `CoinRepository`:
   - **Single Coin Widget**: if `observeCoins()` is empty → render the "no coins yet" state (FR-013)
     with a single action that opens `MainActivity` (coin creation entry point) and leaves this
     widget unconfigured (`RESULT_CANCELED`, widget instance removed by the system). Otherwise,
     render a coin picker list; selecting one and confirming writes `SingleCoinWidgetConfig(coinId)`
     for this `appWidgetId` and calls `setResult(RESULT_OK, Intent().putExtra(EXTRA_APPWIDGET_ID,
     appWidgetId))` then `finish()`.
   - **Quick Coins Widget**: if `observeCoins()` is empty → same "no coins yet" state/behavior as
     above. Otherwise, render a multi-select list defaulting to the current favorites (pre-checked,
     editable) with an explicit "use my favorites (auto-updating)" toggle; confirming with the
     toggle on writes `QuickCoinsWidgetConfig(useFavorites = true)`; confirming with an explicit
     subset writes `QuickCoinsWidgetConfig(useFavorites = false, explicitCoinIds = selection)`.
     Same `RESULT_OK`/`finish()` contract as above.
4. After `setResult(RESULT_OK, ...)`, the system itself triggers the first `onUpdate`/render for
   that `appWidgetId` — the Activity does not need to call `update()` itself.

## 2. App-initiated reconfiguration (from an Unavailable widget)

**Trigger**: User taps the "reconfigure" action shown on an already-placed widget that is in the
**Unavailable** or **No Coins Selected** state (FR-012, US4 AS3).

**Contract**: The widget's `ActionCallback` starts the *same* configuration Activity class,
directly, via a normal (non-system) `Intent`, passing:

- `EXTRA_APPWIDGET_ID: Int` set to that widget's own ID (obtained from the `GlanceId` the
  `ActionCallback` already has).
- An explicit `action` distinguishing this from the system's own configure intent is not required —
  the Activity's logic only depends on `EXTRA_APPWIDGET_ID` being present and valid, and the launch
  can reuse the identical read/save code path described in §1 steps 3–4 for whichever widget type
  it is. Because this is a normal in-app-triggered Activity start (not the system's
  `APPWIDGET_CONFIGURE` broadcast path), no `setResult`/system callback is required afterward; the
  Activity simply writes the new config and `finish()`es, and the reactive `WidgetRefreshCoordinator`
  path (research.md §3) — or a direct `updateAll()` call at the end of the save — picks up the
  change.

## 3. Failure / defensive behavior (FR-015)

If a stored config record is missing or cannot be decoded when a widget renders (e.g., after a
backup/restore mismatch that changes App Widget IDs, or a corrupted `Preferences` value), the
widget treats this identically to "Unavailable" (data-model.md) rather than crashing, and offers
the same reconfigure action described in §2.
