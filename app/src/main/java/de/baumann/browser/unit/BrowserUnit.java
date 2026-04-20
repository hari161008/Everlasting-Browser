package de.baumann.browser.unit;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.ContentValues.TAG;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import de.baumann.browser.R;
import de.baumann.browser.activity.BrowserActivity;
import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.objects.CustomRedirect;
import de.baumann.browser.objects.CustomRedirectsHelper;

public class BrowserUnit {

    public static final int LOADING_STOPPED = 101;  //Must be > PROGRESS_MAX !
    public static final String MIME_TYPE_TEXT_PLAIN = "text/plain";
    public static final String URL_ENCODING = "UTF-8";
    public static final String URL_SCHEME_ABOUT = "about:";
    public static final String URL_SCHEME_MAIL_TO = "mailto:";
    private static final String SEARCH_ENGINE_GOOGLE = "https://www.google.com/search?q=";
    private static final String SEARCH_ENGINE_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    private static final String SEARCH_ENGINE_STARTPAGE = "https://startpage.com/do/search?query=";
    private static final String SEARCH_ENGINE_BING = "https://www.bing.com/search?q=";
    private static final String SEARCH_ENGINE_BAIDU = "https://www.baidu.com/s?wd=";
    private static final String SEARCH_ENGINE_QWANT = "https://www.qwant.com/?q=";
    private static final String SEARCH_ENGINE_ECOSIA = "https://www.ecosia.org/search?q=";
    private static final String SEARCH_ENGINE_Metager = "https://metager.org/meta/meta.ger3?eingabe=";
    private static final String SEARCH_ENGINE_STARTPAGE_DE = "https://startpage.com/do/search?lui=deu&language=deutsch&query=";
    private static final String SEARCH_ENGINE_SEARX = "https://searx.be/?q=";
    private static final String URL_ABOUT_BLANK = "about:blank";
    private static final String URL_SCHEME_FILE = "file://";
    private static final String URL_SCHEME_HTTPS = "https://";
    private static final String URL_SCHEME_HTTP = "http://";
    private static final String URL_SCHEME_FTP = "ftp://";
    private static final String URL_SCHEME_INTENT = "intent://";

    private static final int REQUEST_CODE_ASK_PERMISSIONS_4 = 1234567;

    public static boolean isURL(String url) {
        url = url.toLowerCase(Locale.getDefault());
        if (url.startsWith(URL_ABOUT_BLANK)
                || url.startsWith(URL_SCHEME_MAIL_TO)
                || url.startsWith(URL_SCHEME_FILE)
                || url.startsWith(URL_SCHEME_HTTP)
                || url.startsWith(URL_SCHEME_HTTPS)
                || url.startsWith(URL_SCHEME_FTP)
                || url.startsWith(URL_SCHEME_INTENT)) {
            return true;
        }

        String regex = "^((ftp|http|https|intent)?://)"                      // support scheme
                + "?(([0-9a-z_!~*'().&=+$%-]+: )?[0-9a-z_!~*'().&=+$%-]+@)?" // ftp的user@
                + "(([0-9]{1,3}\\.){3}[0-9]{1,3}"                            // IP形式的URL -> 199.194.52.184
                + "|"                                                        // 允许IP和DOMAIN（域名）
                + "([0-9a-z_!~*'()-]+\\.)*"                                  // 域名 -> www.
                + "([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\."                    // 二级域名
                + "[a-z]{2,6})"                                              // first level domain -> .com or .museum
                + "(:[0-9]{1,4})?"                                           // 端口 -> :80
                + "((/?)|"                                                   // a slash isn't required if there is no file name
                + "(/[0-9a-z_!~*'().;?:@&=+$,%#-]+)+/?)$";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(url).matches();
    }

    public static String queryWrapper(Context context, String query) {

        if (query.contains(";jsessionid=")) {
            String tracking = query.substring(query.lastIndexOf(";"));
            query = query.replace(tracking, "");
        }

        if (isURL(query) || query.isEmpty()) {
            if (query.startsWith(URL_SCHEME_ABOUT) || query.startsWith(URL_SCHEME_MAIL_TO)) {
                return query;
            }
            if (!query.contains("://")) {
                query = URL_SCHEME_HTTPS + query;
            }
            return query;
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String customSearchEngine = sp.getString("sp_search_engine_custom", "");
            String customSearches = sp.getString("sp_search_customSearches", "");
            query = query.replace("&", "%26");
            query = query.replace("#", "");
            //Override UserAgent if own UserAgent is defined
            if (!sp.contains("searchEngineSwitch")) {
                //if new switch_text_preference has never been used initialize the switch
                if (customSearchEngine.isEmpty()) {
                    sp.edit().putBoolean("searchEngineSwitch", false).apply();
                } else {
                    sp.edit().putBoolean("searchEngineSwitch", true).apply();
                }
            }

            if (!customSearches.isEmpty()) {
                return customSearches + query;
            } else if (sp.getBoolean("searchEngineSwitch", false)) {
                //if new switch_text_preference has never been used initialize the switch
                return customSearchEngine + query;
            } else {
                final int i = Integer.parseInt(Objects.requireNonNull(sp.getString("sp_search_engine", "0")));
                switch (i) {
                    case 1:
                        return SEARCH_ENGINE_STARTPAGE_DE + query;
                    case 2:
                        return SEARCH_ENGINE_BAIDU + query;
                    case 3:
                        return SEARCH_ENGINE_BING + query;
                    case 4:
                        return SEARCH_ENGINE_DUCKDUCKGO + query;
                    case 5:
                        return SEARCH_ENGINE_GOOGLE + query;
                    case 6:
                        return SEARCH_ENGINE_SEARX + query;
                    case 7:
                        return SEARCH_ENGINE_QWANT + query;
                    case 8:
                        return SEARCH_ENGINE_ECOSIA + query;
                    case 9:
                        return SEARCH_ENGINE_Metager + query;
                    default:
                        return SEARCH_ENGINE_STARTPAGE + query;
                }
            }
        }
    }

