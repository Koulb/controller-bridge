<!-- Backend notes retained from 0.3. For current installation instructions, see ../README.md. -->
# Controller Bridge technical notes

Android app that detects connected gamepads and provides a virtual DualSense on/off button. The physical controller supplies gameplay input. It does not rename the controller in Bluetooth settings, forward inputs, modify games, or implement Sony haptics.

**Development status: experimental 0.3.0.** Android 17's input API replaces the shell helper that failed Auto Blocker testing in 0.2. The integrated app passed on/off self-test and user-confirmed CoD gameplay with Auto Blocker ON, Wi-Fi OFF, and debugging disconnected. CoD required a fresh launch after enabling the virtual controller. See [0.3 validation](VALIDATION-0.3.md).

**Initial activation still requires a nearby Wi-Fi network. Fresh phone-only setup without nearby Wi-Fi remains unresolved.** This is not yet the full requested solution. Installing an APK cannot grant the required input permissions. See the [offline activation investigation](OFFLINE-ACTIVATION.md) for verified platform gates. No root, bootloader unlock, vendor exploit, SELinux change, Auto Blocker bypass, or automatic debugging re-enable is included.

## Experimental setup

1. Build and install the APK. This backend requires Android 17 or later and compatible firmware; only the connected Samsung S26 Ultra has been tested.
2. Connect a Bluetooth or USB gamepad. The app shows Android's controller name. A real DualSense or DualSense Edge displays **DualSense connected**.
3. Temporarily disable Auto Blocker if it prevents setup, enable Developer options, connect to Wi-Fi, and enable Wireless debugging.
4. Choose **Set up phone access → Pair with this phone**. In Android, open **Pair device with pairing code** and enter its six digits through Controller Bridge's notification while that dialog remains open. If already paired, choose **Activate paired phone**.
5. Activation restarts Controller Bridge. The temporary launcher switches Wireless debugging OFF after the restarted app confirms input access. Verify both debugging modes are OFF and restore Auto Blocker. Wi-Fi may then be switched off.
6. Tap **Turn on DualSense**. Run **Run on/off self-test** to check registration/removal; it restores the previous on/off state. For CoD Mobile, keep virtual DualSense ON, fully close CoD from Recent apps, then reopen it. Returning to an already running game may show a connected controller while ignoring inputs.
7. The toggle turns the virtual controller off while preserving activation. Disconnecting all physical gamepads also stops the virtual controller. **Stop phone access** revokes access and closes the app; reactivation is then required.

Reboot, force-stop, an app update, or Android terminating the app ends access. Activation then needs Wi-Fi again. If discovery fails, the optional port field uses the connection port on Android's main Wireless debugging screen, not the pairing port. The app cannot reliably read Samsung's Auto Blocker state, so confirm it manually.

## Compatibility

The additional device is named `DualSense Wireless Controller`, with Sony IDs `054c:0ce6`, gamepad buttons and joystick axes. Android's registry check confirms the Sony key layout. CoD gameplay with 8BitDo Ultimate MG was user-confirmed for both 0.1 and 0.3; the 0.3 test also restored Auto Blocker and disabled Wi-Fi/debugging.

Controller detection uses capabilities rather than a brand allowlist. This does not guarantee every controller works in every game. Games accepting input only from the virtual device may need input forwarding, which this app does not provide. Other firmware, controller models, and 16 KB page-size devices remain unvalidated.

## Access and privacy

Temporary phone-local ADB launches an Android instrumentation session for this app. A persisted, single-use 256-bit activation token expires after 60 seconds; cancellation clears or deletes it. A failed persistent revocation is reported as failure, not as successful cancellation. Only the two permissions `INJECT_EVENTS` and `ASSOCIATE_INPUT_DEVICE_TO_DISPLAY` are delegated.

These permissions are broader than gamepad creation. A compromised app could inject other input, so this is not identical to stock security. The app's code exposes only fixed controller creation, removal, status and exit operations, with an exact app-UID check and no caller-supplied command or controller configuration. No controller event-injection method is called. The controller interface is process-local; the service is unexported and the previous exported bootstrap provider has been removed.

A temporary shell launcher remains during activation and may remain until debugging stops or instrumentation ends; it is not needed for subsequent controller operations. The app itself then runs under its ordinary app UID with the two delegated permissions. Android revokes delegation when instrumentation ends or the app dies. Explicit Stop access attempts revocation and ends the app even if the launcher's Binder has already died.

ADB pairing keys stay in app-private, non-backed-up storage. The app has no analytics or remote server. To prevent future activation, forget **ControllerBridge** under Android Wireless debugging, clear or uninstall the app, and leave debugging off. Only install a build you trust.

## Build

Requires Python 3, JDK 17+ (tested with 21), Android SDK platform 36, and build-tools 36.0.0. Set `JAVA_HOME` and `ANDROID_HOME` (or `ANDROID_SDK_ROOT`), then run:

```sh
python3 test.py
python3 build.py
```

`build.py` downloads dependencies pinned by URL and SHA-256 in `dependencies.json`, builds Java/resources/DEX, packages all native ABIs supplied by those dependencies, and signs `build/ControllerBridge.apk`. No Gradle daemon or Android Studio is required. Internet access is needed only to build/download dependencies, not to run the installed app.

The first build generates a local `signing.p12`. Keep it private and backed up if publishing updates. Builds signed with different keys cannot update one another; uninstall the previous build first. CI creates a temporary signing key per run and is intended for test artifacts. For stable public releases, build using the same private signing key. Never commit signing keys or phone pairing keys.

## Verification

The app checks Android's input registry on every activation and monitors device presence while running. Host tests cover controller classification, shell framing, and UID/nonce/expiry authorization rules. Separate device tests cover persisted cancellation, token replay, storage-failure reporting, and delegated-permission cleanup. Optional independent device checks:

```sh
python3 test_device.py on --descriptor EXACT_DESCRIPTOR --controller 8BitDo --no-physical-sony
python3 test_device.py off --descriptor EXACT_DESCRIPTOR --controller 8BitDo --no-physical-sony
```

These developer checks use `adb` on a computer; users do not need them. `--adb /path/to/adb` selects a binary. The ON check requires exactly one app virtual device with Sony identity and key layout, a physical controller, and optionally no other Sony controller. OFF requires the app virtual device to be absent. For the API backend, supply its exact descriptor from Android input diagnostics; the default marker identifies only the older uinput backend. See `docs/VALIDATION-0.3.md` for current coverage, `docs/VALIDATION-0.2.md` for the failed shell-helper approach, and `docs/VALIDATION.md` for the original release.

## Source and licenses

App source is GPL-3.0-or-later. Third-party components retain their own licenses; see `THIRD_PARTY_NOTICES.txt`, `licenses/`, and `third-party-src/`. Publish the source bundle alongside the APK. Users may modify/rebuild the app and replace dependencies; update the relevant checksum when using a modified dependency, sign their build, and uninstall the differently signed original before installing it.

Not affiliated with Sony, 8BitDo, Samsung, or Activision. DualSense and game names identify compatibility targets.
