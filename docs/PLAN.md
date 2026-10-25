# Controller Bridge Implementation Plan

Historical plan for version 0.1. Current 0.3 architecture and outstanding requirements are recorded in `superpowers/plans/2026-10-25-input-api.md`, `VALIDATION-0.3.md`, and `OFFLINE-ACTIVATION.md`.

Goal: One downloadable Android APK that detects any Android gamepad and toggles an additional virtual DualSense, with all setup and operation possible on the phone.

Architecture: Native Java UI + connected-device foreground service. Embedded LibADB pairs with this phone's Wireless debugging; its authenticated local shell stream owns uinput. Closing the stream removes the virtual pad. No Shizuku or PC runtime dependency. Min Android 11, compile/target 36. Ordinary apps cannot create virtual controllers without debugging/root/system privileges.

User requirements: real DualSense -> “DualSense connected”; other controllers -> actual names and “Turn on DualSense”; second tap -> off. Recognize gamepad/joystick capabilities, not a brand allowlist. Physical controller supplies gameplay inputs. No claims of universal game/phone compatibility.

Task 1: Pure controller classification tests (no pad, generic pad, multiple pads, DualSense/Edge, virtual pad exclusion, sensor exclusion). Run failing test first, implement ControllerRules, rerun.
Task 2: Embedded pairing, private persistent identity, local-only discovery, foreground input stream, verified on/off state. Guided notification pairing keeps Settings' pairing dialog open. No arbitrary command input/exported control endpoints. Disconnect removes virtual device; process death closes stream. Build signed APK and install.
Task 3: Exercise phone-only pairing without USB privilege bootstrap, UI toggle on/off, independent dumpsys validation, physical DualSense/no-controller branches where hardware available; test simulated classifications otherwise. Capture UI, document limits/setup and provide reproducible build/source for GitHub.

Review focus: fake vs real DualSense; app/background/service lifetime; stale registration after off; rejected pairing/revoked debugging; non-8BitDo and multiple-controller behavior.

Status: implementation and host/device registration checks complete. Public source archive packaged and clean-build verified; final standalone-app CoD Mobile gameplay confirmed by the user with only 8BitDo connected. Pairing, on/off, background persistence, force-stop cleanup, cancellation, and debugging recovery verified. See VALIDATION.md for actual coverage and untested cases.
