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
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DogTamingSuccessPage
    extends InteractiveCustomUIPage<DogTamingSuccessPage.TamingSuccessEventData> {

  private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
  private static final long PAGE_CONFLICT_THRESHOLD_MS = 100;
  private static final String KEY_NAME_INPUT = "@DogTamingNameInput";

  private final UUID dogUuid;
  private final String dogBreed;
  private final String initialName;
  private long openedAt;

  public DogTamingSuccessPage(
      @Nonnull PlayerRef playerRef,
      @Nonnull UUID dogUuid,
      @Nullable String dogBreed,
      @Nonnull String initialName) {
    super(playerRef, CustomPageLifetime.CanDismiss, TamingSuccessEventData.CODEC);
    this.dogUuid = dogUuid;
    this.dogBreed = dogBreed != null ? dogBreed : "Dog";
    this.initialName = initialName;
  }

  private static String formatDogBreed(String roleName) {
    if (roleName == null || roleName.isEmpty()) {
      return "Dog";
    }
    return roleName
        .replaceFirst("^DogsCompanion_?", "")
        .replaceFirst("_Tamed$", "")
        .replace("_", " ")
        .trim();
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder commandBuilder,
      @Nonnull UIEventBuilder eventBuilder,
      @Nonnull Store<EntityStore> store) {
    this.openedAt = System.currentTimeMillis();
    commandBuilder.append(Constants.UI_TAMING_SUCCESS);
    commandBuilder.set(
        "#DogTamingSuccessCongrats.Text",
        Message.translation("dogs_companion.ui.taming_success.congratulations"));
    commandBuilder.set("#DogTamingSuccessBreed.Text", formatDogBreed(this.dogBreed));
    commandBuilder.set(
        "#DogTamingSuccessNameLabel.Text",
        Message.translation("dogs_companion.ui.taming_success.name_label"));
    commandBuilder.set("#DogTamingSuccessNameField.Value", this.initialName);
    commandBuilder.set("#DogTamingSuccessNameField.MaxLength", 32);
    commandBuilder.set(
        "#DogTamingSuccessNameField.PlaceholderText",
        Message.translation("dogs_companion.ui.taming_success.placeholder"));
    commandBuilder.set(
        "#DogTamingSuccessTip1.Text", Message.translation("dogs_companion.ui.taming_success.tip1"));
    commandBuilder.set(
        "#DogTamingSuccessTip2.Text", Message.translation("dogs_companion.ui.taming_success.tip2"));
    commandBuilder.set(
        "#DogTamingSuccessTip3.Text", Message.translation("dogs_companion.ui.taming_success.tip3"));
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#DogTamingSuccessConfirmButton",
        EventData.of(KEY_NAME_INPUT, "#DogTamingSuccessNameField.Value"),
        false);
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull TamingSuccessEventData data) {
    if (data.nameInput != null) {
      String newName = data.nameInput.trim();
      if (!newName.isEmpty()
          && !newName.equals(this.initialName)
          && DogsManager.getInstance() != null) {
        Ref<EntityStore> dogRef = DogsManager.getInstance().getDogByUuid(this.dogUuid, store);
        if (dogRef != null) {
          DogsManager.getInstance().updateDogName(dogRef, newName, store);
        }
      }
    }
    this.close();
  }

  @Override
  public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
    if (System.currentTimeMillis() - this.openedAt < PAGE_CONFLICT_THRESHOLD_MS) {
      LOGGER.at(Level.WARNING).log(
          "[Dogs] Taming success screen for %s was dismissed within %dms of opening — "
              + "likely replaced by another mod (PageManager conflict).",
          playerRef, PAGE_CONFLICT_THRESHOLD_MS);
    }
  }

  public static final class TamingSuccessEventData {
    public static final BuilderCodec<TamingSuccessEventData> CODEC =
        BuilderCodec.builder(TamingSuccessEventData.class, TamingSuccessEventData::new)
            .append(
                new KeyedCodec<>(KEY_NAME_INPUT, Codec.STRING),
                (data, value) -> data.nameInput = value,
                data -> data.nameInput)
            .add()
            .build();

    private String nameInput;
  }
}
