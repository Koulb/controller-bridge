# Controller Bridge 0.2.0 validation

Device: Samsung S26 Ultra, Android 17/API 37, locked bootloader and verified boot green. Physical controller: 8BitDo Ultimate MG. Real DualSense off. Date: 2026-10-25.

## Passed

- APK build, signing verification, install/update with existing app data and pairing key.
- Source archive rebuilt in a fresh directory with SDK environment variables and checksum-verified dependency cache. Tests pass; rebuilt DEX matches release; archive excludes private keys.
- 27 host checks: 9 classification, 4 shell framing, 14 UID/nonce/expiry authorization.
- Embedded phone-local ADB activation; Binder helper survives closing launch session and automatic Wireless debugging shutdown.
- Wi-Fi switched OFF; phone-local on/off self-test passes and restores ON. Independent EventHub check confirms exactly one virtual Sony 054c:0ce6 device with Sony key layout and physical 8BitDo only.
- Rapid OFF/ON/Cancel with Wi-Fi and Wireless debugging OFF leaves no virtual controller.
- Stop phone access during controller startup removes helper and controller; app remains open.
- Force-stop removes controller and helper; independent OFF check passed 0.08 seconds after the force-stop command returned.
- Invalid bootstrap token rejected by live exported provider with SecurityException.
- Independent static review: no remaining critical security/lifecycle finding after fixes. Not a penetration test or proof of absence of vulnerabilities.

## Failed acceptance test

- After Auto Blocker was switched ON and USB ADB disconnected, user reported “Self-test fails or helper inactive.” The build does not yet meet the Auto Blocker requirement. Earlier Wi-Fi OFF tests retained USB debugging, so they did not establish survival when adbd fully stops. Root cause is under investigation; CoD gameplay with this security configuration is not validated.

## Limits

- Exact app UID enforcement has host tests and static review; no separate hostile-app Binder handle injection test was run.
- The missed-heartbeat fallback is implemented (30 seconds, checked every 5 seconds); direct app-death cleanup was tested. Long Doze, low-memory pressure, and multiple Samsung firmware versions were not tested.
- Reboot/force-stop/update or helper loss requires activation again. Activation needs Wi-Fi; cellular-only reactivation is not supported.
- Samsung hides its Auto Blocker setting from this ordinary app. UI says “check Settings”; confirm the setting manually.
- CoD gameplay was user-confirmed on 0.1.0. The 0.2.0 offline security configuration needs separate user confirmation.
