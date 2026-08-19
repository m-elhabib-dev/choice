# Choice

A simple Android app that helps you make decisions when you can't choose between options. Create reusable "coins" representing decision contexts with multiple choices, then flip to let random selection decide.

## Features

- **Create Coins** - Build reusable decision contexts (e.g., "Breakfast") with 2+ choices
- **Flip to Decide** - Randomly select one choice with an unbiased process
- **Quick Access** - Frequently used coins appear at the top based on recency and frequency
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
├── data/           # Repository and Room database
├── domain/         # Business logic (FlipCoin, QuickAccessScore)
└── ui/             # Compose screens and ViewModels
    ├── coinedit/   # Create/edit coin screen
    ├── coinflip/   # Coin flip screen
    ├── main/       # Main screen with Quick Access
    └── theme/      # Catppuccin Mocha theme
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```
