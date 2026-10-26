package com.yonlab.vision;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;
import android.widget.LinearLayout;

/** Reflow existing media and controls without replacing their session-owning views. */
final class MediaPanels {
    private MediaPanels() {}

    static boolean sideBySide(Activity owner) {
        Configuration size=owner.getResources().getConfiguration();
        return size.screenWidthDp>=680 && size.screenWidthDp>size.screenHeightDp;
    }

    static void arrange(Activity owner,LinearLayout body,View media,View controls,float mediaWeight) {
        boolean wide=sideBySide(owner);
        body.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        LinearLayout.LayoutParams image=wide
            ?new LinearLayout.LayoutParams(0,-1,mediaWeight)
            :new LinearLayout.LayoutParams(-1,0,1.15f);
        if(wide)image.rightMargin=Ui.dp(owner,12);else image.bottomMargin=Ui.dp(owner,12);
        media.setLayoutParams(image);
        controls.setLayoutParams(wide?new LinearLayout.LayoutParams(0,-1,1):new LinearLayout.LayoutParams(-1,0,1));
        body.requestLayout();
    }
}
