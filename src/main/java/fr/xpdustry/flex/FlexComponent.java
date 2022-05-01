package fr.xpdustry.flex;

import arc.util.*;
import org.jetbrains.annotations.*;

public final class FlexComponent {

  private final String handler;
  private final String template;

  FlexComponent(final @NotNull String handler, final @NotNull String template) {
    this.handler = handler;
    this.template = template;
  }

  public static @NotNull FlexComponent of(final @NotNull String handler, final @NotNull String template) {
    return new FlexComponent(handler, template);
  }

  public @NotNull String getHandler() {
    return handler;
  }

  public @NotNull String getTemplate() {
    return template;
  }

  @Override
  public @NotNull String toString() {
    return Strings.format("FlexComponent{handler='@', template='@'}", handler, template);
  }
}
