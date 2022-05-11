package fr.xpdustry.flex;

import arc.util.*;
import java.io.*;
import org.jetbrains.annotations.*;

public final class FlexComponent implements Serializable {

  @Serial
  private static final long serialVersionUID = 5679484897360724417L;

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
