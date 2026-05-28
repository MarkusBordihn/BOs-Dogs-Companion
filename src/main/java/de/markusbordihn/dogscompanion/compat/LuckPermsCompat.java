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

package de.markusbordihn.dogscompanion.compat;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import java.util.logging.Level;

public class LuckPermsCompat {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final PluginIdentifier LUCKPERMS_ID =
      new PluginIdentifier("LuckPerms", "LuckPerms");

  private static boolean isAvailable = false;
  private static boolean isDetected = false;

  private LuckPermsCompat() {}

  public static void detect() {
    if (isDetected) {
      return;
    }
    isDetected = true;

    if (PluginManager.get().getAvailablePlugins().containsKey(LUCKPERMS_ID)) {
      isAvailable = true;
      logLuckPermsEnabled();
    }
  }

  public static boolean isAvailable() {
    return isAvailable;
  }

  private static void logLuckPermsEnabled() {
    LOGGER.at(Level.INFO).log(
        "===============================================================================");
    LOGGER.at(Level.INFO).log(
        "| [Dogs Companion] LuckPerms detected! Permission checks are ENABLED.        |");
    LOGGER.at(Level.INFO).log(
        "|=============================================================================|");
    LOGGER.at(Level.INFO).log(
        "| IMPORTANT: Use wildcard permission to grant access to /dog command         |");
    LOGGER.at(Level.INFO).log(
        "| and all sub-commands (/dog list, /dog info, etc.)                          |");
    LOGGER.at(Level.INFO).log(
        "|                                                                             |");
    LOGGER.at(Level.INFO).log(
        "| Recommended: Grant wildcard to player or group:                            |");
    LOGGER.at(Level.INFO).log(
        "|   /lp user <player> permission set markusbordihn.dogs.command.dog.* true   |");
    LOGGER.at(Level.INFO).log(
        "|   /lp group default permission set markusbordihn.dogs.command.dog.* true   |");
    LOGGER.at(Level.INFO).log(
        "|                                                                             |");
    LOGGER.at(Level.INFO).log(
        "| Available permissions:                                                     |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog          - Base /dog command            |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.*        - Wildcard: all sub-commands   |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.attack   - /dog attack                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.despawn  - /dog despawn (admin)         |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.follow   - /dog follow                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.info     - /dog info                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.list     - /dog list                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.name     - /dog name                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.owner    - /dog owner (admin)           |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.play     - /dog play                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.release  - /dog release                 |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.search   - /dog search                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.sit      - /dog sit                     |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.sleep    - /dog sleep                   |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.spawn    - /dog spawn (admin)           |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.wait     - /dog wait                    |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.command.dog.wander   - /dog wander                  |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.admin.bypass         - Bypass ownership checks      |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.limit.unlimited      - Unlimited dog ownership      |");
    LOGGER.at(Level.INFO).log(
        "|   - markusbordihn.dogs.limit.{number}       - Limit to N dogs (e.g. .8-32) |");
    LOGGER.at(Level.INFO).log(
        "===============================================================================");
  }
}
