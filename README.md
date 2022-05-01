# Flex

[![Build status](https://github.com/Xpdustry/Flex/actions/workflows/build.yml/badge.svg?branch=master&event=push)](https://github.com/Xpdustry/Flex/actions/workflows/build.yml)
[![Mindustry 6.0 | 7.0 ](https://img.shields.io/badge/Mindustry-7.0-ffd37f)](https://github.com/Anuken/Mindustry/releases)
[![Xpdustry latest](https://repo.xpdustry.fr/api/badge/latest/releases/fr/xpdustry/flex?color=00FFFF&name=Flex&prefix=v)](https://github.com/Xpdustry/Flex/releases)

## Description

**"Flex on your friends with awesome chat tags!"**

In all seriousness, this plugin allows you to manage the appearance of your chat messages in a very
simple way using a json file. It's even hot-reloadable with the fail-safe `flex-reload` command.

### Usage

Edit the list of Flex component in the `./flex-config.json` file.

Each Flex component has a `handler` and `template` field.

The `template` is a template string that can wrap an optional value represented by `%VALUE%` (there can be multiple `%VALUE%`).

The `handler` is the name of the handler that will provide the value.

The plugin provides 4 simple handlers to get you started :

- `xpdustry-flex:none` returns an empty string, useful if you want to make your config file more
  readable.

- `xpdustry-flex:name` returns the name of the player.

- `xpdustry-flex:name-colorless` returns the name of the player without colors.

- `xpdustry-flex:message` returns the message of the player.

So for example, if you want chat messages with the bare minimum :

```json
[
  {
    "handler": "xpdustry-flex:name",
    "template": "%VALUE%[white]: "
  },
  {
    "handler": "xpdustry-flex:message",
    "template": "%VALUE%"
  }
]
```

![Example-1](.github/example-1.png)

For a more elegant look, I would suggest the name formatted with `%VALUE% [accent]>[white]`.

![Example-2](.github/example-2.png)

As you can see, possibilities are infinite !

### Adding custom handlers

Let's say you want to add an admin prefix with a plugin.

First, add this in your `build.gradle` :

```gradle
repositories {
    // Replace with "https://repo.xpdustry.fr/snapshots" if you want to use snapshots
    maven { url = uri("https://repo.xpdustry.fr/releases") }
}

dependencies {
    // Add "-SNAPSHOT" after the version if you are using the snapshot repository
    compileOnly("fr.xpdustry:flex:0.1.0")
}
```

It will pull the plugin from the Xpdustry maven repo.

Then, to add a new Flex handler, do the following :

```java
FlexPlugin plugin = FlexPlugin.getInstance(); // Get the plugin
FlexHandlerManager manager = plugin.getFlexHandlerManager(); // Get the Flex handler manager
// Register a new handler
manager.registerFlexHandler("your-plugin-internal-name:admin-prefix", (name, uuid, message) -> {
    if (Vars.netServer.admins.getInfo(uuid).admin) {
        return "";
    } else {
        return null;
    }
})
```

You may be wondering why I return an empty string or null, it's because a component is ignored if
the handler returns null.
This way, you can chain components without worrying about template string leftovers.

Finally, add your new component at the beginning of the config file :

```json
{
  "handler": "your-plugin-internal-name:admin-prefix",
  "template": "[red]<ADMIN>[] "
}
```

Using the config of the second example, we get a very nice result :

![Example-3](.github/example-3.png)

## Building

- `./gradlew jar` for a simple jar that contains only the plugin code.

- `./gradlew shadowJar` for a fatJar that contains the plugin and its dependencies (use this for
  your server).

## Testing

- `./gradlew runMindustryClient`: Run Mindustry in desktop with the plugin.

- `./gradlew runMindustryServer`: Run Mindustry in a server with the plugin.

## Running

This plugin is compatible with v135+.

If your version is v135 and one of your plugin uses Flex in some way, you will
need [mod-loader](https://github.com/Xpdustry/ModLoaderPlugin) for the dependency resolution.

## Tips

- List the available Flex handlers with the `flex-handlers` command.

- Reset your components to the default Mindustry chat format with the `flex-reset` command.

- Gimme GitHub star pls.
