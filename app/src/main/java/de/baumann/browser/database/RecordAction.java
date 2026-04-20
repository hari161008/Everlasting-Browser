package de.baumann.browser.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.unit.ClipboardUnit;
import de.baumann.browser.unit.RecordUnit;

public class RecordAction {
    public static final int HISTORY_ITEM = 0;
    public static final int STARTSITE_ITEM = 1;
    public static final int BOOKMARK_ITEM = 2;
    private final RecordHelper helper;
    private SQLiteDatabase database;

    public RecordAction(Context context) {
        this.helper = new RecordHelper(context);
    }
    public void open(boolean rw) {database = rw ? helper.getWritableDatabase() : helper.getReadableDatabase();}
    public void close() {
        helper.close();
    }

    public boolean addStartSite(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getTime() < 0L
                || record.getOrdinal() < 0) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        // Bookmark time is used for color, desktop mode, javascript, and List_standard content
        values.put(RecordUnit.COLUMN_FILENAME, record.getTime());
        values.put(RecordUnit.COLUMN_ORDINAL, record.getOrdinal());
        database.insert(RecordUnit.TABLE_START, null, values);
        return true;
    }

    public List<Record> listStartSite(Activity activity) {
        List<Record> list = new LinkedList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        String sortBy = Objects.requireNonNull(sp.getString("sort_startSite", "ordinal"));
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_START,
                new String[]{
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_FILENAME,
                        RecordUnit.COLUMN_ORDINAL
                },
                null,
                null,
                null,
                null,
                sortBy + " COLLATE NOCASE;"
        );
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getRecord(cursor, STARTSITE_ITEM));
            cursor.moveToNext();
        }
        cursor.close();
        if (!sortBy.equals("ordinal")) {
            Collections.reverse(list);
        }
        if (sp.getBoolean("sort_startSiteDomain", false)) {
            list.sort(Comparator.comparing(Record::getDomain));
        }
        return list;
    }

    public void addBookmark(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getTime() < 0L) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        values.put(RecordUnit.COLUMN_TIME, record.getIconColor());
        database.insert(RecordUnit.TABLE_BOOKMARK, null, values);
    }

    public List<Record> listBookmark(Context context, boolean filter, long filterBy) {

        List<Record> list = new LinkedList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = Objects.requireNonNull(sp.getString("sort_bookmark", "title"));
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_BOOKMARK,
                new String[]{
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME
                },
                null,
                null,
                null,
                null,
                sortBy + " COLLATE NOCASE;"
        );
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (filter) {
                if ((getRecord(cursor, BOOKMARK_ITEM).getIconColor()) == filterBy) {
                    list.add(getRecord(cursor, BOOKMARK_ITEM));
                }
            } else {
                list.add(getRecord(cursor, BOOKMARK_ITEM));
            }
            cursor.moveToNext();
        }
        cursor.close();

        if (sortBy.equals("time")) {
            //ignore desktop mode, JavaScript, and remote content when sorting colors
            list.sort(Comparator.comparing(Record::getTitle));
            list.sort(Comparator.comparingLong(Record::getIconColor));
        }
        if (sp.getBoolean("sort_bookmarkDomain", false)) {
            list.sort(Comparator.comparing(Record::getDomain));
        }
        Collections.reverse(list);
        return list;
    }

    public void addHistory(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getTime() < 0L) {
            return;
        }
        record.setTime(record.getTime());

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        values.put(RecordUnit.COLUMN_TIME, record.getTime());
        database.insert(RecordUnit.TABLE_HISTORY, null, values);
    }

    public List<Record> listHistory(Context context) {
        List<Record> list = new ArrayList<>();
        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_HISTORY,
                new String[]{
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME
                },
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_TIME + " COLLATE NOCASE;"
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getRecord(cursor, HISTORY_ITEM));
            cursor.moveToNext();
        }
        cursor.close();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if (sp.getBoolean("sort_historyDomain", false)) {
            list.sort(Comparator.comparing(Record::getDomain));
        }
        return list;
    }

    public void addDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(table, null, values);
    }

    public boolean checkDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[]{RecordUnit.COLUMN_DOMAIN},
                RecordUnit.COLUMN_DOMAIN + "=?",
                new String[]{domain.trim()},
                null,
                null,
                null
        );
        boolean result = cursor.moveToFirst();
        cursor.close();
        return result;
    }

    public void deleteDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        database.execSQL("DELETE FROM " + table + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public List<String> listDomains(String table) {
        List<String> list = new ArrayList<>();
        Cursor cursor = database.query(
                table,
                new String[]{RecordUnit.COLUMN_DOMAIN},
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_DOMAIN
        );
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public boolean checkUrl(String url, String table) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[]{RecordUnit.COLUMN_URL},
                RecordUnit.COLUMN_URL + "=?",
                new String[]{url.trim()},
                null,
                null,
                null
        );
        boolean result = cursor.moveToFirst();
        cursor.close();

        return result;
    }

    public void deleteURL(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }
        database.execSQL("DELETE FROM " + table + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + domain.trim() + "\"");
    }

    public void clearTable(String table) {
        database.execSQL("DELETE FROM " + table);
    }

    private Record getRecord(Cursor cursor, int type) {
        Record record = new Record();
        record.setTitle(cursor.getString(0));
        record.setURL(cursor.getString(1));
        record.setTime(cursor.getLong(2));

        if (type == BOOKMARK_ITEM) {
            record.setIconColor(record.getTime());
            record.setTime(0);  //time is no longer needed after extracting data
        } else if (type == HISTORY_ITEM) {
            record.setTime(record.getTime());
        }
        return record;
    }


    @Nullable
    private Record getClipboard(Activity activity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        if (!sp.getBoolean("sp_clipboard_record", false)) return null;
        String clipboardEntry = ClipboardUnit.getPrimary(activity);
        if (clipboardEntry == null || !URLUtil.isValidUrl(clipboardEntry)) return null;
        return new Record(
                activity.getString(R.string.link_you_copied), clipboardEntry, 0L, -1, 0L
        );
    }

    public List<Record> listEntries(Activity activity) {
        List<Record> list = new ArrayList<>();
        RecordAction action = new RecordAction(activity);
        action.open(false);
        list.addAll(action.listBookmark(activity, false, 0)); //move bookmarks to top of list
        list.addAll(action.listStartSite(activity));
        list.addAll(action.listHistory(activity.getApplicationContext()));
        // add the latest copied item from the clipboard if it's a valid URL
        Record clipboard = action.getClipboard(activity);
        if (clipboard != null) list.add(clipboard);
        action.close();
        return list;
    }
}