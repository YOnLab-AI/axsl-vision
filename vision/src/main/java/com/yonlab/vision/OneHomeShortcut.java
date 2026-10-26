package com.yonlab.vision;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/** Platform pinning only: API 24/25 use launcher instructions without legacy permissions. */
final class OneHomeShortcut {
    static final String ID = "yonlab_one_home";
    static final String NAME = "AXSL Vision";
    static final String MANUAL_INSTRUCTION = "앱 목록에서 AXSL Vision을 길게 눌러 홈 화면에 배치하세요.";
    private static final String BRAND_REVISION = "axsl-vision-0.1-21";
    private static final AtomicBoolean REFRESH_ATTEMPTED = new AtomicBoolean();
    enum State { READY, PINNED, REQUESTED, UNSUPPORTED, UNAVAILABLE }

    private OneHomeShortcut() {}

    static State state(Context context) {
        if (Build.VERSION.SDK_INT < 26) return State.UNSUPPORTED;
        try {
            return Api26.state(context);
        } catch (RuntimeException error) {
            Log.w("YOnHomeShortcut", "Cannot query launcher shortcut state", error);
            return State.UNAVAILABLE;
        }
    }

    /** Call on a worker while the app's activity is in the foreground. */
    static State request(Context context) {
        if (Build.VERSION.SDK_INT < 26) return State.UNSUPPORTED;
        try {
            return Api26.request(context);
        } catch (RuntimeException error) {
            Log.w("YOnHomeShortcut", "Cannot request launcher shortcut", error);
            return State.UNAVAILABLE;
        }
    }

    @TargetApi(26)
    private static final class Api26 {
        static State state(Context context) {
            android.content.pm.ShortcutManager manager =
                    context.getSystemService(android.content.pm.ShortcutManager.class);
            if (manager == null) return State.UNSUPPORTED;
            for (android.content.pm.ShortcutInfo shortcut : manager.getPinnedShortcuts()) {
                if (ID.equals(shortcut.getId()) && shortcut.isEnabled()) {
                    refresh(context, manager, shortcut);
                    return State.PINNED;
                }
            }
            return manager.isRequestPinShortcutSupported() ? State.READY : State.UNSUPPORTED;
        }

        static State request(Context context) {
            State current = state(context);
            if (current != State.READY) return current;
            android.content.pm.ShortcutInfo shortcut = branded(context);
            if (shortcut == null) return State.UNAVAILABLE;
            android.content.pm.ShortcutManager manager =
                    context.getSystemService(android.content.pm.ShortcutManager.class);
            // A true result acknowledges the request, not the user's confirmation.
            return manager != null && manager.requestPinShortcut(shortcut, null)
                    ? State.REQUESTED : State.UNSUPPORTED;
        }

        /** Preserve the existing pin and ID; never request a new pin during migration. */
        private static void refresh(Context context, android.content.pm.ShortcutManager manager,
                                    android.content.pm.ShortcutInfo old) {
            android.content.SharedPreferences preferences = context.getSharedPreferences("YOnHomeShortcut", 0);
            boolean current = BRAND_REVISION.equals(preferences.getString("brandRevision", ""))
                    && NAME.contentEquals(old.getShortLabel())
                    && old.getLongLabel() != null && NAME.contentEquals(old.getLongLabel());
            if (current || !REFRESH_ATTEMPTED.compareAndSet(false, true)) return;
            try {
                android.content.pm.ShortcutInfo updated = branded(context);
                if (updated != null && manager.updateShortcuts(Collections.singletonList(updated))) {
                    preferences.edit().putString("brandRevision", BRAND_REVISION).apply();
                }
            } catch (RuntimeException error) {
                Log.w("YOnHomeShortcut", "Cannot refresh pinned shortcut branding", error);
            }
        }

        private static android.content.pm.ShortcutInfo branded(Context context) {
            Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (launch == null) return null;
            launch.setAction(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            return new android.content.pm.ShortcutInfo.Builder(context, ID)
                    .setShortLabel(NAME).setLongLabel(NAME)
                    .setIcon(android.graphics.drawable.Icon.createWithResource(context, context.getApplicationInfo().icon))
                    .setIntent(launch).build();
        }
    }
}
