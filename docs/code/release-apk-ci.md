# Release APK CI Setup

This document explains how to configure GitHub Actions so every push to `main` can produce a signed release APK artifact.

The workflow file is:

- `.github/workflows/build-release-apk.yml`

The workflow currently:

- runs on `push` to `main`
- can also be started manually with `workflow_dispatch`
- rebuilds the release keystore from GitHub Actions secrets
- runs `./gradlew :app:assembleRelease`
- uploads `app/build/outputs/apk/release/app-release.apk` as the `lensesreminder-release-apk` artifact
- publishes the same APK to a rolling GitHub prerelease named `Latest Main APK`

## Prerequisites

You need a real Android release keystore before CI signing can work.

Do not commit any of these to the repository:

- keystore files such as `.jks` or `.keystore`
- `keystore.properties`
- keystore passwords

These paths and file types are already ignored by `.gitignore`.

## Option 1: Use an existing release keystore

If the app already has a release keystore, collect:

- the keystore file
- the keystore password
- the key alias
- the key password

You will use those values as GitHub Actions secrets.

## Option 2: Create a new release keystore

If no keystore exists yet, create one locally with `keytool`:

```bash
keytool -genkeypair \
  -v \
  -keystore release-keystore.jks \
  -alias lensesreminder \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

Choose strong passwords and store them in a password manager or other secure location.

Important: if you lose the release keystore, you cannot update an app already distributed under that signing identity.

## Base64-encode the keystore

GitHub Actions secrets are text values, so the binary keystore must be Base64-encoded first.

On macOS:

```bash
base64 -i release-keystore.jks | pbcopy
```

On Linux:

```bash
base64 -w 0 release-keystore.jks
```

Copy the full Base64 output without adding extra spaces or line breaks.

## Add GitHub repository secrets

In GitHub, open:

`Repository -> Settings -> Secrets and variables -> Actions`

Add these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: Base64-encoded contents of the keystore file
- `ANDROID_KEYSTORE_PASSWORD`: password for the keystore
- `ANDROID_KEY_ALIAS`: alias of the signing key
- `ANDROID_KEY_PASSWORD`: password for the key alias

Optional:

- `ANDROID_KEYSTORE_FILE_NAME`: filename used when the workflow recreates the keystore in CI. If omitted, the workflow defaults to `release-keystore.jks`.

## How local release signing works

The Android build is configured to resolve release signing values from several sources:

- Gradle properties
- Android Studio injected signing properties
- environment variables
- an untracked local `keystore.properties` file

In GitHub Actions, the workflow reconstructs the keystore into a temporary file and exports:

- `ANDROID_KEYSTORE_PATH`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

That is enough for `:app:assembleRelease` to produce a signed APK.

## Trigger the workflow

After the secrets are added:

1. Push a commit to `main`, or
2. Open the Actions tab in GitHub and run `Build Release APK` manually

The workflow will fail early if required signing secrets are missing.

## Download the signed APK

### Option 1: GitHub Releases

For non-technical users, this is the main download path:

1. Open the repository `Releases` page
2. Open `Latest Main APK`
3. Download the attached `app-release.apk`

This prerelease is updated from the latest successful `main` build.

### Option 2: Workflow artifact

After a successful workflow run:

1. Open the run in the GitHub Actions tab
2. Find the `Artifacts` section
3. Download `lensesreminder-release-apk`
4. Extract the archive to get `app-release.apk`

## Local release build example

If you want to validate release signing outside GitHub Actions, use environment variables:

```bash
export ANDROID_KEYSTORE_PATH=/absolute/path/to/release-keystore.jks
export ANDROID_KEYSTORE_PASSWORD='your-keystore-password'
export ANDROID_KEY_ALIAS='your-key-alias'
export ANDROID_KEY_PASSWORD='your-key-password'

./gradlew :app:assembleRelease
```

## Scope and limitations

This workflow is intended for downloadable testing artifacts, not store publishing.

It does not currently:

- publish to Google Play
- derive version names or version codes from Git metadata
- build `debug` artifacts as part of the release workflow
