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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import java.util.Set;

public class ItemInteractionOwner {

  private static final Set<String> TOY_ITEMS =
      Set.of("Item_Stick", "Stick", "Dog_Ball", "Dog_Toy", "Item_Ball");

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem) {

    String itemName = heldItem != null ? heldItem.getItemId() : null;

    if (itemName != null && Constants.DOG_FOOD_ITEMS.contains(itemName)) {
      return InteractionFeeding.handle(entityRef, role, store, player, heldItem, true);
    }

    if (itemName != null && TOY_ITEMS.contains(itemName)) {
      return InteractionPlaying.handle(entityRef, role, store, player, itemName);
    }

    if (Constants.DOG_WHISTLE_ITEM_ID.equals(itemName)) {
      return InteractionDogWhistle.handleOnDog(entityRef, role, store, player);
    }

    if (player != null) {
      DogNameComponent nameComponent =
          store.getComponent(entityRef, DogNameComponent.getComponentType());
      String dogName = nameComponent != null ? nameComponent.getName() : null;
      PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());

      if (playerRef != null) {
        if (dogName != null && !dogName.isEmpty()) {
          playerRef.sendMessage(
              Message.translation("dogs_companion.interactions.item.unknown.named")
                  .param("dogName", dogName)
                  .color(Constants.COLOR_HINT));
        } else {
          playerRef.sendMessage(
              Message.translation("dogs_companion.interactions.item.unknown")
                  .color(Constants.COLOR_HINT));
        }
      }
    }

    return InteractionOwner.handle(entityRef, role, store, player);
  }
}
