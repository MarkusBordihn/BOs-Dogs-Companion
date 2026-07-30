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

package de.markusbordihn.dogscompanion.interaction;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogSearchReturn;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.inventory.InventoryHelper;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.ui.DogActionWheelPage;
import de.markusbordihn.dogscompanion.utils.DogNameplateUtils;
import javax.annotation.Nonnull;

public class InteractionOwner {

  public static boolean handle(
      @Nonnull Ref<EntityStore> entityRef,
      @Nonnull Role role,
      @Nonnull Store<EntityStore> store,
      @Nonnull Player player) {

    if (BuilderActionDogSearchReturn.ActionDogSearchReturn.hasPendingItem(entityRef)) {
      String itemId = BuilderActionDogSearchReturn.ActionDogSearchReturn.claimFoundItem(entityRef);
      if (itemId != null) {
        DogNameComponent nameComponent =
            store.getComponent(entityRef, DogNameComponent.getComponentType());
        String dogName = nameComponent != null ? nameComponent.getName() : "Your dog";

        InventoryHelper.giveItem(player, store, itemId);
        PlayerRef ownerPlayerRef =
            store.getComponent(player.getReference(), PlayerRef.getComponentType());
        if (ownerPlayerRef != null) {
          ownerPlayerRef.sendMessage(
              Message.translation("dogs_companion.interactions.search.give")
                  .param("name", dogName)
                  .param("item", itemId)
                  .color(Constants.COLOR_GOLD));
        }

        DogsManager.getInstance().updateDogState(entityRef, DogState.FOLLOWING, store);
        role.getStateSupport().setState(entityRef, "Pet", "Default", store);
        DogNameplateUtils.updateNameplateWithState(entityRef, dogName, store);
      }
      return true;
    }

    Ref<EntityStore> playerEntityRef = role.getStateSupport().getInteractionIterationTarget();
    if (playerEntityRef == null || !playerEntityRef.isValid()) {
      return false;
    }

    PlayerRef playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType());
    if (playerRef == null) {
      return false;
    }

    DogActionWheelPage wheel =
        DogActionWheelPage.create(playerRef, entityRef, player, playerEntityRef, store);
    player.getPageManager().openCustomPage(playerEntityRef, store, wheel);
    return true;
  }

  public static boolean pet(
      @Nonnull Ref<EntityStore> entityRef,
      @Nonnull Role role,
      @Nonnull Store<EntityStore> store,
      @Nonnull Player player) {
    role.getStateSupport().setState(entityRef, "Pet", "Playing", store);
    return true;
  }
}
