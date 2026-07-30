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

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Constants {

  public static final String MOD_ID = "dogs_companion";
  public static final String MOD_NAME = "Dogs Companion";

  public static final String COLOR_ERROR = "#FF0000";
  public static final String COLOR_SUCCESS = "#00FF00";
  public static final String COLOR_WARNING = "#FFAA00";
  public static final String COLOR_INFO = "#FFFF00";
  public static final String COLOR_GRAY = "#808080";
  public static final String COLOR_GOLD = "#FFD700";
  public static final String COLOR_CYAN = "#00FFFF";
  public static final String COLOR_ORANGE = "#FFA500";
  public static final String COLOR_HEAL = "#66FF66";
  public static final String COLOR_HINT = "#FFAA66";

  public static final int DEFAULT_DOG_LIMIT = 16;

  public static final String DOG_WHISTLE_ITEM_ID = "DogWhistle";
  public static final int DOG_WHISTLE_COOLDOWN_MS = 1500;
  public static final double DOG_WHISTLE_DOG_RANGE = 64.0;

  public static final Set<String> DOG_FOOD_ITEMS_RAW =
      Set.of(
          "Food_Beef_Raw",
          "Food_Pork_Raw",
          "Food_Chicken_Raw",
          "Food_Wildmeat_Raw",
          "Food_Fish_Raw",
          "Ingredient_Bone_Fragment");

  public static final Set<String> DOG_FOOD_ITEMS_COOKED =
      Set.of("Food_Wildmeat_Cooked", "Food_Fish_Grilled", "Food_Kebab_Meat", "Food_Pie_Meat");

  public static final Set<String> DOG_FOOD_ITEMS =
      Stream.concat(DOG_FOOD_ITEMS_RAW.stream(), DOG_FOOD_ITEMS_COOKED.stream())
          .collect(Collectors.toUnmodifiableSet());

  public static final String UI_PATH = "Dogs/";
  public static final String UI_ACTION_WHEEL = UI_PATH + "DogsActionWheel.ui";
  public static final String UI_NAME_INPUT = UI_PATH + "DogNameInput.ui";
  public static final String UI_TAMING_SUCCESS = UI_PATH + "DogTamingSuccess.ui";

  private Constants() {}
}
