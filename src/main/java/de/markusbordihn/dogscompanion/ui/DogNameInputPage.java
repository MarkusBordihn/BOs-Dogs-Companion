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
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import de.markusbordihn.dogscompanion.Constants;
import de.markusbordihn.dogscompanion.manager.DogsManager;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DogNameInputPage
    extends InteractiveCustomUIPage<DogNameInputPage.NameInputEventData> {

  private static final String KEY_NAME_INPUT = "@DogNameInput";

  private final Ref<EntityStore> dogRef;
  private final String initialValue;

  public DogNameInputPage(
      @Nonnull PlayerRef playerRef,
      @Nonnull Ref<EntityStore> dogRef,
      @Nullable String initialValue) {
    super(playerRef, CustomPageLifetime.CanDismiss, NameInputEventData.CODEC);
    this.dogRef = dogRef;
    this.initialValue = initialValue != null ? initialValue : "";
  }

  @Override
  public void build(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull UICommandBuilder commandBuilder,
      @Nonnull UIEventBuilder eventBuilder,
      @Nonnull Store<EntityStore> store) {
    commandBuilder.append(Constants.UI_NAME_INPUT);
    commandBuilder.set("#DogNameInputField.Value", this.initialValue);
    commandBuilder.set("#DogNameInputField.MaxLength", 32);
    eventBuilder.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#DogNameApplyButton",
        EventData.of(KEY_NAME_INPUT, "#DogNameInputField.Value"),
        false);
  }

  @Override
  public void handleDataEvent(
      @Nonnull Ref<EntityStore> ref,
      @Nonnull Store<EntityStore> store,
      @Nonnull NameInputEventData data) {
    String newName = data.nameInput != null ? data.nameInput.trim() : "";
    if (!newName.isEmpty() && DogsManager.getInstance() != null) {
      DogsManager.getInstance().updateDogName(this.dogRef, newName, store);
    }
    this.close();
  }

  @Override
  public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

  public static final class NameInputEventData {
    public static final BuilderCodec<NameInputEventData> CODEC =
        BuilderCodec.builder(NameInputEventData.class, NameInputEventData::new)
            .append(
                new KeyedCodec<>(KEY_NAME_INPUT, Codec.STRING),
                (data, value) -> data.nameInput = value,
                data -> data.nameInput)
            .add()
            .build();

    private String nameInput;
  }
}
