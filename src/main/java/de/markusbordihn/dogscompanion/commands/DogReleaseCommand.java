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
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;

final class DogReleaseCommand extends DogCommand {
  @Nonnull private final EntityWrappedArg entityArg;

  public DogReleaseCommand() {
    super("release", "Releases ownership of a dog (makes it wild again)");
    this.entityArg = this.withOptionalArg("entity", "The dog entity", ArgTypes.ENTITY_ID);
  }

  @Override
  protected void execute(
      @Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
    Optional<Ref<EntityStore>> entityRefOpt = getEntityFromArgument(this.entityArg, store, context);

    if (entityRefOpt.isEmpty()) {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.no_dog").color(Constants.COLOR_ERROR));
      return;
    }

    Ref<EntityStore> entityRef = entityRefOpt.get();
    if (!checkOwnership(entityRef, store, context)) {
      return;
    }

    String dogName = getDogDisplayName(entityRef, store);
    DogsManager dogsManager = DogsManager.getInstance();
    UUID dogUuid = dogsManager.getUuid(entityRef, store);

    if (dogUuid != null) {
      dogsManager.releaseOwnership(entityRef, store);
      context.sendMessage(
          Message.translation("dogs_companion.commands.release.success")
              .param("name", dogName)
              .color(Constants.COLOR_SUCCESS));
    } else {
      context.sendMessage(
          Message.translation("dogs_companion.commands.error.release_failed")
              .color(Constants.COLOR_ERROR));
    }
  }
}
