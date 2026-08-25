package com.choice.app.locale

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented — needs a real Context, unlike LocalePreferenceTest's pure functions.
 *
 * Run on both an API 33+ device (LocaleManager path) and an API 26–32 device (backport path) per
 * quickstart.md's prerequisites — contracts/localization-contract.md §4, LA-1.
 *
 * Uses a stable platform resource (android.R.string.cancel), which ships an Arabic translation in
 * every AOSP build, so this test does not depend on Choice's own values-ar/ content being complete
 * yet — it proves resource *resolution* switches, independent of translation coverage.
 */
@RunWith(AndroidJUnit4::class)
class LocaleApplierTest {

    @Test
    fun localizedContext_resolvesArabicResources() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        LocaleApplier.apply(context, SupportedLanguages.ARABIC)
        val arabicContext = LocaleApplier.localizedContext(context)
        val arabicCancel = arabicContext.getString(android.R.string.cancel)

        LocaleApplier.apply(context, SupportedLanguages.ENGLISH)
        val englishContext = LocaleApplier.localizedContext(context)
        val englishCancel = englishContext.getString(android.R.string.cancel)

        assertEquals(
            "expected Arabic and English resolutions of a platform string to differ",
            false,
            arabicCancel == englishCancel,
        )
    }

    @Test
    fun localizedContext_isIdempotent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LocaleApplier.apply(context, SupportedLanguages.ARABIC)

        val wrappedOnce = LocaleApplier.localizedContext(context)
        val wrappedTwice = LocaleApplier.localizedContext(wrappedOnce)

        assertEquals(
            wrappedOnce.getString(android.R.string.cancel),
            wrappedTwice.getString(android.R.string.cancel),
        )
        assertEquals(
            wrappedOnce.resources.configuration.locales[0],
            wrappedTwice.resources.configuration.locales[0],
        )
    }
}
