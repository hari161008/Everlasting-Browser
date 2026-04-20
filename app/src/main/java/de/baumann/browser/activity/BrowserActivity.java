package de.baumann.browser.activity;

import static android.content.ContentValues.TAG;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.webkit.WebView.HitTestResult.IMAGE_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_ANCHOR_TYPE;
import static android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;

import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.webkit.WebViewFeature;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import de.baumann.browser.BuildConfig;
import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BannerBlock;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.dialogs.CustomRedirectsDialog;
import de.baumann.browser.fragment.Fragment_settings_Backup;
import de.baumann.browser.objects.CustomRedirect;
import de.baumann.browser.objects.CustomSearchesHelper;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.AdapterCustomSearches;
import de.baumann.browser.view.AdapterSearch;
import de.baumann.browser.view.GridAdapter;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;
import de.baumann.browser.view.AdapterRecord;
import de.baumann.browser.view.SwipeTouchListener;

/** @noinspection ExtractMethodRecommender*/
public class BrowserActivity extends AppCompatActivity implements BrowserController {

    // Menus
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private AdapterRecord adapter;
    private ImageButton omniBox_overview;
    private ListView listView;

    // Views
    private TextInputEditText omniBox_text;
    private TextView omniBox_title;
    private EditText searchBox;
    private static NinjaWebView ninjaWebView;
    private View customView;
    private VideoView videoView;
    private FloatingActionButton omniBox_tab;
    private BadgeDrawable badgeDrawable;
    private AdapterSearch adapterSearch;

    // Layouts
    private LinearProgressIndicator progressBar;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;
    private ListView list_search;

    // Others
    private BottomNavigationView bottom_navigation;
    private String overViewTab;
    private Activity activity;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private SharedPreferences sp;
    private List_standard listStandard;
    private long newIcon;
    private long filterBy;
    private boolean filter;
    private ValueCallback<Uri[]> filePathCallback = null;
    private AlbumController currentAlbumController = null;
    private ValueCallback<Uri[]> mFilePathCallback;

    public static Context getAppContext() {
        return context;
    }
    private AlertDialog dialogOverview;

    private AlertDialog dialog_overflow;
    private AlertDialog dialogSearch;
    private View dialogViewSearch;
    private BottomSheetDialog bottomSheetDialog_searchOnSite;
    private AlertDialog dialogCustomSearches;

    private AlbumController nextAlbumController(boolean next) {
        if (BrowserContainer.size() <= 1) return currentAlbumController;
        List<AlbumController> list = BrowserContainer.list();
        int index = list.indexOf(currentAlbumController);
        if (next) {
            index++;
            if (index >= list.size()) index = 0; }
        else {
            index--;
            if (index < 0) index = list.size() - 1; }
        return list.get(index);
    }

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = BrowserActivity.this;
        context = BrowserActivity.this;
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        //noinspection InstantiationOfUtilityClass
        new BannerBlock(context);
        HelperUnit.initTheme(activity);

        if (sp.getBoolean("sp_screenOn", false)) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (sp.getBoolean("sp_standard_restart", false)) sp.edit().putString("profile", "profileStandard").apply();

        sp.edit()
                .putInt("restart_changed", 0)
                .putBoolean("pdf_create", false)
                .putBoolean("show_overview", true)
                .putString("openBackground_dialog", "show").apply();

        switch (Objects.requireNonNull(sp.getString("start_tab", "3"))) {
            case "3":
                overViewTab = getString(R.string.album_title_bookmarks);
                break;
            case "4":
                overViewTab = getString(R.string.album_title_history);
                break;
            default:
                overViewTab = getString(R.string.album_title_home);
                break;
        }
        setContentView(R.layout.activity_main);
        contentFrame = findViewById(R.id.main_content);

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {

            boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            //Do your job here
            if (isKeyboardVisible) {
                v.setPadding(0, 0, 0, keyboardHeight);
            } else {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            return insets;
        });

        MaterialAlertDialogBuilder builderOverview = new MaterialAlertDialogBuilder(context);
        View dialogViewOverview = View.inflate(context, R.layout.sheet_overview, null);
        builderOverview.setView(dialogViewOverview);
        dialogOverview = builderOverview.create();
        bottom_navigation = dialogViewOverview.findViewById(R.id.bottom_navigation);
        tab_container = dialogViewOverview.findViewById(R.id.listTabs);
        HelperUnit.setupDialog(context, dialogOverview);
        dialogOverview.show();

