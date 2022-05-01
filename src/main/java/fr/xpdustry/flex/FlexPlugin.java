package fr.xpdustry.flex;

import arc.util.*;
import com.google.gson.*;
import io.leangen.geantyref.*;
import java.util.*;
import mindustry.*;
import mindustry.mod.*;
import net.mindustry_ddns.filestore.*;
import net.mindustry_ddns.filestore.serial.*;
import org.jetbrains.annotations.*;

@SuppressWarnings("unused")
public class FlexPlugin extends Plugin implements FlexHandlerManager, FlexChatFormatter {

  private static final List<FlexComponent> DEFAULT_COMPONENTS = List.of(
    FlexComponent.of("xpdustry-flex:name", "[coral][[%VALUE%[coral]]:[white] "),
    FlexComponent.of("xpdustry-flex:message", "%VALUE%")
  );

  @SuppressWarnings("NullAway.Init")
  private static FlexPlugin INSTANCE;
  private final Map<String, FlexHandler> handlers = new HashMap<>();
  private final Store<List<FlexComponent>> components = FileStore.of(
    "./flex-config.json",
    Serializers.gson(new GsonBuilder().setPrettyPrinting().create()),
    new TypeToken<>() {},
    DEFAULT_COMPONENTS
  );

  public FlexPlugin() {
    INSTANCE = this;
  }

  public static @NotNull FlexPlugin getInstance() {
    return Objects.requireNonNull(INSTANCE, ""
      + "FlexPlugin is null, did you call getInstance() before init() ? "
      + "Or did you forgot to add the dependency in your plugin.json ?"
    );
  }

  public @NotNull FlexChatFormatter getFlexChatFormatter() {
    return this;
  }

  public @NotNull FlexHandlerManager getFlexHandlerManager() {
    return this;
  }

  /**
   * This method is called when game initializes.
   */
  @Override
  public void init() {
    Vars.netServer.chatFormatter = this;

    registerFlexHandler("xpdustry-flex:none", (name, uuid, message) -> "");
    registerFlexHandler("xpdustry-flex:name", (name, uuid, message) -> name);
    registerFlexHandler("xpdustry-flex:name-colorless", (name, uuid, message) -> Strings.stripColors(name));
    registerFlexHandler("xpdustry-flex:message", (name, uuid, message) -> message);

    reloadComponents();
  }

  /**
   * This method is called when the game register the server-side commands.
   */
  @Override
  public void registerServerCommands(final @NotNull CommandHandler handler) {
    handler.register("flex-reload", "Reload the Flex config", args -> {
      reloadComponents();
    });

    handler.register("flex-handlers", "List the available Flex handlers.", args -> {
      if (handlers.isEmpty()) {
        Log.info("None");
      } else {
        final var builder = new StringBuilder();
        builder.append("\nFlex handlers:");
        handlers.keySet().stream().sorted().forEach(k -> builder.append("\n- ").append(k));
        Log.info(builder.toString());
      }
    });

    handler.register("flex-reset", "Reset the Flex components to the default. ATTENTION, IT ALSO RESETS THE CONFIG FILE!!!", args -> {
      components.set(DEFAULT_COMPONENTS);
      components.save();
      Log.info("The Flex components have been reset.");
    });
  }

  @Override
  public @NotNull Map<String, FlexHandler> getHandlers() {
    return Collections.unmodifiableMap(handlers);
  }

  @Override
  public void registerFlexHandler(final @NotNull String name, final @NotNull FlexHandler handler) {
    handlers.put(name, handler);
  }

  @SuppressWarnings("NullAway")
  @Override
  public @NotNull String format(@NotNull String name, @NotNull String uuid, @NotNull String message) {
    final var builder = new StringBuilder();
    for (final var component : components.get()) {
      final var handler = handlers.get(component.getHandler());
      final var value = handler.handle(name, uuid, message);
      if (value != null) {
        builder.append(component.getTemplate().replaceAll("%VALUE%", value));
      }
    }
    return builder.toString();
  }

  private void reloadComponents() {
    final var old = components.get();

    try {
      components.load();
    } catch (final Exception e) {
      Log.err("An error occurred while loading the Flex components, fallback to previous.", e);
      return;
    }

    final var missing = components.get().stream()
      .map(FlexComponent::getHandler)
      .filter(handler -> !handlers.containsKey(handler))
      .toList();

    if (!missing.isEmpty()) {
      components.set(old);
      Log.err("Missing Flex handlers @ for the given components, fallback to default.", missing);
    } else {
      Log.info("Successfully loaded the Flex components.");
    }
  }
}
