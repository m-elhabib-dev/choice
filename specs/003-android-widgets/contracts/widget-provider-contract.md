# Contract: App Widget Provider Metadata

This is the interface the app exposes to Android itself (the launcher / widget picker / widget
host), declared once per widget type as an `AppWidgetProviderInfo` XML resource plus a matching
`<receiver>` entry in `AndroidManifest.xml`. This is the only "external" contract this feature has
in the traditional sense — the widget host (home-screen launcher) is the consumer.

## `res/xml/single_coin_widget_info.xml`

| Attribute | Value | Why |
|---|---|---|
| `android:minWidth` / `minHeight` | 1 cell (≈ 40–48dp) equivalent | Smallest useful size still shows coin name + result/ready text + flip control per FR-005-equivalent adaptivity for Single Coin Widget. |
| `android:targetCellWidth` / `targetCellHeight` | 2 x 1 | Reasonable default footprint at add-time. |
| `android:resizeMode` | `horizontal|vertical` | Spec requires adapting to resize (edge case: "resized smaller or larger ... layout must adapt"). |
| `android:widgetCategory` | `home_screen` | No keyguard/lockscreen widget support needed. |
| `android:updatePeriodMillis` | `0` | No periodic background refresh (research.md §3; spec Assumptions). |
| `android:configure` | `com.choice.app.widget.SingleCoinWidgetConfigActivity` | Configuration required before first display (FR-001). |
| `android:previewImage` | app icon or simple static preview drawable | Shown in the widget picker. |
| `android:description` | string resource describing the widget | Shown in the widget picker (accessibility + discoverability). |

## `res/xml/quick_coins_widget_info.xml`

| Attribute | Value | Why |
|---|---|---|
| `android:minWidth` / `minHeight` | ≈ 2 cells wide x 2 cells tall | Needs room for at least one coin row plus affordance that more exist. |
| `android:targetCellWidth` / `targetCellHeight` | 3 x 2 | Reasonable default footprint showing a few coins. |
| `android:resizeMode` | `horizontal|vertical` | Spec requires adapting the number of shown coins to available space (FR-005, edge case). |
| `android:widgetCategory` | `home_screen` | Same as above. |
| `android:updatePeriodMillis` | `0` | Same as above. |
| `android:configure` | `com.choice.app.widget.QuickCoinsWidgetConfigActivity` | Optional customization of the coin set (FR-002); still required as the entry point so "no explicit selection → favorites default" can be confirmed/skippable in one step. |
| `android:previewImage` | app icon or simple static preview drawable | Same as above. |
| `android:description` | string resource describing the widget | Same as above. |

## `AndroidManifest.xml` additions

```xml
<receiver
    android:name=".widget.SingleCoinWidgetReceiver"
    android:exported="false"
    android:label="@string/widget_single_coin_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/single_coin_widget_info" />
</receiver>

<receiver
    android:name=".widget.QuickCoinsWidgetReceiver"
    android:exported="false"
    android:label="@string/widget_quick_coins_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/quick_coins_widget_info" />
</receiver>

<activity
    android:name=".widget.SingleCoinWidgetConfigActivity"
    android:exported="true"
    android:theme="@style/Theme.Choice">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
    </intent-filter>
</activity>

<activity
    android:name=".widget.QuickCoinsWidgetConfigActivity"
    android:exported="true"
    android:theme="@style/Theme.Choice">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_CONFIGURE" />
    </intent-filter>
</activity>
```

`exported="true"` on the configuration Activities is required by the platform contract for
`APPWIDGET_CONFIGURE` handlers (the launcher, a different app/process, must be able to start them);
this is not a general-purpose deep link and does not expose app data — it only ever operates on the
`EXTRA_APPWIDGET_ID` the system hands it. The receivers themselves stay `exported="false"`; Glance's
manifest-merger guidance already expects the system to reach them via the intent-filter action
above without requiring `exported="true"` on modern targetSdk levels for this action.
