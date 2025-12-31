# Better Window
A better Compose Window, that converts the title bar into insets and allowing you to draw behind it.

## Acknowledgements
A lot of the code is directly taken from [Animeko](https://github.com/open-ani/animeko/tree/main/app/shared/ui-foundation/src/desktopMain/kotlin/platform)
I have just modified it to make it usable for other projects. Feel free to check out their project, it's really cool!

## Setup
Add the dependency to your `build.gradle.kts` file
```kotlin
dependencies {
    implementation("dev.brahmkshatriya:betterwindow:1.0.0")
}
```

## Usage
You can directly replace your `Window` with `BetterWindow` and it should work out of the box.

But if you want to use some of the other features, check out the [Sample App](./app/src/jvmMain/kotlin/dev/brahmkshatriya/betterwindow)