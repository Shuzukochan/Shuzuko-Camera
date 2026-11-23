package com.app.shuzukocamera;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

public class ShuzukoFileProvider extends ContentProvider {
    public static final String AUTHORITY = "com.app.shuzukocamera.provider";

    @Override
    public boolean onCreate() {
        return true;
    }

    private File getFileFromUri(Uri uri) {
        String fileName = uri.getLastPathSegment();
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        // Security check: prevent directory traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return null;
        }

        // 1. Try public DCIM directory
        File dcimFile = new File(Environment.getExternalStorageDirectory(), "DCIM/ShuzukoCamera/" + fileName);
        if (dcimFile.exists() && dcimFile.canRead()) {
            return dcimFile;
        }

        // 2. Try app's internal files directory
        if (getContext() != null) {
            File internalFile = new File(getContext().getFilesDir(), fileName);
            if (internalFile.exists() && internalFile.canRead()) {
                return internalFile;
            }
        }

        return null;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = getFileFromUri(uri);
        if (file != null && file.exists() && file.canRead()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }

        Log.e("ShuzukoFileProvider", "File not found: " + uri);
        throw new FileNotFoundException("File not found: " + uri);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        File file = getFileFromUri(uri);
        if (file == null || !file.exists()) {
            return null;
        }

        MatrixCursor cursor = new MatrixCursor(new String[]{"_display_name", "_size", "last_modified"});
        cursor.addRow(new Object[]{file.getName(), file.length(), file.lastModified()});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name != null) {
            if (name.endsWith(".mp4")) return "video/mp4";
            if (name.endsWith(".bmp") || name.endsWith(".jpg") || name.endsWith(".png")) return "image/jpeg";
            if (name.endsWith(".txt")) return "text/plain";
        }
        return "application/octet-stream";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
