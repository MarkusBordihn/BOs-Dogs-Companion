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
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.event.PluginSetupEvent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import de.markusbordihn.dogscompanion.commands.DogCommands;
import de.markusbordihn.dogscompanion.compat.LuckPermsCompat;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogOwnerComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.component.DogTamingProgressComponent;
import de.markusbordihn.dogscompanion.handler.DamageSetupHandler;
import de.markusbordihn.dogscompanion.handler.NPCSetupHandler;
import de.markusbordihn.dogscompanion.interaction.InteractionDogWhistle;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import de.markusbordihn.dogscompanion.manager.DogsNamesManager;
import de.markusbordihn.dogscompanion.permission.PermissionManager;
import de.markusbordihn.dogscompanion.world.storage.DogsCompanionDataResource;
import java.util.logging.Level;

@SuppressWarnings("unused")
public class DogsCompanion extends JavaPlugin {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

  private static DogsCompanion instance;
  public ComponentType<EntityStore, DogOwnerComponent> dogOwnerComponentType;
  public ComponentType<EntityStore, DogNameComponent> dogNameComponentType;
  public ResourceType<EntityStore, DogsCompanionDataResource> dogsDataResourceType;
  public ComponentType<EntityStore, DogStateComponent> dogStateComponentType;
  public ComponentType<EntityStore, DogTamingProgressComponent> dogTamingProgressComponentType;
  private DogCommands dogCommands;

  public DogsCompanion(JavaPluginInit init) {
    super(init);
    instance = this;
  }

  public static DogsCompanion getInstance() {
    return instance;
  }

  @Override
  protected void setup() {
    super.setup();
    LOGGER.at(Level.INFO).log(
        "Setting up %s (v%s by %s): %s",
        getManifest().getName(),
        getManifest().getVersion(),
        getManifest().getAuthors(),
        getManifest().getDescription());

    LOGGER.at(Level.INFO).log("Registering dog components and resources...");
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
    DamageSetupHandler damageSetupHandler = new DamageSetupHandler(getEntityStoreRegistry());
    if (!damageSetupHandler.tryRegister()) {
      LOGGER.at(Level.INFO).log(
          "DamageModule not ready yet, deferring combat system registration...");
      getEventRegistry().registerGlobal(PluginSetupEvent.class, damageSetupHandler::onPluginSetup);
    }

    LOGGER.at(Level.INFO).log("Initializing dog names manager...");
    DogsNamesManager.initialize();

    NPCSetupHandler npcSetupHandler = new NPCSetupHandler();
    if (NPCPlugin.get() instanceof NPCPlugin npcPlugin) {
      LOGGER.at(Level.INFO).log("Registering NPC Plugin ...");
      npcSetupHandler.onNpcPluginReady(npcPlugin);
    } else {
      LOGGER.at(Level.INFO).log("Registering NPC Plugin setup listener...");
      getEventRegistry().registerGlobal(PluginSetupEvent.class, npcSetupHandler::onPluginSetup);
    }

    LOGGER.at(Level.INFO).log("Registering interaction codecs...");
    this.getCodecRegistry(Interaction.CODEC)
        .register(
            InteractionDogWhistle.ID, InteractionDogWhistle.class, InteractionDogWhistle.CODEC);

    LOGGER.at(Level.INFO).log("Registering commands...");
    dogCommands = new DogCommands();
    this.getCommandRegistry().registerCommand(dogCommands);
  }

  @Override
  protected void start() {
    super.start();
    LOGGER.at(Level.INFO).log("Starting Dogs Companion Plugin...");

    if (dogCommands != null) {
      PermissionManager.initializeDefaultPermissions(dogCommands.buildPlayerPermissionNodes());
    }

    Universe.get().getUniverseReady().thenRun(LuckPermsCompat::detect);
  }

  @Override
  protected void shutdown() {
    super.shutdown();
    LOGGER.at(Level.INFO).log("Shutting down Dogs Companion Plugin...");
  }
}
