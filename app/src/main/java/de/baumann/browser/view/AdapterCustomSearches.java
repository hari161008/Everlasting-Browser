package de.baumann.browser.view;

import static android.view.View.VISIBLE;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONException;

import java.util.ArrayList;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.objects.CustomRedirect;
import de.baumann.browser.objects.CustomSearchesHelper;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

public class AdapterCustomSearches extends RecyclerView.Adapter<RedirectsViewHolder> {
    final private ArrayList<CustomRedirect> redirects;
    private final Context context;
    private final String url;

    public AdapterCustomSearches(Context context, String url, ArrayList<CustomRedirect> redirects) {
        super();
        this.redirects = redirects;
        this.context = context;
        this.url = url;
    }

    @NonNull
    @Override
    public RedirectsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list, parent, false);
        return new RedirectsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RedirectsViewHolder holder, int position) {
        CustomRedirect current = redirects.get(position);
        TextView source = holder.itemView.findViewById(R.id.titleView);
        TextView target = holder.itemView.findViewById(R.id.dateView);
        ImageView remove = holder.itemView.findViewById(R.id.iconView);
        ImageView favicon = holder.itemView.findViewById(R.id.faviconView);
        CardView albumCardView = holder.itemView.findViewById(R.id.albumCardView);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorSurfaceContainerHighest, typedValue, true);
        int color = typedValue.data;
        albumCardView.setCardBackgroundColor(color);

        remove.setVisibility(VISIBLE);
        source.setText(current.getSource());
        target.setText(current.getTarget());
        remove.setOnClickListener((iV) -> {
            MaterialAlertDialogBuilder builderSubMenu = new MaterialAlertDialogBuilder(context);
            builderSubMenu.setTitle(R.string.menu_delete);
            builderSubMenu.setMessage(R.string.hint_database);
            builderSubMenu.setIcon(R.drawable.icon_delete);
            builderSubMenu.setPositiveButton(R.string.app_ok, (dialog2, whichButton) -> {
                redirects.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount());
                try {
                    CustomSearchesHelper.saveRedirects(redirects);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
            builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog2, whichButton) -> builderSubMenu.setCancelable(true));
            Dialog dialogSubMenu = builderSubMenu.create();
            dialogSubMenu.show();
            HelperUnit.setupDialog(context, dialogSubMenu);
        });
        holder.itemView.setOnClickListener(v -> {
            NinjaWebView.getBrowserController().hideSearch();
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sp.edit().putString("sp_search_customSearches", current.getTarget()).apply();
            String t = BrowserUnit.queryWrapper(context, url);

            if (BrowserUnit.isURL(t)) {
                BrowserUnit.intentURL(context, Uri.parse(t));
                sp.edit().putString("sp_search_customSearches", "").apply();
            } else {
                NinjaToast.show(BrowserActivity.getAppContext(), R.string.app_error);
            }
        });
        FaviconHelper.setFavicon(context, favicon, target.getText().toString(), R.id.faviconView, R.drawable.icon_image_broken);
    }

    @Override
    public int getItemCount() {
        return redirects.size();
    }

    public ArrayList<CustomRedirect> getRedirects() {
        return redirects;
    }

    public void addRedirect(CustomRedirect redirect) {
        redirects.add(redirect);
        notifyItemInserted(getItemCount() - 1);
    }
}

