# Controller Bridge 0.4.0 prerelease validation

Date: 2026-10-25. Device: Samsung Galaxy S26 Ultra (SM-S948U1), Android 17/API 37, security patch 2026-10-05. Gamepad: 8BitDo Ultimate MG; physical DualSense off.

## Passed before distribution

- 27 existing host checks pass (controller classification, shell framing, helper authorization policy).
- APK builds and signature verification passes. Existing dependency annotation/deprecation warnings remain; no new build error.
- Reviewed the simplified screen, setup dialog and menu on the physical phone. Controller name and main controls fit the display.
- Final APK activates through its saved on-phone Wi-Fi pairing, restarts, shows Ready and automatically changes wireless debugging to OFF.
- Independent Android registry verifier passes for ON and OFF: expected Sony identity/key layout, physical 8BitDo present, no physical Sony controller.
- In-app self-test reports success and restores the previous OFF state. This run retained USB debugging for observation; it is not a new Auto Blocker survival test.
- A recorded OFF → ON → OFF cycle shows the controller light/ring brighten and pulse, then dim. Displayed state reads Android's device registry rather than an early service flag. Pause/detach cancels animation; system-disabled animation is handled in code, but that device setting was not changed for this test.
- Independent review found an early-OFF display issue and a first-install notification-permission instruction mismatch; both were corrected and re-reviewed.

## Limits

- Fresh installation from the private GitHub release, fresh pairing after uninstall, and user gameplay confirmation on 0.4 remain the next acceptance test.
- Gameplay and protected-state operation were confirmed by the user on the unchanged 0.3 backend. Do not present those as a new gameplay test of 0.4.
- Only the phone/controller combination above is validated. Reboot or app termination requires another Wi-Fi activation. Offline activation without an access point is not offered.
- The Android permissions retained during the activated session remain broader than gamepad creation; interface simplification does not change this security tradeoff.

Raw screenshots and screen recordings stay in local `evidence/` and are excluded from Git and the source release.
