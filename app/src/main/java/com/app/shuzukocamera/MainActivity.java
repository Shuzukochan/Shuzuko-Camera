package com.app.shuzukocamera;


import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity {

    private static final int REQUEST_IMAGE_PICK = 1002;
    
    private int targetWidth = 2400;  // Default width
    private int targetHeight = 1080; // Default height
    private int rotationDegrees = 0; // Default no rotation
    private boolean useCenterCrop = true; // Default center crop

    private Switch force_show_switch;
    private Switch disable_switch;
    private Switch play_sound_switch;
    private Switch force_private_dir;
    private Switch disable_toast_switch;

    private Button btnSelectImage;
    private RadioGroup radioGroupRotation;
    private RadioGroup radioGroupScaleMode;
    private TextView txtImageStatus;
    private TextView txtVideoStatus;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show();
            }else {
                File camera_dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/ShuzukoCamera/");
                if (!camera_dir.exists()){
                    camera_dir.mkdir();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sync_statue_with_files();
        updateFileStatus();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectImage = findViewById(R.id.btnSelectImage);
        radioGroupRotation = findViewById(R.id.radioGroupRotation);
        radioGroupScaleMode = findViewById(R.id.radioGroupScaleMode);
        txtImageStatus = findViewById(R.id.txtImageStatus);
        txtVideoStatus = findViewById(R.id.txtVideoStatus);

        force_show_switch = findViewById(R.id.switch1);
        disable_switch = findViewById(R.id.switch2);
        play_sound_switch = findViewById(R.id.switch3);
        force_private_dir = findViewById(R.id.switch4);
        disable_toast_switch = findViewById(R.id.switch5);

        sync_statue_with_files();
        updateFileStatus();

        // Rotation listener
        radioGroupRotation.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioRotate0) {
                rotationDegrees = 0;
            } else if (checkedId == R.id.radioRotate270) {
                rotationDegrees = 270;
            }
        });

        // Scale mode listener
        radioGroupScaleMode.setOnCheckedChangeListener((group, checkedId) -> {
            useCenterCrop = (checkedId == R.id.radioScaleCenterCrop);
        });

        btnSelectImage.setOnClickListener(v -> {
            if (!has_permission()) {
                request_permission();
            } else {
                // Ask for target resolution first
                showResolutionDialog();
            }
        });

        disable_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File disable_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/disable.jpg");
                    if (disable_file.exists() != b){
                        if (b){
                            try {
                                disable_file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            disable_file.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });

        force_show_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File force_show_switch = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/force_show.jpg");
                    if (force_show_switch.exists() != b){
                        if (b){
                            try {
                                force_show_switch.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            force_show_switch.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });

        play_sound_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (compoundButton.isPressed()) {
                    if (!has_permission()) {
                        request_permission();
                    } else {
                        File play_sound_switch = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/no-silent.jpg");
                        if (play_sound_switch.exists() != b){
                            if (b){
                                try {
                                    play_sound_switch.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else {
                                play_sound_switch.delete();
                            }
                        }
                    }
                    sync_statue_with_files();
                }
            }
        });

        force_private_dir.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File force_private_dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/private_dir.jpg");
                    if (force_private_dir.exists() != b){
                        if (b){
                            try {
                                force_private_dir.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            force_private_dir.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });


        disable_toast_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File disable_toast_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/no_toast.jpg");
                    if (disable_toast_file.exists() != b){
                        if (b){
                            try {
                                disable_toast_file.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            disable_toast_file.delete();
                        }
                    }
                }
                sync_statue_with_files();
            }
        });

    }

    private void request_permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                    || this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.permission_lack_warn);
                builder.setMessage(R.string.permission_description);

                builder.setNegativeButton(R.string.negative, (dialo gInterface, i) -> Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show());

                builder.setPositiveButton(R.string.positive, (dialogInterface, i) -> requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1));
                builder.show();
            }
        }
    }

    private boolean has_permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED
                    && this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED;
        }
        return true;
    }


    private void sync_statue_with_files() {
        Log.d(this.getApplication().getPackageName(), "【VCAM】[sync]同步开关状态");

        if (!has_permission()){
            request_permission();
        }else {
            File camera_dir = new File (Environment.getExternalStorageDirectory().getAbsolutePath()+"/DCIM/ShuzukoCamera");
            if (!camera_dir.exists()){
                camera_dir.mkdir();
            }
        }

        File disable_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/disable.jpg");
        disable_switch.setChecked(disable_file.exists());

        File force_show_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/force_show.jpg");
        force_show_switch.setChecked(force_show_file.exists());

        File play_sound_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/no-silent.jpg");
        play_sound_switch.setChecked(play_sound_file.exists());

        File force_private_dir_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/private_dir.jpg");
        force_private_dir.setChecked(force_private_dir_file.exists());

        File disable_toast_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/no_toast.jpg");
        disable_toast_switch.setChecked(disable_toast_file.exists());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri selectedUri = data.getData();
        if (selectedUri == null) {
            return;
        }

        try {
            if (requestCode == REQUEST_IMAGE_PICK) {
                // Show progress
                android.app.ProgressDialog progress = new android.app.ProgressDialog(this);
                progress.setMessage("Processing image...\n1. Resizing to " + targetWidth + "x" + targetHeight + "\n2. Creating video for preview");
                progress.setCancelable(false);
                progress.show();
                
                new Thread(() -> {
                    try {
                        // Resize and save image
                        resizeAndSaveImage(selectedUri);
                        
                        // Create video from image
                        File imageFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/1000.bmp");
                        createVideoFromImage(imageFile);
                        
                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(this, "✓ DONE!\n• Image saved: 1000.bmp\n• Preview created: virtual.mp4\n\nNow: Restart target app!", Toast.LENGTH_LONG).show();
                            updateFileStatus();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("MainActivity", "Processing failed", e);
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error copying file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("MainActivity", "Error copying file", e);
        }
    }

    private void copyFileToShuzukoCamera(Uri sourceUri, String targetFileName) throws IOException {
        File ShuzukoCameraDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/");
        if (!ShuzukoCameraDir.exists()) {
            ShuzukoCameraDir.mkdirs();
        }

        File targetFile = new File(ShuzukoCameraDir, targetFileName);
        
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri);
             FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            
            if (inputStream == null) {
                throw new IOException("Cannot open input stream");
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }

    private void updateFileStatus() {
        File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/1000.bmp");
        File videoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/virtual.mp4");

        if (imageFile.exists()) {
            long sizeKB = imageFile.length() / 1024;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                txtImageStatus.setText("Capture: ✓ " + options.outWidth + "x" + options.outHeight + " (" + sizeKB + " KB)");
            } catch (Exception e) {
                txtImageStatus.setText("Capture: ✓ (" + sizeKB + " KB)");
            }
        } else {
            txtImageStatus.setText("Capture: Not selected");
        }

        if (videoFile.exists()) {
            long sizeKB = videoFile.length() / 1024;
            txtVideoStatus.setText("Preview: ✓ Video created (" + sizeKB + " KB)");
        } else {
            txtVideoStatus.setText("Preview: Not created");
        }
    }

    private void checkAllPaths() {
        StringBuilder info = new StringBuilder();
        info.append("=== FILE LOCATIONS ===\n\n");

        String publicPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/";
        File publicDir = new File(publicPath);
        
        info.append("📁 Files are stored in:\n");
        info.append(publicPath).append("\n\n");
        info.append("Directory exists: ").append(publicDir.exists() ? "✓ Yes" : "✗ No (will be created)").append("\n\n");

        if (publicDir.exists()) {
            File image = new File(publicDir, "1000.bmp");
            
            info.append("🖼️ 1000.bmp: ");
            if (image.exists()) {
                long kb = image.length() / 1024;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
                String time = sdf.format(new java.util.Date(image.lastModified()));
                info.append("✓ (").append(kb).append(" KB)\n");
                info.append("   Last modified: ").append(time).append("\n");
                
                // Show dimensions
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(image.getAbsolutePath(), options);
                    info.append("   Dimensions: ").append(options.outWidth).append("x").append(options.outHeight).append("\n");
                } catch (Exception e) {
                    info.append("   Error reading dimensions\n");
                }
            } else {
                info.append("✗ Not found\n");
            }
        }

        info.append("\n💡 How it works:\n");
        info.append("1. Select image from gallery\n");
        info.append("2. Set target resolution (e.g., 2400x1080)\n");
        info.append("3. Image auto-resized and saved\n");
        info.append("4. Module hooks camera takePicture()\n");
        info.append("5. Shows your image when capturing!");

        new AlertDialog.Builder(this)
                .setTitle("File Locations")
                .setMessage(info.toString())
                .setPositiveButton("Open Folder", (dialog, which) -> {
                    openFolder();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openFolder() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADCIM%2FShuzukoCamera");
            intent.setDataAndType(uri, "resource/folder");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open folder. Path: /DCIM/ShuzukoCamera/", Toast.LENGTH_LONG).show();
        }
    }

    private void showDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== DIAGNOSTIC & SOLUTION ===\n\n");
        
        // Check PUBLIC path
        File publicImage = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/1000.bmp");
        
        info.append("📁 YOUR NEW IMAGE:\n");
        info.append("Location: /DCIM/ShuzukoCamera/\n");
        if (publicImage.exists()) {
            long kb = publicImage.length() / 1024;
            info.append("✓ Image exists (").append(kb).append(" KB)\n");
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(publicImage.getAbsolutePath(), options);
                info.append("✓ Size: ").append(options.outWidth).append("x").append(options.outHeight).append("\n");
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
                String time = sdf.format(new java.util.Date(publicImage.lastModified()));
                info.append("✓ Modified: ").append(time).append("\n");
            } catch (Exception e) {
                info.append("Error reading\n");
            }
        } else {
            info.append("✗ Image NOT found!\n");
            info.append("Please select image first!\n");
        }
        
        info.append("\n⚠️ PROBLEM:\n");
        info.append("Zalo may have old image in\nprivate directory that we\ncan't access (Android security)\n");
        
        info.append("\n✅ SOLUTION:\n");
        info.append("1. Open Zalo Settings\n");
        info.append("2. Go to Permissions\n");
        info.append("3. Enable 'Storage' permission\n");
        info.append("4. Force stop Zalo\n");
        info.append("5. Clear recent apps\n");
        info.append("6. Open Zalo again\n");
        info.append("7. Module will use PUBLIC path!\n");
        
        info.append("\n💡 WHY?\n");
        info.append("• No storage permission → Private path\n");
        info.append("• With storage permission → Public path\n");
        info.append("• Public path = Your new image!\n");
        
        info.append("\n🔧 MODULE STATUS:\n");
        File disable = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/disable.jpg");
        info.append(disable.exists() ? "⚠️ DISABLED (Turn OFF Switch 2!)" : "✓ Active");
        
        new AlertDialog.Builder(this)
                .setTitle("🔍 Solution Guide")
                .setMessage(info.toString())
                .setPositiveButton("Open Zalo Settings", (dialog, which) -> {
                    openZaloSettings();
                })
                .setNeutralButton("Open Folder", (dialog, which) -> {
                    openFolder();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void openZaloSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.zing.zalo"));
            startActivity(intent);
            Toast.makeText(this, "Go to Permissions → Enable Storage!\nThen Force Stop Zalo", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Cannot open settings. Go manually:\nSettings → Apps → Zalo → Permissions", Toast.LENGTH_LONG).show();
        }
    }

    private void copyToZaloPrivate() {
        // This won't work on Android 11+ due to Scoped Storage
        Toast.makeText(this, "⚠️ Cannot access private directory on Android 11+\n\nSolution: Give Zalo storage permission!\n\nTap 'Open Zalo Settings' button", Toast.LENGTH_LONG).show();
    }

    private void deleteAndReset() {
        File publicImage = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/1000.bmp");
        
        boolean deleted = !publicImage.exists() || publicImage.delete();
        
        if (deleted) {
            Toast.makeText(this, "✓ Public image deleted!\n\nNow:\n1. Select new image\n2. Give Zalo storage permission\n3. Restart Zalo", Toast.LENGTH_LONG).show();
            updateFileStatus();
        } else {
            Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show();
        }
    }

    private void createVideoFromImage(File imageFile) throws IOException {
        // Read and resize image if needed
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        if (bitmap == null) {
            throw new IOException("Cannot decode image");
        }

        // Ensure correct size
        if (bitmap.getWidth() != targetWidth || bitmap.getHeight() != targetHeight) {
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
            bitmap.recycle();
            bitmap = resized;
        }

        // Output video file
        File outputFile = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/virtual.mp4");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, targetWidth, targetHeight);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 2000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        MediaCodec codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        android.view.Surface surface = codec.createInputSurface();
        codec.start();

        Canvas canvas = surface.lockCanvas(null);
        canvas.drawBitmap(bitmap, 0, 0, null);
        surface.unlockCanvasAndPost(canvas);

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int trackIndex = -1;
        boolean muxerStarted = false;

        // Encode 3 seconds of video (same frame)
        int frameCount = 90; // 3 seconds * 30 fps
        long frameDuration = 1000000 / 30; // microseconds

        for (int i = 0; i < frameCount; i++) {
            int outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000);
            
            if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = codec.getOutputFormat();
                trackIndex = muxer.addTrack(newFormat);
                muxer.start();
                muxerStarted = true;
            } else if (outputBufferId >= 0) {
                java.nio.ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
                
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && muxerStarted) {
                    bufferInfo.presentationTimeUs = i * frameDuration;
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                }
                
                codec.releaseOutputBuffer(outputBufferId, false);
                
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }

        codec.signalEndOfInputStream();
        codec.stop();
        codec.release();
        surface.release();
        muxer.stop();
        muxer.release();
        bitmap.recycle();
        
        Log.d("MainActivity", "Video created: " + outputFile.getAbsolutePath());
    }

    private void showResolutionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎯 Set Target Resolution");
        
        // Create custom view with EditTexts
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        TextView info = new TextView(this);
        info.setText("Enter the resolution from toast message\n(e.g., Zalo shows: 2400x1080)\n");
        info.setTextSize(14);
        layout.addView(info);
        
        EditText widthInput = new EditText(this);
        widthInput.setHint("Width (e.g., 2400)");
        widthInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        widthInput.setText(String.valueOf(targetWidth));
        layout.addView(widthInput);
        
        EditText heightInput = new EditText(this);
        heightInput.setHint("Height (e.g., 1080)");
        heightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        heightInput.setText(String.valueOf(targetHeight));
        layout.addView(heightInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Select Image", (dialog, which) -> {
            try {
                targetWidth = Integer.parseInt(widthInput.getText().toString());
                targetHeight = Integer.parseInt(heightInput.getText().toString());
                
                // Now open image picker
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_IMAGE_PICK);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid resolution!", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void resizeAndSaveImage(Uri imageUri) throws IOException {
        File camera1Dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/");
        if (!camera1Dir.exists()) {
            camera1Dir.mkdirs();
        }

        // Decode image
        Bitmap original;
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            if (inputStream == null) {
                throw new IOException("Cannot open image");
            }
            original = BitmapFactory.decodeStream(inputStream);
            if (original == null) {
                throw new IOException("Cannot decode image");
            }
        }

        Log.d("MainActivity", "Original image: " + original.getWidth() + "x" + original.getHeight());
        Log.d("MainActivity", "Rotation: " + rotationDegrees + "°");
        Log.d("MainActivity", "Scale mode: " + (useCenterCrop ? "Center Crop" : "Stretch"));

        // Rotate if needed
        Bitmap rotated = original;
        if (rotationDegrees != 0) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.postRotate(rotationDegrees);
            rotated = Bitmap.createBitmap(original, 0, 0, original.getWidth(), original.getHeight(), matrix, true);
            if (rotated != original) {
                original.recycle();
            }
            Log.d("MainActivity", "After rotation: " + rotated.getWidth() + "x" + rotated.getHeight());
        }

        // Target dimensions - NO SWAP after rotation!
        // After rotation, the bitmap is already in correct orientation
        int finalWidth = targetWidth;
        int finalHeight = targetHeight;

        Log.d("MainActivity", "Target size: " + finalWidth + "x" + finalHeight);

        // Process based on scale mode
        Bitmap processed;
        if (useCenterCrop) {
            processed = centerCropBitmap(rotated, finalWidth, finalHeight);
        } else {
            processed = Bitmap.createScaledBitmap(rotated, finalWidth, finalHeight, true);
        }
        
        if (processed != rotated) {
            rotated.recycle();
        }
        
        // Save as PNG
        File targetFile = new File(camera1Dir, "1000.bmp");
        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            processed.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
        }

        Log.d("MainActivity", "Image saved: " + targetFile.getAbsolutePath());
        Log.d("MainActivity", "Final size: " + processed.getWidth() + "x" + processed.getHeight());
    }

    private Bitmap centerCropBitmap(Bitmap source, int targetWidth, int targetHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        
        // Calculate aspect ratios
        float sourceAspect = (float) sourceWidth / sourceHeight;
        float targetAspect = (float) targetWidth / targetHeight;
        
        int cropX, cropY, cropWidth, cropHeight;
        
        if (sourceAspect > targetAspect) {
            // Source is wider - crop sides
            cropHeight = sourceHeight;
            cropWidth = (int) (sourceHeight * targetAspect);
            cropX = (sourceWidth - cropWidth) / 2;
            cropY = 0;
        } else {
            // Source is taller - crop top/bottom
            cropWidth = sourceWidth;
            cropHeight = (int) (sourceWidth / targetAspect);
            cropX = 0;
            cropY = (sourceHeight - cropHeight) / 2;
        }
        
        Log.d("MainActivity", "Crop region: x=" + cropX + " y=" + cropY + " w=" + cropWidth + " h=" + cropHeight);
        
        // Crop to aspect ratio
        Bitmap cropped = Bitmap.createBitmap(source, cropX, cropY, cropWidth, cropHeight);
        
        // Scale to target size
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true);
        
        if (scaled != cropped) {
            cropped.recycle();
        }
        
        return scaled;
    }


}





