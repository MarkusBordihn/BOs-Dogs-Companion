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

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.component.DogNameComponent;
import de.markusbordihn.dogscompanion.component.DogStateComponent;
import de.markusbordihn.dogscompanion.data.DogState;
import de.markusbordihn.dogscompanion.interaction.InteractionOwner;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DogActionWheelPage
    extends InteractiveCustomUIPage<DogActionWheelPage.WheelEventData> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final long PAGE_CONFLICT_THRESHOLD_MS = 100;

  private static final String KEY_CMD = "CommandId";
  private static final String UI_WHEEL = "#DogsActionMenuWheel";
  private static final String UI_TITLE = "#DogsActionMenuTitle";
  private static final String UI_SUBTITLE = "#DogsActionMenuSubtitle";
  private static final String UI_STOP_LABEL = "#DogsActionWheelStopLabel";
  private static final String UI_STATUS_HEADER = "#DogsActionWheelStatusHeader";
  private static final String UI_CENTER_TEXT = "#DogsActionMenuCenterText";
  private static final String UI_CENTER_BUTTON = "#DogsActionWheelCenterButton";
  private static final String UI_BUTTON_PREFIX = "#DogsActionWheelButton";
  private static final String UI_LABEL_PREFIX = "#DogsActionWheelLabel";
  private static final String UI_RENAME_BUTTON = "#DogActionWheelRenameButton";

  private final Ref<EntityStore> dogRef;
  private final Player player;
  private final Ref<EntityStore> playerEntityRef;
  private final DogState currentState;
  private long openedAt;

  public DogActionWheelPage(
      @Nonnull PlayerRef playerRef,
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Player player,
      @Nonnull Ref<EntityStore> playerEntityRef,
      @Nullable DogState currentState) {
    super(playerRef, CustomPageLifetime.CanDismiss, WheelEventData.CODEC);
    this.dogRef = dogRef;
    this.player = player;
    this.playerEntityRef = playerEntityRef;
    this.currentState = currentState != null ? currentState : DogState.WAITING;
  }

  public static DogActionWheelPage create(
      @Nonnull PlayerRef playerRef,
      @Nonnull Ref<EntityStore> dogRef,
      @Nonnull Player player,
      @Nonnull Ref<EntityStore> playerEntityRef,
      @Nonnull Store<EntityStore> store) {
    DogStateComponent stateComponent =
        store.getComponent(dogRef, DogStateComponent.getComponentType());
    return new DogActionWheelPage(
        playerRef,
        dogRef,
        player,
        playerEntityRef,
        stateComponent != null ? stateComponent.getState() : DogState.WAITING);
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder commandBuilder,
      @Nonnull UIEventBuilder eventBuilder,
      @Nonnull Store<EntityStore> store) {
    this.openedAt = System.currentTimeMillis();
    commandBuilder.append(Constants.UI_ACTION_WHEEL);
    commandBuilder.set(UI_WHEEL + ".Visible", true);
    commandBuilder.set(UI_TITLE + ".Text", getDogDisplayName(store));
    commandBuilder.set(UI_SUBTITLE + ".Text", resolveStateText());
    commandBuilder.set(
        UI_STOP_LABEL + ".Text", Message.translation("dogs_companion.ui.wheel.stop"));
    commandBuilder.set(
        UI_STATUS_HEADER + ".Text", Message.translation("dogs_companion.ui.wheel.status_header"));
    commandBuilder.set(UI_CENTER_TEXT + ".Text", resolveStateText());

    buildCommandButtons(commandBuilder, eventBuilder);

    commandBuilder.set(
        UI_RENAME_BUTTON + ".Text", Message.translation("dogs_companion.ui.wheel.rename"));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        UI_RENAME_BUTTON,
        EventData.of(KEY_CMD, "rename"),
        false);

    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        UI_CENTER_BUTTON,
        EventData.of(KEY_CMD, "stop"),
        false);
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull WheelEventData data) {
    if (data.commandId == null || data.commandId.isBlank()) {
      this.close();
      return;
    }

    // Rename opens a follow-up page and therefore must not close the wheel.
    if ("rename".equals(data.commandId)) {
      DogNameInputPage namePage =
          new DogNameInputPage(this.playerRef, this.dogRef, getDogDisplayName(store));
      this.player.getPageManager().openCustomPage(this.playerEntityRef, store, namePage);
      return;
    }

    switch (data.commandId) {
      case "stop" -> DogActionHelper.stop(this.dogRef, store);
      case "follow" -> DogActionHelper.follow(this.dogRef, store);
      case "sit" -> DogActionHelper.sit(this.dogRef, store);
      case "play" -> DogActionHelper.play(this.dogRef, store);
      case "search" -> DogActionHelper.search(this.dogRef, store);
      case "cycle_combat" -> DogActionHelper.cycleCombatMode(this.dogRef, this.currentState, store);
      case "sleep_wakeup" -> {
        if (this.currentState == DogState.SLEEPING) {
          DogActionHelper.stop(this.dogRef, store);
        } else {
          DogActionHelper.sleep(this.dogRef, store);
        }
      }
      case "wander_return" -> {
        if (this.currentState == DogState.WANDERING) {
          DogActionHelper.follow(this.dogRef, store);
        } else {
          DogActionHelper.wander(this.dogRef, store);
        }
      }
      case "pet" -> {
        NPCEntity npc = store.getComponent(this.dogRef, NPCEntity.getComponentType());
        if (npc != null && npc.getRole() != null) {
          InteractionOwner.pet(this.dogRef, npc.getRole(), store, this.player);
        }
      }
      default -> LOGGER.at(Level.FINE).log("Unknown wheel command: %s", data.commandId);
    }

    this.close();
  }

  @Override
  public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    if (System.currentTimeMillis() - this.openedAt < PAGE_CONFLICT_THRESHOLD_MS) {
      LOGGER.at(Level.WARNING).log(
          "[Dogs] Action wheel for %s was dismissed within %dms of opening — "
              + "likely replaced by another mod (PageManager conflict).",
          playerRef, PAGE_CONFLICT_THRESHOLD_MS);
    }
  }

  private void buildCommandButtons(
      @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
    WheelSlot[] slots = WheelSlot.values();
    for (int i = 0; i < slots.length; i++) {
      String buttonId = UI_BUTTON_PREFIX + i;
      String labelId = UI_LABEL_PREFIX + i;

      commandBuilder.set(buttonId + ".Visible", true);
      commandBuilder.set(buttonId + ".Text", "");
      commandBuilder.set(labelId + ".Visible", true);
      commandBuilder.set(labelId + ".Text", slots[i].resolveLabel(this.currentState));
      eventBuilder.addEventBinding(
          CustomUIEventBindingType.Activating,
          buttonId,
          EventData.of(KEY_CMD, slots[i].getCommandId()),
          false);
    }
  }

  @Nonnull
  private Message resolveStateText() {
    return switch (this.currentState) {
      case ATTACKING -> Message.translation("dogs_companion.ui.state.attacking");
      case DEFENSE -> Message.translation("dogs_companion.ui.state.defense");
      case FOLLOWING -> Message.translation("dogs_companion.ui.state.following");
      case OFFENSE -> Message.translation("dogs_companion.ui.state.offense");
      case PLAYING -> Message.translation("dogs_companion.ui.state.playing");
      case SEARCHING -> Message.translation("dogs_companion.ui.state.searching");
      case SITTING -> Message.translation("dogs_companion.ui.state.sitting");
      case SLEEPING -> Message.translation("dogs_companion.ui.state.sleeping");
      case STRIKING -> Message.translation("dogs_companion.ui.state.striking");
      case WAITING -> Message.translation("dogs_companion.ui.state.waiting");
      case WANDERING -> Message.translation("dogs_companion.ui.state.wandering");
    };
  }

  @Nonnull
  private String getDogDisplayName(@Nonnull Store<EntityStore> store) {
    DogNameComponent nameComponent =
        store.getComponent(this.dogRef, DogNameComponent.getComponentType());
    if (nameComponent != null
        && nameComponent.getName() != null
        && !nameComponent.getName().isEmpty()) {
      return nameComponent.getName();
    }
    return "Dog";
  }

  /** Slot order defines the button index in the wheel UI, so entries must not be reordered. */
  private enum WheelSlot {
    FOLLOW("follow", "follow"),
    SIT("sit", "sit"),
    SLEEP("sleep_wakeup", "sleep") {
      @Override
      String resolveLabelKey(@Nonnull DogState currentState) {
        return currentState == DogState.SLEEPING ? "wakeup" : "sleep";
      }
    },
    PLAY("play", "play"),
    WANDER("wander_return", "wander") {
      @Override
      String resolveLabelKey(@Nonnull DogState currentState) {
        return currentState == DogState.WANDERING ? "return" : "wander";
      }
    },
    COMBAT("cycle_combat", "normal") {
      @Override
      String resolveLabelKey(@Nonnull DogState currentState) {
        return switch (currentState) {
          case DEFENSE -> "defense";
          case OFFENSE -> "offense";
          default -> "normal";
        };
      }
    },
    SEARCH("search", "search"),
    PET("pet", "pet");

    private final String commandId;
    private final String labelKey;

    WheelSlot(String commandId, String labelKey) {
      this.commandId = commandId;
      this.labelKey = labelKey;
    }

    String getCommandId() {
      return this.commandId;
    }

    String resolveLabelKey(@Nonnull DogState currentState) {
      return this.labelKey;
    }

    @Nonnull
    Message resolveLabel(@Nonnull DogState currentState) {
      return Message.translation(
          "dogs_companion.ui.wheel.slot." + this.resolveLabelKey(currentState));
    }
  }

  public static final class WheelEventData {
    public static final BuilderCodec<WheelEventData> CODEC =
        BuilderCodec.builder(WheelEventData.class, WheelEventData::new)
            .append(
                new KeyedCodec<>(KEY_CMD, Codec.STRING),
                (data, value) -> data.commandId = value,
                data -> data.commandId)
            .add()
            .build();

    private String commandId;
  }
}
