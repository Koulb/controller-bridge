# Distribution release implementation plan

**Goal:** Publish a private GitHub prerelease with a minimal controller UI, confirmed-state animation, short setup instructions, and a clean phone for the user's installation test.

**Approach:** Keep the tested Wi-Fi activation and input backend. Simplify the native Java activity; move secondary actions to a menu. Draw and animate a controller locally without adding dependencies. Publish source and a signed APK under `Koulb/controller-bridge`, excluding signing material, device evidence, caches, and exploratory probes.

## Work

- [ ] Replace the main activity's diagnostic-heavy layout with controller name, compact access state, illustrated controller and one toggle. Preserve actionable errors and essential protection guidance. Keep setup, self-test, access revocation, fallback connection port and licenses reachable.
- [ ] Animate only confirmed virtual-controller state changes; cancel animations when the activity pauses and honor system-disabled animations.
- [ ] Bump to 0.4.0. Write minimal install/setup/retest README, ending with tested phone, Android and gamepad. Move technical details into linked documentation.
- [ ] Run existing host checks, build/sign, and inspect the final UI on the phone. Check on/off states when hardware and activation are available. Report any untested behavior.
- [ ] Audit the exact source bundle and Git file list for credentials and device data. Create the private repository and upload a prerelease APK, corresponding source and checksums. Verify privacy and downloaded artifact hash.
- [ ] Uninstall Controller Bridge and obsolete diagnostic app from the phone, verify absence, restore Auto Blocker, and give the user the release link plus fresh-install test steps. Do not silently install the final app again afterward.

UI edits are reversible presentation changes; use the existing behavioral suite and device visual checks rather than tests that copy layout implementation. Activation and security implementation remain unchanged.
