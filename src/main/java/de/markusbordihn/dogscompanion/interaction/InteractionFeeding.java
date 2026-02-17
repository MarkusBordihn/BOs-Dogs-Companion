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
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.inventory.InventoryHelper;
import java.util.logging.Level;

public class InteractionFeeding {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  // Healing values based on food type
  private static final float COOKED_FOOD_HEAL = 10.0f; // Cooked food heals more
  private static final float RAW_FOOD_HEAL = 5.0f; // Raw food heals less

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem,
      boolean isOwner) {
    String itemName = heldItem != null ? heldItem.getItemId() : null;
    String interactionType = isOwner ? "FEEDING: By Owner" : "FEEDING: By Stranger";

    LOGGER.at(Level.INFO).log(
        "%s - Dog fed with %s by player %s",
        interactionType, itemName, player != null ? player.getDisplayName() : "unknown");

    // Apply healing effect based on food type
    if (itemName != null) {
      float healAmount = getHealAmount(itemName);
      healDog(entityRef, store, player, healAmount);
    }

    // Trigger feeding animation
    role.getStateSupport().setState(entityRef, "Pet", "Feeding", store);

    InventoryHelper.consumeActiveHotbarItem(player, heldItem);

    return false;
  }

  private static float getHealAmount(String itemName) {
    return itemName.contains("Cooked") || itemName.contains("_Cooked")
        ? COOKED_FOOD_HEAL
        : RAW_FOOD_HEAL;
  }

  private static void healDog(
      Ref<EntityStore> entityRef,
      Store<EntityStore> store,
      Player player,
      float healAmount) {

    EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
    if (entityStatMap == null) {
      LOGGER.at(Level.WARNING).log("Could not find EntityStatMap for dog");
      return;
    }

    EntityStatValue healthStat = entityStatMap.get(DefaultEntityStatTypes.getHealth());
    if (healthStat == null) {
      LOGGER.at(Level.WARNING).log("Could not find health stat for dog");
      return;
    }

    float currentHealth = healthStat.get();
    float maxHealth = healthStat.getMax();
    float newHealth = Math.min(currentHealth + healAmount, maxHealth);

    try {
      entityStatMap.setStatValue(healthStat.getIndex(), newHealth);
      float actualHealAmount = newHealth - currentHealth;

      // Send feedback to player
      if (player != null && actualHealAmount > 0) {
        DogNameComponent nameComponent =
            store.getComponent(entityRef, DogNameComponent.getComponentType());
        String dogName = nameComponent != null ? nameComponent.getName() : "Your dog";

        player.sendMessage(
            Message.translation("dogs_companion.interactions.healing")
                .param("dogName", dogName)
                .param("amount", String.format("%.1f", actualHealAmount))
                .color("#66FF66"));

        LOGGER.at(Level.INFO).log(
            "Dog healed: %s (+%.1f HP, now %.1f/%.1f)",
            dogName, actualHealAmount, newHealth, maxHealth);
      } else if (player != null && actualHealAmount == 0) {
        DogNameComponent nameComponent =
            store.getComponent(entityRef, DogNameComponent.getComponentType());
        String dogName = nameComponent != null ? nameComponent.getName() : "Your dog";

        player.sendMessage(
            Message.translation("dogs_companion.interactions.already_full_health")
                .param("dogName", dogName)
                .color("#FFAA66"));
      }
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).log("Failed to heal dog: %s", e.getMessage());
    }
  }
}
