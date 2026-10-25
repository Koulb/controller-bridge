# Controller Bridge 0.5.0 validation

Date: 2026-10-25. Samsung Galaxy S26 Ultra (SM-S948U1), Android 17/API 37, security patch 2026-10-05. Physical gamepad: 8BitDo Ultimate MG. Physical DualSense off.

## Passed

- 27 host checks: controller classification, shell protocol and helper authorization policy.
- APK builds and signature verification passes; existing dependency annotation/deprecation warnings remain.
- Separate device instrumentation verifies notification and in-app code extraction, leading zeroes, notification precedence, rejection of missing/malformed/non-ASCII codes, and existing activation cancellation/replay protections.
- Activated through existing saved pairing. Red Reset setup button and explicit confirmation appear. Cancel preserves the connected virtual controller, verified independently from Android's input registry.
- Confirmed reset with a virtual controller connected. Independent registry check verifies removal before starting the test harness. Device instrumentation then verifies the private ADB key/certificate, controller settings and activation request are gone; input/display/notification permissions are revoked. Reopening shows initial setup and no reset button.
- First reset automation attempt sent a tap before the dialog had opened and correctly failed validation. Repeating with observed UI transitions passed; no reset implementation change was needed.
- Fresh pairing after reset through the app's code field succeeds with Android Settings in Samsung split screen. Settings launches in a separate task. The pairing dialog stays open while the app receives the code. Activation restarts the app, shows Ready, automatically disables Wireless debugging, and produces the expected virtual Sony controller with only 8BitDo physical.
- Built-in self-test reports Self-test passed and restores the prior ON state.
- Reset remains available when a saved key exists even if access has ended. Independent review identified this recovery case; corrected before release.
- USB-debugging warning is visible while USB debugging remains on. Code hides the entire footer when both debugging flags are off, preserving actionable errors.

## Limits

- This test retained USB debugging for observation. The footer's both-debugging-off state and fresh downloaded 0.5 gameplay remain part of the user's acceptance test.
- User confirmed fresh installation, setup and gameplay from release 0.4. This release keeps the same controller backend; no new CoD gameplay claim is made for 0.5.
- In-app pairing was validated in split screen. Full-screen switching away from Android's pairing dialog is not the supported entry flow. Notification entry remains available and its parser is regression-tested.
- Reset deletes this app's private key; Android can retain an unusable old ControllerBridge public-key entry. System debugging and Auto Blocker switches are unchanged by reset.
- Only the hardware above is validated. Access still needs Wi-Fi activation after reboot or app termination. No overlay permission, root or permanent privileged helper was added.

README setup/code-entry captures have privacy-blurred status bars; the pairing field is empty. Developer detail lives in AGENTS.md.

Raw UI evidence stays in local evidence/ and is excluded from Git and release archives.
