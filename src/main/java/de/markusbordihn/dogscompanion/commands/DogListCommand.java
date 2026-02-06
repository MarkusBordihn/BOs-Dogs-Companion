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

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    if (!context.isPlayer()) {
      context.sendMessage(
          Message.raw("This command can only be used by players").color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    if (playerUuid == null) {
      context.sendMessage(Message.raw("Unable to get player UUID").color(Constants.COLOR_ERROR));
      return;
    }

    DogsManager dogsManager = DogsManager.getInstance();
    Collection<DogDataEntry> playerDogs = dogsManager.getDogDataByOwner(playerUuid, store);

    int currentCount = playerDogs.size();
    int limit = getDogLimit(context);
    String limitText = limit == -1 ? "unlimited" : String.valueOf(limit);

    if (playerDogs.isEmpty()) {
      context.sendMessage(Message.raw("=== Your Dogs (0/" + limitText + ") ===").color("#FFD700"));
      context.sendMessage(Message.raw("You don't have any dogs yet.").color(Constants.COLOR_GRAY));
      context.sendMessage(
          Message.raw("Tip: Tame a wild dog by giving it bones or meat!")
              .color(Constants.COLOR_INFO));
      return;
    }

    context.sendMessage(
        Message.raw("=== Your Dogs (" + currentCount + "/" + limitText + ") ===").color("#FFD700"));

    int index = 1;
    for (DogDataEntry dogData : playerDogs) {
      String dogName =
          dogData.name() != null && !dogData.name().isEmpty() ? dogData.name() : "Unnamed Dog";
      String statusIndicator =
          dogsManager.getDogByUuid(dogData.uuid(), store) == null
              ? "[DESPAWNED]"
              : dogsManager.isDogAliveInWorld(dogData.uuid(), store) ? "[ALIVE]" : "[DEAD]";
      String positionStr =
          dogData.position() != null
              ? String.format(
                  "(%d,%d,%d)", dogData.position().x, dogData.position().y, dogData.position().z)
              : "(no position)";
      context.sendMessage(
          Message.raw(index + ". " + dogName + " " + statusIndicator + " " + positionStr)
              .color("#00FFFF"));
      context.sendMessage(
          Message.raw("   Type: " + dogData.dogType() + " | State: " + dogData.state())
              .color(Constants.COLOR_GRAY));
      context.sendMessage(
          Message.raw("   UUID: " + dogData.uuid().toString().substring(0, 8) + "...")
              .color(Constants.COLOR_GRAY));

      index++;
    }

    context.sendMessage(Message.raw(""));
    context.sendMessage(
        Message.raw("Tip: Use /dog info while looking at a dog for more details")
            .color(Constants.COLOR_INFO));
  }
}
