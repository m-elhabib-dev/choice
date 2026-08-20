# Choice

A simple Android app that helps you make decisions when you can't choose between options. Create reusable "coins" representing decision contexts with multiple choices, then flip to let random selection decide.

## Features

- **Create Coins** - Build reusable decision contexts (e.g., "Breakfast") with 2+ choices
- **Flip to Decide** - Randomly select one choice with an unbiased process
- **Accept or Override** - Primary "Accept" action commits the decision; a secondary "Flip again" lets you override
- **Quick Coins** - Favorite coins for one-tap access, sorted by most recently used
- **Reorder Choices** - Drag and drop to rearrange choice order within a coin
- **Weighted Selection** - Assign per-choice weights so some options are more likely than others
- **Avoid Last Result** - Exclude the previous result from the next flip for variety
- **Decision History** - Browse a timestamped log of every accepted decision per coin
- **Statistics** - View total decisions, most/least selected choices, and a per-choice breakdown
- **Search** - Find coins instantly by name from the home screen
- **Templates** - Start from bundled templates (Breakfast, Lunch, Workout, Movie) or begin blank
- **Share & Import** - Export a coin as a JSON payload; paste it into another device to import
- **Full Offline** - Works completely offline with local Room database persistence
- **Modern UI** - Clean, minimal design using Catppuccin Mocha color palette

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose with Material3
- **Database**: Room (SQLite)
- **Architecture**: MVVM with Repository pattern
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)

## Project Structure

```
app/src/main/kotlin/com/choice/app/
├── data/               # Repository and Room database
├── domain/             # Business logic (FlipCoin, WeightedSelection, Statistics, Search, Templates, SharePayload)
└── ui/                 # Compose screens and ViewModels
    ├── coinflip/       # Coin flip screen
    ├── coinedit/       # Create/edit coin screen
    ├── coinsettings/   # Weighted & avoid-last-result settings
    ├── history/        # Decision history screen
    ├── statistics/     # Per-coin statistics screen
    ├── templates/      # Template picker screen
    ├── share/          # Import coin screen
    ├── main/           # Main screen with Quick Coins and search
    ├── components/     # Shared composables (EmptyState, etc.)
    └── theme/          # Catppuccin Mocha theme
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```