        MaterialAlertDialogBuilder builderSearch = new MaterialAlertDialogBuilder(context);
        dialogViewSearch = View.inflate(context, R.layout.sheet_search, null);
        builderSearch.setView(dialogViewSearch);
        dialogSearch = builderSearch.create();
        HelperUnit.setupDialog(context, dialogSearch);
        dialogSearch.show();

        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String text = getString(R.string.app_done) + ". " + getString(R.string.menu_download) +"?";
                Snackbar snackbar = Snackbar.make(ninjaWebView, text, Snackbar.LENGTH_LONG);
                snackbar.setAction(context.getString(R.string.app_ok), v -> startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
                snackbar.show();
            }};

        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        initOmniBox();
        initSearchPanel();
        initOverview();
        hideSearch();
        dispatchIntent(getIntent());

        //restore open Tabs from shared preferences if app got killed
        if (sp.getBoolean("sp_restoreTabs", false)
                || sp.getBoolean("sp_reloadTabs", false)
                || sp.getBoolean("restoreOnRestart", false)) {
            String saveDefaultProfile = sp.getString("profile", "profileStandard");
            ArrayList<String> openTabs;
            openTabs = new ArrayList<>(Arrays.asList(TextUtils.split(sp.getString("openTabs", ""), "‚‗‚")));
            if (!openTabs.isEmpty()) {
                for (int counter = 0; counter < openTabs.size(); counter++) {
                    addAlbum(getString(R.string.app_name), openTabs.get(counter), BrowserContainer.size() < 1);
                }
            }
            sp.edit().putString("profile", saveDefaultProfile).apply();
            sp.edit().putBoolean("restoreOnRestart", false).apply();
        }

        //if still no open Tab open default page
        if (BrowserContainer.size() < 1) {
            addAlbum(getString(R.string.app_name), sp.getString("favoriteURL", "https://www.google.com"), true);
        }
        if (sp.getBoolean("start_tabStart", false) && sp.getBoolean("show_overview", true)) {
            showOverview();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // If there is not data, then we may have taken a photo
                String dataString = data.getDataString();
                if (dataString != null) results = new Uri[]{Uri.parse(dataString)};
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sp.getBoolean("sp_camera", false)) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
        if (sp.getInt("restart_changed", 1) == 1) {
            triggerRebirth(context);
        }
        if (sp.getBoolean("pdf_create", false)) {
            sp.edit().putBoolean("pdf_create", false).apply();
            String text = getString(R.string.app_done) + ". " + getString(R.string.menu_download) +"?";
            Snackbar snackbar = Snackbar.make(ninjaWebView, text, Snackbar.LENGTH_LONG);
            snackbar.setAction(context.getString(R.string.app_ok), v -> startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
            snackbar.show();
        }
        dispatchIntent(getIntent());
    }

    @Override
    public void onDestroy() {
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(1);
        if (sp.getBoolean("sp_clear_quit", true)) {
            BrowserUnit.clearBrowserData(this);
        }
        if (sp.getBoolean("sp_backup_quit", false)) {
            Fragment_settings_Backup.backup(activity);
        }
        BrowserContainer.clear();
        if (!sp.getBoolean("sp_reloadTabs", false) || sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putString("openTabs", "").apply();
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showOverflow();
            case KeyEvent.KEYCODE_BACK:
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    Log.v(TAG, "FOSS Browser in fullscreen mode");
                } else if (ninjaWebView.canGoBack()){
                    ninjaWebView.goBack();
                } else removeAlbum(currentAlbumController);
                return true;
        }
        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {
        View av = (View) controller;
        if (currentAlbumController != null) currentAlbumController.deactivate();
        currentAlbumController = controller;
        currentAlbumController.activate();
        contentFrame.removeAllViews();
        contentFrame.addView(av);
        updateOmniBox();
    }

    @Override
    public synchronized void removeAlbum(final AlbumController controller) {

        if (BrowserContainer.size() <= 1) {
            if (!sp.getBoolean("sp_reopenLastTab", false)) {
                doubleTapsQuit();
            } else {
                ninjaWebView.loadUrl(Objects.requireNonNull(sp.getString("favoriteURL", "https://www.google.com")));
            }
        } else {
            closeTabConfirmation(() -> {
                AlbumController predecessor;
                if (controller == currentAlbumController) predecessor = ((NinjaWebView) controller).getPredecessor();
                else predecessor = currentAlbumController;
                //if not the current TAB is being closed return to current TAB
                tab_container.removeView(controller.getAlbumView());
                int index = BrowserContainer.indexOf(controller);
                BrowserContainer.remove(controller);
                if ((predecessor != null) && (BrowserContainer.indexOf(predecessor) != -1)) {
                    //if predecessor is stored and has not been closed in the meantime
                    showAlbum(predecessor);
                } else {
                    if (index >= BrowserContainer.size()) index = BrowserContainer.size() - 1;
                    showAlbum(BrowserContainer.get(index));
                }
            });
        }
        updateOmniBox();
        saveOpenedTabs();
    }

    @Override
    public synchronized void updateProgress(int progress) {
        progressBar.setProgressCompat(progress, true);
        if (progress != BrowserUnit.LOADING_STOPPED) {
            updateOmniBox();
        }
        if (progress < 100) {
            progressBar.setVisibility(VISIBLE);
            saveOpenedTabs();
        }
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
        if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
        mFilePathCallback = filePathCallback;

        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent[] intentArray;
        intentArray = new Intent[0];

        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
        //noinspection deprecation
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null) return;
        if (customView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        fullscreenHolder = new FrameLayout(context);
        fullscreenHolder.addView(
                customView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(
                fullscreenHolder,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        customView.setKeepScreenOn(true);
        ((View) currentAlbumController).setVisibility(GONE);
        setCustomFullscreen(true);

        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                videoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                videoView.setOnErrorListener(new VideoCompletionListener());
                videoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
    }

    @Override
    public void onHideCustomView() {
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.removeView(fullscreenHolder);
        customView.setKeepScreenOn(false);
        ((View) currentAlbumController).setVisibility(VISIBLE);
        setCustomFullscreen(false);
        fullscreenHolder = null;
        customView = null;
        if (videoView != null) {
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null; }
        contentFrame.requestFocus();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        WebView.HitTestResult result = ninjaWebView.getHitTestResult();
        if (result.getExtra() != null) {
            if (result.getType() == SRC_ANCHOR_TYPE)
                showContextMenuLink("", result.getExtra(), SRC_ANCHOR_TYPE, false);
            else if (result.getType() == SRC_IMAGE_ANCHOR_TYPE) {
                // Create a background thread that has a Looper
                HandlerThread handlerThread = new HandlerThread("HandlerThread");
                handlerThread.start();
                // Create a handler to execute tasks in the background thread.
                Handler backgroundHandler = new Handler(handlerThread.getLooper());
                Message msg = backgroundHandler.obtainMessage();
                ninjaWebView.requestFocusNodeHref(msg);
                String url = (String) msg.getData().get("url");
                showContextMenuLink("", url, SRC_ANCHOR_TYPE, false); }
            else if (result.getType() == IMAGE_TYPE) {
                showContextMenuLink("", result.getExtra(), IMAGE_TYPE, false);
            }
            else showContextMenuLink("", result.getExtra(), 0, false); }
    }

    // Views

    @SuppressLint("ClickableViewAccessibility")
    private void initOverview() {
        listView = dialogOverview.findViewById(R.id.list_overView);
        AtomicInteger intPage = new AtomicInteger();

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryInverse, typedValue, true);
        int color = typedValue.data;
        TypedValue typedValue2 = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorOnSurface, typedValue2, true);
        int color2 = typedValue2.data;

        bottom_navigation.getOrCreateBadge(R.id.page_0).setBackgroundColor(color);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setBadgeTextColor(color2);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setHorizontalOffset(0);
        bottom_navigation.getOrCreateBadge(R.id.page_0).setVerticalOffset(0);

        if (BrowserContainer.size() > 1) {
            bottom_navigation.getOrCreateBadge(R.id.page_0).setNumber(BrowserContainer.size());
        }

        NavigationBarView.OnItemSelectedListener navListener = menuItem -> {

            if (menuItem.getItemId() == R.id.page_0) {
                omniBox_overview.setImageResource(R.drawable.icon_tab);
                overViewTab = getString(R.string.album_title_tab);
                intPage.set(R.id.page_0);
                listView.setVisibility(GONE);
                tab_container.setVisibility(VISIBLE);}
            else if (menuItem.getItemId() == R.id.page_1) {
                omniBox_overview.setImageResource(R.drawable.icon_web);
                overViewTab = getString(R.string.album_title_home);
                intPage.set(R.id.page_1);
                listView.setVisibility(VISIBLE);
                tab_container.setVisibility(GONE);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list = action.listStartSite(activity);
                action.close();

                adapter = new AdapterRecord(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                }); }

            else if (menuItem.getItemId() == R.id.page_2) {
                try {
                    RecordAction action = new RecordAction(context);
                    action.open(true);
                    if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) {
                        omniBox_overview.setImageResource(R.drawable.icon_bookmark_added);
                    } else {
                        omniBox_overview.setImageResource(R.drawable.icon_bookmark);
                    }
                    action.close();
                }
                catch (Exception e) {Log.i(TAG, "dialogCustomSearches:" + e);}
                overViewTab = getString(R.string.album_title_bookmarks);
                intPage.set(R.id.page_2);
                listView.setVisibility(VISIBLE);
                tab_container.setVisibility(GONE);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listBookmark(activity, filter, filterBy);
                action.close();
                adapter = new AdapterRecord(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                filter = false;
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                }); }
            else if (menuItem.getItemId() == R.id.page_3) {
                omniBox_overview.setImageResource(R.drawable.icon_history);
                overViewTab = getString(R.string.album_title_history);
                intPage.set(R.id.page_3);
                listView.setVisibility(VISIBLE);
                tab_container.setVisibility(GONE);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listHistory(context);
                action.close();
                //noinspection NullableProblems
                adapter = new AdapterRecord(context, list) {
                    @Override
                    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView record_item_time = v.findViewById(R.id.dateView);
                        record_item_time.setVisibility(VISIBLE);
                        return v;
                    }
                };
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });
                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    showContextMenuList(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position);
                    return true;
                }); }
            else if (menuItem.getItemId() == R.id.page_4) {
                PopupMenu popup = new PopupMenu(this, bottom_navigation.findViewById(R.id.page_2));
                popup.setForceShowIcon(true);
                popup.setOnDismissListener(menu -> setSelectedTab());
                if (bottom_navigation.getSelectedItemId() == R.id.page_0)
                    popup.inflate(R.menu.menu_help);
                else if (bottom_navigation.getSelectedItemId() == R.id.page_1)
                    popup.inflate(R.menu.menu_list_start);
                else if (bottom_navigation.getSelectedItemId() == R.id.page_2)
                    popup.inflate(R.menu.menu_list_bookmark);
                else if (bottom_navigation.getSelectedItemId() == R.id.page_3)
                    popup.inflate(R.menu.menu_list_history);
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.menu_delete) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                        builder.setTitle(R.string.menu_delete);
                        builder.setMessage(R.string.hint_database);
                        builder.setIcon(R.drawable.icon_delete);
                        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                            if (overViewTab.equals(getString(R.string.album_title_home))) {
                                BrowserUnit.clearHome(context);
                                bottom_navigation.setSelectedItemId(R.id.page_1); }
                            else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                BrowserUnit.clearBookmark(context);
                                bottom_navigation.setSelectedItemId(R.id.page_2); }
                            else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                BrowserUnit.clearHistory(context);
                                bottom_navigation.setSelectedItemId(R.id.page_3); }
                        });
                        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        HelperUnit.setupDialog(context, dialog);
                    } else if (item.getItemId() == R.id.menu_sortName) {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            sp.edit().putString("sort_bookmark", "title").apply();
                            sp.edit().putBoolean("sort_bookmarkDomain", false).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_2); }
                        else if (overViewTab.equals(getString(R.string.album_title_home))) {
                            sp.edit().putString("sort_startSite", "title").apply();
                            sp.edit().putBoolean("sort_startSiteDomain", false).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_1); }
                    } else if (item.getItemId() == R.id.menu_sortIcon) {
                        sp.edit().putString("sort_bookmark", "time").apply();
                        sp.edit().putBoolean("sort_bookmarkDomain", false).apply();
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                    } else if (item.getItemId() == R.id.menu_delete) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                        builder.setTitle(R.string.menu_delete);
                        builder.setMessage(R.string.hint_database);
                        builder.setIcon(R.drawable.icon_delete);
                        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                            if (overViewTab.equals(getString(R.string.album_title_home))) {
                                BrowserUnit.clearHome(context);
                                bottom_navigation.setSelectedItemId(R.id.page_1); }
                            else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                BrowserUnit.clearBookmark(context);
                                bottom_navigation.setSelectedItemId(R.id.page_2); }
                            else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                BrowserUnit.clearHistory(context);
                                bottom_navigation.setSelectedItemId(R.id.page_3); }
                        });
                        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        HelperUnit.setupDialog(context, dialog);
                    } else if (item.getItemId() == R.id.menu_sortName) {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            sp.edit().putString("sort_bookmark", "title").apply();
                            sp.edit().putBoolean("sort_bookmarkDomain", false).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_2); }
                        else if (overViewTab.equals(getString(R.string.album_title_home))) {
                            sp.edit().putString("sort_startSite", "title").apply();
                            sp.edit().putBoolean("sort_startSiteDomain", false).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_1); }
                    } else if (item.getItemId() == R.id.menu_sortDate) {
                        if (overViewTab.equals(getString(R.string.album_title_history))) {
                            sp.edit().putBoolean("sort_historyDomain", false).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_3);
                        }
                        else if (overViewTab.equals(getString(R.string.album_title_home))) {
                            sp.edit().putString("sort_startSite", "ordinal").apply();
                            sp.edit().putBoolean("sort_startSiteDomain", false).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_1);
                        }
                    } else if (item.getItemId() == R.id.menu_sortDomain) {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            sp.edit().putBoolean("sort_bookmarkDomain", true).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_2); }
                        else if (overViewTab.equals(getString(R.string.album_title_home))) {
                            sp.edit().putBoolean("sort_startSiteDomain", true).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_1);
                        }
                        else if (overViewTab.equals(getString(R.string.album_title_history))) {
                            sp.edit().putBoolean("sort_historyDomain", true).apply();
                            bottom_navigation.setSelectedItemId(R.id.page_3);
                        }
                    } else if (item.getItemId() == R.id.menu_filter) {
                        showDialogFilter();
                    } else if (item.getItemId() == R.id.menu_help) {
                        Uri webpage = Uri.parse("https://www.google.com");
                        BrowserUnit.intentURL(this, webpage); }
                    return true;
                });
                popup.show();
            }

            return true;
        };
        bottom_navigation.setOnItemSelectedListener(navListener);
        bottom_navigation.findViewById(R.id.page_2).setOnLongClickListener(v -> {
            showDialogFilter();
            return true;
        });
        setSelectedTab();
    }

    @SuppressLint({"ClickableViewAccessibility", "UnsafeOptInUsageError"})
    private void initOmniBox() {

        omniBox_text = dialogViewSearch.findViewById(R.id.omniBox_input);
        omniBox_title = findViewById(R.id.omniBox_title);
        omniBox_title.setOnClickListener(view -> {
            omniBox_text.setText(ninjaWebView.getUrl());
            dialogSearch.show();
            HelperUnit.showSoftKeyboard(omniBox_text);
        });

        omniBox_tab = findViewById(R.id.omniBox_tab);
        omniBox_tab.setOnClickListener(view -> showOverflow());
        omniBox_tab.setOnLongClickListener(view -> {
            performGesture("setting_gesture_tabButton");
            return true;
        });
        omniBox_overview = findViewById(R.id.omnibox_overview);

        list_search = dialogViewSearch.findViewById(R.id.list_search);
        Button omnibox_close = dialogViewSearch.findViewById(R.id.omnibox_close);
        assert omnibox_close != null;
        omnibox_close.setOnClickListener(view -> {
            if (Objects.requireNonNull(omniBox_text.getText()).length() > 0)
                omniBox_text.setText("");
            else hideSearch();
        });
        Button omnibox_customSearch = dialogViewSearch.findViewById(R.id.omnibox_customSearch);
        assert omnibox_customSearch != null;
        omnibox_customSearch.setOnClickListener(view -> {
            String query = Objects.requireNonNull(omniBox_text.getText()).toString().trim();
            if (query.isEmpty()) {
                NinjaToast.show(context, getString(R.string.toast_input_empty));
            } else {
                hideSearch();
                showDialogCustomSearches(query);
            }
        });
        Button omnibox_search = dialogViewSearch.findViewById(R.id.omnibox_search);
        assert omnibox_search != null;
        omnibox_search.setOnClickListener(view -> {
            String query = Objects.requireNonNull(omniBox_text.getText()).toString().trim();
            if (query.isEmpty()) {
                NinjaToast.show(context, getString(R.string.toast_input_empty));
            } else {
                hideSearch();
                ninjaWebView.loadUrl(omniBox_text.getText().toString());
            }
        });

        progressBar = findViewById(R.id.main_progress_bar);
        progressBar.setOnClickListener(v -> {
            ninjaWebView.stopLoading();
            progressBar.setVisibility(GONE);
        });
        badgeDrawable = BadgeDrawable.create(context);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorPrimaryInverse, typedValue, true);
        int color = typedValue.data;
        TypedValue typedValue2 = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorOnSurface, typedValue2, true);
        int color2 = typedValue2.data;
        badgeDrawable.setBackgroundColor(color);
        badgeDrawable.setBadgeTextColor(color2);

        omniBox_overview.setOnTouchListener(new SwipeTouchListener(context) {
        public void onSwipeTop() {
            performGesture("setting_gesture_tb_up");
            hideOverview(); }
            public void onSwipeBottom() {
                performGesture("setting_gesture_tb_down");
                hideOverview(); }
            public void onSwipeRight() {
                performGesture("setting_gesture_tb_right");
                hideOverview(); }
            public void onSwipeLeft() {
                performGesture("setting_gesture_tb_left");
                hideOverview(); }});

        omniBox_tab.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeTop() {
                performGesture("setting_gesture_nav_up");
                hideOverflow(); }
            public void onSwipeBottom() {
                performGesture("setting_gesture_nav_down");
                hideOverflow();}
            public void onSwipeRight() {
                performGesture("setting_gesture_nav_right");
                hideOverflow();}
            public void onSwipeLeft() {
                performGesture("setting_gesture_nav_left");
                hideOverflow(); }});

        omniBox_text.setOnEditorActionListener((v, actionId, event) -> {
            String query = Objects.requireNonNull(omniBox_text.getText()).toString().trim();
            if (omniBox_text.getText().toString().isEmpty()) {
                NinjaToast.show(context, getString(R.string.toast_input_empty));
            } else {
                hideSearch();
                ninjaWebView.loadUrl(query);
            }
            return false;
        });
        omniBox_text.setOnFocusChangeListener((v, hasFocus) -> {
            if (omniBox_text.hasFocus()) {
                HelperUnit.showSoftKeyboard(omniBox_text);
                sp.edit().putString("sp_search_customSearches", "").apply();
                ninjaWebView.stopLoading();
                initSearch();
                omniBox_text.selectAll();
            }
        });
        omniBox_overview.setOnClickListener(v -> showOverview());
        omniBox_overview.setOnLongClickListener(v -> {
            performGesture("setting_gesture_overViewButton");
            return true;
        });
    }

    @SuppressLint({"UnsafeOptInUsageError"})
    private void updateOmniBox() {
        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            try {
                RecordAction action = new RecordAction(context);
                action.open(true);
                if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK)) {
                    omniBox_overview.setImageResource(R.drawable.icon_bookmark_added);
                } else {
                    omniBox_overview.setImageResource(R.drawable.icon_bookmark);
                }
                action.close();
            }
            catch (Exception e) {Log.i(TAG, "dialogCustomSearches:" + e);}
        }

        badgeDrawable.setVisible(BrowserContainer.size() > 1);
        badgeDrawable.setNumber(BrowserContainer.size());
        BadgeUtils.attachBadgeDrawable(badgeDrawable, omniBox_overview, findViewById(R.id.layout));
        bottom_navigation.getOrCreateBadge(R.id.page_0).setNumber(BrowserContainer.size());

        ninjaWebView = (NinjaWebView) currentAlbumController;
        String url = ninjaWebView.getUrl();
        ninjaWebView.initPreferences(url);

        if (url != null) {
            progressBar.setVisibility(GONE);
            ninjaWebView.setProfileIcon(omniBox_tab,omniBox_tab, url);
            if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty())
                omniBox_title.setText(url);
            else omniBox_title.setText(ninjaWebView.getTitle());
        }
    }

    private void initSearchPanel() {

        bottomSheetDialog_searchOnSite = new BottomSheetDialog(context);
        bottomSheetDialog_searchOnSite.setContentView(R.layout.sheet_search_site);
        bottomSheetDialog_searchOnSite.setCancelable(false);
        Objects.requireNonNull(bottomSheetDialog_searchOnSite.getWindow()).clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        searchBox = bottomSheetDialog_searchOnSite.findViewById(R.id.searchBox_input);
        Button searchUp = bottomSheetDialog_searchOnSite.findViewById(R.id.searchBox_up);
        Button searchDown = bottomSheetDialog_searchOnSite.findViewById(R.id.searchBox_down);
        Button searchCancel = bottomSheetDialog_searchOnSite.findViewById(R.id.searchBox_cancel);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) { if (currentAlbumController != null) ((NinjaWebView) currentAlbumController).findAllAsync(s.toString()); }
        });
        assert searchUp != null;
        searchUp.setOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(false));
        assert searchDown != null;
        searchDown.setOnClickListener(v -> ((NinjaWebView) currentAlbumController).findNext(true));
        assert searchCancel != null;
        searchCancel.setOnClickListener(v -> {
            if (searchBox.getText().length() > 0) searchBox.setText("");
            else {
                bottomSheetDialog_searchOnSite.cancel();}
        });
    }

    public void initSearch() {
        RecordAction action = new RecordAction(this);
        List<Record> list = action.listEntries(activity);
        adapterSearch = new AdapterSearch(this, R.layout.item_list, list);
        list_search.setAdapter(adapterSearch);
        list_search.setTextFilterEnabled(true);
        adapterSearch.notifyDataSetChanged();
        list_search.setOnItemClickListener((parent, view, position, id) -> {
            hideSearch();
            omniBox_text.clearFocus();
            String url = ((TextView) view.findViewById(R.id.dateView)).getText().toString();
            ninjaWebView.loadUrl(url);
        });
        list_search.setOnItemLongClickListener((adapterView, view, i, l) -> {
            String title = ((TextView) view.findViewById(R.id.titleView)).getText().toString();
            String url = ((TextView) view.findViewById(R.id.dateView)).getText().toString();
            showContextMenuLink(title, url, SRC_ANCHOR_TYPE, true);
            return true;
        });
        omniBox_text.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapterSearch.getFilter().filter(s);
                sp.edit().putString("searchInput", s.toString()).apply();
            }
        });
    }

    private void showOverview() {
        initOverview();
        dialogOverview.show();
    }

    public void hideSearch() {
        dialogSearch.cancel();
        try {dialogCustomSearches.cancel();} catch (Exception e) {Log.i(TAG, "dialogCustomSearches:" + e);}
    }

    public void hideOverview() {
        dialogOverview.cancel();
    }

    private void setSelectedTab() {
        if (overViewTab.equals(getString(R.string.album_title_tab))) bottom_navigation.setSelectedItemId(R.id.page_0);
        else if (overViewTab.equals(getString(R.string.album_title_home))) bottom_navigation.setSelectedItemId(R.id.page_1);
        else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) bottom_navigation.setSelectedItemId(R.id.page_2);
        else if (overViewTab.equals(getString(R.string.album_title_history))) bottom_navigation.setSelectedItemId(R.id.page_3);
    }

    // OverflowMenu

    private void hideOverflow () {
        dialog_overflow.cancel();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showOverflow() {

        if (ninjaWebView.getUrl() == null) {
            ninjaWebView.loadUrl("about:blank");
        }

        final String url = ninjaWebView.getUrl();
        final String title = ninjaWebView.getTitle();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu_overflow, null);

        LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
        TextView overflowURL = dialogView.findViewById(R.id.overflowURL);
        overflowURL.setText(url);
        HelperUnit.setHighLightedText(context, overflowURL, url, HelperUnit.domain(url));
        textGroup.setOnClickListener(v -> NinjaToast.show(context, url));
        TextView menuTitle = dialogView.findViewById(R.id.overflowTitle);
        menuTitle.setText(title);

        builder.setView(dialogView);
        dialog_overflow = builder.create();

        FloatingActionButton buttonProfile = dialogView.findViewById(R.id.buttonProfile);
        buttonProfile.setOnClickListener(v -> {
            showDialogFastToggle();
            dialog_overflow.cancel();
        });

        List_standard listStandard = new List_standard(context);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.colorError, typedValue, true);
        int color = typedValue.data;
        if (listStandard.isWhite(url)) {
            buttonProfile.getDrawable().mutate().setTint(color);
        }

        final GridView menu_grid_tab = dialogView.findViewById(R.id.overflow_tab);
        final GridView menu_grid_share = dialogView.findViewById(R.id.overflow_share);
        final GridView menu_grid_save = dialogView.findViewById(R.id.overflow_save);
        final GridView menu_grid_other = dialogView.findViewById(R.id.overflow_other);

        menu_grid_tab.setVisibility(VISIBLE);
        menu_grid_share.setVisibility(GONE);
        menu_grid_save.setVisibility(GONE);
        menu_grid_other.setVisibility(GONE);

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            menu_grid_tab.setNumColumns(2);
            menu_grid_share.setNumColumns(2);
            menu_grid_save.setNumColumns(2);
            menu_grid_other.setNumColumns(2);
        }

        // Tab

        GridItem openFav = new GridItem( getString(R.string.menu_openFav), R.drawable.icon_fav_pref);
        GridItem openTab = new GridItem( getString(R.string.main_menu_new_tabOpen), R.drawable.icon_tab_plus_pref);
        GridItem reload = new GridItem( getString(R.string.menu_reload), R.drawable.icon_refresh_pref);
        GridItem close = new GridItem( getString(R.string.menu_closeTab), R.drawable.icon_tab_remove_pref);
        GridItem exit = new GridItem( getString(R.string.menu_quit), R.drawable.icon_close_pref);

        final List<GridItem> gridList_tab = new LinkedList<>();

        gridList_tab.add(gridList_tab.size(), openFav);
        gridList_tab.add(gridList_tab.size(), openTab);
        gridList_tab.add(gridList_tab.size(), close);
        gridList_tab.add(gridList_tab.size(), reload);
        gridList_tab.add(gridList_tab.size(), exit);

        GridAdapter gridAdapter_tab = new GridAdapter(context, gridList_tab);
        menu_grid_tab.setAdapter(gridAdapter_tab);
        gridAdapter_tab.notifyDataSetChanged();

        menu_grid_tab.setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
            if (position == 0) NinjaToast.show(context, openFav.getTitle());
            else if (position == 1) NinjaToast.show(context, openTab.getTitle());
            else if (position == 2) NinjaToast.show(context, close.getTitle());
            else if (position == 3) NinjaToast.show(context, reload.getTitle());
            else if (position == 4) NinjaToast.show(context, exit.getTitle());
            return true;
        });

        menu_grid_tab.setOnItemClickListener((parent, view14, position, id) -> {
            String favURL = Objects.requireNonNull(sp.getString("favoriteURL", "https://www.google.com"));
            if (position == 0) {
                ninjaWebView.loadUrl(favURL);
                dialog_overflow.cancel();
            }  else if (position == 1) {
                addAlbum(getString(R.string.app_name), favURL, true);
                dialog_overflow.cancel();
            } else if (position == 2) {
                removeAlbum(currentAlbumController);
                dialog_overflow.cancel();
            } else if (position == 3) {
                ninjaWebView.reload();
                dialog_overflow.cancel();
            } else if (position == 4) {
                doubleTapsQuit();
                dialog_overflow.cancel();
            } });

        // Save
        GridItem saveHome = new GridItem( getString(R.string.menu_save_home), R.drawable.icon_web_pref);
        GridItem saveBookmark = new GridItem( getString(R.string.menu_save_bookmark), R.drawable.icon_bookmark_pref);
        GridItem savePDF = new GridItem( getString(R.string.menu_save_pdf), R.drawable.icon_file_pref);
        GridItem saveAs = new GridItem( getString(R.string.menu_save_as), R.drawable.icon_menu_save_pref);
        GridItem saveFav = new GridItem( getString(R.string.menu_fav), R.drawable.icon_fav_pref);

        final List<GridItem> gridList_save = new LinkedList<>();
        gridList_save.add(gridList_save.size(), saveHome);
        gridList_save.add(gridList_save.size(), saveBookmark);
        gridList_save.add(gridList_save.size(), savePDF);
        gridList_save.add(gridList_save.size(), saveAs);
        gridList_save.add(gridList_save.size(), saveFav);

        GridAdapter gridAdapter_save = new GridAdapter(context, gridList_save);
        menu_grid_save.setAdapter(gridAdapter_save);
        gridAdapter_save.notifyDataSetChanged();

        menu_grid_save.setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
            if (position == 0) NinjaToast.show(context, saveHome.getTitle());
            else if (position == 1) NinjaToast.show(context, saveBookmark.getTitle());
            else if (position == 2) NinjaToast.show(context, saveBookmark.getTitle());
            else if (position == 3) NinjaToast.show(context, savePDF.getTitle());
            else if (position == 4) NinjaToast.show(context, saveAs.getTitle());
            return true;
        });

        menu_grid_save.setOnItemClickListener((parent, view13, position, id) -> {
            RecordAction action = new RecordAction(context);
            if (position == 0) {
                save_atHome(title, url);
                dialog_overflow.cancel();
            }
            else if (position == 1) {
                saveBookmark();
                action.close();
                dialog_overflow.cancel();
            }
            else if (position == 2) {
                printPDF();
            }
            else if (position == 3) {
                assert url != null;
                if (url.startsWith("data:")) {
                    DataURIParser dataURIParser = new DataURIParser(url);
                    HelperUnit.saveDataURI(activity, dataURIParser, dialog_overflow);
                } else HelperUnit.saveAs(activity, url, null, dialog_overflow);
                dialog_overflow.cancel();
            }
            else if (position == 4) {
                sp.edit().putString("favoriteURL", url).apply();
                NinjaToast.show(this, R.string.app_done);
                dialog_overflow.cancel();
            }
        });

        // Share
        GridItem shareLink = new GridItem( getString(R.string.menu_share_link), R.drawable.icon_link_pref);
        GridItem post = new GridItem( getString(R.string.dialog_postOnWebsite), R.drawable.icon_post_pref);
        GridItem shareClipboard = new GridItem( getString(R.string.menu_shareClipboard), R.drawable.icon_clipboard_pref);
        GridItem openWith = new GridItem( getString(R.string.menu_shareOpenWith), R.drawable.icon_share_open_with_pref);
        GridItem shortcut = new GridItem( getString(R.string.menu_sc), R.drawable.icon_home_pref);

        final List<GridItem> gridList_share = new LinkedList<>();
        gridList_share.add(gridList_share.size(), shareLink);
        gridList_share.add(gridList_share.size(), shareClipboard);
        gridList_share.add(gridList_share.size(), openWith);
        gridList_share.add(gridList_share.size(), post);
        gridList_share.add(gridList_share.size(), shortcut);

        GridAdapter gridAdapter_share = new GridAdapter(context, gridList_share);
        menu_grid_share.setAdapter(gridAdapter_share);
        gridAdapter_share.notifyDataSetChanged();

        menu_grid_share.setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
            if (position == 0) NinjaToast.show(context, shareLink.getTitle());
            else if (position == 1) NinjaToast.show(context, shareClipboard.getTitle());
            else if (position == 2) NinjaToast.show(context, openWith.getTitle());
            else if (position == 3) NinjaToast.show(context, post.getTitle());
            else if (position == 4) NinjaToast.show(context, shortcut.getTitle());
            return true;
        });

        menu_grid_share.setOnItemClickListener((parent, view12, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                assert url != null;
                shareLink(title, url);
            } else if (position == 1) {
                copyLink(url);
            } else if (position == 2) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                context.startActivity(Intent.createChooser(intent, null));
            } else if (position == 3) {
                String text = title + ": " + url;
                postLink(text, dialog_overflow);
            }
            else if (position == 4) {
                HelperUnit.createShortcut(context, ninjaWebView, ninjaWebView.getTitle(), ninjaWebView.getOriginalUrl());
                dialog_overflow.cancel();
            }
        });

        // Other
        GridItem searchSite = new GridItem( getString(R.string.menu_other_searchSite), R.drawable.icon_search_site_pref);
        GridItem openDownload = new GridItem( getString(R.string.menu_download), R.drawable.icon_download_pref);
        GridItem openSettings = new GridItem( getString(R.string.setting_label), R.drawable.icon_settings_pref);
        GridItem restartAndReload = new GridItem( getString(R.string.menu_restart), R.drawable.icon_restart_pref);
        GridItem help = new GridItem( getString((R.string.app_help)), R.drawable.icon_help_pref);

        final List<GridItem> gridList_other = new LinkedList<>();
        gridList_other.add(gridList_other.size(), searchSite);
        gridList_other.add(gridList_other.size(), openDownload);
        gridList_other.add(gridList_other.size(), openSettings);
        gridList_other.add(gridList_other.size(), restartAndReload);
        gridList_other.add(gridList_other.size(), help);

        GridAdapter gridAdapter_other = new GridAdapter(context, gridList_other);
        menu_grid_other.setAdapter(gridAdapter_other);
        gridAdapter_other.notifyDataSetChanged();

        menu_grid_other.setOnItemLongClickListener((arg0, arg1, position, arg3) -> {
            if (position == 0) NinjaToast.show(context, searchSite.getTitle());
            else if (position == 1) NinjaToast.show(context, openDownload.getTitle());
            else if (position == 2) NinjaToast.show(context, openSettings.getTitle());
            else if (position == 3) NinjaToast.show(context, restartAndReload.getTitle());
            else if (position == 4) NinjaToast.show(context, help.getTitle());
            return true;
        });

        menu_grid_other.setOnItemClickListener((parent, view1, position, id) -> {
            if (position == 0) {
                searchOnSite();
                dialog_overflow.cancel();
            } else if (position == 1) {
                startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null));
                dialog_overflow.cancel();
            } else if (position == 2) {
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
                dialog_overflow.cancel();
            } else if (position == 3) {
                dialog_overflow.cancel();
                triggerRebirth(context);
            } else if (position == 4) {
                Uri webpage = Uri.parse("https://www.google.com");
                BrowserUnit.intentURL(this, webpage);
                dialog_overflow.cancel();
            }
        });

        TabLayout tabLayout = dialogView.findViewById(R.id.tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    menu_grid_tab.setVisibility(VISIBLE);
                    menu_grid_share.setVisibility(GONE);
                    menu_grid_save.setVisibility(GONE);
                    menu_grid_other.setVisibility(GONE); }
                else if (tab.getPosition() == 1) {
                    menu_grid_tab.setVisibility(GONE);
                    menu_grid_share.setVisibility(VISIBLE);
                    menu_grid_save.setVisibility(GONE);
                    menu_grid_other.setVisibility(GONE); }
                else if (tab.getPosition() == 2) {
                    menu_grid_tab.setVisibility(GONE);
                    menu_grid_share.setVisibility(GONE);
                    menu_grid_save.setVisibility(VISIBLE);
                    menu_grid_other.setVisibility(GONE); }
                else if (tab.getPosition() == 3) {
                    menu_grid_tab.setVisibility(GONE);
                    menu_grid_share.setVisibility(GONE);
                    menu_grid_save.setVisibility(GONE);
                    menu_grid_other.setVisibility(VISIBLE); }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        menu_grid_tab.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeRight() { Objects.requireNonNull(tabLayout.getTabAt(3)).select();}
            public void onSwipeLeft() { Objects.requireNonNull(tabLayout.getTabAt(1)).select();}});
        menu_grid_share.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeRight() { Objects.requireNonNull(tabLayout.getTabAt(0)).select();}
            public void onSwipeLeft() { Objects.requireNonNull(tabLayout.getTabAt(2)).select();}});
        menu_grid_save.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeRight() { Objects.requireNonNull(tabLayout.getTabAt(1)).select();}
            public void onSwipeLeft() { Objects.requireNonNull(tabLayout.getTabAt(3)).select();}});
        menu_grid_other.setOnTouchListener(new SwipeTouchListener(context) {
            public void onSwipeRight() { Objects.requireNonNull(tabLayout.getTabAt(2)).select();}
            public void onSwipeLeft() { Objects.requireNonNull(tabLayout.getTabAt(0)).select();}});

        HelperUnit.setupDialog(context, dialog_overflow);
        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.icon_image_broken);
        dialog_overflow.show();
    }

    // Menus

    public void showContextMenuLink (String title, final String url, int type, boolean showAll) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        if (title.isEmpty()) {
            title = HelperUnit.domain(url);
        }
        String finalTitle = title;
        LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
        TextView menuURL = dialogView.findViewById(R.id.menuURL);
        menuURL.setText(url);
        HelperUnit.setHighLightedText(context, menuURL, url, HelperUnit.domain(url));
        textGroup.setOnClickListener(v -> NinjaToast.show(context, url));
        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(finalTitle);
        ImageView menu_icon = dialogView.findViewById(R.id.menu_icon);

        if (type == SRC_ANCHOR_TYPE) {
            try(FaviconHelper faviconHelper = new FaviconHelper(context)) {
                Bitmap bitmap = faviconHelper.getFavicon(url);
                if (bitmap != null) menu_icon.setImageBitmap(bitmap);
                else menu_icon.setImageResource(R.drawable.icon_link);
            }
        }
        else if (type == IMAGE_TYPE) menu_icon.setImageResource(R.drawable.icon_image);
        else menu_icon.setImageResource(R.drawable.icon_link);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        FloatingActionButton buttonProfile = dialogView.findViewById(R.id.buttonProfile);
        ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
        buttonProfile.setOnClickListener(v -> {
            sp.edit().putString("profile", "profileStandard").apply();
            ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
        });
        HelperUnit.setupDialog(context, dialog);

        GridItem tabOpen = new GridItem( getString(R.string.main_menu_new_tabOpen), R.drawable.icon_tab_plus_pref);
        GridItem tabOpenBack = new GridItem( getString(R.string.main_menu_new_tab), R.drawable.icon_tab_background_pref);
        GridItem shareLink = new GridItem( getString(R.string.menu_share_link), R.drawable.icon_link_pref);
        GridItem shareClipboard = new GridItem( getString(R.string.menu_shareClipboard), R.drawable.icon_clipboard_pref);
        GridItem saveAs = new GridItem( getString(R.string.menu_save_as), R.drawable.icon_menu_save_pref);
        GridItem saveHome = new GridItem( getString(R.string.menu_save_home), R.drawable.icon_web_pref);
        GridItem openWith = new GridItem( getString(R.string.menu_shareOpenWith), R.drawable.icon_share_open_with_pref);
        GridItem delete = new GridItem( getString(R.string.menu_delete), R.drawable.icon_delete_pref);

        final List<GridItem> gridList = new LinkedList<>();

        gridList.add(gridList.size(), tabOpen);
        gridList.add(gridList.size(), tabOpenBack);
        gridList.add(gridList.size(), openWith);
        gridList.add(gridList.size(), shareLink);
        gridList.add(gridList.size(), shareClipboard);
        gridList.add(gridList.size(), saveAs);
        gridList.add(gridList.size(), saveHome);

        if (showAll) {
            gridList.add(gridList.size(), delete);
        }

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            menu_grid.setNumColumns(2);
        }
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {

            dialog.cancel();

            switch (position) {
                case 0:
                    hideSearch();
                    addAlbum(finalTitle, url, true);
                    break;
                case 1:
                    addAlbum(finalTitle, url, false);
                    break;
                case 2:
                    hideSearch();
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    context.startActivity(Intent.createChooser(intent, null));
                    break;
                case 3:
                    hideSearch();
                    shareLink(HelperUnit.domain(url), url);
                    dialogSearch.cancel();
                    break;
                case 4:
                    hideSearch();
                    copyLink(url);
                    break;
                case 5:
                    hideSearch();
                    HelperUnit.saveAs(activity,  url, null, dialog);
                    break;
                case 6:
                    hideSearch();
                    save_atHome(finalTitle, url);
                    break;
                case 7:
                    hideSearch();
                    MaterialAlertDialogBuilder builderSubMenu = new MaterialAlertDialogBuilder(context);
                    builderSubMenu.setTitle(R.string.menu_delete);
                    builderSubMenu.setMessage(R.string.hint_database);
                    builderSubMenu.setIcon(R.drawable.icon_delete);
                    builderSubMenu.setPositiveButton(R.string.app_ok, (dialog2, whichButton) -> {
                        RecordAction action = new RecordAction(this);
                        action.open(true);
                        action.deleteURL(url, RecordUnit.TABLE_START);
                        action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                        action.deleteURL(url, RecordUnit.TABLE_HISTORY);
                        action.close();
                        adapterSearch.notifyDataSetChanged();
                        initSearch();
                    });
                    builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog2, whichButton) -> builderSubMenu.setCancelable(true));
                    Dialog dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    HelperUnit.setupDialog(context, dialogSubMenu);
                    break; }
        });
    }

    private void showContextMenuList(final String title, final String url,
                                     final AdapterRecord adapterRecord, final List<Record> recordList, final int location) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        LinearLayout textGroup = dialogView.findViewById(R.id.textGroup);
        TextView menuURL = dialogView.findViewById(R.id.menuURL);
        menuURL.setText(url);
        HelperUnit.setHighLightedText(context, menuURL, url, HelperUnit.domain(url));
        textGroup.setOnClickListener(v -> NinjaToast.show(context, url));
        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(title);

        FaviconHelper.setFavicon(context, dialogView, url, R.id.menu_icon, R.drawable.icon_image_broken);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        FloatingActionButton buttonProfile = dialogView.findViewById(R.id.buttonProfile);
        ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
        buttonProfile.setOnClickListener(v -> {
            sp.edit().putString("profile", "profileStandard").apply();
            ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
        });
        HelperUnit.setupDialog(context, dialog);

        GridItem tabOpen = new GridItem( getString(R.string.main_menu_new_tabOpen), R.drawable.icon_tab_plus_pref);
        GridItem tabOpenBack = new GridItem( getString(R.string.main_menu_new_tab), R.drawable.icon_tab_background_pref);
        GridItem shareLink = new GridItem( getString(R.string.menu_share_link), R.drawable.icon_link_pref);
        GridItem delete = new GridItem( getString(R.string.menu_delete), R.drawable.icon_delete_pref);
        GridItem edit = new GridItem( getString(R.string.menu_edit), R.drawable.icon_edit_pref);

        final List<GridItem> gridList = new LinkedList<>();

        if (overViewTab.equals(getString(R.string.album_title_bookmarks)) || overViewTab.equals(getString(R.string.album_title_home))) {
            gridList.add(gridList.size(), tabOpen);
            gridList.add(gridList.size(), tabOpenBack);
            gridList.add(gridList.size(), shareLink);
            gridList.add(gridList.size(), delete);
            gridList.add(gridList.size(), edit); }
        else {
            gridList.add(gridList.size(), tabOpen);
            gridList.add(gridList.size(), tabOpenBack);
            gridList.add(gridList.size(), shareLink);
            gridList.add(gridList.size(), delete); }

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // In landscape
            menu_grid.setNumColumns(2);
        }
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            MaterialAlertDialogBuilder builderSubMenu;
            AlertDialog dialogSubMenu;
            switch (position) {
                case 0:
                    addAlbum(title, url, true);
                    dialog.cancel();
                    break;
                case 1:
                    addAlbum(title, url, false);
                    dialog.cancel();
                    break;
                case 2:
                    shareLink(title, url);
                    dialog.cancel();
                    break;
                case 3:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    builderSubMenu.setTitle(R.string.menu_delete);
                    builderSubMenu.setIcon(R.drawable.icon_delete);
                    builderSubMenu.setMessage(R.string.hint_database);
                    builderSubMenu.setPositiveButton(R.string.app_ok, (dialog2, whichButton) -> {
                        Record record = recordList.get(location);
                        RecordAction action = new RecordAction(context);
                        action.open(true);
                        if (overViewTab.equals(getString(R.string.album_title_home))) action.deleteURL(record.getURL(), RecordUnit.TABLE_START);
                        else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) action.deleteURL(record.getURL(), RecordUnit.TABLE_BOOKMARK);
                        else if (overViewTab.equals(getString(R.string.album_title_history))) action.deleteURL(record.getURL(), RecordUnit.TABLE_HISTORY);
                        action.close();
                        recordList.remove(location);
                        adapterRecord.notifyDataSetChanged();
                        dialog.cancel();
                        updateOmniBox();
                    });
                    builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog2, whichButton) -> builderSubMenu.setCancelable(true));
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    HelperUnit.setupDialog(context, dialogSubMenu);
                    break;
                case 4:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);

                    View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit, null);
                    TextInputLayout editBottomLayout = dialogViewSubMenu.findViewById(R.id.editBottomLayout);
                    TextInputLayout editTopLayout = dialogViewSubMenu.findViewById(R.id.editTopLayout);
                    editBottomLayout.setHint(activity.getString(R.string.dialog_URL_hint));
                    editTopLayout.setHint(activity.getString(R.string.dialog_title_hint));
                    EditText editTop = dialogViewSubMenu.findViewById(R.id.editTop);
                    EditText editBottom = dialogViewSubMenu.findViewById(R.id.editBottom);
                    editTop.setText(title);
                    editBottom.setText(url);

                    MaterialCardView ib_icon = dialogViewSubMenu.findViewById(R.id.editIcon);
                    ib_icon.setVisibility(VISIBLE);

                    if (!overViewTab.equals(getString(R.string.album_title_bookmarks))) ib_icon.setVisibility(GONE);
                    ib_icon.setOnClickListener(v -> {
                        MaterialAlertDialogBuilder builderFilter = new MaterialAlertDialogBuilder(context);
                        View dialogViewFilter = View.inflate(context, R.layout.dialog_menu, null);
                        builderFilter.setView(dialogViewFilter);
                        builderFilter.setTitle(R.string.setting_filter);
                        builderFilter.setIcon(R.drawable.icon_sort_icon);
                        AlertDialog dialogFilter = builderFilter.create();
                        dialogFilter.show();
                        HelperUnit.setupDialog(context, dialogFilter);
                        CardView cardView = dialogViewFilter.findViewById(R.id.albumCardView);
                        cardView.setVisibility(GONE);

                        GridView menuEditFilter = dialogViewFilter.findViewById(R.id.menu_grid);
                        final List<GridItem> menuEditFilterList = new LinkedList<>();
                        sp.edit().putString("showFilterDialogX", "true").apply();
                        HelperUnit.addFilterItems(activity, menuEditFilterList);
                        GridAdapter menuEditFilterAdapter = new GridAdapter(context, menuEditFilterList);
                        menuEditFilter.setNumColumns(2);
                        menuEditFilter.setHorizontalSpacing(20);
                        menuEditFilter.setVerticalSpacing(20);
                        menuEditFilter.setAdapter(menuEditFilterAdapter);
                        menuEditFilterAdapter.notifyDataSetChanged();
                        menuEditFilter.setOnItemClickListener((parent2, view2, position2, id2) -> {
                            newIcon = menuEditFilterList.get(position2).getData();
                            HelperUnit.setFilterIcons(context, ib_icon, newIcon);
                            dialogFilter.cancel();
                        });
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                WRAP_CONTENT,
                                WRAP_CONTENT
                        );
                        params.setMargins(HelperUnit.convertDpToPixel(20f, context),
                                HelperUnit.convertDpToPixel(10f, context),
                                HelperUnit.convertDpToPixel(20f, context),
                                HelperUnit.convertDpToPixel(10f, context));
                        menuEditFilter.setLayoutParams(params);
                        dialogFilter.setOnCancelListener(dialogInterface -> sp.edit().putString("showFilterDialogX", "false").apply());
                    });
                    newIcon = recordList.get(location).getIconColor();
                    HelperUnit.setFilterIcons(context, ib_icon, newIcon);

                    builderSubMenu.setTitle(R.string.menu_edit);
                    builderSubMenu.setIcon(R.drawable.icon_edit);
                    builderSubMenu.setView(dialogViewSubMenu);
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    HelperUnit.setupDialog(context, dialogSubMenu);

                    Button ib_cancel = dialogViewSubMenu.findViewById(R.id.editCancel);
                    ib_cancel.setOnClickListener(v -> dialogSubMenu.cancel());
                    Button ib_ok = dialogViewSubMenu.findViewById(R.id.editOK);
                    ib_ok.setOnClickListener(v -> {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                            action.deleteURL(editBottom.getText().toString(), RecordUnit.TABLE_BOOKMARK);
                            action.addBookmark(new Record(editTop.getText().toString(), editBottom.getText().toString(), 0, 0, newIcon));
                            updateOmniBox();
                            NinjaToast.show(this, R.string.app_done);
                            action.close();
                            bottom_navigation.setSelectedItemId(R.id.page_2);}
                        else {
                            RecordAction action = new RecordAction(context);
                            action.open(true);
                            action.deleteURL(url, RecordUnit.TABLE_START);
                            action.deleteURL(editBottom.getText().toString(), RecordUnit.TABLE_START);
                            action.addStartSite(new Record(editTop.getText().toString(), editBottom.getText().toString(), 0, 0, newIcon));
                            NinjaToast.show(this, R.string.app_done);
                            action.close();
                            bottom_navigation.setSelectedItemId(R.id.page_1); }
                        dialogSubMenu.cancel();
                        dialog.cancel();
                    });
                    break;
            }
        });
    }

    // Dialogs

    private void showDialogFastToggle() {

        listStandard = new List_standard(context);
        ninjaWebView = (NinjaWebView) currentAlbumController;
        String url = ninjaWebView.getUrl();

        String profile;
        if (listStandard.isWhite(url)) {
            profile = HelperUnit.domain(url);
        } else {
            profile = sp.getString("profile", "profileStandard");
        }

        if (url != null) {

            MaterialAlertDialogBuilder builderFastToggle = new MaterialAlertDialogBuilder(context);
            View dialogViewFastToggle = View.inflate(context, R.layout.dialog_settings, null);
            builderFastToggle.setView(dialogViewFastToggle);
            AlertDialog dialogFastToggle = builderFastToggle.create();
            HelperUnit.setupDialog(context, dialogFastToggle);

            LinearLayout textGroup = dialogViewFastToggle.findViewById(R.id.textGroup);
            TextView overflowURL = dialogViewFastToggle.findViewById(R.id.textGroup_menuURL);
            overflowURL.setText(url);
            HelperUnit.setHighLightedText(context, overflowURL, url, HelperUnit.domain(url));
            textGroup.setOnClickListener(v -> NinjaToast.show(context, url));
            TextView overflowTitle = dialogViewFastToggle.findViewById(R.id.textGroup_menuTitle);
            overflowTitle.setText(ninjaWebView.getTitle());
            FaviconHelper.setFavicon(context, dialogViewFastToggle, url, R.id.menu_icon, R.drawable.icon_image_broken);

            FloatingActionButton buttonProfile = dialogViewFastToggle.findViewById(R.id.buttonProfile);
            ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
            buttonProfile.setOnClickListener(v -> {
                sp.edit().putString("profile", "profileStandard").apply();
                ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
            });

            Button ib_save = dialogViewFastToggle.findViewById(R.id.ib_save);
            Button ib_delete = dialogViewFastToggle.findViewById(R.id.ib_delete);

            if (listStandard.isWhite(url)) {
                ib_save.setVisibility(GONE);
                ib_delete.setVisibility(VISIBLE);
            } else {
                ib_save.setVisibility(VISIBLE);
                ib_delete.setVisibility(GONE);
            }

            RelativeLayout checkbox_reset = dialogViewFastToggle.findViewById(R.id.checkbox_reset);
            ImageView icon_standard = dialogViewFastToggle.findViewById(R.id.icon_standard);

            if (sp.getBoolean("sp_standard_always", true)) {
                icon_standard.setImageResource(R.drawable.icon_check);
            } else {
                icon_standard.setImageResource(R.drawable.icon_close);
            }

            if (sp.getBoolean("sp_standard_restart", true)) {
                icon_standard.setImageResource(R.drawable.icon_restart);
            }

            checkbox_reset.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(context, checkbox_reset);
                popupMenu.getMenuInflater().inflate(R.menu.menu_standard, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    if (menuItem.getItemId() == R.id.menu_standardAlways) {
                        sp.edit().putBoolean("sp_standard_always", true).apply();
                        sp.edit().putBoolean("sp_standard_restart", false).apply();
                        icon_standard.setImageResource(R.drawable.icon_check);
                    } else if (menuItem.getItemId() == R.id.menu_standardNever) {
                        sp.edit().putBoolean("sp_standard_always", false).apply();
                        sp.edit().putBoolean("sp_standard_restart", false).apply();
                        icon_standard.setImageResource(R.drawable.icon_close);
                    } else if (menuItem.getItemId() == R.id.menu_standardRestart) {
                        sp.edit().putBoolean("sp_standard_always", false).apply();
                        sp.edit().putBoolean("sp_standard_restart", true).apply();
                        icon_standard.setImageResource(R.drawable.icon_restart);
                    }
                    return true;
                });
                // Showing the popup menu
                popupMenu.show();
            });

            Button checkbox_redirect = dialogViewFastToggle.findViewById(R.id.checkbox_redirect);
            checkbox_redirect.setOnClickListener(v -> new CustomRedirectsDialog().show(getSupportFragmentManager(),"redirect"));

            CheckBox checkbox_screenOn = dialogViewFastToggle.findViewById(R.id.checkbox_screenOn);
            checkbox_screenOn.setChecked(sp.getBoolean("sp_screenOn", false));
            checkbox_screenOn.setOnClickListener(v -> {
                sp.edit().putBoolean("sp_screenOn", checkbox_screenOn.isChecked()).apply();
                checkbox_screenOn.setChecked(sp.getBoolean("sp_screenOn", true));
                dialogFastToggle.cancel();
                triggerRebirth(context);
            });

            CheckBox checkbox_links = dialogViewFastToggle.findViewById(R.id.checkbox_links);
            if (SDK_INT >= Build.VERSION_CODES.TIRAMISU && sp.getBoolean("sp_tabBackground", false)) {
                int notificationAllowed = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
                if (notificationAllowed != PackageManager.PERMISSION_GRANTED) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
                    builder.setIcon(R.drawable.icon_alert);
                    builder.setMessage(R.string.app_permission);
                    builder.setTitle(R.string.app_permission_notification);
                    builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1234567));
                    builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    HelperUnit.setupDialog(activity, dialog);
                }
            }
            checkbox_links.setChecked(sp.getBoolean("sp_tabBackground", false));
            checkbox_links.setOnClickListener(v -> {
                sp.edit().putBoolean("sp_tabBackground", checkbox_links.isChecked()).apply();
                checkbox_links.setChecked(sp.getBoolean("sp_tabBackground", true));
            });

            TextView titleViewSettings = dialogViewFastToggle.findViewById(R.id.titeViewSettings);
            String s = context.getString(R.string.app_name) + " " + context.getString(R.string.setting_label);
            titleViewSettings.setText(s);

            CheckBox checkbox_image = dialogViewFastToggle.findViewById(R.id.checkbox_image);
            checkbox_image.setChecked(sp.getBoolean(profile + "_images", false));
            checkbox_image.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_images", checkbox_image.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_images", checkbox_image.isChecked()).apply();
                }
            });

            CheckBox checkbox_java = dialogViewFastToggle.findViewById(R.id.checkbox_java);
            checkbox_java.setChecked(sp.getBoolean(profile + "_javascript", false));
            checkbox_java.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_javascript", checkbox_java.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_javascript", checkbox_java.isChecked()).apply();
                }
            });

            CheckBox checkbox_javaPopUp = dialogViewFastToggle.findViewById(R.id.checkbox_javaPopUp);
            checkbox_javaPopUp.setChecked(sp.getBoolean(profile + "_javascriptPopUp", false));
            checkbox_javaPopUp.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_javascriptPopUp", checkbox_javaPopUp.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_javascriptPopUp", checkbox_javaPopUp.isChecked()).apply();
                }
            });

            CheckBox checkbox_cookies = dialogViewFastToggle.findViewById(R.id.checkbox_cookies);
            checkbox_cookies.setChecked(sp.getBoolean(profile + "_cookies", false));
            checkbox_cookies.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_cookies", checkbox_cookies.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_cookies", checkbox_cookies.isChecked()).apply();
                }
            });

            CheckBox checkbox_cookiesThirdParty = dialogViewFastToggle.findViewById(R.id.checkbox_cookiesThirdParty);
            checkbox_cookiesThirdParty.setChecked(sp.getBoolean(profile + "_cookiesThirdParty", false));
            checkbox_cookiesThirdParty.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked()).apply();
                }
            });

            CheckBox checkbox_cookiesBanner = dialogViewFastToggle.findViewById(R.id.checkbox_cookiesBanner);
            checkbox_cookiesBanner.setChecked(sp.getBoolean(profile + "_deny_cookie_banners", true));
            checkbox_cookiesBanner.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_deny_cookie_banners", checkbox_cookiesBanner.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_deny_cookie_banners", checkbox_cookiesBanner.isChecked()).apply();
                }
            });

            CheckBox checkbox_fingerPrint = dialogViewFastToggle.findViewById(R.id.checkbox_fingerPrint);
            checkbox_fingerPrint.setChecked(sp.getBoolean(profile + "_fingerPrintProtection", true));
            checkbox_fingerPrint.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_fingerPrintProtection", checkbox_fingerPrint.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_fingerPrintProtection", checkbox_fingerPrint.isChecked()).apply();
                }
            });

            CheckBox checkbox_adBlock = dialogViewFastToggle.findViewById(R.id.checkbox_adBlock);
            checkbox_adBlock.setChecked(sp.getBoolean(profile + "_adBlock", true));
            checkbox_adBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_adBlock", checkbox_adBlock.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_adBlock", checkbox_adBlock.isChecked()).apply();
                }
            });

            CheckBox checkbox_trackingURL = dialogViewFastToggle.findViewById(R.id.checkbox_trackingURL);
            checkbox_trackingURL.setChecked(sp.getBoolean(profile + "_trackingULS", true));
            checkbox_trackingURL.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_trackingULS", checkbox_trackingURL.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_trackingULS", checkbox_trackingURL.isChecked()).apply();
                }
            });

            CheckBox checkbox_saveData = dialogViewFastToggle.findViewById(R.id.checkbox_saveData);
            checkbox_saveData.setChecked(sp.getBoolean(profile + "_saveData", true));
            checkbox_saveData.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_saveData", checkbox_saveData.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_saveData", checkbox_saveData.isChecked()).apply();
                }
            });

            CheckBox checkbox_history = dialogViewFastToggle.findViewById(R.id.checkbox_history);
            checkbox_history.setChecked(sp.getBoolean(profile + "_saveHistory", true));
            checkbox_history.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_saveHistory", checkbox_history.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_saveHistory", checkbox_history.isChecked()).apply();
                }
            });

            CheckBox checkbox_location = dialogViewFastToggle.findViewById(R.id.checkbox_location);
            checkbox_location.setChecked(sp.getBoolean(profile + "_location", false));
            checkbox_location.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_location", checkbox_location.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_location", checkbox_location.isChecked()).apply();
                }
            });

            CheckBox checkbox_mic = dialogViewFastToggle.findViewById(R.id.checkbox_mic);
            checkbox_mic.setChecked(sp.getBoolean(profile + "_microphone", false));
            checkbox_mic.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_microphone", checkbox_mic.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_microphone", checkbox_mic.isChecked()).apply();
                }
            });

            CheckBox checkbox_camera = dialogViewFastToggle.findViewById(R.id.checkbox_camera);
            checkbox_camera.setChecked(sp.getBoolean(profile + "_camera", false));
            checkbox_camera.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_camera", checkbox_camera.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_camera", checkbox_camera.isChecked()).apply();
                }
            });

            CheckBox checkbox_dom = dialogViewFastToggle.findViewById(R.id.checkbox_dom);
            checkbox_dom.setChecked(sp.getBoolean(profile + "_dom", false));
            checkbox_dom.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_dom", checkbox_dom.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_dom", checkbox_dom.isChecked()).apply();
                }
            });

            RelativeLayout layout_nightView = dialogViewFastToggle.findViewById(R.id.layout_nightView);
            CheckBox checkbox_nightView = dialogViewFastToggle.findViewById(R.id.checkbox_nightView);
            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if ((nightModeFlags == Configuration.UI_MODE_NIGHT_YES) && !sp.getString("sp_theme", "1").equals("2")) {
                layout_nightView.setVisibility(VISIBLE);
            } else  {
                layout_nightView.setVisibility(GONE);
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                checkbox_nightView.setChecked(sp.getBoolean(profile + "_night", true));
                checkbox_nightView.setOnClickListener(v -> {
                    if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                        sp.edit().putBoolean(profile + "_night", checkbox_nightView.isChecked()).apply();
                    } else if (profile.equals("profileStandard")) {
                        ninjaWebView.setProfileChanged();
                        ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                        sp.edit().putBoolean("profileChanged_night", checkbox_nightView.isChecked()).apply();
                    }
                });
            }

            CheckBox checkbox_desktop = dialogViewFastToggle.findViewById(R.id.checkbox_desktop);
            checkbox_desktop.setChecked(sp.getBoolean(profile + "_desktop", false));
            checkbox_desktop.setOnClickListener(v -> {
                if (listStandard.isWhite(url) || profile.equals("profileChanged")){
                    sp.edit().putBoolean(profile + "_desktop", checkbox_desktop.isChecked()).apply();
                } else if (profile.equals("profileStandard")) {
                    ninjaWebView.setProfileChanged();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                    sp.edit().putBoolean("profileChanged_desktop", checkbox_desktop.isChecked()).apply();
                }
            });

            ib_save.setOnClickListener(v -> {
                listStandard.removeDomain(HelperUnit.domain(url));
                listStandard.addDomain(HelperUnit.domain(url));
                sp.edit().putString("profile", HelperUnit.domain(url)).apply();
                String profileToSave = HelperUnit.domain(url);
                sp.edit()
                        .putBoolean(profileToSave + "_saveData", checkbox_saveData.isChecked())
                        .putBoolean(profileToSave + "_images", checkbox_image.isChecked())
                        .putBoolean(profileToSave + "_adBlock", checkbox_adBlock.isChecked())
                        .putBoolean(profileToSave + "_trackingULS", checkbox_trackingURL.isChecked())
                        .putBoolean(profileToSave + "_location", checkbox_location.isChecked())
                        .putBoolean(profileToSave + "_fingerPrintProtection", checkbox_fingerPrint.isChecked())
                        .putBoolean(profileToSave + "_cookies", checkbox_cookies.isChecked())
                        .putBoolean(profileToSave + "_cookiesThirdParty", checkbox_cookiesThirdParty.isChecked())
                        .putBoolean(profileToSave + "_deny_cookie_banners", checkbox_cookiesBanner.isChecked())
                        .putBoolean(profileToSave + "_javascript", checkbox_java.isChecked())
                        .putBoolean(profileToSave + "_javascriptPopUp", checkbox_javaPopUp.isChecked())
                        .putBoolean(profileToSave + "_saveHistory", checkbox_history.isChecked())
                        .putBoolean(profileToSave + "_camera", checkbox_camera.isChecked())
                        .putBoolean(profileToSave + "_microphone", checkbox_mic.isChecked())
                        .putBoolean(profileToSave + "_dom", checkbox_dom.isChecked())
                        .putBoolean(profileToSave + "_night", checkbox_nightView.isChecked())
                        .putBoolean(profileToSave + "_desktop", checkbox_desktop.isChecked()).apply();
                if (sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").apply();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                }
                ninjaWebView.setProfileIcon(buttonProfile,omniBox_tab, url);
                dialogFastToggle.cancel();
                ninjaWebView.reload();
            });

            ib_delete.setOnClickListener(view -> {
                listStandard.removeDomain(HelperUnit.domain(url));
                String profileToSave = HelperUnit.domain(url);
                sp.edit()
                        .remove(profileToSave + "_saveData")
                        .remove(profileToSave + "_images")
                        .remove(profileToSave + "_adBlock")
                        .remove(profileToSave + "_trackingULS")
                        .remove(profileToSave + "_location")
                        .remove(profileToSave + "_fingerPrintProtection")
                        .remove(profileToSave + "_cookies")
                        .remove(profileToSave + "_cookiesThirdParty")
                        .remove(profileToSave + "_deny_cookie_banners")
                        .remove(profileToSave + "_javascript")
                        .remove(profileToSave + "_javascriptPopUp")
                        .remove(profileToSave + "_saveHistory")
                        .remove(profileToSave + "_camera")
                        .remove(profileToSave + "_microphone")
                        .remove(profileToSave + "_dom")
                        .remove(profileToSave + "_night")
                        .remove(profileToSave + "_desktop").apply();
                if (sp.getBoolean("sp_standard_always", true)) {
                    sp.edit().putString("profile", "profileStandard").apply();
                    ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                }
                ninjaWebView.setProfileIcon(buttonProfile, omniBox_tab, url);
                dialogFastToggle.cancel();
                ninjaWebView.reload();
            });

            Button ib_reload = dialogViewFastToggle.findViewById(R.id.ib_reload);
            ib_reload.setOnClickListener(view -> {
                if (ninjaWebView != null) {
                    dialogFastToggle.cancel();
                    ninjaWebView.reload();
                }
            });

            Button ib_settings = dialogViewFastToggle.findViewById(R.id.ib_settings);
            ib_settings.setOnClickListener(view -> {
                if (ninjaWebView != null) {
                    dialogFastToggle.cancel();
                    Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                    startActivity(settings);
                }
            });

            Button button_help = dialogViewFastToggle.findViewById(R.id.button_help);
            button_help.setOnClickListener(view -> {
                dialogFastToggle.cancel();
                Uri webpage = Uri.parse("https://www.google.com");
                BrowserUnit.intentURL(this, webpage);
            });

            dialogFastToggle.show();
        } else {
            NinjaToast.show(context, getString(R.string.app_error));
        }
    }

    private void showDialogFilter() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);
        builder.setTitle(R.string.setting_filter);
        builder.setIcon(R.drawable.icon_sort_icon);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        HelperUnit.setupDialog(context, dialog);
        CardView cardView = dialogView.findViewById(R.id.albumCardView);
        cardView.setVisibility(GONE);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        final List<GridItem> gridList = new LinkedList<>();
        sp.edit().putString("showFilterDialogX", "true").apply();
        HelperUnit.addFilterItems(activity, gridList);

        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setNumColumns(2);
        menu_grid.setHorizontalSpacing(20);
        menu_grid.setVerticalSpacing(20);
        menu_grid.setAdapter(gridAdapter);

        if (menu_grid.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) menu_grid.getLayoutParams();
            p.setMargins(56, 56, 56, 56);
            menu_grid.requestLayout();
        }

        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            filter = true;
            filterBy = gridList.get(position).getData();
            dialog.cancel();
            bottom_navigation.setSelectedItemId(R.id.page_2);
        });
        dialog.setOnCancelListener(dialogInterface -> sp.edit().putString("showFilterDialogX", "false").apply());
    }

    private void showDialogCustomSearches(String url) {

        if (dialogOverview.isShowing()) {
            dialogOverview.cancel();
        }

        if (!url.isEmpty()) {
            ninjaWebView.stopLoading();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            View dialogView = View.inflate(context, R.layout.custom_redirects_list, null);
            RecyclerView recyclerView = dialogView.findViewById(R.id.redirects_recycler);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            ArrayList<CustomRedirect> redirects = new ArrayList<>();
            try {
                redirects = CustomSearchesHelper.getRedirects(sp);
            } catch (JSONException e) {
                Log.e("Searches parsing", e.toString());
            }
            AdapterCustomSearches adapter = new AdapterCustomSearches(context, url, redirects);
            recyclerView.setAdapter(adapter);

            builder.setTitle(url);
            builder.setIcon(R.drawable.icon_custom_searches);
            builder.setPositiveButton(R.string.create_new, ((dialogInterface, i) -> {
                if (Objects.equals(ninjaWebView.getUrl(), "about:blank")) {
                    ninjaWebView.loadUrl(sp.getString("favoriteURL", "https://www.google.com"));
                } else {
                    dialogCustomSearches.cancel();
                }
                MaterialAlertDialogBuilder builderAddCustom = new MaterialAlertDialogBuilder(context);
                View dialogViewAddCustom = View.inflate(context, R.layout.create_new_searches, null);
                TextInputEditText source = dialogViewAddCustom.findViewById(R.id.source);
                TextInputEditText target = dialogViewAddCustom.findViewById(R.id.target);
                builderAddCustom.setTitle(R.string.custom_searches_title);
                builderAddCustom.setIcon(R.drawable.icon_custom_searches);
                builderAddCustom.setNegativeButton(R.string.app_cancel, null);
                builderAddCustom.setPositiveButton(R.string.app_ok, ((dialogInterface2, i2) -> {
                    String sourceText = Objects.requireNonNull(source.getText()).toString();
                    String targetText = Objects.requireNonNull(target.getText()).toString();
                    if (targetText.isEmpty() || sourceText.isEmpty()) return;
                    adapter.addRedirect(new CustomRedirect(sourceText, targetText));
                    try {
                        CustomSearchesHelper.saveRedirects(adapter.getRedirects());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }));
                builderAddCustom.setView(dialogViewAddCustom);
                AlertDialog dialogCustomSearchesNew = builderAddCustom.create();
                dialogCustomSearchesNew.show();
                HelperUnit.setupDialog(context, dialogCustomSearchesNew);
            }));
            builder.setNegativeButton(R.string.app_cancel, ((dialogInterface, i) -> {
                if (Objects.equals(ninjaWebView.getUrl(), "about:blank")) {
                    ninjaWebView.loadUrl(sp.getString("favoriteURL", "https://www.google.com"));
                } else {
                    dialogCustomSearches.cancel();
                }
            }));
            builder.setView(dialogView);
            dialogCustomSearches = builder.create();
            dialogCustomSearches.show();
            dialogCustomSearches.setCancelable(false);
            HelperUnit.setupDialog(context, dialogCustomSearches);
        } else {
            NinjaToast.show(this, R.string.toast_input_empty);
        }
    }

    // Voids

    private void doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) finishAndRemoveTask();
        else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.setting_title_confirm_exit);
            builder.setIcon(R.drawable.icon_alert);
            builder.setMessage(R.string.toast_quit);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> finishAndRemoveTask());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);}
    }

    private void saveOpenedTabs() {
        ArrayList<String> openTabs = new ArrayList<>();
        for (int i = 0; i < BrowserContainer.size(); i++) {
            if (currentAlbumController == BrowserContainer.get(i))
                openTabs.add(0, ((NinjaWebView) (BrowserContainer.get(i))).getUrl());
            else openTabs.add(((NinjaWebView) (BrowserContainer.get(i))).getUrl()); }
        sp.edit().putString("openTabs", TextUtils.join("‚‗‚", openTabs)).apply();
    }

    private void setCustomFullscreen(boolean fullscreen) {
        if (fullscreen) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.hide(WindowInsets.Type.statusBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            }
            else getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); }
        else {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                final WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    insetsController.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    insetsController.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE); }
            }
            else getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN); }
    }

    private void printPDF() {
        String title = HelperUnit.fileName(ninjaWebView.getUrl());
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
        Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        sp.edit().putBoolean("pdf_create", true).apply();
    }

    private void save_atHome(final String title, final String url) {
        FaviconHelper faviconHelper = new FaviconHelper(context);
        faviconHelper.addFavicon(context, ninjaWebView.getUrl(), ninjaWebView.getFavicon());
        String message = context.getString(R.string.app_error) + ": " + context.getString(R.string.app_error_save);

        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(url, RecordUnit.TABLE_START)) NinjaToast.show(this, message);
        else {
            int counter = sp.getInt("counter", 0);
            counter = counter + 1;
            sp.edit().putInt("counter", counter).apply();
            if (action.addStartSite(new Record(title, url, 0, counter, 0))) NinjaToast.show(this, R.string.app_done);
            else NinjaToast.show(this, R.string.app_error); }
        action.close();
    }

    private void copyLink(String url) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("text", url);
        Objects.requireNonNull(clipboard).setPrimaryClip(clip);
        String text = getString(R.string.toast_copy_successful) + ": " + url;
        NinjaToast.show(this, text);
    }

    public void shareLink(String title, String url) {

        List_standard listStandard = new List_standard(context);
        String profile = sp.getString("profile", "profileStandard");
        if (listStandard.isWhite(url)) profile = HelperUnit.domain(url);

        boolean removeTracking = sp.getBoolean(profile + "_trackingULS", true);

        if (removeTracking && url.contains("?") && url.contains("/")) {

            String lastIndex = url.substring(url.lastIndexOf("/"));
            String tracking = url.substring(url.lastIndexOf("?"));
            String urlClean = url.replace(tracking, "");

            if (lastIndex.contains(tracking)) {

                String m = context.getString(R.string.dialog_tracking) + " \"" + tracking + "\"" + "?";

                if (m.length() > 150) {
                    m = m.substring(0, 150) + " [...]?\"";
                }

                GridItem item_01 = new GridItem(context.getString(R.string.app_ok), R.drawable.icon_check_pref);
                GridItem item_02 = new GridItem( context.getString(R.string.app_no), R.drawable.icon_close_pref);
                GridItem item_03 = new GridItem( context.getString(R.string.menu_edit), R.drawable.icon_edit_pref);

                View dialogView = View.inflate(context, R.layout.dialog_menu, null);
                MaterialAlertDialogBuilder builderTrack = new MaterialAlertDialogBuilder(context);

                CardView albumCardView = dialogView.findViewById(R.id.albumCardView);
                albumCardView.setVisibility(GONE);
                builderTrack.setTitle(url);
                builderTrack.setIcon(R.drawable.icon_tracking);
                builderTrack.setMessage(m);
                builderTrack.setPositiveButton(R.string.app_cancel, (dialog2, whichButton) -> dialog2.cancel());
                builderTrack.setView(dialogView);
                AlertDialog dialogTrack = builderTrack.create();
                dialogTrack.show();
                HelperUnit.setupDialog(context, dialogTrack);

                GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
                final List<GridItem> gridList = new LinkedList<>();
                gridList.add(gridList.size(), item_01);
                gridList.add(gridList.size(), item_02);
                gridList.add(gridList.size(), item_03);
                GridAdapter gridAdapter = new GridAdapter(context, gridList);
                menu_grid.setAdapter(gridAdapter);
                gridAdapter.notifyDataSetChanged();
                menu_grid.setOnItemClickListener((parent, view, position, id) -> {
                    switch (position) {

                        case 0:
                            dialogTrack.cancel();
                            Intent sharingIntentClean;
                            sharingIntentClean = new Intent(Intent.ACTION_SEND);
                            sharingIntentClean.setType("text/plain");
                            sharingIntentClean.putExtra(Intent.EXTRA_SUBJECT, title);
                            sharingIntentClean.putExtra(Intent.EXTRA_TEXT, urlClean);
                            context.startActivity(Intent.createChooser(sharingIntentClean, (context.getString(R.string.menu_share_link))));
                            break;
                        case 1:
                            dialogTrack.cancel();
                            Intent sharingIntent;
                            sharingIntent = new Intent(Intent.ACTION_SEND);
                            sharingIntent.setType("text/plain");
                            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
                            sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
                            context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
                            break;
                        case 2:
                            dialogTrack.cancel();
                            View dialogEdit = View.inflate(context, R.layout.dialog_edit, null);
                            TextInputLayout editBottomLayout = dialogEdit.findViewById(R.id.editBottomLayout);
                            TextInputLayout editTopLayout = dialogEdit.findViewById(R.id.editTopLayout);
                            editBottomLayout.setHint(activity.getString(R.string.dialog_URL_hint));
                            editTopLayout.setVisibility(GONE);
                            EditText input = dialogEdit.findViewById(R.id.editBottom);
                            input.setText(url);
                            HelperUnit.showSoftKeyboard(input);

                            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                            builder.setTitle(context.getString(R.string.menu_edit));
                            builder.setIcon(R.drawable.icon_tracking);
                            builder.setView(dialogEdit);
                            Dialog dialog = builder.create();

                            Button ib_cancel = dialogEdit.findViewById(R.id.editCancel);
                            ib_cancel.setOnClickListener(v -> dialog.cancel());
                            Button ib_ok = dialogEdit.findViewById(R.id.editOK);
                            ib_ok.setOnClickListener(v -> {
                                dialog.dismiss();
                                String newValue = Objects.requireNonNull(input.getText()).toString();
                                Intent sharingIntentEdit;
                                sharingIntentEdit = new Intent(Intent.ACTION_SEND);
                                sharingIntentEdit.setType("text/plain");
                                sharingIntentEdit.putExtra(Intent.EXTRA_SUBJECT, title);
                                sharingIntentEdit.putExtra(Intent.EXTRA_TEXT, newValue);
                                context.startActivity(Intent.createChooser(sharingIntentEdit, (context.getString(R.string.menu_share_link))));
                            });
                            dialog.show();
                            HelperUnit.setupDialog(context, dialog);
                            break;
                    }
                });
            }
        } else {
            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
            sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
            context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
        }
    }

    private void postLink(String data, Dialog dialogParent) {
        String urlForPosting = sp.getString("urlForPosting", "");

        if (!urlForPosting.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("text", data);
            Objects.requireNonNull(clipboard).setPrimaryClip(clip);
            String text = getString(R.string.toast_copy_successful) + " -  " + data;
            NinjaToast.show(this, text);
            addAlbum("", urlForPosting, true);
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit, null);
            TextInputLayout editBottomLayout = dialogViewSubMenu.findViewById(R.id.editBottomLayout);
            TextInputLayout editTopLayout = dialogViewSubMenu.findViewById(R.id.editTopLayout);
            editBottomLayout.setHint(activity.getString(R.string.dialog_URL_hint));
            editTopLayout.setVisibility(GONE);
            EditText editBottom = dialogViewSubMenu.findViewById(R.id.editBottom);
            editBottomLayout.setHelperText(getString(R.string.dialog_postOnWebsiteHint));

            builder.setView(dialogViewSubMenu);
            builder.setTitle(data);
            builder.setIcon(R.drawable.icon_post);

            Dialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);

            Button ib_cancel = dialogViewSubMenu.findViewById(R.id.editCancel);
            ib_cancel.setOnClickListener(v -> dialog.cancel());
            Button ib_ok = dialogViewSubMenu.findViewById(R.id.editOK);
            ib_ok.setOnClickListener(v -> {
                String shareTop = editBottom.getText().toString().trim();
                sp.edit().putString("urlForPosting", shareTop).apply();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", data);
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                String text = getString(R.string.toast_copy_successful) + " -  " + data;
                NinjaToast.show(this, text);
                addAlbum("", shareTop, true);
                dialog.cancel();
                try {
                    dialogParent.cancel();
                } catch (Exception e) {
                    Log.i(TAG, "shouldOverrideUrlLoading Exception:" + e);
                }
            });
        }
    }

    private void searchOnSite() {
        bottomSheetDialog_searchOnSite.show();
        HelperUnit.showSoftKeyboard(searchBox);
    }

    private void saveBookmark() {
        FaviconHelper faviconHelper = new FaviconHelper(context);
        faviconHelper.addFavicon(context, ninjaWebView.getUrl(), ninjaWebView.getFavicon());
        RecordAction action = new RecordAction(context);
        action.open(true);
        String message = context.getString(R.string.app_error) + ": " + context.getString(R.string.app_error_save);
        if (action.checkUrl(ninjaWebView.getUrl(), RecordUnit.TABLE_BOOKMARK))
            NinjaToast.show(this, message);
        else {
            long value = 0;  //default red icon
            action.addBookmark(new Record(ninjaWebView.getTitle(), ninjaWebView.getUrl(), 0, 0, value));
            updateOmniBox();
            NinjaToast.show(this, R.string.app_done); }
        action.close();
    }

    private void performGesture(String gesture) {
        String gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"));
        switch (gestureAction) {
            case "01":
                break;
            case "02":
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.stopLoading();
                    WebBackForwardList mWebBackForwardList = ninjaWebView.copyBackForwardList();
                    String historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() + 1).getUrl();
                    ninjaWebView.initPreferences(historyUrl);
                    ninjaWebView.goForward();
                }
                else NinjaToast.show(this, R.string.toast_webview_forward);
                break;
            case "03":
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    Log.v(TAG, "FOSS Browser in fullscreen mode");
                } else if (ninjaWebView.canGoBack()){
                    ninjaWebView.goBack();
                } else removeAlbum(currentAlbumController);
                break;
            case "04":
                ninjaWebView.pageUp(true);
                break;
            case "05":
                ninjaWebView.pageDown(true);
                break;
            case "06":
                showAlbum(nextAlbumController(false));
                break;
            case "07":
                showAlbum(nextAlbumController(true));
                break;
            case "08":
                showOverview();
                break;
            case "09":
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://www.google.com")), true);
                break;
            case "10":
                removeAlbum(currentAlbumController);
                break;
            case "11":
                overViewTab = getString(R.string.album_title_tab);
                setSelectedTab();
                showOverview();
                break;
            case "12":
                shareLink(ninjaWebView.getTitle(), Objects.requireNonNull(ninjaWebView.getUrl()));
                break;
            case "13":
                searchOnSite();
                break;
            case "14":
                saveBookmark();
                break;
            case "15":
                save_atHome(ninjaWebView.getTitle(), ninjaWebView.getUrl());
                break;
            case "16":
                ninjaWebView.reload();
                break;
            case "17":
                ninjaWebView.loadUrl(Objects.requireNonNull(sp.getString("favoriteURL", "https://www.google.com")));
                break;
            case "18":
                bottom_navigation.setSelectedItemId(R.id.page_2);
                showOverview();
                showDialogFilter();
                break;
            case "19":
                showDialogFastToggle();
                break;
            case "22":
                sp.edit().putBoolean("sp_screenOn", !sp.getBoolean("sp_screenOn", false)).apply();
                triggerRebirth(context);
                break;
            case "24":
                copyLink(ninjaWebView.getUrl());
                break;
            case "25":
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
                break;
            case "26":
                doubleTapsQuit();
                break;
            case "27":
                sp.edit().putString("profile", "profileStandard").apply();
                ninjaWebView.reload();
                break;
            case "28":
                sp.edit().putBoolean("redirect", !sp.getBoolean("redirect", false)).apply();
                ninjaWebView.reload();
                break;
            case "29":
                startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null));
                break;
            case "30":
                overViewTab = getString(R.string.album_title_bookmarks);
                setSelectedTab();
                showOverview();
                break;
            case "31":
                overViewTab = getString(R.string.album_title_home);
                setSelectedTab();
                showOverview();
                break;
            case "32":
                overViewTab = getString(R.string.album_title_history);
                setSelectedTab();
                showOverview();
                break;
        }
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if (!sp.getBoolean("sp_close_tab_confirm", false)) okAction.run();
        else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.menu_closeTab);
            builder.setIcon(R.drawable.icon_alert);
            builder.setMessage(R.string.toast_quit_TAB);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> okAction.run());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog); }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void dispatchIntent(Intent intent) {

        String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);

        if ("".equals(action)) {
            Log.i(TAG, "resumed FOSS browser");
        } else if (filePathCallback != null) {
            filePathCallback = null;
            getIntent().setAction("");
        } else if (Intent.ACTION_VIEW.equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            addAlbum(null, Objects.requireNonNull(getIntent().getData()).toString(), true);
            BrowserUnit.openInBackground(activity, ninjaWebView);
        } else if ("postLink".equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            postLink(url, null);
        } else if ("customSearches".equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            if (BrowserContainer.size() == 0) {
                addAlbum(null, "", true);
            }
            assert url != null;
            showDialogCustomSearches(url);
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_PROCESS_TEXT)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            CharSequence text = getIntent().getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            assert text != null;
            url = text.toString();
            addAlbum(null, url, true);
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            url = Objects.requireNonNull(intent.getStringExtra(SearchManager.QUERY));
            addAlbum(null, url, true);
        } else if (url != null && Intent.ACTION_SEND.equals(action)) {
            sp.edit().putBoolean("show_overview", false).apply();
            getIntent().setAction("");
            addAlbum(null, url, true);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setWebView(String title, final String url, final boolean foreground) {
        ninjaWebView = new NinjaWebView(context);

        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
            sp.edit().putString("saved_key_ok", "yes")
                    .putString("setting_gesture_tb_up", "04")
                    .putString("setting_gesture_tb_down", "05")
                    .putString("setting_gesture_tb_left", "03")
                    .putString("setting_gesture_tb_right", "02")
                    .putString("setting_gesture_nav_up", "16")
                    .putString("setting_gesture_nav_down", "10")
                    .putString("setting_gesture_nav_left", "07")
                    .putString("setting_gesture_nav_right", "06")
                    .putString("setting_gesture_tabButton", "19")
                    .putString("setting_gesture_overViewButton", "18")
                    .putBoolean("sp_autofill", true)
                    .apply();
            ninjaWebView.setProfileDefaultValues();

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            View dialogView = View.inflate(context, R.layout.dialog_intro, null);
            builder.setView(dialogView);
            builder.setTitle(R.string.app_intro_a);
            builder.setIcon(R.drawable.icon_alert);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);

            MaterialButton startBrowsing = dialogView.findViewById(R.id.startBrowsing);
            startBrowsing.setOnClickListener(v -> {
                dialog.cancel();
                HelperUnit.showSoftKeyboard(omniBox_text);
            });

            MaterialButton openSettings = dialogView.findViewById(R.id.openSettings);
            openSettings.setOnClickListener(v -> {
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
                dialog.cancel();
            });
        }

        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title, url);
        activity.registerForContextMenu(ninjaWebView);

        if (url.isEmpty()) ninjaWebView.loadUrl("about:blank");
        else ninjaWebView.loadUrl(url);

        if (currentAlbumController != null) {
            ninjaWebView.setPredecessor(currentAlbumController);
            //save currentAlbumController and use when TAB is closed via Back button
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index); }
        else BrowserContainer.add(ninjaWebView);

        if (!foreground) ninjaWebView.deactivate();
        else {
            hideOverview();
            ninjaWebView.setBrowserController(this);
            ninjaWebView.activate();
            dialogOverview.cancel();
            showAlbum(ninjaWebView);
        }

        View albumView = ninjaWebView.getAlbumView();
        tab_container.addView(albumView, WRAP_CONTENT, WRAP_CONTENT);
        updateOmniBox();
    }

    private synchronized void addAlbum(String title, final String url, final boolean foreground) {
        setWebView(title, url, foreground);
    }

    private void triggerRebirth(Context context) {
        sp.edit().putInt("restart_changed", 0).apply();
        sp.edit().putBoolean("restoreOnRestart", true).apply();
        Snackbar snackbar = Snackbar.make(ninjaWebView, R.string.toast_restart, Snackbar.LENGTH_LONG);
        snackbar.setAction(context.getString(R.string.app_ok), (v -> {
            PackageManager packageManager = context.getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
            assert intent != null;
            ComponentName componentName = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(componentName);
            context.startActivity(mainIntent);
            System.exit(0);
        }));
        snackbar.show();
    }

    public static View getView() {
        return ninjaWebView.getRootView();
    }
}