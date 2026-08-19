# Research: Coin Flip Decisions

**Feature**: `001-coin-flip-decisions` | **Date**: 2026-08-18

This document resolves every `NEEDS CLARIFICATION` item from the plan's Technical Context and
records the rationale for each technology/design choice, per the project constitution
(`.specify/memory/constitution.md`).

## 1. Platform baseline (Kotlin, SDK levels)

- **Decision**: Kotlin 2.0+, Android Gradle Plugin (current stable), `compileSdk` = latest stable
  (35, Android 15), `targetSdk` = 35, `minSdk` = 26 (Android 8.0 Oreo).
- **Rationale**: Constitution Principle IV (Modern Android) requires current, officially supported
  tooling. `minSdk` 26 covers the overwhelming majority of active Android devices (per official
  Android distribution data trends) while unlocking modern language/runtime features (e.g.
  `java.time`, adaptive icons) without backport dependencies — keeping dependency count low
  (Principle VII).
- **Alternatives considered**: `minSdk` 21 (rejected — forces `java.time`/desugaring dependencies
  and legacy compatibility shims for no user-facing benefit to this MVP); `minSdk` 30+ (rejected —
  excludes a meaningful slice of real devices for no feature gain).

## 2. UI toolkit

- **Decision**: Jetpack Compose (Material 3) as the sole UI toolkit; no XML layouts.
- **Rationale**: Compose is the current officially-recommended Android UI toolkit (Principle IV),
  and its theming APIs (`MaterialTheme`, custom `ColorScheme`) are the natural home for a single
  shared Catppuccin Mocha token layer (Principle V), avoiding per-screen styling duplication.
- **Alternatives considered**: Classic View/XML system (rejected — legacy, more boilerplate for
  theming consistency, contradicts Modern Android).

## 3. Architecture pattern

- **Decision**: MVVM — Compose screens (stateless where practical) driven by `ViewModel`s exposing
  `StateFlow<UiState>`; a small `data`/`domain` layer separates persistence and decision logic from
  UI, per Jetpack's recommended app architecture.
- **Rationale**: Matches Principle VI (Testable Behavior) — ViewModels and repository/domain logic
  are plain Kotlin/coroutines, testable without Android UI instrumentation. Matches Principle IV
  (official Jetpack architecture guidance).
- **Alternatives considered**: MVI with a single global reducer (rejected — adds ceremony beyond
  what this small MVP's screens need; violates Simplicity First); putting logic directly in
  Composables (rejected — fails Testable Behavior).

## 4. Dependency injection

- **Decision**: Manual DI via a small `AppContainer` (plain Kotlin object graph constructed in the
  `Application` class), injected into `ViewModel`s via a simple `ViewModelProvider.Factory`. No DI
  framework.
- **Rationale**: Principle VII (Minimal Dependencies) requires justifying any third-party/Jetpack
  DI framework. This app has ~4 screens and a handful of classes (one repository, one database);
  a full Hilt setup (annotation processing, generated components) adds build complexity and a
  dependency that isn't earning its cost at this scale. Manual DI is fully testable (fakes/mocks
  passed directly) and simpler (Principle I).
- **Alternatives considered**: Hilt (rejected for MVP scale — reconsider if the app grows enough
  modules/screens that manual wiring becomes unwieldy; note left for future amendment, not a
  current violation since Hilt was never adopted).

## 5. Persistence

- **Decision**: Room (SQLite) with two tables (`coins`, `choices`), accessed through DAOs wrapped
  by a single `CoinRepository`. All access via Kotlin coroutines/`Flow` for reactive UI updates.
- **Rationale**: Principle III (Local First) requires on-device storage with no network dependency;
  Room is the Jetpack-recommended persistence library (Principle IV/VII) and provides compile-time
  verified SQL, migrations, and `Flow` support that maps directly onto the reactive Compose UI.
- **Alternatives considered**: Raw SQLite (rejected — more boilerplate, no compile-time query
  checking, violates Simplicity First); DataStore/Proto (rejected — designed for
  key-value/preferences, not a relational one-to-many Coin→Choices structure); a flat JSON file
  (rejected — no query support, harder to guarantee atomic updates under concurrent
  read/write, weaker durability guarantees than SQLite).

## 6. Random selection algorithm

- **Decision**: `kotlin.random.Random` (default seeded instance) to pick a uniformly random index
  into the coin's ordered choice list at flip time: `choices[Random.nextInt(choices.size)]`.
