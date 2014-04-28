Muzei: androidwallpape.rs
=========================

Building
--------

Edit `local.properties` to contain not only the `sdk.dir` property, but also:

* `releasekey.file`
* `releasekey.alias`
* `releasekey.storepw`
* `releasekey.keypw`

These properties are used to configure the [signing configuration][signingConfiguration] for the
Android plugin when building the release APK.

[signingConfiguration]: http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Signing-Configurations

**Note:** The current version of this project is also intended to consume an [updated version of the
Muzei API package](https://github.com/slyfoxza/muzei), containing a fix not yet merged into Roman
Nurik's codebase, that should be installed into a local Maven repository. See the linked Github
repository for more information.