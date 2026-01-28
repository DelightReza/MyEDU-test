#!/bin/bash

# Build Verification Script for MyEDU iOS Support

echo "========================================="
echo "MyEDU iOS Support Verification"
echo "========================================="
echo ""

# Check if we're on macOS (required for iOS builds)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "✓ Running on macOS"
    
    # Check for Xcode
    if command -v xcodebuild &> /dev/null; then
        echo "✓ Xcode is installed"
        xcodebuild -version
    else
        echo "✗ Xcode is not installed"
        echo "  Please install Xcode from the Mac App Store"
    fi
else
    echo "⚠ Not running on macOS"
    echo "  iOS builds require macOS with Xcode installed"
    echo "  Android builds can still be performed on this platform"
fi

echo ""
echo "Checking project structure..."

# Check for required directories
directories=(
    "shared"
    "shared/src/commonMain"
    "shared/src/androidMain"
    "shared/src/iosMain"
    "iosApp"
    "iosApp/iosApp"
    "app"
)

for dir in "${directories[@]}"; do
    if [ -d "$dir" ]; then
        echo "✓ $dir exists"
    else
        echo "✗ $dir is missing"
    fi
done

echo ""
echo "Checking configuration files..."

# Check for required files
files=(
    "build.gradle.kts"
    "settings.gradle.kts"
    "gradle.properties"
    "shared/build.gradle.kts"
    "iosApp/iosApp.xcodeproj/project.pbxproj"
    "iosApp/iosApp/iOSApp.swift"
    "iosApp/iosApp/ContentView.swift"
    "README.md"
    "IOS_BUILD_GUIDE.md"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file is missing"
    fi
done

echo ""
echo "========================================="
echo "Verification Complete"
echo "========================================="
echo ""
echo "Next steps:"
echo "1. For Android builds: ./gradlew :app:build"
echo "2. For iOS builds (macOS only):"
echo "   a. ./gradlew :shared:build"
echo "   b. open iosApp/iosApp.xcodeproj"
echo "   c. Build and run in Xcode"
echo ""
echo "See IOS_BUILD_GUIDE.md for detailed iOS build instructions."
