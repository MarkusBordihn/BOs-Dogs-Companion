/*
 * Copyright 2026 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.dogscompanion.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DogNameplateUtils {

  public static final String SYMBOL_ATTACKING = "[ATK]";
  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final String SYMBOL_PASSIVE = "";
  private static final String SYMBOL_DEFENSE = "[DEF]";
  private static final String SYMBOL_OFFENSE = "[OFF]";
  private static final String SYMBOL_SITTING = "[SIT]";
  private static final String SYMBOL_SLEEPING = "[ZzZ]";
  private static final String SYMBOL_PLAYING = "[PLAY]";
  private static final String SYMBOL_WAITING = "[WAIT]";
  private static final String SYMBOL_WANDERING = "[---]";
  private static final String SYMBOL_SEARCHING = "[SEEK]";

  public static void updateNameplateWithState(
      @Nonnull Ref<EntityStore> dogRef,
      @Nullable String baseName,
      @Nonnull Store<EntityStore> store) {

    if (baseName == null || baseName.isEmpty()) {
      baseName = "Dog";
    }

    DogStateComponent stateComponent =
        store.getComponent(dogRef, DogStateComponent.getComponentType());
    if (stateComponent == null) {
      LOGGER.at(Level.WARNING).log("updateNameplateWithState: No state component found");
      return;
    }

    DogState state = stateComponent.getState();
    String stateSymbol = getStateSymbol(state);
    String nameplateText;

    if (stateSymbol.isEmpty()) {
      nameplateText = baseName;
    } else {
      nameplateText = stateSymbol + " " + baseName;
    }

    LOGGER.at(Level.FINE).log(
        "Updating nameplate: state=%s, symbol=%s, text=%s", state, stateSymbol, nameplateText);

    Nameplate nameplate = store.ensureAndGetComponent(dogRef, Nameplate.getComponentType());
    String oldText = nameplate.getText();
    nameplate.setText(nameplateText);

    LOGGER.at(Level.FINE).log("Nameplate updated: '%s' -> '%s'", oldText, nameplateText);
  }

  @Nonnull
  public static String getStateSymbol(@Nonnull DogState state) {
    return switch (state) {
      case DEFENSE -> SYMBOL_DEFENSE;
      case OFFENSE -> SYMBOL_OFFENSE;
      case SITTING -> SYMBOL_SITTING;
      case ATTACKING, STRIKING -> SYMBOL_ATTACKING;
      case SLEEPING -> SYMBOL_SLEEPING;
      case PLAYING -> SYMBOL_PLAYING;
      case WAITING -> SYMBOL_WAITING;
      case WANDERING -> SYMBOL_WANDERING;
      case SEARCHING -> SYMBOL_SEARCHING;
      case FOLLOWING -> SYMBOL_PASSIVE;
    };
  }

  @Nonnull
  public static String getBaseNameFromNameplate(@Nonnull String nameplateText) {
    if (nameplateText.startsWith("[") && nameplateText.contains("] ")) {
      int endBracket = nameplateText.indexOf(']');
      if (endBracket > 0 && endBracket < nameplateText.length() - 1) {
        return nameplateText.substring(endBracket + 2); // Skip "] "
      }
    }
    return nameplateText;
  }

  @Nonnull
  public static String getSymbolFromNameplate(@Nonnull String nameplateText) {
    if (nameplateText.startsWith("[") && nameplateText.contains("]")) {
      int endBracket = nameplateText.indexOf(']');
      if (endBracket > 0) {
        return nameplateText.substring(0, endBracket + 1);
      }
    }
    return "";
  }
}
