# Version 0.1.0 validation — 2026-10-25

This record describes the original persistent-ADB release. See [VALIDATION-0.2.md](VALIDATION-0.2.md) for the offline-helper release.

Hardware: Samsung S26 Ultra (SM-S948U1), Android 17/API 37, 4 KB pages; Bluetooth 8BitDo Ultimate MG. Physical DualSense remained off for app virtual-device tests. Old computer-installed helper was stopped before app testing.

## Passed

- Host tests: 9 controller classification checks, including generic/Xbox-shaped capability data, real DualSense/Edge, multiple controllers, own virtual exclusion, and non-controller exclusion.
- Host tests: 4 shell protocol checks, including fragmented output, exit handling, truncated packet rejection, and packet size validation.
- APK compilation, signing verification, and installation. Public source archive rebuilt in a fresh directory using standard SDK environment variables and checksum-verified dependency cache; rebuilt DEX matches the installed APK.
- Pairing through app notification with code from Android Settings. App generated its own key; no computer key copied or computer-issued pairing used. Android displayed ControllerBridge as connected.
- Empty-controller UI: no controller, activation disabled.
- Actual 8BitDo name displayed. Activation adds one Sony 054c:0ce6 gamepad with Sony key layout, independently checked through Android Event Hub.
- Two complete on/off cycles: exactly one app virtual device when on, none when off, 8BitDo remains connected.
- Background persistence with Home pressed.
- Force-stop removes app virtual device. Relaunch reconnects using saved pairing.
- Rapid cancellation leaves no virtual device after the startup operation finishes.
- Disabling Wireless debugging removes virtual device. Cancellation during discovery prevents delayed activation, checked again after 14 seconds.
- Re-enabling Wireless debugging, accepting Android's current-network prompt, and tapping Turn on reconnects without new pairing code.

The test driver used USB ADB to tap UI and independently inspect results. The app performed its own pairing, local connection, virtual registration, and cleanup. USB ADB is not an app runtime dependency.

## Gameplay coverage

The same virtual Sony identity and input configuration previously enabled CoD Mobile's Connected / PS5 state with only 8BitDo physically connected; the user confirmed movement, aiming, and buttons. The user also confirmed the final standalone app works in CoD Mobile after reopening the game and testing movement/buttons, with only 8BitDo connected and the physical DualSense off. This confirms the tested phone/controller/game combination; other combinations remain unverified.

## Limits

Actual additional controller brands, physical DualSense UI branch, phone reboot, lost pairing/revocation, 16 KB devices, other Android versions/vendors, and other games were not hardware-tested. Real Sony and generic-controller classification branches were tested with synthetic input data. A full hardware compatibility matrix is not claimed.

## Fixes validated during development

Legacy shell/process cleanup and main-thread network cleanup caused off failures in earlier builds. Final code uses raw shell v2, background stream closure, and input-registry health checks. Earlier failures were corrected before the passing lifecycle tests. Diagnostics use a completion marker instead of relying on the library's EOF behavior. Cancellation invalidates operations, interrupts blocking OPEN/discovery waits, cancels pairing sockets, and disconnects stale operations before retry.
