package com.choice.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** System entry point for the Quick Coins Widget, per contracts/widget-provider-contract.md. */
class QuickCoinsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickCoinsWidget()
}
