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
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import javax.annotation.Nonnull;

final class DogNameCommand extends DogCommand {
  @Nonnull private final EntityWrappedArg entityArg;
  @Nonnull private final RequiredArg<String> nameArg;

  public DogNameCommand() {
    super("name", "Names your dog");
    this.entityArg = this.withOptionalArg("entity", "The dog entity", ArgTypes.ENTITY_ID);
    this.nameArg = this.withRequiredArg("name", "The name for your dog", ArgTypes.STRING);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    String dogName = this.nameArg.get(context);
    var entityOpt = getEntityFromArgument(this.entityArg, store, context);

    if (entityOpt.isEmpty()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_dog").color(Constants.COLOR_ERROR));
      return;
    }

    Ref<EntityStore> entityRef = entityOpt.get();
    if (!checkOwnership(entityRef, store, context)) {
      return;
    }

    DogOwnerComponent ownerComponent =
        store.getComponent(entityRef, DogOwnerComponent.getComponentType());
    if (ownerComponent == null) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.not_owned")
              .color(Constants.COLOR_ERROR));
      return;
    }

    DogNameComponent nameComponent =
        store.getComponent(entityRef, DogNameComponent.getComponentType());
    String formerDogName = (nameComponent != null) ? nameComponent.getName() : null;

    DogsManager.getInstance().updateDogName(entityRef, dogName, store);

    context.sendMessage(
        Message.translation("dogs_companion.commands.name.success")
            .param("name", formerDogName == null || formerDogName.isEmpty() ? "Dog" : formerDogName)
            .param("newName", dogName)
            .color(Constants.COLOR_SUCCESS));
  }
}
