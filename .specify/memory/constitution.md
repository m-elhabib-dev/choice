<!--
Sync Impact Report
==================
Version change: [TEMPLATE — unratified] → 1.0.0
Rationale: Initial ratification of the Choice project constitution. Bumped to MAJOR (1.0.0)
because this establishes the first concrete governance baseline (template placeholders had no
prior semantic version).

Principles established (template had 5 generic placeholder slots; this project defines 8):
  I.    Simplicity First (new)
  II.   Decision First (new)
  III.  Local First (new)
  IV.   Modern Android (new)
  V.    Consistent Design (new)
  VI.   Testable Behavior (new)
  VII.  Minimal Dependencies (new)
  VIII. Incremental Development (new)

Sections:
  - Added: Technology Constraints (fills template SECTION_2 slot)
  - Added: Development Workflow (fills template SECTION_3 slot)
  - Added: Governance (amendment procedure, versioning policy, compliance review)

Removed: none (first ratified version)

Deferred / TODO items:
  - None. RATIFICATION_DATE set to the date this constitution was authored, since no prior
    ratification date exists.

Templates requiring follow-up review (not modified by this command; verify at next use):
  - .specify/templates/plan-template.md — confirm Constitution Check gates reference the 8
    principles above (esp. Local First, Minimal Dependencies, Testable Behavior).
  - .specify/templates/spec-template.md — no direct dependency on principle count; no changes
    anticipated.
  - .specify/templates/tasks-template.md — confirm task categorization still supports
    Incremental Development (MVP-first slicing) and Testable Behavior (test tasks required).
  - .claude/skills/ (speckit-plan, speckit-tasks, speckit-analyze) — no automated placeholder
    checks found; manual review recommended next time those commands run against this project.
-->

# Choice Constitution

## Core Principles

### I. Simplicity First
The application MUST minimize cognitive load. Every screen and interaction MUST have a single,
clear purpose that a user can state in one sentence. Features that add UI complexity without a
direct benefit to making or acting on a decision MUST be rejected or deferred. When two designs
achieve the same outcome, the simpler one MUST be chosen.
**Rationale**: Choice is a decision-making tool, not a general-purpose app; unnecessary complexity
directly undermines its reason to exist.

### II. Decision First
Choice exists to help users make decisions and stop reconsidering them. The UX MUST NOT encourage
repeated indecision (e.g., unlimited free re-rolls framed as "try again", ambiguous outcomes, or
flows that let a user avoid committing to a result). Once a decision mechanism produces an outcome,
the product MUST treat it as final within that flow; revisiting a decision MUST be an explicit,
deliberate user action, not the path of least resistance.
**Rationale**: The core value proposition is closure, not entertainment through endless choice
generation.

### III. Local First
Core functionality MUST work completely offline. User-created coins and choices MUST be stored
locally on the device, and no core flow (creating a coin, making a choice, viewing history) may
require network connectivity. Any future networked feature (e.g., backup or sync) MUST be
additive and optional, never a prerequisite for core use.
**Rationale**: A decision tool must be available at the moment a decision is needed, regardless of
connectivity, and user data about personal decisions should not require a server by default.

### IV. Modern Android
The application MUST follow current, officially supported Android development practices and
platform conventions (e.g., current Android SDK/tooling baselines, platform-recommended
architecture guidance, and system-level UX conventions such as gestures, back behavior, and
adaptive layouts). Deprecated APIs and patterns MUST NOT be adopted for new work.
**Rationale**: Staying aligned with platform conventions keeps the app maintainable, secure, and
familiar to users without reinventing solved problems.

### V. Consistent Design
Catppuccin Mocha MUST be the application's visual foundation. UI components, typography, spacing,
colors, and interaction patterns MUST remain visually consistent across all screens; deviations
require a documented reason. A shared design/theming layer MUST be used rather than one-off styling
per screen.
**Rationale**: Visual consistency reduces cognitive load (supporting Principle I) and reinforces
that this is a single, coherent product rather than a collection of disconnected screens.

### VI. Testable Behavior
Core decision logic and data operations MUST be independently testable, separate from UI code.
Business logic (e.g., how a decision outcome is computed, how coins and choices are persisted and
retrieved) MUST be covered by automated unit tests before it is considered done. UI/interaction
code SHOULD be structured to allow this separation.
**Rationale**: Decision correctness and data integrity are the app's core trust guarantees; they
must be verifiable independent of manual UI testing.

### VII. Minimal Dependencies
Android and Jetpack libraries MUST be preferred for any given need. A third-party dependency MAY
be added only when it provides clear value that Android/Jetpack libraries cannot reasonably
provide, and its inclusion MUST be justified (what problem it solves, why the standard library
option is insufficient). Dependencies MUST NOT compromise the Local First principle.
**Rationale**: Fewer dependencies reduce maintenance burden, security surface, and app size, and
keep the codebase aligned with long-term platform support.

### VIII. Incremental Development
The smallest useful version of any feature MUST be built first. Features not required for the core
decision workflow (creating a coin, making a choice, seeing the result) MUST remain outside the
MVP and MUST be tracked separately rather than bundled into initial delivery.
**Rationale**: Shipping a minimal, working core faster validates the product's central value
before investing in secondary features.

## Technology Constraints

- **Platform**: Native Android application only; no cross-platform framework unless a future
  amendment explicitly changes this constraint.
- **Language**: Kotlin, per current official Android guidance (Principle IV).
- **Persistence**: On-device storage only for core data (coins, choices, decision history); no
  network calls are permitted on the core decision path (Principle III).
- **Dependencies**: AndroidX/Jetpack libraries are the default choice for architecture, persistence,
  and UI needs; any third-party library addition MUST be justified in the relevant spec or PR
  description (Principle VII).
- **Design system**: A shared Catppuccin Mocha theme/token layer MUST be the single source of
  colors, typography, and spacing used across all screens (Principle V).

## Development Workflow

- **Test-backed core logic**: Any change to decision logic or data operations MUST include or
  update unit tests before being considered complete (Principle VI).
- **MVP-first scoping**: New feature work MUST be broken down so the smallest useful slice ships
  first; non-essential enhancements MUST be explicitly deferred, not silently included
  (Principle VIII).
- **Design review**: UI changes MUST be checked against the Catppuccin Mocha design system and
  existing component patterns before merging (Principle V).
- **Principle check**: Specs and plans MUST be checked against these principles (see
  `.specify/templates/plan-template.md` Constitution Check) before implementation begins.

## Governance

This constitution supersedes other informal practices for the Choice project. All feature specs,
plans, and reviews MUST verify compliance with these principles; any deviation MUST be explicitly
justified in the relevant spec/plan's complexity-tracking section or rejected.

**Amendment procedure**: Amendments are made by editing this file, recording the change in a Sync
Impact Report at the top of the file, and updating the version per the policy below. Amendments
should note which principle(s) or section(s) changed and why.

**Versioning policy** (semantic versioning for this document):
- **MAJOR**: Backward-incompatible governance changes — removing or redefining a principle in a
  way that reverses its prior meaning.
- **MINOR**: Adding a new principle or section, or materially expanding existing guidance.
- **PATCH**: Wording clarifications, typo fixes, or non-semantic refinements.

**Compliance review**: Every `/speckit-plan` run MUST re-check its Constitution Check section
against the current version of this file. Any unresolved violation MUST be documented and
justified before implementation proceeds.

**Version**: 1.0.0 | **Ratified**: 2026-08-18 | **Last Amended**: 2026-08-18
