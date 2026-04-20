package de.baumann.browser.view;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;

import java.util.List;

import de.baumann.browser.R;

public class GridAdapter extends BaseAdapter {
    private final List<GridItem> list;
    private final Context context;

    public GridAdapter(Context context, List<GridItem> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Holder holder;
        View view = convertView;

        if (view == null) {

            GridItem item = list.get(position);
            String text = item.getTitle();

            view = LayoutInflater.from(context).inflate(R.layout.item_menu, parent, false);
            holder = new Holder();
            holder.title = view.findViewById(R.id.menuEntry);
            holder.title.setText(text);
            holder.cardView = view.findViewById(R.id.menuCardView);
            holder.iconMenu = view.findViewById(R.id.iconMenu);

            try {
                holder.iconMenu.setImageResource(item.getData());
                // Clear any inherited tint so the _pref drawable's baked-in colour shows
                holder.iconMenu.setImageTintList(null);
            } catch (Exception e) {
                Log.i("Everlasting Browser", "Exception:" + e);
            }

            // Keep the card background transparent — colour lives inside the icon drawable
            holder.cardView.setCardBackgroundColor(android.graphics.Color.TRANSPARENT);

            view.setTag(holder);
        }
        return view;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int arg0) {
        return list.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    private static class Holder {
        TextView title;
        CardView cardView;
        ImageView iconMenu;
    }
}
