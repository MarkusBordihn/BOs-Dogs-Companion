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

package de.markusbordihn.dogscompanion.data;

import com.hypixel.hytale.codec.codecs.EnumCodec;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum DogType {
  UNKNOWN("", ""),
  GENERIC_DOG("DogsCompanion_Tamed", "DogsCompanion_Wild"),
  GERMAN_SHEPHERD("DogsCompanion_GermanShepherd_Tamed", "DogsCompanion_GermanShepherd_Wild"),
  SHIBA_INU("DogsCompanion_ShibaInu_Tamed", "DogsCompanion_ShibaInu_Wild"),
  AUSTRALIAN_SHEPHERD_BLUE_MERLE(
      "DogsCompanion_AustralianShepherd_BlueMerle_Tamed",
      "DogsCompanion_AustralianShepherd_BlueMerle_Wild");

  public static final EnumCodec<DogType> CODEC = new EnumCodec<>(DogType.class);

  private final String tamedRoleName;
  private final String wildRoleName;

  DogType(String tamedRoleName, String wildRoleName) {
    this.tamedRoleName = tamedRoleName;
    this.wildRoleName = wildRoleName;
  }

  @Nonnull
  public static DogType fromRoleName(@Nullable String roleName) {
    if (roleName == null || roleName.isEmpty()) {
      return UNKNOWN;
    }
    for (DogType type : values()) {
      if (type.tamedRoleName.equals(roleName) || type.wildRoleName.equals(roleName)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  public static DogType fromString(String type) {
    if (type == null || type.isEmpty()) {
      return UNKNOWN;
    }
    try {
      return DogType.valueOf(type.toUpperCase(Locale.ROOT).replace(" ", "_"));
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }

  @Nonnull
  public String getTamedRoleName() {
    return tamedRoleName;
  }

  @Nonnull
  public String getWildRoleName() {
    return wildRoleName;
  }

  @Nonnull
  public String getRoleName(boolean isTamed) {
    return isTamed ? tamedRoleName : wildRoleName;
  }

  @Override
  public String toString() {
    if (this == UNKNOWN) {
      return "Unknown";
    }
    String name = name().toLowerCase(Locale.ROOT).replace("_", " ");
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
