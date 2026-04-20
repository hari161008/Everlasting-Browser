package de.baumann.browser.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.view.GridAdapter;
import de.baumann.browser.view.GridItem;

public class NinjaDownloadListener implements DownloadListener {
    private final Context context;
    private final WebView webView;
    public NinjaDownloadListener(Context context, WebView webView) {
        super();
        this.context = context;
        this.webView = webView;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {

        // Create a background thread that has a Looper
        HandlerThread handlerThread = new HandlerThread("HandlerThread");
        handlerThread.start();
        // Create a handler to execute tasks in the background thread.
        Handler backgroundHandler = new Handler(handlerThread.getLooper());
        Message msg = backgroundHandler.obtainMessage();
        webView.requestFocusNodeHref(msg);
        final String[] msgString = {(String) msg.getData().get("url")};
        if (msgString[0] == null) {
            msgString[0] = url;
        }
        if (url.startsWith("blob:")) {
            String text = context.getString(R.string.app_error) + ": Can not download Blob-files.";
            Toast.makeText(context, text, Toast.LENGTH_LONG).show();
        } else {
            String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);

            GridItem item_01 = new GridItem(context.getString(R.string.app_ok), R.drawable.icon_check);
            GridItem item_02 = new GridItem( context.getString(R.string.menu_share_link), R.drawable.icon_link);
            GridItem item_03 = new GridItem( context.getString(R.string.menu_save_as), R.drawable.icon_menu_save);

            View dialogView = View.inflate(context, R.layout.dialog_menu, null);
            CardView cardView = dialogView.findViewById(R.id.albumCardView);
            cardView.setVisibility(View.GONE);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

            builder.setIcon(R.drawable.icon_download);
            builder.setTitle(R.string.dialog_title_download);

            String filenameShort;
            if (filename.length() > 150) {
                filenameShort = filename.substring(0, 150) + " [...]?\"";
                builder.setMessage(filenameShort);
            } else {
                builder.setMessage(filename);
            }

            builder.setView(dialogView);
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());

            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);

            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
            final List<GridItem> gridList = new LinkedList<>();
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
            GridAdapter gridAdapter = new GridAdapter(context, gridList);
            menu_grid.setAdapter(gridAdapter);
            gridAdapter.notifyDataSetChanged();
            menu_grid.setOnItemClickListener((parent, view, position, id) -> {
                Activity activity = (Activity) context;
                switch (position) {
                    case 0:
                        dialog.cancel();
                        BrowserUnit.download(context, msgString[0], filename, mimeType);
                        break;
                    case 1:
                        dialog.cancel();
                        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                        sharingIntent.setType("text/plain");
                        sharingIntent.putExtra(Intent.EXTRA_TEXT, msgString[0]);
                        context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
                        break;
                    case 2:
                        HelperUnit.saveAs(activity, msgString[0], filename, dialog);
                        break;
                }
            });
        }
    }
}