    public static void download(final Context context, final String url, final String fileName, final String mimeType) {
        Activity activity = (Activity) context;
        if (BackupUnit.checkPermissionStorage(context)) {
            try {
                if (url.startsWith("data:")) {
                    DataURIParser dataURIParser = new DataURIParser(url);
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(dataURIParser.getImagedata());
                    fos.flush();
                    fos.close();

                    String text = context.getString(R.string.app_done) + ". " + context.getString(R.string.menu_download) +"?";
                    Snackbar snackbar = Snackbar.make(BrowserActivity.getView(), text, Snackbar.LENGTH_LONG);
                    snackbar.setAction(context.getString(R.string.app_ok), v -> context.startActivity(Intent.createChooser(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS), null)));
                    snackbar.show();
                } else {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    CookieManager cookieManager = CookieManager.getInstance();
                    String cookie = cookieManager.getCookie(url);
                    request.addRequestHeader("Cookie", cookie);
                    request.addRequestHeader("Accept", "text/html, application/xhtml+xml, *" + "/" + "*");
                    request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
                    request.addRequestHeader("Referer", url);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setTitle(fileName);
                    request.setMimeType(mimeType);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    assert manager != null;
                    manager.enqueue(request);
                }
            } catch (Exception e) {
                Toast.makeText(context, context.getString(R.string.app_error) + e.toString().substring(e.toString().indexOf(":")), Toast.LENGTH_LONG).show();
                Log.i(TAG, "FOSS Browser: Error Downloading File:" + e);
            }
        } else {
            BackupUnit.requestPermission(activity);
        }
    }

    public static void clearHome(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_START);
        action.close();
    }

    public static void clearBookmark(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_BOOKMARK);
        action.close();
    }

    public static void clearHistory(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_HISTORY);
        action.close();
    }

    public static void  clearBrowserData(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        boolean clearCache = sp.getBoolean("sp_clear_cache", false);
        boolean clearCookie = sp.getBoolean("sp_clear_cookie", false);
        boolean clearHistory = sp.getBoolean("sp_clear_history", false);
        boolean clearIndexedDB = sp.getBoolean("sp_clearIndexedDB", false);
        boolean clearDB = sp.getBoolean("sp_deleteDatabase", false);
        boolean clearSettings = sp.getBoolean("sp_clear_settings", false);
        if (clearHistory) BrowserUnit.clearHistory(context);
        if (clearCache)  {
            try {
                File dir = context.getCacheDir();
                if (dir != null && dir.isDirectory()) deleteDir(dir);
            } catch (Exception exception) {
                Log.w("browser", "Error clearing cache");
            }
        }
        if (clearSettings) {
            sp.edit().clear().apply();
            List_standard listStandard = new List_standard(context);
            listStandard.clearDomains();
        }
        if (clearCookie) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.flush();
            cookieManager.removeAllCookies(value -> {
            });
        }
        if (clearDB) {
            context.deleteDatabase("Ninja4.db");
            context.deleteDatabase("faviconView.db");
            sp.edit().putInt("restart_changed", 1).apply();
        }
        if (clearIndexedDB) {
            File data = Environment.getDataDirectory();
            String blob_storage = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//blob_storage";
            String databases = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//databases";
            String indexedDB = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//IndexedDB";
            String localStorage = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Local Storage";
            String serviceWorker = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Service Worker";
            String sessionStorage = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Session Storage";
            String shared_proto_db = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//shared_proto_db";
            String VideoDecodeStats = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//VideoDecodeStats";
            String QuotaManager = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//QuotaManager";
            String QuotaManager_journal = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//QuotaManager-journal";
            String webData = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Web Data";
            String WebDataJournal = "//data//" + context.getPackageName() + "//app_webview//" + "//Default//" + "//Web Data-journal";
            final File blob_storage_file = new File(data, blob_storage);
            final File databases_file = new File(data, databases);
            final File indexedDB_file = new File(data, indexedDB);
            final File localStorage_file = new File(data, localStorage);
            final File serviceWorker_file = new File(data, serviceWorker);
            final File sessionStorage_file = new File(data, sessionStorage);
            final File shared_proto_db_file = new File(data, shared_proto_db);
            final File VideoDecodeStats_file = new File(data, VideoDecodeStats);
            final File QuotaManager_file = new File(data, QuotaManager);
            final File QuotaManager_journal_file = new File(data, QuotaManager_journal);
            final File webData_file = new File(data, webData);
            final File WebDataJournal_file = new File(data, WebDataJournal);

            BrowserUnit.deleteDir(blob_storage_file);
            BrowserUnit.deleteDir(databases_file);
            BrowserUnit.deleteDir(indexedDB_file);
            BrowserUnit.deleteDir(localStorage_file);
            BrowserUnit.deleteDir(serviceWorker_file);
            BrowserUnit.deleteDir(sessionStorage_file);
            BrowserUnit.deleteDir(shared_proto_db_file);
            BrowserUnit.deleteDir(VideoDecodeStats_file);
            BrowserUnit.deleteDir(QuotaManager_file);
            BrowserUnit.deleteDir(QuotaManager_journal_file);
            BrowserUnit.deleteDir(webData_file);
            BrowserUnit.deleteDir(WebDataJournal_file);
            WebStorage.getInstance().deleteAllData();
        }
    }

    public static void intentURL(Context context, Uri uri) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(uri);
        browserIntent.setPackage("de.baumann.browser");
        context.startActivity(browserIntent);
    }

    public static String redirectURL (WebView ninjaWebView, SharedPreferences sp, String url) {
        try {
            List<CustomRedirect> redirects = CustomRedirectsHelper.getRedirects(sp);
            for (int i = 0; i < redirects.size(); i++) {
                CustomRedirect customRedirect = redirects.get(i);
                if (url.contains(customRedirect.getSource()) && sp.getBoolean(customRedirect.getSource(), true)) {
                    ninjaWebView.stopLoading();
                    url = url.replace(customRedirect.getSource(), customRedirect.getTarget());
                    return url;
                }
            }
        } catch (JSONException e) {
            Log.e("Redirect error", e.toString());
        }
        return url;
    }

    public static void openInBackground(Activity activity, WebView webView) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);

        if (sp.getBoolean("sp_tabBackground", false)) {

            String url = webView.getUrl();
            String m = activity.getString(R.string.dialog_backGround);
            NotificationManager mNotifyMgr = (NotificationManager) activity.getSystemService(NOTIFICATION_SERVICE);
            Intent intentP = new Intent(activity, BrowserActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intentP, FLAG_IMMUTABLE);

            String name = "Links background";
            String description = "Open links in background -> click to open";
            int importance = NotificationManager.IMPORTANCE_LOW;
            //Important for heads-up notification
            NotificationChannel channel = new NotificationChannel("1", name, importance);
            channel.setDescription(description);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = activity.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(activity, "1")
                    .setSmallIcon(R.drawable.icon_web)
                    .setAutoCancel(true)
                    .setContentTitle(HelperUnit.domain(url))
                    .setContentText(m)
                    .setContentIntent(pendingIntent);
            Notification buildNotification = mBuilder.build();

            if (sp.getString("openBackground_dialog", "show").equals("show")) {
                MaterialAlertDialogBuilder builderSubMenu = new MaterialAlertDialogBuilder(activity);
                builderSubMenu.setTitle(R.string.dialog_backGround);
                builderSubMenu.setMessage(R.string.app_session);
                builderSubMenu.setIcon(R.drawable.icon_alert);
                builderSubMenu.setPositiveButton(R.string.app_always, (dialog2, whichButton) -> {
                    sp.edit().putString("openBackground_dialog", "always").apply();
                    displayNotification ( activity,  mNotifyMgr,  buildNotification);
                });
                builderSubMenu.setNegativeButton(R.string.app_never, (dialog2, whichButton) -> sp.edit().putString("openBackground_dialog", "never").apply());
                Dialog dialogSubMenu = builderSubMenu.create();
                dialogSubMenu.show();
                HelperUnit.setupDialog(activity, dialogSubMenu);
            } else  {
                if (!sp.getString("openBackground_dialog", "no").equals("never")) {
                    displayNotification ( activity,  mNotifyMgr,  buildNotification);
                }
            }
        }
    }

    private static void displayNotification(Activity activity, NotificationManager mNotifyMgr, Notification buildNotification) {
        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            int notificationAllowed = activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS);
            if (notificationAllowed != PackageManager.PERMISSION_GRANTED) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
                builder.setIcon(R.drawable.icon_alert);
                builder.setMessage(R.string.app_permission);
                builder.setTitle(R.string.app_permission_notification);
                builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> activity.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_ASK_PERMISSIONS_4));
                builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                AlertDialog dialog = builder.create();
                dialog.show();
                HelperUnit.setupDialog(activity, dialog);
            } else {
                mNotifyMgr.notify(4, buildNotification);
                activity.moveTaskToBack(true);
            }
        } else {
            mNotifyMgr.notify(4, buildNotification);
            activity.moveTaskToBack(true);
        }
    }
    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : Objects.requireNonNull(children)) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir != null && dir.delete();
    }
}