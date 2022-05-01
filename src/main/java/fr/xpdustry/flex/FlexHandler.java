package fr.xpdustry.flex;

import org.jetbrains.annotations.*;

@FunctionalInterface
public interface FlexHandler {

  @Nullable String handle(final @NotNull String name, final @NotNull String uuid, final @NotNull String message);
}
