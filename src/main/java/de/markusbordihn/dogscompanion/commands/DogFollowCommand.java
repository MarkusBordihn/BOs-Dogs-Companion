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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.data.DogState;
import javax.annotation.Nonnull;

final class DogFollowCommand extends DogStateCommand {

  public DogFollowCommand() {
    super(
        "follow",
        "Makes the dog you're looking at follow you",
        DogState.FOLLOWING,
        "dogs_companion.commands.follow.success");
  }

  @Override
  protected void onStateApplied(
      @Nonnull Ref<EntityStore> entityRef,
      @Nonnull NPCEntity npcEntity,
      @Nonnull Store<EntityStore> store,
      @Nonnull CommandContext context,
      boolean wasAlreadyInState) {

    if (!wasAlreadyInState) {
      super.onStateApplied(entityRef, npcEntity, store, context, false);
      return;
    }

    // Already following, force a refresh by toggling through the Playing state
    StateSupport stateSupport = npcEntity.getRole().getStateSupport();
    stateSupport.setState(entityRef, "Pet", DogState.PLAYING.getNpcSubstate(), store);
    stateSupport.setState(entityRef, "Pet", DogState.FOLLOWING.getNpcSubstate(), store);

    String dogName = getDogDisplayName(entityRef, store);
    context.sendMessage(
        Message.translation("dogs_companion.commands.follow.re_triggered")
            .param("name", dogName)
            .color(Constants.COLOR_WARNING));
    context.sendMessage(
        Message.translation("dogs_companion.commands.follow.re_triggered.hint")
            .param("name", dogName)
            .color(Constants.COLOR_GRAY));
  }
}
