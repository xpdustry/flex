# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/),
and this project adheres to [Semantic Versioning](http://semver.org/).

## v1.2.0 - 2025-02-03

### Features

- Flex translator has now a dedicated module! Get it at `com.xpdustry:flex-translator:VERSION`,
  the plugin API is still located at `com.xpdustry:flex:VERSION`.
- Added `Translator#translateDetecting` and deprecate `Translator#translate`.
- Added bulk translation methods for `Translator`.
- Made the `PlayerNameHook` config re-loadable at runtime.
- Added the `hooks.name.maximum-name-size` config for `PlayerNameHook`.
  If a name goes beyond that size, it will be reset to the original player name.

### Bugfixes

- Fixed rolling translator not rolling correctly.
- Fixed the tests not running correctly in the CI, leading to unexpected behavior.

## v1.1.0 - 2025-01-09

### Features

- Added option to toggle foo client compatibility in the message pipeline.
- Made LibreTranslate api key optional, in order to support instances with free access.

### Bugfixes

- Fixed placeholder pipeline replacements crashing when containing meta-characters.
- Fixed LibreTranslate translator crashing when supported languages do not specify their supported target languages.
- Fixed foo client compatibility applying to vanilla clients instead of foo clients.

## v1.0.0 - 2024-12-17

Initial stable release. Check out the [wiki](https://github.com/xpdustry/flex/wiki) the usage guide and features list.

## v1.0.0-rc.1 - 2024-12-14

Initial release candidate of flex given how stable it has been lately and how nicely it integrates with imperium.