- **Rationale**: Satisfies FR-011 (equal chance per choice, regardless of count) and SC-003
  (statistically even frequency proportional to each choice's share — duplicate text choices are
  distinct list entries, so duplicates naturally get proportionally more weight, matching the
  spec's documented edge case). `kotlin.random.Random` is uniform over `[0, size)` with no need for
  cryptographic randomness (this is a decision aid, not a security control).
- **Alternatives considered**: `java.security.SecureRandom` (rejected — no security requirement,
  unnecessary dependency/overhead); weighting choices unequally (rejected — explicitly contradicts
  FR-011).

## 7. Quick Access ranking (combined recency + frequency score)

- **Decision**: Each `Coin` stores `interactionCount: Int` (incremented whenever the coin is opened
  or flipped) and `lastInteractionAt: Instant` (updated on the same events). Quick Access rank is
  computed at read time as:

  ```text
  score = interactionCount * 2^(-hoursSinceLastInteraction / HALF_LIFE_HOURS)
  HALF_LIFE_HOURS = 72  (3 days)
  ```

  The main screen's Quick Access section shows the top 5 coins by `score` (descending), omitting
  coins with `interactionCount == 0`; if fewer than 5 coins have any interaction, the section shows
  only those that qualify (minimum 0 — the section itself is hidden if no coin has ever been
  used, deferring to the empty-state guidance in that case).
- **Rationale**: Directly implements the clarified requirement ("combined score — a blend of
  recency and frequency, frequency weighted higher for recent activity") and SC-004 (top 3 always
  visible without scrolling — 5 gives headroom above the required 3 while still fitting one screen
  on typical devices). Exponential half-life decay is a simple, well-understood formula (Simplicity
  First) that naturally lets a recently-opened coin overtake an older, more-frequently-used one
  without unbounded growth or manual tuning per coin.
- **Alternatives considered**: Pure "last used" ordering (rejected — clarification explicitly
  requires a frequency contribution, not sort-by-recency alone); pure frequency count (rejected —
  clarification explicitly requires recency weighting); a machine-learned or configurable-weight
  scheme (rejected — unjustified complexity for an MVP, violates Simplicity First).

## 8. Character limits (coin name, choice text)

- **Decision**: Coin name maximum 40 characters; choice text maximum 60 characters. Enforced both
  as a `maxLength` on the Compose `TextField` (blocks further input) and as a save-time validation
  in the ViewModel/domain layer (defense in depth, and required for any future non-UI entry path).
- **Rationale**: FR-018 requires "a reasonable maximum ... so names and choices always display
  clearly and unambiguously, including in the flip result view." 40/60 characters comfortably fit
  on a single line (or clean two-line wrap) across common Android screen widths in the large flip
  result typography, without truncation, while remaining generous enough for realistic entries
  (e.g., "Falafel wrap with extra tahini" fits in 60 chars).
- **Alternatives considered**: Very short limits (e.g., 15/20 chars) (rejected — would clip
  realistic entries like longer food names); no enforced limit beyond "blank check" (rejected —
  explicitly contradicts FR-018, which mandates a cap).

## 9. Testing strategy

- **Decision**:
  - Unit tests (JUnit4 + `kotlinx-coroutines-test` + Room in-memory database): repository CRUD,
    minimum-two-choices enforcement, blank-field validation, character-limit validation, random
    flip distribution (statistical bounds check), and Quick Access score/ranking computation.
  - Compose UI tests (`androidx.compose.ui.test.junit4`, instrumented): the four acceptance-scenario
    flows (flip a saved coin, create a coin, edit a coin, delete a coin with confirmation).
- **Rationale**: Principle VI requires core decision/data logic to be independently unit-tested
  before being "done"; Compose UI tests give end-to-end confidence for the acceptance scenarios
  without over-relying on manual QA, while staying inside standard AndroidX tooling (Principle VII
  — no third-party test frameworks like Robolectric or Turbine needed for this scope).
- **Alternatives considered**: Robolectric for host-side UI tests (rejected — adds a dependency for
  speed gains not needed at this app's size); snapshot/screenshot testing (rejected — no design
  system regression risk at MVP scope, deferred as a future enhancement).

## 10. Catppuccin Mocha implementation

- **Decision**: A single `Color.kt`/`Theme.kt` file defining the full Catppuccin Mocha palette as
  named `Color` constants, mapped once into a Compose `darkColorScheme` (the palette is a dark
  theme by definition) exposed via one app-wide `ChoiceTheme` wrapper applied at the `MainActivity`
  root. All screens consume theme colors via `MaterialTheme.colorScheme.*` — no screen defines its
  own literal color values.
- **Rationale**: Directly satisfies Principle V (single shared design/theming layer, no per-screen
  styling) and FR-017. Catppuccin Mocha is a fixed, published palette (base/text/surface/accent
  colors), so no external theming library is needed (Principle VII).
- **Alternatives considered**: Supporting a light/dark toggle with a second palette (rejected — out
  of scope; spec calls for "a modern, minimal Catppuccin Mocha visual design," not
  theme-switching; would add unjustified complexity).

## Resolved Technical Context

| Item | Resolution |
|---|---|
| Language/Version | Kotlin 2.0+, JVM target 17 |
| Primary Dependencies | Jetpack Compose (Material 3), Room, Navigation-Compose, Kotlin Coroutines/Flow, Lifecycle-ViewModel-Compose |
| Storage | Room (SQLite), on-device only |
| Testing | JUnit4, kotlinx-coroutines-test, Room in-memory DB tests, androidx.compose.ui.test (instrumented) |
| Target Platform | Android, minSdk 26, targetSdk/compileSdk 35 |
| Project Type | Mobile app (single Android application module) |
| Performance Goals | 60fps UI/animations; flip outcome computed and displayed in a single frame (<16ms compute, no perceptible delay before the flip animation completes); cold start to interactive main screen well under 1s on a mid-range device |
| Constraints | Fully offline, no network permissions requested; all core flows work in airplane mode; local storage only |
| Scale/Scope | Single local user; tens of coins realistically, no hard cap; unbounded choices per coin (UI scrolls); ~4-5 screens (Main, Coin Flip/Result, Create/Edit Coin, Delete confirmation dialog) |

All `NEEDS CLARIFICATION` markers are resolved; no open unknowns remain.
