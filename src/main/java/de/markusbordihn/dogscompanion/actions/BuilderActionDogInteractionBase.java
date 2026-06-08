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

package de.markusbordihn.dogscompanion.actions;

import com.google.gson.JsonElement;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.sensorinfo.EntityPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.IPositionProvider;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import javax.annotation.Nonnull;

public abstract class BuilderActionDogInteractionBase extends BuilderActionBase {

  public abstract String getBuilderId();

  @Nonnull
  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Stable;
  }

  @Nonnull
  @Override
  public BuilderActionDogInteractionBase readConfig(JsonElement config) {
    return this;
  }

  protected abstract static class ActionDogInteractionBase extends ActionBase {

    protected ActionDogInteractionBase(BuilderActionBase builder) {
      super(builder);
    }

    @Override
    public boolean canExecute(
        @Nonnull Ref<EntityStore> entityRef,
        @Nonnull Role role,
        InfoProvider infoProvider,
        double deltaTime,
        @Nonnull Store<EntityStore> store) {
      return true;
    }

    protected boolean isDogTamed(Ref<EntityStore> entityRef, Store<EntityStore> store) {
      DogOwnerComponent ownerComponent =
          store.getComponent(entityRef, DogOwnerComponent.getComponentType());
      return ownerComponent != null && ownerComponent.hasOwner();
    }

    protected java.util.UUID getPlayerUUID(Player player, Store<EntityStore> store) {
      if (player == null) {
        return null;
      }
      UUIDComponent uuidComponent =
          store.getComponent(player.getReference(), UUIDComponent.getComponentType());
      return uuidComponent != null ? uuidComponent.getUuid() : null;
    }

    protected Player getPlayerFromInfoProvider(
        Role role, InfoProvider infoProvider, Store<EntityStore> store) {
      if (role != null && role.getStateSupport() != null) {
        Ref<EntityStore> playerRef = role.getStateSupport().getInteractionIterationTarget();
        if (playerRef != null && playerRef.isValid()) {
          return store.getComponent(playerRef, Player.getComponentType());
        }
      }

      if (infoProvider != null && infoProvider.hasPosition()) {
        IPositionProvider posProvider = infoProvider.getPositionProvider();
        if (posProvider instanceof EntityPositionProvider) {
          Ref<EntityStore> playerRef = posProvider.getTarget();
          if (playerRef != null && playerRef.isValid()) {
            return store.getComponent(playerRef, Player.getComponentType());
          }
        }
      }

      return null;
    }

    protected ItemStack getHeldItem(Player player, Store<EntityStore> store) {
      if (player == null) {
        return null;
      }

      InventoryComponent.Hotbar hotbar =
          store.getComponent(player.getReference(), InventoryComponent.Hotbar.getComponentType());
      if (hotbar == null) {
        return null;
      }

      ItemStack activeItem = hotbar.getActiveItem();
      if (activeItem == null || activeItem.isEmpty()) {
        return null;
      }

      return activeItem;
    }

    protected String getHeldItemName(Player player, Store<EntityStore> store) {
      ItemStack item = getHeldItem(player, store);
      return item != null ? item.getItemId() : null;
    }

    protected boolean isOwner(
        Ref<EntityStore> entityRef, java.util.UUID playerUuid, Store<EntityStore> store) {
      if (playerUuid == null) {
        return false;
      }
      DogOwnerComponent ownerComponent =
          store.getComponent(entityRef, DogOwnerComponent.getComponentType());
      return ownerComponent != null && playerUuid.equals(ownerComponent.getOwnerUUID());
    }
  }
}
