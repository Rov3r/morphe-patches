# Rov3r Patches

Android app patches maintained by Rov3r and distributed as a single Morphe patch
bundle.

## Add this source

[Add Rov3r Patches to Morphe](https://morphe.software/add-source?github=Rov3r/morphe-patches)

## Patches

<!-- PATCHES_START EXPANDED -->
> **[v0.0.7](https://github.com/Rov3r/morphe-patches/releases/tag/v0.0.7)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;1 patches total
<details open>
<summary>📦 VSCO&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 489.1 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Download posts](#download-posts) | Adds configurable original-quality downloads, profile tools, feed cleanup, media controls, and privacy options. |  |

</details>

<!-- PATCHES_END -->

## VSCO feature controls

The patch adds a **Patch Settings** entry above VSCO's Settings menu with:

- Original-quality image and DSCO downloads to a remembered folder.
- Optional username subfolders, filename templates, and timestamp control.
- Tracking-clean copy-link and share-original actions.
- A long-press, full-quality profile-photo viewer with a save action.
- Toggles for profile buttons/badges, suggested users, membership prompts, and promotional cards.
- Video autoplay and unmute controls.
- Selective usage-analytics and marketing-attribution blocking.
- Device-bound offline authorization for manually verified subscribers.

Filename templates support `{id}`, `{username}`, `{type}`, and `{timestamp}`.
Location is never used in filenames or folder organization.

### Offline subscriber authorization

An offline device can create an activation request from **Patch Settings**.
After the maintainer verifies the subscriber's original receipt, the maintainer
can return a signed authorization limited to that receipt's feature tier and
expiration. The user transfers the text file by USB, SD card, or Bluetooth and
imports its contents in Patch Settings; the target device never needs a network
connection.

Authorizations are bound to one VSCO installation and cannot be extended or
edited without invalidating their signature. The APK contains only the public
verification key. Receipt data and the private issuer key are not part of this
repository or release artifacts.

## Build

1. Install Java 21 and Android SDK Platform 36.
2. Add a GitHub token with `read:packages` to your user-level Gradle properties:

   ```properties
   gpr.user=YOUR_GITHUB_USERNAME
   gpr.key=YOUR_GITHUB_TOKEN
   ```

3. Run:

   ```powershell
   .\gradlew.bat buildAndroid
   ```

The patch bundle is written to `patches/build/libs/`.

## Releases

The included workflow currently marks every generated GitHub release as a
prerelease. Use semantic `feat:` and `fix:` commits so the release workflow updates
the bundle metadata, changelog, and patch list automatically.

## License

GPLv3. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
