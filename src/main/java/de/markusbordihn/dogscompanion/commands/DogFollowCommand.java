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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.Optional;
import javax.annotation.Nonnull;

final class DogFollowCommand extends DogCommand {

  @Nonnull private final EntityWrappedArg entityArg;

  public DogFollowCommand() {
    super("follow", "Makes the dog you're looking at follow you");
    this.entityArg = this.withOptionalArg("entity", "The dog entity", ArgTypes.ENTITY_ID);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    Optional<Ref<EntityStore>> entityRefOpt = getEntityFromArgument(this.entityArg, store, context);

    if (entityRefOpt.isPresent()) {
      Ref<EntityStore> entityRef = entityRefOpt.get();
      if (!checkOwnership(entityRef, store, context)) {
        return;
      }

      NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
      if (npcEntity != null && npcEntity.getRole() != null) {
        StateSupport stateSupport = npcEntity.getRole().getStateSupport();
        boolean wasAlreadyFollowing = stateSupport.inState("Pet", "Default");

        // Update state (component + persistent data)
        DogsManager.getInstance().updateDogState(entityRef, DogState.FOLLOWING, store);

        // Already following, force refresh by toggling through Playing state
        if (wasAlreadyFollowing) {
          stateSupport.setState(entityRef, "Pet", "Playing", store);
          stateSupport.setState(entityRef, "Pet", "Default", store);
          context.sendMessage(
              Message.translation("dogs_companion.commands.follow.re_triggered")
                  .param("name", getDogDisplayName(entityRef, store))
                  .color(Constants.COLOR_WARNING));
          context.sendMessage(
              Message.translation("dogs_companion.commands.follow.re_triggered.hint")
                  .param("name", getDogDisplayName(entityRef, store))
                  .color(Constants.COLOR_GRAY));
        } else {
          stateSupport.setState(entityRef, "Pet", "Default", store);
          context.sendMessage(
              Message.translation("dogs_companion.commands.follow.success")
                  .param("name", getDogDisplayName(entityRef, store))
                  .color(Constants.COLOR_SUCCESS));
        }
      } else {
        context.sendMessage(
            Message.translation("dogs_companion.commands.error.no_dog")
                .color(Constants.COLOR_INFO));
      }
    } else {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_dog").color(Constants.COLOR_ERROR));
    }
  }
}
