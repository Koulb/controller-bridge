# Remaining offline activation blocker

Status: 2026-10-25. Runtime controller toggling and CoD gameplay work after activation with Auto Blocker ON and Wi-Fi/debugging OFF. A fresh, phone-only activation with no nearby Wi-Fi is not implemented or proven. The complete requested goal remains unmet.

## Follow-up: already paired, no nearby network (2026-10-25)

The user now accepts initial setup on Wi-Fi and asks for subsequent activation without a nearby access point. This is a narrower requirement than a fresh, unpaired installation, but still requires a way to start the privileged session. Saving pairing credentials does not grant that privilege by itself.

Live tests on the same Samsung S26 Ultra (Android 17, security patch 2026-10-05):

- A separate, disposable `app.controllerbridge.offlineprobe` APK used the public local-only-hotspot API. It declared ordinary Wi-Fi/network permissions and `NEARBY_WIFI_DEVICES`; it never requested `WRITE_SECURE_SETTINGS`, input injection, accessibility access, or instrumentation privileges. Source is in the sibling `controller-offline-probe` directory, outside the release archive.
- An initial request failed with error 3 while the lock screen was showing. Waiting for an unlocked, focused activity succeeded. The probe used `FLAG_KEEP_SCREEN_ON` and a 120-second hotspot cleanup timeout.
- With Wi-Fi station mode OFF and the local hotspot ready, a single `adb_wifi_enabled=1` write did not start a TLS debugging listener. An 80-pass bounded read/re-enable loop at a requested 20 ms delay also produced no listener or NSD service. Actual cadence included shell/settings IPC overhead; this was not an exact timing reproduction of an in-process writer.
- The global flag stayed at 1, so subsequent conditional passes did not write again. Framework logs explicitly reported `Not connected to any wireless network. Not enabling adbwifi.` A flag value of 1 must not be reported as activation success.
- Both attempts used USB shell to drive the experiment, with a shell EXIT trap restoring wireless debugging to 0. They are mechanism tests, not successful phone-only activation tests.
- A further ordinary `WifiNetworkSpecifier` request targeted only the probe's own WPA3-capable hotspot. With the Wi-Fi radio temporarily enabled, the system searched and returned `SELF_CONNECTION_UNAVAILABLE` after the 30-second request timeout. This is one failed self-connection attempt, not a proof that all hardware/firmware must fail. Wireless debugging stayed OFF throughout it.
- Probe was force-stopped and uninstalled; Wi-Fi and wireless debugging were verified 0. Listening-socket output returned to its baseline (an unrelated pre-existing port 6400); the temporary hotspot DNS listeners disappeared. No new broad app permission was retained. Controller Bridge's APK was not changed.

Sanitized device evidence: `evidence/offline-hotspot-2026-10-25.txt`.

Research motivating these tests:

- [Shizuku-Next's forced wireless-debugging implementation](https://github.com/rushiranpise/Shizuku-Next/blob/master/manager/src/main/java/moe/shizuku/manager/start/ForcedWirelessDebugging.kt): explicitly experimental timing-dependent behavior, requires settings-write authority.
- [Original hotspot/race reports](https://github.com/thedjchi/Shizuku/issues/165): successes on other devices are not guarantees for this Samsung/Android release.
- [Offline startup/cleanup report](https://github.com/rushiranpise/Shizuku-Next/issues/127): reports an already-paired OPPO Android 16 configuration, with a persistent settings-write grant; not evidence of Samsung compatibility.
- [Public local-only-hotspot API](https://developer.android.com/develop/connectivity/wifi/localonlyhotspot): creates a local network without internet, but does not promise eligibility for wireless debugging.

Result: these tests did not establish a secure, phone-only offline reactivation path. Do not add a permanent settings-write grant merely on the assumption that the older hotspot workaround will work here. Further experiments need a distinct, testable mechanism; repeating the same unchanged setting is not useful progress.

## Verified platform gates

1. Android 17 ActivityManager rejects instrumentation startup by an ordinary uninstrumented app on production builds. The required permission delegation accepts only shell/root callers and an active shell-started instrumentation session. A downloaded APK cannot authorize itself through these APIs.
2. Normal wireless-debugging startup calls `getCurrentWifiApInfo()`. It rejects missing Wi-Fi connection information or network ID `-1`; TLS startup also repeats this check. The Android 17 reconnect flag can leave the setting enabled while waiting for a network, but it does not remove the TLS-start requirement. A phone-hosted hotspot alone does not make the phone a station connected to an access point for this check.
3. The debugging management APIs require `MANAGE_DEBUGGING`, declared `signature|privileged`. `WRITE_SECURE_SETTINGS` also cannot be obtained through an ordinary runtime permission dialog.

Official Android 17 source inspected:

- [Instrumentation startup](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/am/ActivityManagerService.java)
- [Shell permission delegation](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/am/AccessCheckDelegateHelper.java)
- [Wireless-debugging Wi-Fi checks](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/adb/AdbDebuggingManager.java)
- [Debugging service permission enforcement](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/services/core/java/com/android/server/adb/AdbService.java)
- [Permission protection levels](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android17-release/core/res/AndroidManifest.xml)

These explain the current supported activation path; they are not a proof that no vendor-specific mechanism could ever exist. Samsung-specific behavior still requires device evidence.

## Investigated alternatives

- Shizuku-Next's offline hotspot/race method needs an existing `WRITE_SECURE_SETTINGS` grant and repeatedly re-enables wireless debugging. It does not bootstrap an unprivileged fresh install. It was inspected read-only, not added to this app.
- Leaving an ADB TCP listener running conflicts with restoring debugging OFF and Auto Blocker ON. No such listener is configured.
- Vendor privilege-exploit startup is not established as preserving the user's requested security. It was not executed or incorporated.
- A computer/other USB host or a nearby access point can enable temporary setup, but neither proves the requested self-contained offline first activation.

## What would unblock the remaining requirement

A verified on-device, user-authorized way to obtain the startup privilege without a nearby access point, external host, persistent broad settings access, or a vendor security exploit. No such path has been established on this phone. Do not replace this requirement with “Wi-Fi is unnecessary after activation,” which is already proven but narrower.
