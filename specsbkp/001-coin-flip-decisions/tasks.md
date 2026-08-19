---

description: "Task list template for feature implementation"
---

# Tasks: Coin Flip Decisions

**Input**: Design documents from `/specs/001-coin-flip-decisions/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/repository-contract.md,
contracts/ui-state-contract.md, quickstart.md

**Tests**: Included — the plan (research.md §9) and quickstart.md explicitly call for JUnit4 unit
tests and Compose instrumented UI tests covering decision logic, validation, ranking, and the four
user-story flows.

**Organization**: Tasks are grouped by user story (spec.md, priorities P1–P4) to enable independent
implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Every task includes an exact file path

## Path Conventions

Single Android application module (plan.md "Structure Decision"):

```text
app/
├── src/main/kotlin/com/choice/app/
│   ├── ChoiceApplication.kt
│   ├── MainActivity.kt
│   ├── data/{local/, CoinRepository.kt, CoinRepositoryImpl.kt}
│   ├── domain/{Coin.kt, FlipCoin.kt, QuickAccessScore.kt}
│   └── ui/{theme/, main/, coinflip/, coinedit/, components/, ViewModelFactory.kt}
├── src/test/kotlin/com/choice/app/          # unit tests
└── src/androidTest/kotlin/com/choice/app/   # Compose instrumented UI tests
```

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Create the Android project skeleton (Gradle wrapper, `settings.gradle.kts`, `app/`
  module directory) per plan.md's Project Structure, at the repository root
- [ ] T002 Configure `app/build.gradle.kts`: Kotlin 2.0+ / JVM target 17, `minSdk` 26,
  `targetSdk`/`compileSdk` 35, Compose (Material 3) + `kotlinCompilerExtensionVersion`, Room (with
  KSP annotation processing), Navigation-Compose, Kotlin Coroutines/Flow, Lifecycle-ViewModel-Compose
  dependencies, JUnit4, `kotlinx-coroutines-test`, `androidx.compose.ui.test.junit4`
- [ ] T003 [P] Configure root `build.gradle.kts` plugin versions and `.gitignore` for the Android
  project
- [ ] T004 [P] Create `app/src/main/AndroidManifest.xml` declaring `ChoiceApplication` and
  `MainActivity` only, with no network/internet permission requested (Constraints, FR-016)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core persistence, domain logic, theming, and app wiring that every user story builds on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 [P] Define `CoinEntity` (Room) in `app/src/main/kotlin/com/choice/app/data/local/CoinEntity.kt`
  — `id`, `name`, `createdAt`, `interactionCount` (default 0), `lastInteractionAt` (nullable)
  (data-model.md)
- [ ] T006 [P] Define `ChoiceEntity` (Room) in `app/src/main/kotlin/com/choice/app/data/local/ChoiceEntity.kt`
  — `id`, `coinId` (FK → `CoinEntity.id`, `ON DELETE CASCADE`), `text`, `position` (data-model.md)
- [ ] T007 Define `CoinDao` in `app/src/main/kotlin/com/choice/app/data/local/CoinDao.kt` with
  `Flow`-returning queries for coins+choices and a transactional insert/update covering a coin and
  its full choice list (depends on T005, T006)
- [ ] T008 Define `ChoiceDatabase` (Room database, `CoinEntity` + `ChoiceEntity`, version 1) in
  `app/src/main/kotlin/com/choice/app/data/local/ChoiceDatabase.kt` (depends on T005, T006, T007)
- [ ] T009 [P] Define domain models `Coin`, `CoinWithChoices`, `CoinSummary`, `Choice` in
  `app/src/main/kotlin/com/choice/app/domain/Coin.kt` (data-model.md Entities)
- [ ] T010 [P] Implement the pure `flipCoin(choices, random = Random.Default)` function in
  `app/src/main/kotlin/com/choice/app/domain/FlipCoin.kt` per contracts/repository-contract.md
  (FR-011)
- [ ] T011 [P] Implement `QuickAccessScore` computation (exponential half-life decay,
  `HALF_LIFE_HOURS = 72`) in `app/src/main/kotlin/com/choice/app/domain/QuickAccessScore.kt` per
  research.md §7
- [ ] T012 [P] Define the `CoinRepository` interface in
  `app/src/main/kotlin/com/choice/app/data/CoinRepository.kt` exactly per
  contracts/repository-contract.md (`observeCoins`, `observeQuickAccessCoins`, `observeCoin`,
  `saveCoin`, `deleteCoin`, `recordInteraction`)
- [ ] T013 Implement `CoinRepositoryImpl` in
  `app/src/main/kotlin/com/choice/app/data/CoinRepositoryImpl.kt` — wires `CoinDao` +
  `QuickAccessScore`, and enforces `saveCoin`'s validation contract (blank/over-length name or
  choice text, fewer than 2 choices → `IllegalArgumentException`, per FR-002/FR-015/FR-018) (depends
  on T007, T009, T011, T012)
- [ ] T014 [P] Define Catppuccin Mocha color tokens in
  `app/src/main/kotlin/com/choice/app/ui/theme/Color.kt`
- [ ] T015 [P] Define `ChoiceTheme` (`darkColorScheme` built from T014) and `Type.kt` typography in
  `app/src/main/kotlin/com/choice/app/ui/theme/Theme.kt` and
  `app/src/main/kotlin/com/choice/app/ui/theme/Type.kt` (depends on T014)
- [ ] T016 Implement `AppContainer` (manual DI: builds `ChoiceDatabase` + `CoinRepositoryImpl`) owned
  by `ChoiceApplication` in `app/src/main/kotlin/com/choice/app/ChoiceApplication.kt` (depends on
  T008, T013)
- [ ] T017 Implement a shared `ViewModelProvider.Factory` in
  `app/src/main/kotlin/com/choice/app/ui/ViewModelFactory.kt` that constructs each screen's
  `ViewModel` with `AppContainer.coinRepository` (depends on T016)
- [ ] T018 Set up the Navigation-Compose `NavHost` (routes: Main, CoinFlip, CoinEdit) inside
  `ChoiceTheme` in `app/src/main/kotlin/com/choice/app/MainActivity.kt` (depends on T015, T017)

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 - Make a quick decision with a saved coin (Priority: P1) 🎯 MVP

**Goal**: Open a saved coin, flip it, see one clear decision; frequently-used coins surface in a
dedicated Quick Access section.

**Independent Test**: Seed one coin with 2+ choices directly via the repository, open the app,
select the coin, flip it, and verify a single choice is clearly displayed as the outcome.

### Tests for User Story 1 ⚠️

> Write these tests FIRST, ensure they FAIL before implementation

- [ ] T019 [P] [US1] Unit test: `flipCoin` distribution stays within statistical bounds across many
  iterations in `app/src/test/kotlin/com/choice/app/domain/FlipCoinTest.kt` (SC-003)
- [ ] T020 [P] [US1] Unit test: `QuickAccessScore` ranks known `interactionCount`/`lastInteractionAt`
  fixtures as expected in `app/src/test/kotlin/com/choice/app/domain/QuickAccessScoreTest.kt`
  (research.md §7)
- [ ] T021 [P] [US1] Unit test: `CoinRepositoryImpl.observeQuickAccessCoins` orders by score,
  respects `limit`, and excludes `interactionCount == 0` coins, using an in-memory Room database in
  `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplQuickAccessTest.kt`
- [ ] T022 [P] [US1] Compose UI test: open a seeded coin and flip it — verify exactly one choice is
  clearly displayed and no immediate low-friction re-flip control is offered in
  `app/src/androidTest/kotlin/com/choice/app/ui/coinflip/CoinFlipScreenTest.kt` (US1 Acceptance
  Scenarios 1–2, FR-013, SC-007)
- [ ] T023 [P] [US1] Compose UI test: with 3+ used coins seeded, the Quick Access section shows the
  top-ranked coins without scrolling in
  `app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenQuickAccessTest.kt` (US1 Acceptance
  Scenario 3, SC-004)

### Implementation for User Story 1

- [ ] T024 [US1] Implement `CoinFlipViewModel` in
  `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipViewModel.kt` — exposes
  `CoinFlipUiState`, `flip()` calls `flipCoin` and `repository.recordInteraction(coinId)`, `result`
  is always `null` on fresh entry (per Decision Outcome's transient, reset-on-restart nature)
  (depends on T024's dependencies T010, T012)
- [ ] T025 [US1] Implement `CoinFlipScreen` composable in
  `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt` — pre-flip state shows the flip
  control; post-flip state prominently displays the result with no "flip again" affordance
  (FR-012/FR-013, per contracts/ui-state-contract.md) (depends on T024)
- [ ] T026 [US1] Implement `MainViewModel` in
  `app/src/main/kotlin/com/choice/app/ui/main/MainViewModel.kt` — exposes `MainScreenUiState` from
  `observeQuickAccessCoins()` + `observeCoins()`, derives `isEmpty`, calls
  `repository.recordInteraction(coinId)` when a coin is opened (depends on T012)
- [ ] T027 [US1] Implement `MainScreen` composable in
  `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` — dedicated Quick Access section above
  the full coin list, first-run empty-state guidance, navigates to `CoinFlipScreen` on coin tap
  (FR-009, FR-014) (depends on T026, T018)

**Checkpoint**: User Story 1 is fully functional and independently testable

---

## Phase 4: User Story 2 - Create a new coin with choices (Priority: P2)

**Goal**: Create a coin with a name and 2+ choices, save it, and see it on the main screen.

**Independent Test**: From the main screen, start creating a coin, name it, add several choices,
save it, and verify it appears on the main screen and can be opened.

### Tests for User Story 2 ⚠️

- [ ] T028 [P] [US2] Unit test: `CoinRepositoryImpl.saveCoin` rejects a blank name, a blank choice,
  a name over 40 chars, a choice over 60 chars, and fewer than 2 choices in
  `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplSaveValidationTest.kt` (FR-002,
  FR-015, FR-018)
- [ ] T029 [P] [US2] Compose UI test: create a coin end-to-end (name + 2+ choices, save, appears on
  main screen), plus the blocked-save cases (fewer than 2 choices; blank name/choice) in
  `app/src/androidTest/kotlin/com/choice/app/ui/coinedit/CoinEditScreenCreateTest.kt` (US2
  Acceptance Scenarios 1–3)

### Implementation for User Story 2

- [ ] T030 [US2] Implement `CoinEditViewModel` create-mode logic in
  `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditViewModel.kt` — `CoinEditUiState`,
  add/edit/remove choice fields, field-level and form-level validation, `canSave` derivation,
  `save()` calls `repository.saveCoin(null, name, choices)` (depends on T012)
- [ ] T031 [US2] Implement `CoinEditScreen` composable in
  `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditScreen.kt` — name field capped at 40
  chars, dynamic choice fields capped at 60 chars each, "add choice" action, inline field/form error
  display, Save action gated by `canSave` (per contracts/ui-state-contract.md) (depends on T030)
- [ ] T032 [US2] Wire a "Create Coin" entry point from `MainScreen` (including the empty state) to
  `CoinEditScreen` in create mode, in `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` and
  the `NavHost` in `MainActivity.kt` (depends on T027, T031, T018)

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 5: User Story 3 - Edit an existing coin (Priority: P3)

**Goal**: Rename a coin and add/edit/remove its choices while keeping it usable, subject to the
2-choice minimum.

**Independent Test**: Open an existing saved coin, rename it, add a new choice, edit another
choice's text, remove a third choice, save, and verify all changes persist after reopening.

### Tests for User Story 3 ⚠️

- [ ] T033 [P] [US3] Unit test: `CoinRepositoryImpl.saveCoin` update path — rename, add a choice,
  edit a choice's text, remove a choice — persists correctly on reload in
  `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplUpdateTest.kt` (US3 Acceptance
  Scenarios 1–2, SC-005)
- [ ] T034 [P] [US3] Compose UI test: edit an existing coin's name and choices, and verify removing
  a choice from a 2-choice coin is blocked with an explanation in
  `app/src/androidTest/kotlin/com/choice/app/ui/coinedit/CoinEditScreenEditTest.kt` (US3 Acceptance
  Scenarios 1–3, FR-006)

### Implementation for User Story 3

- [ ] T035 [US3] Extend `CoinEditViewModel` to load an existing coin via
  `repository.observeCoin(coinId)` and populate `CoinEditUiState` in edit mode, in
  `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditViewModel.kt` (depends on T030)
- [ ] T036 [US3] Add the `canRemoveChoice` guard to `CoinEditViewModel`, blocking removal that would
  drop a coin below 2 choices and surfacing `formError` (FR-006), in
  `app/src/main/kotlin/com/choice/app/ui/coinedit/CoinEditViewModel.kt` (depends on T035)
- [ ] T037 [US3] Wire "Edit Coin" entry points from `MainScreen`'s coin item and from
  `CoinFlipScreen` to `CoinEditScreen` in edit mode, in
  `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt`,
  `app/src/main/kotlin/com/choice/app/ui/coinflip/CoinFlipScreen.kt`, and the `NavHost` in
  `MainActivity.kt` (depends on T027, T025, T036, T018)

**Checkpoint**: User Stories 1, 2, AND 3 all work independently

---

## Phase 6: User Story 4 - Delete a coin (Priority: P4)

**Goal**: Delete a coin the user no longer needs, with a required confirmation step.

**Independent Test**: From the main screen, delete an existing coin, confirm the deletion, and
verify it no longer appears anywhere in the app.

### Tests for User Story 4 ⚠️

- [ ] T038 [P] [US4] Unit test: `CoinRepositoryImpl.deleteCoin` removes the coin row and cascades
  the deletion to all its choices in
  `app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplDeleteTest.kt` (FR-007)
- [ ] T039 [P] [US4] Compose UI test: delete a coin via cancel-then-confirm, verifying the coin
  survives a cancel and is removed from the main screen after confirming, in
  `app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenDeleteTest.kt` (US4 Acceptance
  Scenarios 1–2)

### Implementation for User Story 4

- [ ] T040 [US4] Implement `DeleteCoinConfirmationDialog` composable driven by
  `DeleteCoinConfirmationState` in
  `app/src/main/kotlin/com/choice/app/ui/components/DeleteCoinConfirmationDialog.kt` per
  contracts/ui-state-contract.md (depends on T015)
- [ ] T041 [US4] Add delete handling to `MainViewModel` — tracks the pending
  `DeleteCoinConfirmationState` and a `deleteCoin(coinId)` action calling
  `repository.deleteCoin(coinId)` on confirm, in
  `app/src/main/kotlin/com/choice/app/ui/main/MainViewModel.kt` (depends on T026)
- [ ] T042 [US4] Wire a delete entry point from `MainScreen`'s coin item to
  `DeleteCoinConfirmationDialog`, confirming calls `MainViewModel.deleteCoin`, cancelling dismisses
  with no side effects, in `app/src/main/kotlin/com/choice/app/ui/main/MainScreen.kt` (depends on
  T040, T041)

**Checkpoint**: All four user stories are independently functional

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and consistency pass across all stories

- [ ] T043 [P] Run the full `./gradlew test` and `./gradlew connectedAndroidTest` suites and confirm
  every unit and instrumented test from Phases 3–6 passes (quickstart.md "Automated checks")
- [ ] T044 [P] Execute all 5 manual validation scenarios in quickstart.md with the device/emulator
  in airplane mode, confirming full offline operation (FR-016, SC-006)
- [ ] T045 [P] Audit `AndroidManifest.xml` and all network-capable dependencies to confirm no
  network/internet permission is declared and no code path performs a network call (Constraints)
- [ ] T046 Final Catppuccin Mocha visual-consistency pass across `MainScreen`, `CoinFlipScreen`,
  `CoinEditScreen`, and `DeleteCoinConfirmationDialog` — confirm every color reference resolves via
  `MaterialTheme.colorScheme.*` with no screen defining its own literal colors (FR-017)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends only on Foundational — no dependency on other stories
- **User Story 2 (Phase 4)**: Depends only on Foundational — independently testable via seeded
  data even though it shares `CoinEditViewModel`/`CoinEditScreen` files with US3
- **User Story 3 (Phase 5)**: Depends on Foundational; T035–T037 extend the create-mode files built
  in US2 (T030, T031) to add edit mode — implement after US2 for a clean diff, though the story
  itself is conceptually independent
- **User Story 4 (Phase 6)**: Depends on Foundational and on `MainViewModel`/`MainScreen` (T026,
  T027) from US1
- **Polish (Phase 7)**: Depends on all four user stories being complete

### Within Each User Story

- Tests MUST be written and FAIL before implementation
- Domain/data layer before ViewModels; ViewModels before Composables
- Story complete and checkpointed before moving to the next priority (if working sequentially)

### Parallel Opportunities

- All Setup tasks marked [P] (T003, T004) can run in parallel once T001/T002 land
- Foundational tasks marked [P] (T005, T006, T009, T010, T011, T012, T014) can run in parallel;
  T007/T008/T013/T015/T016/T017/T018 have sequential dependencies noted above
- All test tasks within a user story phase (marked [P]) can run in parallel with each other
- Once Foundational completes, US1 and US2 can be staffed and built in parallel (US3 and US4 have
  soft dependencies on US2's and US1's files, respectively, as noted above)

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test flipCoin distribution in app/src/test/kotlin/com/choice/app/domain/FlipCoinTest.kt"
Task: "Unit test QuickAccessScore ranking in app/src/test/kotlin/com/choice/app/domain/QuickAccessScoreTest.kt"
Task: "Unit test observeQuickAccessCoins ordering in app/src/test/kotlin/com/choice/app/data/CoinRepositoryImplQuickAccessTest.kt"
Task: "Compose UI test flip a saved coin in app/src/androidTest/kotlin/com/choice/app/ui/coinflip/CoinFlipScreenTest.kt"
Task: "Compose UI test Quick Access section in app/src/androidTest/kotlin/com/choice/app/ui/main/MainScreenQuickAccessTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Seed a coin via the repository, run T022/T023, confirm the flip flow and
   Quick Access section work end-to-end
5. Demo the MVP: flip a pre-seeded "Breakfast" coin and see the result

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Validate independently → Demo (MVP!)
3. Add User Story 2 → Validate independently → Demo (now coins can be created through the UI)
4. Add User Story 3 → Validate independently → Demo (coins can be kept up to date)
5. Add User Story 4 → Validate independently → Demo (full CRUD, feature-complete MVP)
6. Phase 7: Polish — full regression pass in airplane mode, visual consistency audit

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (flip + Quick Access)
   - Developer B: User Story 2 (create coin) — produces `CoinEditViewModel`/`CoinEditScreen` that
     Developer C will extend
   - Developer C: User Story 3 (edit coin), starting once US2's `CoinEditViewModel`/`CoinEditScreen`
     exist
   - Developer D: User Story 4 (delete coin), starting once US1's `MainViewModel`/`MainScreen` exist
3. Stories complete and integrate independently; Phase 7 polish runs once all four are done

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate a story independently
- US3 and US4 share files with US2 and US1 respectively (`CoinEditViewModel`/`CoinEditScreen`,
  `MainViewModel`/`MainScreen`) — this is an intentional, minimal cross-story file dependency
  rather than duplicated screens; each story remains independently testable per its own
  Independent Test criteria
