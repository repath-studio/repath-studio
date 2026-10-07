# Release process

Those are the steps to release a new desktop and mobile version. The web
application follows the `main` branch, and always includes the latest changes.
We usually cut a new release biweekly, but critical fixes should be releases
as soon as possible.

## Update version

- Checkout to main branch `git checkout main`.
- Update the `version` top level key of `package.json`.
- Run `npm install` to also update `package-lock.json`.
- Update the `versionName` and increase the `versionCode` on `android/app/build.gradle`.

## Update changelog

Update `CHANGELOG.md` by following the preexisting format.
Also see [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Git commit, tag and push

- Commit the changes: `git commit -m "release v0.4.21"`
- Tag the release: `git tag v0.4.21`
- Push the changes and tags: `git push && git push --tags`

## Publish the release

When the `Desktop application` and `Android application` action is successfully
complete, go to the [releases](https://github.com/repath-studio/repath-studio/releases)
and edit the new draft release.

- Copy the release notes from `CHANGELOG.md`.
- Select the `Latest` release label.
- Check the `Create a discussion for this release` under the `Announcements` category.
- Publish the release.
