package fr.xpdustry.flex;

import java.util.*;
import org.jetbrains.annotations.*;

public interface FlexHandlerManager {

  @NotNull Map<String, FlexHandler> getHandlers();

  void registerFlexHandler(final @NotNull String name, final @NotNull FlexHandler handler);
}
