package fr.xpdustry.flex;

import arc.*;
import arc.files.*;
import arc.util.*;
import com.google.gson.*;
import com.google.gson.reflect.*;
import java.lang.reflect.*;
import java.util.*;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.net.Administration.*;
import org.jetbrains.annotations.*;

@SuppressWarnings("unused")
public final class FlexPlugin extends Plugin {

  private static final Fi FLEX_DIRECTORY = new Fi("./flex");

  private static final Type FLEX_COMPONENT_LIST_TYPE = TypeToken.getParameterized(List.class, FlexComponent.class).getType();

  private static final List<FlexComponent> DEFAULT_NAME_COMPONENTS = List.of(
    FlexComponent.of("xpdustry-flex:name-colored", "%VALUE%")
  );

  private static final List<FlexComponent> DEFAULT_CHAT_COMPONENTS = List.of(
    FlexComponent.of("xpdustry-flex:name-colored", "[coral][[%VALUE%[coral]]:[white] ")
  );

  private static final List<FlexComponent> DEFAULT_JOIN_COMPONENTS = List.of(
    FlexComponent.of("xpdustry-flex:name", "[accent]%VALUE%[accent] has connected.")
  );

  private static final List<FlexComponent> DEFAULT_LEFT_COMPONENTS = List.of(
    FlexComponent.of("xpdustry-flex:name", "[accent]%VALUE%[accent] has disconnected.")
  );

  @SuppressWarnings("NullAway.Init")
  private static FlexPlugin INSTANCE;

  private final Map<String, List<FlexComponent>> configuration = new HashMap<>(
    Map.of(
      "name", DEFAULT_NAME_COMPONENTS,
      "chat", DEFAULT_CHAT_COMPONENTS,
      "join", DEFAULT_JOIN_COMPONENTS,
      "left", DEFAULT_LEFT_COMPONENTS
    )
  );

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private final ArrayDeque<FlexExtension> extensions = new ArrayDeque<>();

  public FlexPlugin() {
    if (INSTANCE != null) {
      throw new IllegalStateException("Instanced FlexPlugin twice.");
    }
    INSTANCE = this;
  }

  public static Iterator<FlexExtension> getExtensions() {
    return INSTANCE.extensions.descendingIterator();
  }

  public static void registerFlexExtension(final @NotNull FlexExtension extension) {
    INSTANCE.extensions.add(extension);
  }

  @Override
  public void init() {
    FLEX_DIRECTORY.mkdirs();
    reloadAllComponents();
    registerFlexExtension(StandardFlexExtension.INSTANCE);
    Config.showConnectMessages.set(false);

    Events.on(PlayerJoin.class, e -> {
      e.player.name(formatFlexString("name", e.player));

      final var join = formatFlexString("join", e.player);
      if (!join.isBlank()) {
        Call.sendMessage(join);
        if (!Config.showConnectMessages.bool()) {
          Log.info("@ has connected.", Strings.stripColors(e.player.name()));
        }
      }
    });

    Vars.netServer.admins.addChatFilter((player, message) -> {
      Call.sendMessage(formatFlexString("chat", player) + message);
      if (!Config.showConnectMessages.bool()) {
        Log.info("&fi@: @", "&lc" + Strings.stripColors(player.name()), "&lw" + message);
      }
      return null;
    });

    Events.on(PlayerLeave.class, e -> {
      final var left = formatFlexString("left", e.player);
      if (!left.isBlank()) {
        Call.sendMessage(left);
        if (!Config.showConnectMessages.bool()) {
          Log.info("&lb@&fi&lk has disconnected. &fi&lk[&lb@&fi&lk]", Strings.stripColors(e.player.name()), e.player.uuid());
        }
      }
    });
  }

  @Override
  public void registerServerCommands(final @NotNull CommandHandler handler) {
    handler.register("flex-reload", "<name/chat/join/left/all>", "Reload a Flex config", args -> {
      switch (args[0]) {
        case "name", "chat", "join", "left" -> reloadComponents(args[0]);
        case "all" -> reloadAllComponents();
        default -> Log.info("The parameter @ is not recognized.", args[0]);
      }
    });
  }

  private @NotNull String formatFlexString(final @NotNull List<FlexComponent> components, final @NotNull Player player) {
    final var builder = new StringBuilder();
    for (final var component : components) {
      final var iterator = getExtensions();
      String value = null;
      while (value == null && iterator.hasNext()) {
        final var extension = iterator.next();
        value = extension.handleFlexString(component.getHandler(), player);
      }
      if (value != null) {
        builder.append(component.getTemplate().replaceAll("%VALUE%", value));
      }
    }
    return builder.toString();
  }

  private @NotNull String formatFlexString(final @NotNull String name, final @NotNull Player player) {
    if (!configuration.containsKey(name)) {
      throw new IllegalStateException("This ain't supposed to happen my friend...");
    }
    return formatFlexString(configuration.get(name), player);
  }

  private void reloadAllComponents() {
    reloadComponents("name");
    reloadComponents("chat");
    reloadComponents("join");
    reloadComponents("left");
  }

  private void reloadComponents(final @NotNull String name) {
    if (!configuration.containsKey(name)) {
      throw new IllegalStateException("This ain't supposed to happen my friend...");
    }

    final var file = FLEX_DIRECTORY.child(name + "-config.json");

    if (!file.exists()) {
      try (final var writer = file.writer(false)) {
        gson.toJson(configuration.get(name), writer);
        Log.info("Successfully created the default @ Flex components.", name);
      } catch (final Exception e) {
        throw new RuntimeException("An error occurred while creating the default " + name + " Flex components.", e);
      }
    } else {
      try (final var reader = FLEX_DIRECTORY.child(name + "-config.json").reader()) {
        configuration.put(name, gson.fromJson(reader, FLEX_COMPONENT_LIST_TYPE));
        Log.info("Successfully loaded the @ Flex components.", name);
      } catch (final Exception e) {
        Log.err("An error occurred while loading the " + name + " Flex components, fallback to previous.", e);
      }
    }
  }
}
