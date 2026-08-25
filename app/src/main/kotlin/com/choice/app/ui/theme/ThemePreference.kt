package com.choice.app.ui.theme

/**
 * Pure theme-resolution logic. No Android dependency, so this is unit-tested on the JVM
 * (app/src/test) rather than requiring a device — contracts/theme-contract.md §2.
 *
 * Behaviour table (contracts/theme-contract.md §2, data-model.md §5):
 * - A [storedId] matching one of the four flavor ids resolves to that flavor (FR-001).
 * - `null` (never set) resolves to [ThemeFlavor.DEFAULT] (FR-005).
 * - Blank or unrecognized values resolve to [ThemeFlavor.DEFAULT] (FR-006).
 * - Matching is exact, not case-folded — `id` is an internal, never-user-typed constant, unlike a
 *   BCP-47 language tag, so a differently-cased value is corrupt/foreign data, not a
 *   legitimate-but-differently-cased value (data-model.md §5, row 6 rationale).
 */
fun resolveThemeFlavor(storedId: String?): ThemeFlavor =
    storedId?.let { ThemeFlavor.fromId(it) } ?: ThemeFlavor.DEFAULT
