# Controller Bridge maintainer notes

## Project and scope

Native Java Android app. Adds a virtual Sony DualSense identity; the real controller supplies gameplay inputs. It does not rename Bluetooth devices, forward input, or implement Sony haptics. Detection is capability-based, not an 8BitDo allowlist. Compatibility remains device/game-specific.

Keep README limited to installation, setup, play/test, recovery, essential security information and tested hardware. Put developer context here; detailed investigation history stays under docs/.

## Build and tests

Use Python 3, JDK 17+ (tested with 21), Android SDK platform 36 and build-tools 36.0.0. Set JAVA_HOME and ANDROID_HOME, then:

```sh
python3 test.py
python3 build.py
python3 package_source.py
```

Build output: build/ControllerBridge.apk. Dependency URLs and SHA-256 values are pinned in dependencies.json. Keep the release signing key private and reuse it for updates. CI creates a temporary key and produces test artifacts, not the stable release APK.

Separate device tests are in tests/device/. Build with `python3 tests/device/build.py`, install the test APK, then run `adb shell am instrument -w app.controllerbridge.tests/.ActivationTest`. Running instrumentation ends an active controller session. The reset verification mode requires a real UI reset first and the previous virtual-device descriptor:

```sh
adb shell am instrument -w -e verify_reset true -e descriptor EXACT_DESCRIPTOR app.controllerbridge.tests/.ActivationTest
python3 test_device.py on --descriptor EXACT_DESCRIPTOR --controller 8BitDo --no-physical-sony
python3 test_device.py off --descriptor EXACT_DESCRIPTOR --controller 8BitDo --no-physical-sony
```

Do not call a test passed without reading its result. Verify controller removal before starting another instrumentation session so the test itself cannot mask a reset failure.

## Activation and privacy

Initial activation needs trusted Wi-Fi and temporary Android Wireless debugging. A persisted single-use nonce expires after 60 seconds. Android instrumentation delegates only INJECT_EVENTS and ASSOCIATE_INPUT_DEVICE_TO_DISPLAY to the app. The temporary launcher disables Wireless debugging after readiness. Users must verify both debugging flags OFF and restore Samsung Auto Blocker; its state cannot be reliably detected by this app.

These permissions remain powerful. A compromised build could inject other input; this is not identical to stock security. No analytics or remote service. No root, bootloader unlock, security bypass, automatic debugging re-enable or permanent privileged helper. The private service is unexported and the fixed controller interface is process-local with an exact UID check.

Pairing keys live in app-private no-backup storage. Version 0.5.1 accepts pairing codes only through the notification's RemoteInput. Preserve leading zeroes and reject malformed/non-ASCII codes and plain Intent extras. Keep Android's pairing dialog open while using the notification. The user reported in-app entry unreliable; it has been removed, including its screenshot/instructions. Do not reintroduce it without a new request.

The toggle preserves activation. Stop phone access ends input access while keeping pairing. Reset setup confirms, then calls ActivityManager.clearApplicationUserData(): Android stops the app, removes its virtual controller and delegated access, erases key/certificate/settings/nonces, and revokes runtime permissions. Android may retain the old public-key pairing entry; the app no longer has its private key. Reset does not change system debugging or Auto Blocker switches. Show reset when saved pairing exists, even if active access has ended.

After reboot, force-stop, update or app termination, Wi-Fi activation is needed again. Offline activation without a nearby Wi-Fi network is not offered. Optional manual connection uses the port on the main Wireless debugging screen, not the pairing port. CoD must be fully closed and reopened after virtual controller creation.

## Distribution and screenshots

Private repository: https://github.com/Koulb/controller-bridge. Publish APK, matching source archive and SHA256SUMS together. Source packaging uses an explicit allowlist. Include AGENTS.md and redacted docs/images/; exclude signing keys, raw evidence, SDK/vendor/build caches, phone identifiers and pairing secrets. Verify archive contents and release asset hashes.

README images originate from real phone screenshots. Blur sensitive status-bar details before publishing. Keep raw captures in ignored evidence/ only. Never include live pairing codes, network addresses, personal device names or account details in documentation images. Inspect edited images for privacy and unchanged instructions.

## Evidence and limits

Test phone: Samsung Galaxy S26 Ultra SM-S948U1, Android 17/API 37, security patch 2026-10-05. Physical gamepad: 8BitDo Ultimate MG. Physical DualSense off.

User confirmed fresh 0.4 download/install/setup and CoD gameplay. Version 0.5 changes UI, pairing entry and reset; backend controller creation remains unchanged. Fresh 0.5 in-app pairing in Samsung split screen, reset deletion/revocation, independent Sony registry checks and built-in self-test passed. User subsequently reported in-app entry failed, so version 0.5.1 removes it and uses notification-only pairing. Fresh 0.5.1 notification pairing, built-in self-test and independent controller registry checks passed, including Wi-Fi-off operation. Fresh release download/gameplay remains a user acceptance test. Do not conflate earlier gameplay confirmation with a new run.

- Current checks: [0.5.1 validation](docs/VALIDATION-0.5.1.md)
- Prior in-app pairing experiment: [0.5 validation](docs/VALIDATION-0.5.md) (user reported in-app entry failed; removed in 0.5.1)
- Backend/build detail: [technical notes](docs/TECHNICAL.md)
- Earlier evidence: [0.4](docs/VALIDATION-0.4.md), [0.3](docs/VALIDATION-0.3.md)
- Prior platform investigation: [offline activation](docs/OFFLINE-ACTIVATION.md)

## License

App: GPL-3.0-or-later. Preserve LICENSE, THIRD_PARTY_NOTICES.txt, licenses/ and third-party-src/ in source releases. In-app license access remains available. Not affiliated with Sony, Samsung, 8BitDo or Activision.
