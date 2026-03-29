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

public final class Constants {

  public static final String MOD_ID = "dogs_companion";
  public static final String MOD_NAME = "Dogs Companion";

  public static final String COLOR_ERROR = "#FF0000";
  public static final String COLOR_SUCCESS = "#00FF00";
  public static final String COLOR_WARNING = "#FFAA00";
  public static final String COLOR_INFO = "#FFFF00";
  public static final String COLOR_GRAY = "#808080";

  public static final int DEFAULT_DOG_LIMIT = 16;

  public static final String DOG_WHISTLE_ITEM_ID = "DogWhistle";
  public static final int DOG_WHISTLE_COOLDOWN_MS = 1500;
  public static final double DOG_WHISTLE_DOG_RANGE = 64.0;

  public static final Set<String> DOG_FOOD_ITEMS =
      Set.of(
          "Item_Bone",
          "Bone",
          "Food_Meat_Raw",
          "Food_Meat_Cooked",
          "Food_Beef_Raw",
          "Food_Beef_Cooked",
          "Food_Pork_Raw",
          "Food_Pork_Cooked",
          "Food_Chicken_Raw",
          "Food_Chicken_Cooked",
          "Food_Mutton_Raw",
          "Food_Mutton_Cooked",
          "Food_Wildmeat_Raw",
          "Food_Wildmeat_Cooked");

  public static final String UI_PATH = "Dogs/";
  public static final String UI_ACTION_WHEEL = UI_PATH + "DogsActionWheel.ui";
  public static final String UI_NAME_INPUT = UI_PATH + "DogNameInput.ui";
  public static final String UI_TAMING_SUCCESS = UI_PATH + "DogTamingSuccess.ui";

  private Constants() {}
}
