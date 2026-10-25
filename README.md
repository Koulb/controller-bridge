# Controller Bridge

Virtual DualSense toggle for games such as Call of Duty: Mobile. **Experimental · Android 17+ · No root.** Wi-Fi is needed for activation, not gameplay.

## Install and set up

1. Download **[ControllerBridge-0.5.0.apk](https://github.com/Koulb/controller-bridge/releases/tag/v0.5.0)** while signed into GitHub with access to this private repo.
2. Temporarily turn Samsung **Auto Blocker OFF**, install the APK, and connect to trusted Wi-Fi.
3. Enable **Developer options** (About phone → Software information → tap Build number seven times), then **Wireless debugging**. Allow the current network; leave “Always allow” unchecked.
4. App → **Set up phone → Pair phone**. Allow notifications if asked, then tap again. In Android, select **Pair device with pairing code**.
5. Keep Android’s code dialog open. Enter its code through the app’s notification, or use **Enter pairing code** inside the app in **split screen**.
6. App restarts and shows **Ready**. Verify **USB debugging OFF**, **Wireless debugging OFF**, and restore **Auto Blocker ON**. Revoke your browser/file manager’s APK-install permission. Wi-Fi can now be turned off.

For split screen on Samsung: Recent apps → tap Controller Bridge’s icon → **Open in split screen view** → select **Developer options/Settings**. Scroll the app pane to **Enter pairing code**; keep Android’s code dialog visible beside it.

<p>
  <img src="docs/images/setup.png" alt="Controller Bridge setup instructions" width="280">
  <img src="docs/images/pairing.png" alt="In-app pairing-code entry" width="280">
</p>

## Play and test

Connect your gamepad, keep physical DualSense off, then tap **Turn on DualSense**. Run **⋮ → Run self-test**. Fully close and reopen CoD, then test movement, aiming and buttons in training.

**After reboot or app termination:** repeat Wi-Fi/debugging preparation, choose **Activate paired phone**, then restore protection as in step 6.

**Reset setup:** red button → confirm. Deletes the app’s pairing key/settings, revokes access and closes the app. Reopen and pair again. Cancel changes nothing.

Install only a build you trust: activation retains powerful input permissions until the app stops. Other hardware/games are not guaranteed. [Maintainer details](AGENTS.md).

## Tested with

Samsung **Galaxy S26 Ultra (SM-S948U1)** · **Android 17 / API 37** · **8BitDo Ultimate MG** · **Call of Duty: Mobile**. Fresh-install gameplay confirmed on 0.4; 0.5 pairing/reset/self-tests passed, gameplay retest pending.
