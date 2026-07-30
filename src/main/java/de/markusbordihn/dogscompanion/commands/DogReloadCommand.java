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

package de.markusbordihn.dogscompanion.commands;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.manager.DogsNamesManager;
import de.markusbordihn.dogscompanion.permission.PermissionManager;
import java.util.logging.Level;
import javax.annotation.Nonnull;

final class DogReloadCommand extends DogCommand {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final String RELOAD_PERMISSION = "markusbordihn.dogs.command.dog.reload";

  public DogReloadCommand() {
    super("reload", "Reloads the dogs configuration files");
  }

  @Override
  protected boolean requiresOp() {
    return true;
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {

    if (!PermissionManager.hasPermission(context, RELOAD_PERMISSION)) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_permission")
              .color(Constants.COLOR_ERROR));
      return;
    }

    try {
      LOGGER.at(Level.INFO).log("Reloading dogs configuration...");

      DogsNamesManager.reload();

      LOGGER.at(Level.INFO).log("Dogs configuration reloaded successfully");
      context.sendMessage(
          Message.translation("dogs_companion.commands.reload.success")
              .color(Constants.COLOR_SUCCESS));
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).withCause(e).log("Failed to reload dogs configuration");
      context.sendMessage(
          Message.translation("dogs_companion.commands.reload.failed")
              .color(Constants.COLOR_ERROR));
    }
  }
}
