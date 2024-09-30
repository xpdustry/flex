package com.xpdustry.flex;

import arc.util.*;
import mindustry.gen.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.*;

final class StandardFlexExtension implements FlexExtension {

  static final StandardFlexExtension INSTANCE = new StandardFlexExtension();

  private StandardFlexExtension() {
  }

  @Override
  public @Nullable String handleFlexString(final @NotNull String handler, @NotNull Player player) {
    return switch (handler) {
      case "xpdustry-flex:none" -> "";
      case "xpdustry-flex:name" -> player.name();
      case "xpdustry-flex:name-colored" -> "[#" + player.color().toString().toUpperCase() + "]" + player.name();
      case "xpdustry-flex:name-colorless" -> Strings.stripColors(player.name());
      case "xpdustry-flex:admin" -> player.admin() ? "" : null;
      default -> null;
    };
  }
}
