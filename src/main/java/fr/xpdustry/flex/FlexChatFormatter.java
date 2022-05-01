package fr.xpdustry.flex;

import mindustry.core.NetServer.*;
import mindustry.gen.*;
import org.jetbrains.annotations.*;

@FunctionalInterface
public interface FlexChatFormatter extends ChatFormatter {

  @NotNull String format(final @NotNull String name, final @NotNull String uuid, final @NotNull String message);

  @Override
  default String format(final @NotNull Player player, final @NotNull String message) {
    return format(player.coloredName(), player.uuid(), message);
  }
}
