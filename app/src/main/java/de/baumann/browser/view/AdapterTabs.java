package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.unit.HelperUnit;

class AdapterTabs {

    private final Context context;
    private final AlbumController albumController;
    private View albumView;
    private TextView albumTitle;
    private TextView albumUrl;
    private BrowserController browserController;
    private MaterialCardView albumCardView;

    AdapterTabs(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    View getAlbumView() {
        return albumView;
    }

    void setAlbumTitle(String title, String url) {
        albumTitle.setText(title);
        albumUrl.setText(url);
        HelperUnit.setHighLightedText(context, albumUrl, url, HelperUnit.domain(url));
    }

    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.item_list, null, false);
        albumCardView = albumView.findViewById(R.id.albumCardView);
        albumTitle = albumView.findViewById(R.id.titleView);
        albumUrl = albumView.findViewById(R.id.dateView);
        albumCardView = albumView.findViewById(R.id.albumCardView);

        ImageView albumClose = albumView.findViewById(R.id.iconView);
        albumClose.setImageResource(R.drawable.icon_tab_remove);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setOnClickListener(view -> {
            browserController.removeAlbum(albumController);
            if (BrowserContainer.size() < 2) {
                browserController.hideOverview();
            }
        });
    }

    public void activate() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryInverse, typedValue, true);
        int color = typedValue.data;
        context.getTheme().resolveAttribute(R.attr.colorSurface, typedValue, true);
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setTypeface(null, Typeface.BOLD);
        albumView.setOnClickListener(view -> {
            albumCardView.setCardBackgroundColor(color);
            browserController.hideOverview();
        });
    }

    void deactivate() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorSurfaceContainerHighest, typedValue, true);
        int color = typedValue.data;
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setTypeface(null, Typeface.NORMAL);
        albumView.setOnClickListener(view -> {
            browserController.showAlbum(albumController);
            browserController.hideOverview();
        });
    }
}