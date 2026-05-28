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

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.data.DogDataEntry;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;

final class DogInfoCommand extends DogCommand {
  @Nonnull private final EntityWrappedArg entityArg;

  public DogInfoCommand() {
    super("info", "Shows information about a dog NPC");
    this.entityArg = this.withOptionalArg("entity", "The dog entity to check", ArgTypes.ENTITY_ID);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {

    Optional<Ref<EntityStore>> entityOpt = getEntityFromArgument(this.entityArg, store, context);
    if (entityOpt.isEmpty()) {
      context.sendMessage(Message.raw("No entity in view.").color(Constants.COLOR_ERROR));
      context.sendMessage(
          Message.raw("Look at a dog and use: /dog info").color(Constants.COLOR_GRAY));
      return;
    }

    Ref<EntityStore> entityRef = entityOpt.get();

    DogsManager dogsManager = DogsManager.getInstance();
    UUID dogUuid = dogsManager.getUuid(entityRef, store);
    if (dogUuid == null) {
      context.sendMessage(
          Message.raw("Unable to get dog UUID (not a dog entity?)").color(Constants.COLOR_ERROR));
      return;
    }

    DogDataEntry dogData = dogsManager.getDogData(dogUuid, store);
    if (dogData == null) {
      context.sendMessage(
          Message.raw("Dog data not found for UUID: " + dogUuid).color(Constants.COLOR_ERROR));
      return;
    }

    UUID playerUuid = context.sender().getUuid();
    boolean isOwner = playerUuid != null && playerUuid.equals(dogData.ownerUuid());

    if (dogData.name() != null && !dogData.name().isEmpty()) {
      context.sendMessage(Message.raw("Name: " + dogData.name()).color("#FFD700"));
    } else {
      context.sendMessage(Message.raw("Name: (unnamed)").color(Constants.COLOR_GRAY));
    }

    context.sendMessage(Message.raw("UUID: " + dogUuid.toString()).color(Constants.COLOR_GRAY));
    context.sendMessage(Message.raw("Type: " + dogData.dogType()).color(Constants.COLOR_INFO));

    if (dogData.hasOwner()) {
      context.sendMessage(Message.raw("Status: Tamed").color(Constants.COLOR_SUCCESS));
      String ownerName = dogData.ownerName() != null ? dogData.ownerName() : "Unknown";
      context.sendMessage(Message.raw("Owner: " + ownerName).color(Constants.COLOR_INFO));
    } else {
      context.sendMessage(Message.raw("Status: Wild").color(Constants.COLOR_WARNING));
    }

    if (isOwner) {
      String stateColor =
          switch (dogData.state()) {
            case SITTING -> "#FFA500";
            case FOLLOWING -> Constants.COLOR_SUCCESS;
            default -> Constants.COLOR_INFO;
          };
      context.sendMessage(Message.raw("State: " + dogData.state()).color(stateColor));
      context.sendMessage(
          Message.raw("Spawn Status: " + dogData.status()).color(Constants.COLOR_GRAY));

      if (dogData.position() != null) {
        String position =
            String.format(
                "%d, %d, %d", dogData.position().x, dogData.position().y, dogData.position().z);
        context.sendMessage(Message.raw("Last Position: " + position).color(Constants.COLOR_GRAY));
      }
    }

    context.sendMessage(Message.raw(""));
    context.sendMessage(
        Message.raw("Tip: Use /dog name <name> to name your dog").color(Constants.COLOR_INFO));
  }
}
