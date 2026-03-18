# GitHub Actions Release APK Delivery Spec

## Purpose

This document defines the work needed to produce a signed release APK on every push and make that APK easy to download from GitHub.

The repository currently has no GitHub Actions workflow and no release signing configuration in the Android app module. This spec covers the minimum secure setup needed to automate release builds in GitHub-hosted CI.

## Goal

After any push to the repository:

1. GitHub Actions builds the Android `release` APK.
2. The APK is signed with the app's release keystore.
3. The build output is uploaded to GitHub as a downloadable artifact.
4. The workflow fails clearly if signing material or required secrets are missing.

## Non-Goals

- Publishing to Google Play
- Building Android App Bundles as the primary artifact
- Managing version names or changelogs automatically
- Replacing local developer release builds
- Long-term binary distribution outside GitHub

## Desired Outcome

The repository should support a push-driven CI flow where a collaborator can:

1. Push a commit to GitHub.
2. Open the workflow run in the repository Actions tab.
3. Download the signed release APK from that run.
4. Install the APK on a device without rebuilding the project locally.

## Constraints

- The release keystore must never be committed to the repository.
- All signing secrets must be stored in GitHub Actions secrets.
- The workflow must use the existing Gradle project structure and build `:app`.
- Secret values must not be echoed in logs.
- The workflow should be maintainable by someone who is not deeply familiar with Android CI.

## Recommended Approach

Use GitHub Actions to run a release build on each push with signing material injected at runtime from repository secrets.

High-level flow:

1. Checkout the repository.
2. Set up Java and the Android/Gradle build environment.
3. Recreate the release keystore from a Base64-encoded GitHub secret.
4. Pass signing credentials into Gradle through environment variables or a generated properties file.
5. Run `./gradlew :app:assembleRelease`.
6. Upload the generated `app-release.apk` as a GitHub Actions artifact.

## Required Secrets

The repository should define these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
  Base64-encoded contents of the release keystore file.
- `ANDROID_KEYSTORE_PASSWORD`
  Password for the keystore.
- `ANDROID_KEY_ALIAS`
  Alias name of the signing key.
- `ANDROID_KEY_PASSWORD`
  Password for the key alias.

Optional but recommended:

- `ANDROID_KEYSTORE_FILE_NAME`
  Keystore filename to use in CI if the workflow should not hardcode one.

## Workstreams

## Workstream 1: Add release signing support to Gradle

Status: Done

Implementation notes:

- `release` signing is configured in [app/build.gradle.kts](/Users/atokarev/AndroidStudioProjects/LensesReminder/app/build.gradle.kts).
- Signing values now resolve from Gradle properties, Android Studio injected signing properties, environment variables, or an untracked `keystore.properties` file.
- The build fails early for release tasks when required signing inputs are missing.
- Local signing files are protected by ignore rules in [.gitignore](/Users/atokarev/AndroidStudioProjects/LensesReminder/.gitignore).

Plan:

- Extend the Android build configuration so the `release` build type uses a signing config when CI signing values are available.
- Keep the configuration compatible with local development, where secrets may not be present.

Tasks:

- Add a `signingConfigs.release` block in [app/build.gradle.kts](/Users/atokarev/AndroidStudioProjects/LensesReminder/app/build.gradle.kts).
- Load signing values from environment variables or Gradle properties.
- Point the signing config at a keystore file path created during CI.
- Attach the signing config to the `release` build type.
- Fail with a clear message when a release build is requested without required signing inputs.

Acceptance criteria:

- `:app:assembleRelease` produces a signed APK when valid signing inputs are supplied.
- The build fails clearly when signing inputs are missing or incomplete.
- No keystore file or passwords are committed to the repository.

## Workstream 2: Add GitHub Actions workflow

Plan:

- Create a workflow that runs on push and builds the signed release APK in GitHub-hosted CI.

Tasks:

- Add a workflow file under `.github/workflows/`.
- Trigger the workflow on pushes to the branches the team cares about, at minimum the default branch.
- Set up the required JDK version for the Android Gradle Plugin and project toolchain.
- Cache Gradle dependencies where practical.
- Reconstruct the keystore file from `ANDROID_KEYSTORE_BASE64`.
- Export signing values for Gradle without printing them.
- Run `./gradlew :app:assembleRelease`.
- Upload the resulting APK as a named artifact such as `lensesreminder-release-apk`.

Acceptance criteria:

- A push creates a workflow run automatically.
- The workflow produces a signed release APK artifact on success.
- The artifact can be downloaded directly from the workflow run page.
- The workflow logs do not expose signing secrets.

## Workstream 3: Document repository setup

Plan:

- Add repository-facing documentation so another maintainer can reproduce the setup.

Tasks:

- Document how to create or export the Android release keystore.
- Document how to Base64-encode the keystore for GitHub Secrets.
- Document which GitHub secrets must be added and what each one means.
- Document how to trigger the workflow and where to download the APK.
- Document the difference between a signed release artifact and a future store-publishing flow.

Acceptance criteria:

- A maintainer can configure secrets without reading workflow internals.
- A maintainer can locate the downloaded APK from GitHub after a push.
- The setup instructions are specific enough to be followed once without guesswork.

## Workstream 4: Validate the artifact path and signing result

Plan:

- Add enough verification to prove the workflow is producing the expected output rather than an unsigned or missing APK.

Tasks:

- Confirm the workflow uploads the actual `release` APK path from the Gradle output directory.
- Add a lightweight verification step if practical, such as checking that the APK file exists before upload.
- Optionally inspect APK signing with Android SDK tooling if the environment supports it cleanly.
- Make artifact naming stable so users know which file to download.

Acceptance criteria:

- The uploaded artifact corresponds to the current commit's `release` build output.
- The workflow fails rather than uploading an empty or wrong path.
- The artifact name and contents are predictable across runs.

## Delivery Tasks

Implementation should be broken into these concrete tasks:

1. Update [app/build.gradle.kts](/Users/atokarev/AndroidStudioProjects/LensesReminder/app/build.gradle.kts) to support injected release signing values.
2. Add `.github/workflows/build-release-apk.yml` to build on push and upload the APK artifact.
3. Add maintainer documentation describing keystore creation/export and GitHub Secrets setup.
4. Run or simulate the Gradle release build locally with non-committed signing inputs if available.
5. Push the workflow and validate one successful GitHub Actions run end to end.

## Risks

- Release signing cannot work until a real keystore exists.
- If the keystore alias or passwords are wrong, CI will fail during signing.
- If the workflow is configured for all pushes, every branch update will consume CI minutes.
- A signed APK built from every push is convenient for testing, but it is not the same as a governed release process.

## Open Decisions

These decisions should be made before implementation is finalized:

- Which branches should trigger signed release builds: all pushes, only `main`, or a restricted set?
- Should artifacts be retained only as workflow artifacts, or later also be attached to GitHub Releases?
- Should version code/version name remain manual, or should CI derive them from Git metadata in the future?
- Should the workflow also build `debug` for faster feedback, or only `release`?

## Suggested Next Step

Implement Workstreams 1 and 2 first, then add setup documentation immediately after the first successful CI run so the written instructions match the real workflow.
