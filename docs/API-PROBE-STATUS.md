# Android 17 input API investigation

Date: 2026-10-25. This is an investigation record, not release validation.

## Required outcome

A downloadable app must activate entirely on the phone even without a nearby Wi-Fi network. After temporary setup, Auto Blocker must be on and both debugging modes off. The app must then create and remove the virtual DualSense until reboot. Access retained after setup must be tightly restricted. CoD Mobile must work with only the physical 8BitDo connected.

Neither existing release proves this outcome. Version 0.1 worked in CoD while using ADB; version 0.2 failed after Auto Blocker was enabled.

## Separate development probe

Source is in the sibling `controller-probe` directory. Package: `app.controllerbridge.probe`; launcher label: **Controller API Probe**. It is separate from Controller Bridge and is not included in release archives.

Temporary USB ADB starts an instrumentation session. The probe delegates only `INJECT_EVENTS` and `ASSOCIATE_INPUT_DEVICE_TO_DISPLAY`, then invokes Android 17's hidden `InputManager.createVirtualGamepad` API. It creates a fixed Sony 054c:0ce6 gamepad, checks Android's input registry, closes it, and checks disappearance. It sends no input events. Instrumentation is launched with hidden-API checks disabled for this session.

Observed evidence:

- Delegating only `INJECT_EVENTS` failed because display association also required `ASSOCIATE_INPUT_DEVICE_TO_DISPLAY`.
- With both permissions, the registry create/remove test passed under the app UID (10035) while USB debugging was enabled. Evidence: `evidence/api-probe-2.png`.
- Wi-Fi was then disabled and Auto Blocker enabled. USB ADB disconnected; a fresh `adb devices` check returned no devices at 13:39 local time.
- A fresh test result **after** enabling Auto Blocker is still missing. The user needed help locating the separate probe app. Its manifest contains a launcher entry.

Follow-up at 13:41–13:43:

- User reported “No active test session,” then temporarily re-enabled USB debugging for inspection.
- Android exit history records an app exception at 13:39:48. Crash stack points specifically to the probe's `end()` action calling `Instrumentation.finish()`, which attempted to disconnect a dead UiAutomation Binder. This does **not** prove Auto Blocker independently killed the app.
- Filtered probe logs contain another registry PASS at 13:39:22, after the earlier Auto Blocker switch. That version did not record debug flags with each test, so this is encouraging but insufficient protected-state evidence.
- Probe now automatically tests once both debugging flags remain zero for two seconds, records both flags with PASS, and requires confirmation before ending the session. Cleanup catches a disconnected UiAutomation and terminates its own process so Android can clear instrumentation permissions.
- Updated APK compiled, signed, installed; baseline PASS at 13:42:58 records USB=1, wireless=0. Before switching Auto Blocker on again, independent settings reads showed USB=1, wireless=0, Wi-Fi=0. Protected-state result from this controlled retest is pending.
- User subsequently reported both displayed debugging values as zero. PASS/FAIL text is still pending; zero flags alone do not establish successful gamepad creation. A subsequent independent `adb devices` check still returned no devices.
- User then explicitly confirmed **PASS: created and removed**, with **USB at test=0 and Wireless at test=0**. This proves the probe's registry create/remove operation after Auto Blocker activation on this phone. It does not establish CoD compatibility, phone-local activation, offline first activation, or survival until reboot under memory pressure.

A prior PASS is insufficient: the user must tap **Run API on/off test** again with both displayed debugging values at zero. If it passes, later tests must still establish gameplay, lifecycle behavior, permission cleanup, and secure phone-only activation. Retained input-injection permission is broader than gamepad creation alone; do not describe it as an OS-enforced gamepad-only permission.

## Offline activation finding

Read-only review of Shizuku-Next's `ForcedWirelessDebugging.kt` and `StartPermissions.kt` found that its experimental offline start requires an existing `WRITE_SECURE_SETTINGS` grant. It creates a local-only hotspot and repeatedly writes the wireless-debugging setting, relying on an undocumented race. The code itself states that a fresh install without the grant cannot bootstrap this way.

Sources:

- https://github.com/rushiranpise/Shizuku-Next/blob/master/manager/src/main/java/moe/shizuku/manager/start/ForcedWirelessDebugging.kt
- https://github.com/rushiranpise/Shizuku-Next/blob/master/manager/src/main/java/moe/shizuku/manager/start/StartPermissions.kt
- https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/java/android/hardware/input/InputManager.java

Copies of the inspected source are in `evidence/`; no third-party startup code was executed. This method has not been added to Controller Bridge. It does not establish the required fresh, offline, phone-only setup. Vendor exploit startup is also not implemented or accepted as preserving the user's security requirement.

## Next decision

The protected-state registry probe passed. Next validate activation from the installed app and a persistent controller toggle in CoD. `am instrument --no-restart` may avoid interrupting the app's own ADB connection, but hidden-API access in an already running process needs a live check. If that fails, a detached, temporary instrumentation launcher with normal app restart needs validation before integration. Neither route resolves offline first activation; USB activation must not be presented as meeting the full requirement.
