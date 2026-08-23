package com.choice.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** System entry point for the Single Coin Widget, per contracts/widget-provider-contract.md. */
class SingleCoinWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleCoinWidget()
}
