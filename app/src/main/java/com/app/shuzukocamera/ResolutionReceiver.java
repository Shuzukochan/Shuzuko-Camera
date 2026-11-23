package com.app.shuzukocamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

public class ResolutionReceiver extends BroadcastReceiver {
    public static final String ACTION_RESOLUTION = "com.app.shuzukocamera.ACTION_RESOLUTION_DETECTED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!ACTION_RESOLUTION.equals(action)) return;

        int width = intent.getIntExtra("width", 0);
        int height = intent.getIntExtra("height", 0);
        String packageName = intent.getStringExtra("package");
        String source = intent.getStringExtra("source");

        if (width <= 0 || height <= 0) return;

        Log.d("ShuzukoCamera", "Broadcast received resolution: " + width + "x" + height + " from " + packageName + " (" + source + ")");

        // 1. Save to SharedPreferences (reliable across all Android versions, immune to Scoped Storage)
        SharedPreferences prefs = context.getSharedPreferences("ShuzukoPrefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("detected_width", width)
                .putInt("detected_height", height)
                .putString("detected_package", packageName != null ? packageName : "")
                .putString("detected_source", source != null ? source : "")
                .putLong("detected_time", System.currentTimeMillis())
                .apply();

        // 2. Also save to DCIM/ShuzukoCamera/last_res.txt (ShuzukoCamera has storage permission)
        try {
            File dcimDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/ShuzukoCamera/");
            if (!dcimDir.exists()) {
                dcimDir.mkdirs();
            }
            File resFile = new File(dcimDir, "last_res.txt");
            FileOutputStream fos = new FileOutputStream(resFile, false);
            String content = "width=" + width + "\n" +
                    "height=" + height + "\n" +
                    "package=" + (packageName != null ? packageName : "") + "\n" +
                    "source=" + (source != null ? source : "") + "\n" +
                    "time=" + System.currentTimeMillis() + "\n";
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
        } catch (Throwable t) {
            Log.e("ShuzukoCamera", "Error saving last_res.txt: " + t.getMessage());
        }

        // 3. Notify MainActivity if active
        MainActivity mainActivity = MainActivity.getInstance();
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> mainActivity.onResolutionUpdated(width, height, packageName));
        }
    }
}
