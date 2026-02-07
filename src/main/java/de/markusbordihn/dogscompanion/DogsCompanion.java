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

package de.markusbordihn.dogscompanion;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.event.PluginSetupEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.BuilderFactory;
import com.hypixel.hytale.server.npc.instructions.Action;
import com.hypixel.hytale.server.npc.instructions.Sensor;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogCycleState;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionBase;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionOwner;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionStranger;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogInteractionWild;
import de.markusbordihn.dogscompanion.actions.BuilderActionDogReturnToPreviousState;
import de.markusbordihn.dogscompanion.commands.DogCommands;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.component.DogTamingProgressComponent;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.manager.DogsNamesManager;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsDogTamed;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorIsOwner;
import de.markusbordihn.dogscompanion.sensors.BuilderSensorOwnerAttacked;
import de.markusbordihn.dogscompanion.systems.DogCombatDamageSystem;
import de.markusbordihn.dogscompanion.systems.DogDefenseSystem;
import de.markusbordihn.dogscompanion.systems.DogOffenseSystem;
import de.markusbordihn.dogscompanion.world.storage.DogsCompanionDataResource;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class DogsCompanion extends JavaPlugin {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static final Class<? extends BuilderActionDogInteractionBase>[] DOG_INTERACTION_BUILDERS =
      new Class[] {
        BuilderActionDogInteractionWild.class,
        BuilderActionDogInteractionOwner.class,
        BuilderActionDogInteractionStranger.class
      };

  private static DogsCompanion instance;
  public ComponentType<EntityStore, DogOwnerComponent> dogOwnerComponentType;
  public ComponentType<EntityStore, DogNameComponent> dogNameComponentType;
  public ResourceType<EntityStore, DogsCompanionDataResource> dogsDataResourceType;
  public ComponentType<EntityStore, DogStateComponent> dogStateComponentType;
  public ComponentType<EntityStore, DogTamingProgressComponent> dogTamingProgressComponentType;
  private boolean actionsRegistered = false;
  private boolean sensorsRegistered = false;

  public DogsCompanion(JavaPluginInit init) {
    super(init);
    instance = this;
  }

  public static DogsCompanion getInstance() {
    return instance;
  }

  private void registerDogInteractionActions(NPCPlugin npcPlugin) {
    if (actionsRegistered) {
      LOGGER.at(Level.INFO).log("Custom actions already registered - skipping");
      return;
    }

    LOGGER.at(Level.INFO).log("NPC Plugin setup detected - registering custom actions");

    BuilderFactory<Action> actionFactory = npcPlugin.getBuilderManager().getFactory(Action.class);

    int registeredCount = 0;
    for (Class<? extends BuilderActionDogInteractionBase> builderClass : DOG_INTERACTION_BUILDERS) {
      try {
        BuilderActionDogInteractionBase builder =
            builderClass.getDeclaredConstructor().newInstance();
        String builderId = builder.getBuilderId();
        actionFactory.add(
            builderId,
            () -> {
              try {
                return builderClass.getDeclaredConstructor().newInstance();
              } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to instantiate action builder: " + builderClass.getSimpleName(), e);
              }
            });
        registeredCount++;
        LOGGER.at(Level.INFO).log("Registered action: %s", builderId);
      } catch (Exception e) {
        LOGGER.at(Level.SEVERE).log(
            "Failed to register action builder: %s", builderClass.getSimpleName(), e);
      }
    }

    // Register custom combat actions
    try {
      actionFactory.add("DogCycleState", BuilderActionDogCycleState::new);
      LOGGER.at(Level.INFO).log("Registered action: DogCycleState");
      registeredCount++;
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log("Failed to register action: DogCycleState", e);
    }

    try {
      actionFactory.add("DogReturnToPreviousState", BuilderActionDogReturnToPreviousState::new);
      LOGGER.at(Level.INFO).log("Registered action: DogReturnToPreviousState");
      registeredCount++;
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log("Failed to register action: DogReturnToPreviousState", e);
    }

    LOGGER.at(Level.INFO).log("Registered %d dog interaction actions", registeredCount);
    actionsRegistered = true;
  }

  private void registerDogSensors(NPCPlugin npcPlugin) {
    if (sensorsRegistered) {
      LOGGER.at(Level.INFO).log("Custom sensors already registered - skipping");
      return;
    }

    LOGGER.at(Level.INFO).log("Registering custom dog sensors...");

    BuilderFactory<Sensor> sensorFactory = npcPlugin.getBuilderManager().getFactory(Sensor.class);

    try {
      sensorFactory.add(BuilderSensorIsDogTamed.SENSOR_ID, BuilderSensorIsDogTamed::new);
      LOGGER.at(Level.INFO).log("Registered sensor: %s", BuilderSensorIsDogTamed.SENSOR_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register sensor: %s", BuilderSensorIsDogTamed.SENSOR_ID, e);
    }

    try {
      sensorFactory.add(BuilderSensorIsOwner.SENSOR_ID, BuilderSensorIsOwner::new);
      LOGGER.at(Level.INFO).log("Registered sensor: %s", BuilderSensorIsOwner.SENSOR_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register sensor: %s", BuilderSensorIsOwner.SENSOR_ID, e);
    }

    try {
      sensorFactory.add(BuilderSensorOwnerAttacked.BUILDER_ID, BuilderSensorOwnerAttacked::new);
      LOGGER.at(Level.INFO).log("Registered sensor: %s", BuilderSensorOwnerAttacked.BUILDER_ID);
    } catch (Exception e) {
      LOGGER.at(Level.SEVERE).log(
          "Failed to register sensor: %s", BuilderSensorOwnerAttacked.BUILDER_ID, e);
    }

    sensorsRegistered = true;
    LOGGER.at(Level.INFO).log("Finished registering custom dog sensors");
  }

  @Override
  protected void setup() {
    super.setup();
    LOGGER.at(Level.INFO).log("Setting up %s Plugin...", Constants.MOD_NAME);
    LOGGER.at(Level.INFO).log("Plugin: %s", getManifest().getName());
    LOGGER.at(Level.INFO).log("Version: %s", getManifest().getVersion());
    LOGGER.at(Level.INFO).log("Author: %s", getManifest().getAuthors());
    LOGGER.at(Level.INFO).log("Description: %s", getManifest().getDescription());

    LOGGER.at(Level.INFO).log("Registering dog components...");
    dogOwnerComponentType =
        getEntityStoreRegistry()
            .registerComponent(DogOwnerComponent.class, "DogOwner", DogOwnerComponent.CODEC);
    dogNameComponentType =
        getEntityStoreRegistry()
            .registerComponent(DogNameComponent.class, "DogName", DogNameComponent.CODEC);
    dogStateComponentType =
        getEntityStoreRegistry()
            .registerComponent(DogStateComponent.class, "DogState", DogStateComponent.CODEC);
    dogTamingProgressComponentType =
        getEntityStoreRegistry()
            .registerComponent(
                DogTamingProgressComponent.class,
                "DogTamingProgress",
                DogTamingProgressComponent.CODEC);

    LOGGER.at(Level.INFO).log("Registering dog resources...");
    dogsDataResourceType =
        getEntityStoreRegistry()
            .registerResource(
                DogsCompanionDataResource.class,
                "DogsCompanionData",
                DogsCompanionDataResource.CODEC);
    DogsCompanionDataResource.setResourceType(dogsDataResourceType);

    LOGGER.at(Level.INFO).log("Registering dogs manager...");
    getEntityStoreRegistry().registerSystem(new DogsManager(dogStateComponentType));

    LOGGER.at(Level.INFO).log("Registering dog combat systems...");
    getEntityStoreRegistry().registerSystem(new DogDefenseSystem());
    getEntityStoreRegistry().registerSystem(new DogOffenseSystem());
    getEntityStoreRegistry().registerSystem(new DogCombatDamageSystem());

    LOGGER.at(Level.INFO).log("Initializing dog names manager...");
    DogsNamesManager.initialize();

    if (NPCPlugin.get() instanceof NPCPlugin npcPlugin) {
      LOGGER.at(Level.INFO).log("Registering NPC Plugin ...");
      registerDogInteractionActions(npcPlugin);
      registerDogSensors(npcPlugin);
    } else if (getEventRegistry() != null) {
      LOGGER.at(Level.INFO).log("Registering NPC Plugin setup listener...");
      getEventRegistry()
          .registerGlobal(
              PluginSetupEvent.class,
              event -> {
                if (event.getPlugin() instanceof NPCPlugin npcPlugin) {
                  registerDogInteractionActions(npcPlugin);
                  registerDogSensors(npcPlugin);
                }
              });
    } else {
      LOGGER.at(Level.SEVERE).log(
          "Event registry is not available, cannot register NPC Plugin setup listener");
    }

    // Register commands
    LOGGER.at(Level.INFO).log("Registering commands...");
    this.getCommandRegistry().registerCommand(new DogCommands());
  }

  @Override
  protected void start() {
    super.start();
    LOGGER.at(Level.INFO).log("Starting Dogs Companion Plugin...");
  }

  @Override
  protected void shutdown() {
    super.shutdown();
    LOGGER.at(Level.INFO).log("Shutting down Dogs Companion Plugin...");
  }
}
