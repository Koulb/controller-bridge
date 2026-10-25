# Controller Bridge 0.5.1 validation

Device: Samsung Galaxy S26 Ultra (SM-S948U1), Android 17/API 37, security patch 2026-10-05. Gamepad: 8BitDo Ultimate MG; physical DualSense off.

## Change

User reported in-app pairing failed while notification pairing worked. Version 0.5.1 removes the in-app form, both entry buttons and the plain-Intent code fallback. Notification RemoteInput is the sole code entry path. Reset and controller creation are unchanged. Prior 0.5 split-screen results do not establish reliable user operation; that path is no longer supported.

## Verified

- 27 host checks pass; APK builds and signature verification passes.
- New device regression test fails on 0.5 because it still accepts the removed direct-code route, then passes on 0.5.1. Tests verify notification code parsing with leading zeroes, rejection of direct extras and malformed codes, and activation cancellation/replay protections.
- Current setup UI has no in-app code entry. Instructions explain keeping Android's pairing dialog open and entering its code through the notification.

- Clean install and fresh notification pairing on the physical phone: allow notifications, Pair phone, open Android pairing dialog, expand Controller Bridge notification, Enter pairing code, enter all six digits, Send. App restarts, shows Ready and automatically sets Wireless debugging to OFF.
- Built-in self-test reports Self-test passed and restores the prior ON state. Independent registry checks confirm expected Sony identity with only 8BitDo physical, continued registration after Wi-Fi OFF, and removal after Turn off DualSense.
- Samsung's notification uses a Send text button, not an arrow; README matches the observed UI. Documentation screenshot is captured before typing any pairing code.

## Distribution

README contains only current notification instructions and a privacy-edited notification screenshot. Historical in-app screenshots are removed from the current source tree. Raw captures remain ignored under evidence/.

Older Controller Bridge APKs 0.1.0, 0.2.0, 0.4.0 and 0.5.0 were removed from this phone's Downloads at the user's request. Other downloads were not touched.

## Limits

USB debugging is kept on during developer observation. Fresh download/gameplay and the both-debugging-off footer remain user acceptance checks. Earlier 0.5 reset and 0.4 gameplay results are historical, not new 0.5.1 gameplay tests.
