# Bluetooth Deck — Android prototype v0.1

An Android 9+ source project for a Bluetooth keyboard, touchpad/mouse, and generic HID gamepad. No PC receiver is required for the direct HID mode. A phone with Bluetooth HID Device profile support is required; Android version alone does not guarantee OEM support.

## Status

Source prototype, not a tested or installable release. HID descriptor input/output lengths and manifest XML passed local Python checks. Java serialization tests are supplied but could not be run because javac is unavailable. Android compilation, pairing, multi-touch behavior, game compatibility, and actual haptics have not been verified on hardware. No APK was built in the authoring environment because Android SDK, Gradle, and javac were unavailable.

## Included

- Explicit Nearby devices permissions on Android 12+; system pairing/discoverability UI.
- Pick a paired PC, connect, disconnect, and show HID state.
- US-layout letter/number keyboard, Shift/Ctrl toggles, navigation and special keys.
- Relative mouse movement, left/right clicks, and two-finger scrolling.
- Two sticks with dead zone, 16-button report, D-pad buttons, and binary L2/R2 trigger controls.
- Local touch haptics with strength control on amplitude-capable phones; basic vibration fallback.
- Neutral reports when changing mode, backgrounding, connecting, and disconnecting.
- No network permissions, advertisements, accounts, or telemetry.

Keep the app visible and the phone awake. Android can unregister a HID app when it leaves the foreground. This initial version does not claim background operation.

## Build an APK

1. Install current Android Studio, Android SDK platform 35, and JDK 17.
2. Open this folder as a Gradle project. Use Gradle 8.9 with Android Gradle Plugin 8.7.3. A Gradle wrapper is not included.
3. Let Android Studio sync dependencies, then use Build → Build App Bundle(s) / APK(s) → Build APK(s).
4. Install `app/build/outputs/apk/debug/app-debug.apk` on your phone.

Alternatively install Gradle 8.9 and run `gradle :app:assembleDebug`. The supplied GitHub Actions workflow also builds a debug APK after you upload the project to your own repository. That workflow has not been run here. Debug APKs are for development, not Play Store publishing.

## Pair and test

1. Open the phone app, grant Nearby devices permission, and enable Bluetooth.
2. Tap Setup, wait for Ready, and tap Pair PC. On Windows, open Settings → Bluetooth & devices → Add device. Select the phone and confirm matching pairing codes.
3. Return to the app, tap Connect, and select your PC. Keep the app visible.
4. Test the keyboard in an empty text editor and mouse on the desktop.
5. On Windows, run `joy.cpl`. Inspect buttons and axes before testing a game. Map D-pad button usages manually where required.
6. Test simultaneous sticks and buttons, releases, interruptions, reconnects, and haptic intensity. If a connection fails, remove the pairing at both ends and re-pair with this app registered.

The generic gamepad may work with HID/DirectInput-aware games, Linux, or configurable input software. Some games require XInput. No claim of universal compatibility or console support is made.

## Xbox / PlayStation compatibility plan

Changing the Bluetooth name or product ID does not implement an Xbox or DualSense protocol.

**Not implemented:** Xbox emulation, PS5/DualSense emulation, game-generated rumble, adaptive trigger force, PC companion, signed virtual-controller driver, and console authentication.

For the next stage, target Windows 10/11 first:

- An authenticated paired Bluetooth RFCOMM channel carries sequenced full controller-state packets to a Windows companion. Keep it a separate transport from direct HID mode.
- Companion forwards input to a maintained, signed virtual Xbox controller backend. Verify licensing, support status, redistribution terms, and driver installation before selecting a backend; do not silently install drivers.
- Use a bounded report rate and latest-state queue; reset to neutral on a short timeout or disconnect to prevent stuck input.
- Companion receives rumble from supported PC games and sends intensity/duration commands back to the phone, with duration bounds and an off switch.
- Phone renders that rumble using its available vibrator hardware. It cannot produce physical adaptive trigger resistance or guarantee two independent actuator channels.
- Treat DualSense emulation as a separate compatibility project requiring protocol implementation and game-specific hardware testing. Do not advertise PS5 console support.

## Local protocol checks

```sh
mkdir -p /tmp/bluetoothdeck-tests
javac -d /tmp/bluetoothdeck-tests app/src/main/java/com/bluetoothdeck/app/Reports.java tests/ReportsTest.java
java -cp /tmp/bluetoothdeck-tests ReportsTest
```

Technical references:
- https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice
- https://developer.android.com/develop/ui/views/haptics/haptics-apis
- https://learn.microsoft.com/en-us/windows/win32/xinput/xinput-and-directinput
- https://www.playstation.com/en-in/accessories/dualsense-wireless-controller/
