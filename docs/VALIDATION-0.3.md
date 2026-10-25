# Controller Bridge 0.3.0 experimental validation

Device: Samsung S26 Ultra, Android 17/API 37. Date: 2026-10-25. Physical controller: 8BitDo Ultimate MG; physical DualSense off.

## Passed

- Separate API probe: registry create/remove PASS after Auto Blocker ON, with USB=0 and wireless=0 recorded in each result at 13:43:32 and 13:43:56. User independently confirmed PASS and both zero values.
- Main APK builds and signature verification passes. Existing host suite: 27 checks; these do not substitute for API/lifecycle device tests.
- Integrated API backend: exactly one Sony 054c:0ce6 gamepad, enabled with Sony key layout, 8BitDo present and no physical Sony. Independent EventHub evidence: `evidence/api-final-on-input.txt`. Device is classified virtual and has location `virtualGamepad:…`.
- Exact-descriptor device verifier accepts the observed controller and rejects a different descriptor.
- Force-stop removes the virtual Sony while leaving the physical 8BitDo connected.
- Phone-local activation using saved pairing works across the required app restart; input access returns and Wireless debugging automatically becomes zero. Repeated on the final installed build at 14:06. Wi-Fi was temporarily available for this setup.
- Integrated on/off self-test passes before Auto Blocker activation; the specific recorded test still had USB debugging enabled.
- Separate signed device-test APK passes: prior delegated input permission absent; cancelled request rejected before adoption; failed preference write plus failed deletion reports failure; successful deletion fallback removes authorization; valid token consumed exactly once; replay denied. Test APK removed afterward.
- Actual instrumentation entry rejects an unarmed token; subsequent permission-cleanup check passes.
- Independent code review found and prompted a cancellation fix; no remaining high/critical issue found in the reviewed fix. This is not a penetration test.
- Final integrated app: user confirmed on/off self-test PASS after Auto Blocker was restored ON, Wi-Fi OFF, and ADB disconnected. CoD initially showed a connected controller but ignored inputs. Keeping virtual DualSense ON, fully closing CoD from Recent apps, and reopening it restored gameplay; user confirmed “now it works.” Controller-state caching is a plausible explanation, not a demonstrated internal CoD mechanism.

## Pending acceptance and limits

- Successful protected-state gameplay requires the tested launch order: enable virtual DualSense, then fully close/reopen CoD. Merely returning to an already running CoD instance was insufficient in this test.
- No fresh phone-only activation without nearby Wi-Fi. The full requested goal is not achieved.
- Actual UI cancellation while a deliberately delayed detached launcher is pending has not been exercised. Device tests cover the persisted authorization path and failure cases, not that full UI timing scenario.
- Explicit Stop phone access on the final integrated app needs its own end-to-end UI check. Force-stop cleanup and instrumentation permission cleanup have been checked.
- Long Doze/memory-pressure survival until reboot, physical-controller reconnect after sleep, other Android versions/firmware and other controller models remain unverified.
- The two retained permissions allow more than gamepad creation. Restriction to fixed operations is enforced by app code and nonexported interfaces, not by a gamepad-only Android permission.
- The temporary launcher may live until debugging stops or instrumentation ends; it must not be described as already gone merely because activation succeeded.

Build is experimental and has not been published to GitHub. Previous 0.1/0.2 release APKs remain unchanged.
