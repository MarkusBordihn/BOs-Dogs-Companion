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

package de.markusbordihn.dogscompanion.permission;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.exceptions.NoPermissionException;
import com.hypixel.hytale.server.core.permissions.PermissionHolder;
import de.markusbordihn.dogscompanion.Constants;
import javax.annotation.Nonnull;

public class PermissionManager {

  private static final int MAX_DOG_LIMIT = 32;

  private PermissionManager() {}

  public static void checkPermissionAlways(
      @Nonnull CommandContext context, @Nonnull String permission) throws NoPermissionException {
    if (!context.isPlayer()) {
      return;
    }

    if (context.sender() instanceof PermissionHolder permissionHolder) {
      if (!permissionHolder.hasPermission(permission, false)) {
        throw new NoPermissionException(permission);
      }
    } else {
      throw new NoPermissionException(permission);
    }
  }

  public static boolean hasAdminBypass(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      return true;
    }

    if (context.sender() instanceof PermissionHolder permissionHolder) {
      return permissionHolder.hasPermission("markusbordihn.dogs.admin.bypass");
    }

    return false;
  }

  public static int getDogLimit(@Nonnull PermissionHolder permissionHolder) {
    if (permissionHolder.hasPermission("markusbordihn.dogs.limit.unlimited", false)) {
      return -1;
    }

    int maxLimit = 0;
    for (int i = 1; i <= MAX_DOG_LIMIT; i++) {
      if (permissionHolder.hasPermission("markusbordihn.dogs.limit." + i, false)) {
        maxLimit = Math.max(maxLimit, i);
      }
    }

    return maxLimit > 0 ? maxLimit : Constants.DEFAULT_DOG_LIMIT;
  }

  public static int getDogLimit(@Nonnull CommandContext context) {
    if (!context.isPlayer()) {
      return -1;
    }

    if (!(context.sender() instanceof PermissionHolder permissionHolder)) {
      return Constants.DEFAULT_DOG_LIMIT;
    }

    return getDogLimit(permissionHolder);
  }
}
