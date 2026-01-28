# MyEDU

A cross-platform educational application built with Kotlin Multiplatform Mobile (KMM).

## Platforms

- **Android**: Native Android app using Jetpack Compose
- **iOS**: Native iOS app using SwiftUI

## Building for Android

1. Open the project in Android Studio
2. Build and run the `app` module
3. Or use command line:
```bash
./gradlew :app:assembleDebug
```

## Building for iOS (iPhone)

### Prerequisites

- macOS with Xcode 14.0 or later
- CocoaPods (optional, for dependency management)
- Kotlin Multiplatform Mobile plugin for Android Studio

### Steps

1. **Build the shared framework:**
```bash
./gradlew :shared:build
```

2. **Open the iOS project in Xcode:**
```bash
open iosApp/iosApp.xcodeproj
```

3. **Select your target device:**
   - Choose an iPhone simulator or connected device
   - Select a development team for code signing

4. **Build and run:**
   - Click the Run button (▶️) in Xcode
   - Or use command line: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp`

### iOS Build Configuration

The iOS app is configured with:
- Minimum deployment target: iOS 14.0
- Bundle identifier: `myedu.oshsu.kg.iosApp`
- Version: 2.0

### Architecture

The project uses Kotlin Multiplatform to share business logic between Android and iOS:

- **shared**: Common Kotlin code shared between platforms
  - `commonMain`: Platform-independent code
  - `androidMain`: Android-specific implementations
  - `iosMain`: iOS-specific implementations
- **app**: Android application module
- **iosApp**: iOS application (SwiftUI)

### Platform-Specific Features

Some features require platform-specific implementations:
- Network monitoring
- Push notifications
- File storage
- Background tasks

These are implemented using the expect/actual pattern in Kotlin Multiplatform.

## Development

### Shared Module

The shared module contains business logic that is common to both platforms. When adding new features:

1. Add common code to `shared/src/commonMain`
2. Add platform-specific implementations to `shared/src/androidMain` and `shared/src/iosMain`
3. Use the `expect`/`actual` pattern for platform-specific APIs

### Testing

Run tests for the shared module:
```bash
./gradlew :shared:test
```

## License

This project is licensed under the terms specified in the repository.