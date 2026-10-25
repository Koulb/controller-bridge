# Offline controller helper implementation plan

User accepted temporary activation after reboot if helper access is tightly restricted. Preserve Auto Blocker re-enable and locked/verified boot. Do not keep ADB listeners running during gameplay, disable security enforcement, or expose general commands.

Architecture decision: keep embedded local ADB only for activation. Launch an app-owned, detached shell helper with fixed controller operations. Prefer Binder IPC with OS-authenticated calling UID plus a one-time bootstrap nonce; no TCP listener, no arbitrary shell command/config arguments. Explicitly disclose that a privileged process remains and activation may be lost if Samsung kills it or the phone reboots.

Approaches considered: current persistent ADB fails offline/security requirement; normal app/API creation is blocked by signature/internal permissions (confirmed on device); temporary scoped helper is accepted approach. Root/system modification rejected by user security requirement. Public new Android gamepad API still needs signature INJECT_EVENTS.

- [x] Spike detached helper lifecycle and private IPC on Samsung without changing security policy.
- [x] Implement fixed on/off/status/shutdown protocol, UID authorization, expiring bootstrap, caller death/lease cleanup, no user-controlled commands or paths.
- [x] Adapt service/UI to activation vs offline availability, disconnect ADB after bootstrap, cancellation and honest state checks.
- [ ] Test unauthorized caller, repeated offline on/off, disconnect/force-stop, and actual Auto Blocker on. Keep original 0.1 release available.
- [x] Independent security/lifecycle code review, final APK/source tests, document exact startup and restart limits.

Device evidence: offline on/off and independent registry checks pass; force-stop and explicit EXIT remove helper/controller; invalid nonce rejected; clean source rebuild DEX matches. Auto Blocker switched ON and USB ADB disconnected. Awaiting user’s phone-local self-test/gameplay result with protection enabled.
