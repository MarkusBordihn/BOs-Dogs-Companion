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

package de.markusbordihn.dogscompanion.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import javax.annotation.Nonnull;

public final class DogActionHelper {

  private DogActionHelper() {}

  public static void follow(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.FOLLOWING, store);
    setNpcState(dogRef, store, "Default");
  }

  public static void sit(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.SITTING, store);
    setNpcState(dogRef, store, "Sitting");
  }

  public static void sleep(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.SLEEPING, store);
    setNpcState(dogRef, store, "Sleeping");
  }

  public static void play(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.PLAYING, store);
    setNpcState(dogRef, store, "Playing");
  }

  public static void wander(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.WANDERING, store);
    setNpcState(dogRef, store, "Wandering");
  }

  public static void stop(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.WAITING, store);
    setNpcState(dogRef, store, "Waiting");
  }

  public static void attack(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.OFFENSE, store);
    setNpcState(dogRef, store, "Default");
  }

  public static void defense(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.DEFENSE, store);
    setNpcState(dogRef, store, "Defense");
  }

  public static void search(@Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store) {
    DogsManager.getInstance().updateDogState(dogRef, DogState.SEARCHING, store);
    setNpcState(dogRef, store, "Searching");
  }

  public static void cycleCombatMode(
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull DogState currentState,
      @Nonnull Store<EntityStore> store) {
    switch (currentState) {
      case DogState.DEFENSE -> attack(dogRef, store);
      case DogState.OFFENSE -> stop(dogRef, store);
      default -> defense(dogRef, store);
    }
  }

  private static void setNpcState(
      @Nonnull Ref<EntityStore> dogRef, @Nonnull Store<EntityStore> store, @Nonnull String state) {
    NPCEntity npcEntity = store.getComponent(dogRef, NPCEntity.getComponentType());
    if (npcEntity != null && npcEntity.getRole() != null) {
      npcEntity.getRole().getStateSupport().setState(dogRef, "Pet", state, store);
    }
  }
}
