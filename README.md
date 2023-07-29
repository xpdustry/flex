# flex

[![Build status](https://github.com/Xpdustry/Flex/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Flex/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-6.0%20%7C%207.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://maven.xpdustry.com/api/badge/latest/legacy-releases/fr/xpdustry/flex?color=00FFFF&name=flex&prefix=v)](https://github.com/Xpdustry/Flex/releases)

## Description

**"Flex on your friends with awesome chat tags!"**

In all seriousness, this plugin allows you to manage the appearance of your chat messages and the name of the players in a very
simple way using a json file. It's even hot-reloadable with the fail-safe `flex-reload` command.

### Available configs

- Chat appearance configured by `./flex/chat-config.json`.

- Player name appearance configured by `./flex/name-config.json`.

- Join message appearance configured by `./flex/join-config.json`.

- Left message appearance configured by `./flex/left-config.json`.

### Usage

To format the player names or chat messages, Flex uses a list of components.

Each Flex component has a `handler` and `template` field.

- The `template` is a template string that can wrap an optional value represented by `%VALUE%` (there can be multiple `%VALUE%`).

- The `handler` is the name of the handler that will provide the value.

The plugin provides 5 simple handlers to get you started :

- `xpdustry-flex:none` returns an empty string, useful if you want to make your config files more readable.

- `xpdustry-flex:name` returns the name of the player.

- `xpdustry-flex:name-colored` returns the name of the player prefixed by its default color.

- `xpdustry-flex:name-colorless` returns the name of the player without colors.

- `xpdustry-flex:admin` returns an empty string if the player is an admin, otherwise null.

So for example, editing `./flex/chat-config.json` this way, you have a very minimal chat message :

```json
[
  {
    "handler": "xpdustry-flex:name",
    "template": "%VALUE%[white]: "
  }
]
```

![Example-1](.github/example-1.png)

For a more elegant look, I would suggest the name formatted with `%VALUE% [accent]>[white]`.

![Example-2](.github/example-2.png)

As you can see, possibilities are infinite !

### Adding extensions

Let's say you have a plugin that monitors the playtime of your players, and you want to add a name prefix for it using Flex, such as `<hours-played> player-name`.

First, add this in your `build.gradle` :

```gradle
repositories {
    maven { url = uri("https://maven.xpdustry.com/legacy-releases") }
}

dependencies {
    // Add "-SNAPSHOT" after the version if you are using the snapshot repository
    compileOnly("fr.xpdustry:flex:0.2.0")
}
```

It will pull the plugin from the Xpdustry maven repo.

Then, to add a new Flex extension, do the following :

```java
FlexPlugin.registerFlexExtension((handler, player) -> {
    if (handler.equals("your-plugin-internal-name:playtime")) {
        int playtime = YourPlugin.getPlaytime(player);
        return Integer.toString(playtime); // Returns a String
    } else {
        return null;
    }
})
```

You may be wondering why It can return null, it's because a component is ignored if all the queried extensions return null.

This way, you can chain components without worrying about template string leftovers.

Finally, add your new component at the beginning of the name Flex config in `./flex/name-config.json` :

```json
{
  "handler": "your-plugin-internal-name:playtime",
  "template": "[red]<[white]%VALUE%[red]>[white] "
}
```

We get a very nice result :

![Example-3](.github/example-3.png)

## Building

- `./gradlew jar` for a simple jar that contains only the plugin code.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

This plugin is compatible with V6.

If your version is below v136 and that one of your plugin uses Flex in some way, you will need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin) for the dependency resolution.

## Tips

- This plugin set `showConnectMessage` to false.

- This plugin uses chat filters to format chat messages, so a message recently sent by a player won't show up on his head. I will use the `ChatFormatter` class to avoid this issue when V7 will be fully released.
