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

package de.markusbordihn.dogscompanion.utils;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DogEntityNameUtils {

  public static final String FALLBACK_TARGET = "target";
  public static final String FALLBACK_DOG = "Dog";

  private DogEntityNameUtils() {}

  @Nonnull
  public static String getEntityName(
      @Nullable Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
    if (entityRef == null || !entityRef.isValid()) {
      return FALLBACK_TARGET;
    }

    String dogName = getDogNameOrNull(entityRef, store);
    if (dogName != null) {
      return dogName;
    }

    DisplayNameComponent displayNameComponent =
        store.getComponent(entityRef, DisplayNameComponent.getComponentType());
    if (displayNameComponent != null && displayNameComponent.getDisplayName() != null) {
      String displayName = displayNameComponent.getDisplayName().getRawText();
      if (displayName != null && !displayName.isEmpty()) {
        return displayName;
      }
    }

    String roleName = getRoleNameOrNull(entityRef, store);
    return roleName != null ? roleName : FALLBACK_TARGET;
  }

  @Nonnull
  public static String getNameplateName(
      @Nullable Ref<EntityStore> entityRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull String fallback) {
    if (entityRef == null || !entityRef.isValid()) {
      return fallback;
    }

    Nameplate nameplate = store.getComponent(entityRef, Nameplate.getComponentType());
    if (nameplate != null && nameplate.getText() != null && !nameplate.getText().isEmpty()) {
      return nameplate.getText();
    }

    String roleName = getRoleNameOrNull(entityRef, store);
    return roleName != null ? roleName : fallback;
  }

  @Nonnull
  public static String getDogName(
      @Nullable Ref<EntityStore> dogRef,
      @Nonnull Store<EntityStore> store,
      @Nonnull String fallback) {
    if (dogRef == null || !dogRef.isValid()) {
      return fallback;
    }

    String dogName = getDogNameOrNull(dogRef, store);
    if (dogName != null) {
      return dogName;
    }

    Nameplate nameplate = store.getComponent(dogRef, Nameplate.getComponentType());
    if (nameplate != null && nameplate.getText() != null && !nameplate.getText().isEmpty()) {
      return DogNameplateUtils.getBaseNameFromNameplate(nameplate.getText());
    }

    return fallback;
  }

  @Nullable
  private static String getDogNameOrNull(
      @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
    DogNameComponent dogNameComponent =
        store.getComponent(entityRef, DogNameComponent.getComponentType());
    if (dogNameComponent == null
        || dogNameComponent.getName() == null
        || dogNameComponent.getName().isEmpty()) {
      return null;
    }

    return dogNameComponent.getName();
  }

  @Nullable
  private static String getRoleNameOrNull(
      @Nonnull Ref<EntityStore> entityRef, @Nonnull Store<EntityStore> store) {
    NPCEntity npcEntity = store.getComponent(entityRef, NPCEntity.getComponentType());
    if (npcEntity == null || npcEntity.getRole() == null) {
      return null;
    }

    String roleName = npcEntity.getRole().getRoleName();
    return roleName != null && !roleName.isEmpty() ? roleName : null;
  }
}
