package de.baumann.browser.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.fragment.Fragment_settings_Delete;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;

public class Settings_Delete extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_settings_delete);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new Fragment_settings_Delete())
                .commit();

        Button button = findViewById(R.id.profileListAdd);
        button.setOnClickListener(v -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setIcon(R.drawable.icon_delete);
            builder.setTitle(R.string.menu_delete);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> BrowserUnit.clearBrowserData(this));
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(this, dialog);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) finish();
        if (menuItem.getItemId() == R.id.menu_help) {
            Uri webpage = Uri.parse("https://codeberg.org/Gaukler_Faun/FOSS_Browser/wiki/Delete");
            BrowserUnit.intentURL(this, webpage);
        }
        return true;
    }
}