package de.baumann.browser.unit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.Nullable;

public class ClipboardUnit {
    @Nullable
    public static String getPrimary(Activity activity) {
        ClipboardManager clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData primaryClip = clipboardManager.getPrimaryClip();
        if (primaryClip == null) return null;
        return primaryClip.getItemAt(0).getText().toString();
    }
}
