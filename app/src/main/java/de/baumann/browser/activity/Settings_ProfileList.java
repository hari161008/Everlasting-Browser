package de.baumann.browser.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.AdapterProfileList;

public class Settings_ProfileList extends AppCompatActivity {

    private List<String> list;
    private List_standard listStandard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_settings_profile_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        listStandard = new List_standard(this);
        RecordAction action = new RecordAction(this);
        action.open(false);
        list = action.listDomains(RecordUnit.TABLE_STANDARD);
        action.close();

        ListView listView = findViewById(R.id.whitelist);
        listView.setEmptyView(findViewById(R.id.whitelist_empty));

        //noinspection NullableProblems
        AdapterProfileList adapter = new AdapterProfileList(this, list) {
            @Override
            public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ImageView deleteEntry = v.findViewById(R.id.iconView);
                deleteEntry.setVisibility(View.VISIBLE);
                MaterialCardView cardView = v.findViewById(R.id.cardView);
                cardView.setVisibility(View.GONE);
                deleteEntry.setOnClickListener(v1 -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(Settings_ProfileList.this);
                    builder.setIcon(R.drawable.icon_delete);
                    builder.setTitle(R.string.menu_delete);
                    builder.setMessage(R.string.hint_database);
                    builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                        listStandard.removeDomain(list.get(position));
                        list.remove(position);
                        notifyDataSetChanged();

                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.getContext());
                        sp.edit()
                                .remove(list.get(position) + "_saveData")
                                .remove(list.get(position) + "_images")
                                .remove(list.get(position) + "_adBlock")
                                .remove(list.get(position) + "_trackingULS")
                                .remove(list.get(position) + "_location")
                                .remove(list.get(position) + "_fingerPrintProtection")
                                .remove(list.get(position) + "_cookies")
                                .remove(list.get(position) + "_cookiesThirdParty")
                                .remove(list.get(position) + "_deny_cookie_banners")
                                .remove(list.get(position) + "_javascript")
                                .remove(list.get(position) + "_javascriptPopUp")
                                .remove(list.get(position) + "_saveHistory")
                                .remove(list.get(position) + "_camera")
                                .remove(list.get(position) + "_microphone")
                                .remove(list.get(position) + "_dom")
                                .remove(list.get(position) + "_night")
                                .remove(list.get(position) + "_desktop").apply();
                        NinjaToast.show(Settings_ProfileList.this, R.string.toast_delete_successful);
                    });
                    builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    HelperUnit.setupDialog(Settings_ProfileList.this, dialog);
                });
                return v;
            }
        };
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if (menuItem.getItemId() == android.R.id.home) finish();
        else if (menuItem.getItemId() == R.id.menu_help) {
            Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Saved-websites");
            BrowserUnit.intentURL(this, webpage);
        }
        return true;
    }
}