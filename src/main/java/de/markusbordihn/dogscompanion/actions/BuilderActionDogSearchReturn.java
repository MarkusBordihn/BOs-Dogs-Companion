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
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.support.StateSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.SearchFindType;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuilderActionDogSearchReturn extends BuilderActionBase {

  public static final String BUILDER_ID = "DogSearchReturn";

  private static final double MIN_SEARCH_TIME = 30.0;
  private static final double MAX_SEARCH_TIME = 90.0;

  public String getBuilderId() {
    return BUILDER_ID;
  }

  @Nonnull
  @Override
  public BuilderDescriptorState getBuilderDescriptorState() {
    return BuilderDescriptorState.Stable;
  }

  @Nonnull
  @Override
  public BuilderActionDogSearchReturn readConfig(@Nullable JsonElement config) {
    return this;
  }

  @Nonnull
  @Override
  public ActionDogSearchReturn build(BuilderSupport support) {
    return new ActionDogSearchReturn(this);
  }

  @Nonnull
  @Override
  public String getShortDescription() {
    return "Accumulates search time and transitions to Returning with a found item";
  }

  @Nonnull
  @Override
  public String getLongDescription() {
    return "Runs in the Pet/Searching state. After a random 30-90s delay, picks a random item "
        + "from SearchFindType, stores it per-entity, and transitions to Pet/Returning "
        + "so the owner can claim the item by interacting with the dog.";
  }

  public static class ActionDogSearchReturn extends ActionBase {

    private static final Map<Ref<EntityStore>, SearchTimer> timerByEntity =
        new ConcurrentHashMap<>();
    private static final Map<Ref<EntityStore>, String> foundItemByEntity =
        new ConcurrentHashMap<>();

    public ActionDogSearchReturn(@Nonnull BuilderActionBase builder) {
      super(builder);
    }

    public static void resetSearch(@Nonnull Ref<EntityStore> entityRef) {
      timerByEntity.remove(entityRef);
      foundItemByEntity.remove(entityRef);
    }

    @Nullable
    public static String claimFoundItem(@Nonnull Ref<EntityStore> entityRef) {
      return foundItemByEntity.remove(entityRef);
    }

    public static boolean hasPendingItem(@Nonnull Ref<EntityStore> entityRef) {
      return foundItemByEntity.containsKey(entityRef);
    }

    @Override
    public boolean canExecute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {
      return store.getComponent(entityRef, DogStateComponent.getComponentType()) != null;
    }

    @Override
    public boolean execute(
        Ref<EntityStore> entityRef,
        Role role,
        InfoProvider infoProvider,
        double deltaTime,
        Store<EntityStore> store) {

      if (foundItemByEntity.containsKey(entityRef)) {
        return true;
      }

      SearchTimer timer =
          timerByEntity.compute(
              entityRef,
              (key, previous) ->
                  previous == null ? SearchTimer.started() : previous.advance(deltaTime));

      if (timer.elapsed() < timer.threshold()) {
        return true;
      }

      timerByEntity.put(entityRef, SearchTimer.reset());

      SearchFindType findType = SearchFindType.random();
      String itemId = findType.getItemId();
      foundItemByEntity.put(entityRef, itemId);

      DogNameComponent nameComponent =
          store.getComponent(entityRef, DogNameComponent.getComponentType());
      String dogName = nameComponent != null ? nameComponent.getName() : "Your dog";
      DogsManager.getInstance()
          .sendMessageToOwner(
              entityRef,
              store,
              Message.translation("dogs_companion.interactions.search.found")
                  .param("name", dogName)
                  .color(Constants.COLOR_SUCCESS));

      StateSupport stateSupport = role.getStateSupport();
      stateSupport.setState(entityRef, "Pet", "Returning", store);

      return true;
    }

    private record SearchTimer(double elapsed, double threshold) {

      static SearchTimer started() {
        // Stagger the first search so dogs spawned together do not fire in lockstep.
        return new SearchTimer(
            ThreadLocalRandom.current().nextDouble(0, MIN_SEARCH_TIME), nextThreshold());
      }

      static SearchTimer reset() {
        return new SearchTimer(0, nextThreshold());
      }

      private static double nextThreshold() {
        return ThreadLocalRandom.current().nextDouble(MIN_SEARCH_TIME, MAX_SEARCH_TIME);
      }

      SearchTimer advance(double deltaTime) {
        return new SearchTimer(this.elapsed + deltaTime, this.threshold);
      }
    }
  }
}
