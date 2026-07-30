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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.role.Role;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.inventory.InventoryHelper;
import java.util.logging.Level;

public class InteractionFeeding {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final float COOKED_FOOD_HEAL = 10.0f;
  private static final float RAW_FOOD_HEAL = 5.0f;

  public static boolean handle(
      Ref<EntityStore> entityRef,
      Role role,
      Store<EntityStore> store,
      Player player,
      ItemStack heldItem,
      boolean isOwner) {
    String itemName = heldItem != null ? heldItem.getItemId() : null;

    LOGGER.at(Level.FINE).log(
        "%s - Dog fed with %s by player %s",
        isOwner ? "FEEDING: By Owner" : "FEEDING: By Stranger",
        itemName,
        getUsername(player, store));

    if (itemName != null) {
      healDog(entityRef, store, player, getHealAmount(itemName));
    }
    role.getStateSupport().setState(entityRef, "Pet", "Feeding", store);

    InventoryHelper.consumeActiveHotbarItem(player, store, heldItem);

    return false;
  }

  private static String getUsername(Player player, Store<EntityStore> store) {
    if (player == null) {
      return "unknown";
    }

    PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());
    return playerRef != null ? playerRef.getUsername() : "unknown";
  }

  private static float getHealAmount(String itemName) {
    return Constants.DOG_FOOD_ITEMS_COOKED.contains(itemName) ? COOKED_FOOD_HEAL : RAW_FOOD_HEAL;
  }

  private static void healDog(
      Ref<EntityStore> entityRef, Store<EntityStore> store, Player player, float healAmount) {

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

      if (player != null) {
        DogNameComponent nameComponent =
            store.getComponent(entityRef, DogNameComponent.getComponentType());
        String dogName = nameComponent != null ? nameComponent.getName() : "Your dog";
        PlayerRef playerRef =
            store.getComponent(player.getReference(), PlayerRef.getComponentType());

        if (actualHealAmount > 0 && playerRef != null) {
          playerRef.sendMessage(
              Message.translation("dogs_companion.interactions.healing")
                  .param("dogName", dogName)
                  .param("amount", String.format("%.1f", actualHealAmount))
                  .color(Constants.COLOR_HEAL));

          LOGGER.at(Level.FINE).log(
              "Dog healed: %s (+%.1f HP, now %.1f/%.1f)",
              dogName, actualHealAmount, newHealth, maxHealth);
        } else if (playerRef != null) {
          playerRef.sendMessage(
              Message.translation("dogs_companion.interactions.already_full_health")
                  .param("dogName", dogName)
                  .color(Constants.COLOR_HINT));
        }
      }
    } catch (Exception e) {
      LOGGER.at(Level.WARNING).withCause(e).log("Failed to heal dog");
    }
  }
}
