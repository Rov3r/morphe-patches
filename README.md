# Rov3r Patches

Android app patches maintained by Rov3r and distributed as a single Morphe patch
bundle.

## Add this source

[Add Rov3r Patches to Morphe](https://morphe.software/add-source?github=Rov3r/morphe-patches)

## Patches

<!-- PATCHES_START EXPANDED -->

<!-- This section is updated automatically by the release workflow. -->

### VSCO

- **Download posts** — Downloads images and DSCO videos at their original quality.

<!-- PATCHES_END -->

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

## Repository layout

- `patches/src/main/kotlin/com/rov3r/patches/<app>/` contains each app's patch
  definitions, fingerprints, and compatibility metadata.
- `extensions/<app>/` contains runtime code merged into that app.
- Each extension writes a uniquely named file such as `extensions/vsco.mpe` into
  the shared patch bundle.

To add another app, create a separate package beneath `com.rov3r.patches`, add a
matching extension module only if runtime code is needed, and declare that app's
own `Compatibility` targets. All visible patches are published together in the
same `.mpp` bundle.

## Releases

The included workflows publish prereleases from `dev` and stable releases from
`main`. Use semantic `feat:` and `fix:` commits so the release workflow updates
the bundle metadata, changelog, and patch list automatically.

## License

GPLv3. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
