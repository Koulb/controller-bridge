# Controller Bridge

Use your gamepad in games that require a connected DualSense. Adds a virtual controller; your real gamepad supplies the inputs.

**Experimental · Android 17+ · Wi-Fi required for activation · No root**

## Install

1. Sign in to GitHub with an account that can access this private repo.
2. Open [v0.4.0 downloads](https://github.com/Koulb/controller-bridge/releases/tag/v0.4.0) and download **ControllerBridge-0.4.0.apk**.
3. Temporarily turn Samsung **Auto Blocker off**. Open the APK and allow installation from your browser/file manager when asked.

## Set up

1. Connect to trusted Wi-Fi. Enable **Developer options**: Settings → About phone → Software information → tap **Build number** seven times.
2. In Developer options, turn **Wireless debugging on**. Approve the current network when asked; leave “Always allow” unchecked.
3. Open Controller Bridge → **Set up phone → Pair phone**. If asked, allow notifications and tap **Pair phone** again.
4. In Android, open **Wireless debugging → Pair device with pairing code**. Keep that dialog open. Pull down notifications and enter its six-digit code in the **Controller Bridge** notification.
5. The app restarts during activation. When it shows **Ready**, verify **Wireless debugging OFF** and **USB debugging OFF**, restore **Auto Blocker ON**, and revoke the browser/file manager’s installation permission. Wi-Fi can now be turned off.

After reboot or app termination: connect to Wi-Fi, temporarily turn Auto Blocker off, enable Wireless debugging, then choose **⋮ → Set up phone → Activate paired phone**. Repeat step 5. Pair again only if Android no longer trusts the saved pairing.

## Play and test

1. Keep the physical DualSense off. Connect your other gamepad.
2. Tap **Turn on DualSense**. The controller lights up when Android confirms it is connected.
3. Choose **⋮ → Run self-test**; expect **Self-test passed**. The test restores the previous on/off state.
4. Keep virtual DualSense on. Fully close Call of Duty: Mobile from Recent apps, reopen it, and test movement, aiming and buttons in training.
5. Tap **Turn off DualSense**; the illustration dims. Turn it on again and repeat the fresh-launch test.

The toggle preserves activation. **⋮ → Stop phone access** revokes access and closes the app. Other phones, controllers and games are not guaranteed to work.

## Access and source

The app temporarily retains two powerful input permissions. A compromised build could misuse them; this is not identical to stock phone security. Install only a build you trust. No analytics or remote service. [Technical details and build instructions](docs/TECHNICAL.md).

Source: GPL-3.0-or-later; see [third-party notices](THIRD_PARTY_NOTICES.txt). Not affiliated with Sony, Samsung, 8BitDo or Activision.

## Tested with

- **Phone:** Samsung Galaxy S26 Ultra (SM-S948U1)
- **Android:** 17 (API 37), security patch 2026-10-05
- **Gamepad:** 8BitDo Ultimate MG
- **Game:** Call of Duty: Mobile

Gameplay was confirmed with the 0.3 backend, Auto Blocker ON and Wi-Fi/debugging OFF. Version 0.4 activation, self-test, controller registration/removal and animation checks passed; its fresh-install distribution and gameplay test is pending.
