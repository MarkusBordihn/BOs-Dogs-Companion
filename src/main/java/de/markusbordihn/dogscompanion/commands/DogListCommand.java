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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.data.DogDataEntry;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;

final class DogListCommand extends DogCommand {

  public DogListCommand() {
    super("list", "Lists all your dogs with name, UUID and position");
  }

  @Nonnull
  private static String getDogName(@Nonnull DogDataEntry dogData) {
    if (dogData.name() != null && !dogData.name().isEmpty()) {
      return dogData.name();
    }

    return Message.translation("dogs_companion.commands.list.unnamed").getRawText();
  }

  @Nonnull
  private static String getStatusIndicator(
      @Nonnull DogsManager dogsManager,
      @Nonnull DogDataEntry dogData,
      @Nonnull Store<EntityStore> store) {
    String statusKey =
        dogsManager.getDogByUuid(dogData.uuid(), store) == null
            ? "despawned"
            : dogsManager.isDogAliveInWorld(dogData.uuid(), store) ? "alive" : "dead";

    return Message.translation("dogs_companion.commands.list.status." + statusKey).getRawText();
  }

  @Nonnull
  private static String getPositionText(@Nonnull DogDataEntry dogData) {
    if (dogData.position() == null) {
      return Message.translation("dogs_companion.commands.list.no_position").getRawText();
    }

    return String.format(
        "(%d,%d,%d)", dogData.position().x, dogData.position().y, dogData.position().z);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    if (!context.isPlayer()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.player_only")
              .color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    if (playerUuid == null) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_uuid")
              .color(Constants.COLOR_ERROR));
      return;
    }

    DogsManager dogsManager = DogsManager.getInstance();
    Collection<DogDataEntry> playerDogs = dogsManager.getDogDataByOwner(playerUuid, store);

    int limit = getDogLimit(context);
    String limitText =
        limit == -1
            ? Message.translation("dogs_companion.commands.list.unlimited").getRawText()
            : String.valueOf(limit);

    context.sendMessage(
        Message.translation("dogs_companion.commands.list.header")
            .param("count", String.valueOf(playerDogs.size()))
            .param("limit", limitText)
            .color(Constants.COLOR_GOLD));

    if (playerDogs.isEmpty()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.list.empty").color(Constants.COLOR_GRAY));
      context.sendMessage(
          Message.translation("dogs_companion.commands.list.empty.hint")
              .color(Constants.COLOR_INFO));
      return;
    }

    int index = 1;
    for (DogDataEntry dogData : playerDogs) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.list.entry")
              .param("index", String.valueOf(index))
              .param("name", getDogName(dogData))
              .param("status", getStatusIndicator(dogsManager, dogData, store))
              .param("position", getPositionText(dogData))
              .color(Constants.COLOR_CYAN));
      context.sendMessage(
          Message.translation("dogs_companion.commands.list.entry.details")
              .param("type", String.valueOf(dogData.dogType()))
              .param("state", String.valueOf(dogData.state()))
              .color(Constants.COLOR_GRAY));
      context.sendMessage(
          Message.translation("dogs_companion.commands.list.entry.uuid")
              .param("uuid", dogData.uuid().toString().substring(0, 8) + "...")
              .color(Constants.COLOR_GRAY));

      index++;
    }

    context.sendMessage(
        Message.translation("dogs_companion.commands.list.hint").color(Constants.COLOR_INFO));
  }
}
