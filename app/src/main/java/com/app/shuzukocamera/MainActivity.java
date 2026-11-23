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

    private int detectedWidth = 0;
    private int detectedHeight = 0;
    private String detectedPackage = "";

    private Button btnSelectImage;
    private RadioGroup radioGroupRotation;
    private RadioGroup radioGroupScaleMode;
    private TextView txtDetectedResolution;
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

    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (instance == this) {
            instance = null;
        }
    }

    public void onResolutionUpdated(int width, int height, String packageName) {
        detectedWidth = width;
        detectedHeight = height;
        detectedPackage = packageName;
        targetWidth = width;
        targetHeight = height;
        updateFileStatus();
        Toast.makeText(this, "🎯 Đã nhận diện camera: " + width + "x" + height + " (" + getAppName(packageName) + ")", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        btnSelectImage = findViewById(R.id.btnSelectImage);
        radioGroupRotation = findViewById(R.id.radioGroupRotation);
        radioGroupScaleMode = findViewById(R.id.radioGroupScaleMode);
        txtDetectedResolution = findViewById(R.id.txtDetectedResolution);
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
                // Show auto-detect or choice dialog
                showResolutionChoiceDialog();
            }
        });

        disable_switch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isPressed()) {
                if (!has_permission()) {
                    request_permission();
                } else {
                    File disable_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/disable.jpg");
                    File internalDisable = new File(getFilesDir(), "disable.jpg");
                    if (disable_file.exists() != b){
                        if (b){
                            try {
                                disable_file.createNewFile();
                                internalDisable.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            disable_file.delete();
                            internalDisable.delete();
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
                    File internalForceShow = new File(getFilesDir(), "force_show.jpg");
                    if (force_show_switch.exists() != b){
                        if (b){
                            try {
                                force_show_switch.createNewFile();
                                internalForceShow.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            force_show_switch.delete();
                            internalForceShow.delete();
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
                        File internalSound = new File(getFilesDir(), "no-silent.jpg");
                        if (play_sound_switch.exists() != b){
                            if (b){
                                try {
                                    play_sound_switch.createNewFile();
                                    internalSound.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                play_sound_switch.delete();
                                internalSound.delete();
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
                    File internalPrivateDir = new File(getFilesDir(), "private_dir.jpg");
                    if (force_private_dir.exists() != b){
                        if (b){
                            try {
                                force_private_dir.createNewFile();
                                internalPrivateDir.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            force_private_dir.delete();
                            internalPrivateDir.delete();
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
                    File internalNoToast = new File(getFilesDir(), "no_toast.jpg");
                    if (disable_toast_file.exists() != b){
                        if (b){
                            try {
                                disable_toast_file.createNewFile();
                                internalNoToast.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            disable_toast_file.delete();
                            internalNoToast.delete();
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

                builder.setNegativeButton(R.string.negative, (dialogInterface, i) -> Toast.makeText(MainActivity.this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show());

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
        Log.d(this.getApplication().getPackageName(), "[ShuzukoCamera][sync] Đồng bộ trạng thái cấu hình");

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
                progress.setMessage("Đang xử lý ảnh...\n1. Cắt và chỉnh tỉ lệ về " + targetWidth + "x" + targetHeight + "\n2. Tạo video xem trước (virtual.mp4)");
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
                            Toast.makeText(this, "✓ ĐÃ CẬP NHẬT ẢNH & VIDEO THÀNH CÔNG!\n• Ảnh: 1000.bmp\n• Video: virtual.mp4\n\n⚠️ QUAN TRỌNG: Hãy BUỘC DỪNG (hoặc vuốt tắt) app mục tiêu rồi mở lại để nhận ảnh mới!", Toast.LENGTH_LONG).show();
                            updateFileStatus();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progress.dismiss();
                            Toast.makeText(this, "Lỗi xử lý: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("MainActivity", "Processing failed", e);
                        });
                    }
                }).start();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi sao chép file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("MainActivity", "Error copying file", e);
        }
    }

    private void saveInternalCopy(File sourceFile, String targetFileName) {
        try {
            File internalFile = new File(getFilesDir(), targetFileName);
            if (sourceFile.exists()) {
                try (InputStream in = new FileInputStream(sourceFile);
                     FileOutputStream out = new FileOutputStream(internalFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                }
                Log.d("MainActivity", "Internal copy saved: " + targetFileName + " (" + internalFile.length() + " bytes)");
            } else {
                if (internalFile.exists()) {
                    internalFile.delete();
                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error saving internal copy: " + targetFileName, e);
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

        saveInternalCopy(targetFile, targetFileName);
    }

    private void updateFileStatus() {
        loadDetectedResolution();
        if (txtDetectedResolution != null) {
            if (detectedWidth > 0 && detectedHeight > 0) {
                targetWidth = detectedWidth;
                targetHeight = detectedHeight;
                txtDetectedResolution.setText("🎯 Độ phân giải tự động: ✓ " + detectedWidth + "x" + detectedHeight + " (" + getAppName(detectedPackage) + ")");
                txtDetectedResolution.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                txtDetectedResolution.setText("🎯 Độ phân giải tự động: Chưa nhận diện\n(Mở camera trong app mục tiêu 1 lần để tự động bắt)");
                txtDetectedResolution.setTextColor(Color.parseColor("#E65100"));
            }
        }

        File imageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/1000.bmp");
        File videoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/virtual.mp4");

        if (imageFile.exists()) {
            long sizeKB = imageFile.length() / 1024;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
                txtImageStatus.setText("Ảnh chụp: ✓ " + options.outWidth + "x" + options.outHeight + " (" + sizeKB + " KB)");
            } catch (Exception e) {
                txtImageStatus.setText("Ảnh chụp: ✓ (" + sizeKB + " KB)");
            }
        } else {
            txtImageStatus.setText("Ảnh chụp: Chưa chọn ảnh");
        }

        if (videoFile.exists()) {
            long sizeKB = videoFile.length() / 1024;
            txtVideoStatus.setText("Khung xem trước: ✓ Đã tạo video (" + sizeKB + " KB)");
        } else {
            txtVideoStatus.setText("Khung xem trước: Chưa tạo video");
        }
    }

    private void checkAllPaths() {
        StringBuilder info = new StringBuilder();
        info.append("=== VỊ TRÍ LƯU FILE ===\n\n");

        String publicPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/";
        File publicDir = new File(publicPath);
        
        info.append("📁 Thư mục lưu trữ:\n");
        info.append(publicPath).append("\n\n");
        info.append("Trạng thái thư mục: ").append(publicDir.exists() ? "✓ Đã tồn tại" : "✗ Chưa có (sẽ tự tạo)").append("\n\n");

        loadDetectedResolution();
        if (detectedWidth > 0 && detectedHeight > 0) {
            info.append("🎯 Độ phân giải đã nhận diện: ").append(detectedWidth).append("x").append(detectedHeight)
                .append(" (").append(getAppName(detectedPackage)).append(")\n\n");
        }

        if (publicDir.exists()) {
            File image = new File(publicDir, "1000.bmp");
            
            info.append("🖼️ File ảnh 1000.bmp: ");
            if (image.exists()) {
                long kb = image.length() / 1024;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
                String time = sdf.format(new java.util.Date(image.lastModified()));
                info.append("✓ (").append(kb).append(" KB)\n");
                info.append("   Cập nhật lúc: ").append(time).append("\n");
                
                // Show dimensions
                try {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(image.getAbsolutePath(), options);
                    info.append("   Kích thước: ").append(options.outWidth).append("x").append(options.outHeight).append("\n");
                } catch (Exception e) {
                    info.append("   Lỗi đọc kích thước\n");
                }
            } else {
                info.append("✗ Chưa tìm thấy\n");
            }
        }

        info.append("\n💡 Cách thức hoạt động:\n");
        info.append("1. Mở camera app mục tiêu 1 lần để Shuzuko tự nhận diện độ phân giải\n");
        info.append("2. Chọn ảnh chân dung từ thư viện\n");
        info.append("3. App tự động crop & render video virtual.mp4\n");
        info.append("4. Vào lại app mục tiêu để chụp ảnh giả lập!");

        new AlertDialog.Builder(this)
                .setTitle("Vị Trí File")
                .setMessage(info.toString())
                .setPositiveButton("Mở Thư Mục", (dialog, which) -> {
                    openFolder();
                })
                .setNegativeButton("Đóng", null)
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
            Toast.makeText(this, "Không thể mở trình duyệt file. Đường dẫn: /DCIM/ShuzukoCamera/", Toast.LENGTH_LONG).show();
        }
    }

    private void showDebugInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== CHẨN ĐOÁN & HƯỚNG DẪN ===\n\n");
        
        loadDetectedResolution();
        if (detectedWidth > 0 && detectedHeight > 0) {
            info.append("🎯 ĐỘ PHÂN GIẢI CAMERA ĐÃ NHẬN DIỆN:\n");
            info.append("✓ Kích thước: ").append(detectedWidth).append("x").append(detectedHeight).append("\n");
            info.append("✓ Ứng dụng: ").append(getAppName(detectedPackage)).append("\n\n");
        }
        
        // Check PUBLIC path
        File publicImage = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/1000.bmp");
        
        info.append("📁 ẢNH THAY THẾ CỦA BẠN:\n");
        info.append("Vị trí: /DCIM/ShuzukoCamera/\n");
        if (publicImage.exists()) {
            long kb = publicImage.length() / 1024;
            info.append("✓ File ảnh tồn tại (").append(kb).append(" KB)\n");
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(publicImage.getAbsolutePath(), options);
                info.append("✓ Kích thước: ").append(options.outWidth).append("x").append(options.outHeight).append("\n");
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
                String time = sdf.format(new java.util.Date(publicImage.lastModified()));
                info.append("✓ Sửa đổi lúc: ").append(time).append("\n");
            } catch (Exception e) {
                info.append("Lỗi đọc file\n");
            }
        } else {
            info.append("✗ Chưa có file ảnh!\n");
            info.append("Vui lòng bấm chọn ảnh trước!\n");
        }
        
        info.append("\n⚠️ LƯU Ý BỘ NHỚ:\n");
        info.append("Một số app (như Zalo) trên Android 11+ cần được cấp quyền 'Bộ nhớ/Ảnh' trong Cài đặt để có thể đọc ảnh chung.\n");
        
        info.append("\n✅ HƯỚNG DẪN:\n");
        info.append("1. Cấp quyền Bộ nhớ cho app mục tiêu\n");
        info.append("2. Buộc dừng (Force stop) app mục tiêu\n");
        info.append("3. Mở lại app mục tiêu để nhận ảnh\n");
        
        info.append("\n🔧 TRẠNG THÁI MODULE:\n");
        File disable = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/disable.jpg");
        info.append(disable.exists() ? "⚠️ ĐANG TẮT (Hãy tắt Công tắc 2!)" : "✓ Đang kích hoạt");
        
        new AlertDialog.Builder(this)
                .setTitle("🔍 Hướng Dẫn & Chẩn Đoán")
                .setMessage(info.toString())
                .setPositiveButton("Cài Đặt Zalo", (dialog, which) -> {
                    openZaloSettings();
                })
                .setNeutralButton("Mở Thư Mục", (dialog, which) -> {
                    openFolder();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void openZaloSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:com.zing.zalo"));
            startActivity(intent);
            Toast.makeText(this, "Vào Quyền → Bật quyền Bộ nhớ / Ảnh!\nSau đó Buộc dừng Zalo", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Hãy mở thủ công: Cài đặt → Ứng dụng → Zalo → Quyền", Toast.LENGTH_LONG).show();
        }
    }

    private void copyToZaloPrivate() {
        // This won't work on Android 11+ due to Scoped Storage
        Toast.makeText(this, "⚠️ Cannot access private directory on Android 11+\n\nSolution: Give Zalo storage permission!\n\nTap 'Open Zalo Settings' button", Toast.LENGTH_LONG).show();
    }

    private void deleteAndReset() {
        File publicImage = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/1000.bmp");
        File publicVideo = new File(Environment.getExternalStorageDirectory() + "/DCIM/ShuzukoCamera/virtual.mp4");
        if (publicImage.exists()) publicImage.delete();
        if (publicVideo.exists()) publicVideo.delete();

        File internalImage = new File(getFilesDir(), "1000.bmp");
        if (internalImage.exists()) internalImage.delete();
        File internalVideo = new File(getFilesDir(), "virtual.mp4");
        if (internalVideo.exists()) internalVideo.delete();

        Toast.makeText(this, "✓ Đã xóa ảnh và video thay thế!\n\nHãy chọn ảnh mới và mở lại app mục tiêu.", Toast.LENGTH_LONG).show();
        updateFileStatus();
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
        
        saveInternalCopy(outputFile, "virtual.mp4");
        Log.d("MainActivity", "Video created: " + outputFile.getAbsolutePath());
    }

    private void loadDetectedResolution() {
        // 1. Read from SharedPreferences first (received from broadcast, immune to Scoped Storage)
        android.content.SharedPreferences prefs = getSharedPreferences("ShuzukoPrefs", MODE_PRIVATE);
        int pWidth = prefs.getInt("detected_width", 0);
        int pHeight = prefs.getInt("detected_height", 0);
        String pPkg = prefs.getString("detected_package", "");
        if (pWidth > 0 && pHeight > 0) {
            detectedWidth = pWidth;
            detectedHeight = pHeight;
            detectedPackage = pPkg;
            return;
        }

        // 2. Read from DCIM file as fallback
        File resFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCIM/ShuzukoCamera/last_res.txt");
        if (resFile.exists()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(resFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("width=")) {
                        detectedWidth = Integer.parseInt(line.substring(6).trim());
                    } else if (line.startsWith("height=")) {
                        detectedHeight = Integer.parseInt(line.substring(7).trim());
                    } else if (line.startsWith("package=")) {
                        detectedPackage = line.substring(8).trim();
                    }
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error reading last_res.txt", e);
            }
        }
    }

    private String getAppName(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return "ứng dụng mục tiêu";
        }
        try {
            PackageManager pm = getPackageManager();
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private void showResolutionChoiceDialog() {
        loadDetectedResolution();
        if (detectedWidth > 0 && detectedHeight > 0) {
            targetWidth = detectedWidth;
            targetHeight = detectedHeight;
            String appName = getAppName(detectedPackage);
            new AlertDialog.Builder(this)
                    .setTitle("🎯 Tự Động Nhận Diện Độ Phân Giải")
                    .setMessage("Đã phát hiện độ phân giải camera từ " + appName + ":\n\n" +
                            "👉 " + detectedWidth + " x " + detectedHeight + "\n\n" +
                            "Bạn có muốn áp dụng kích thước này để tự động tối ưu ảnh chụp và video xem trước?")
                    .setPositiveButton("Dùng " + detectedWidth + "x" + detectedHeight + " (Khuyên dùng)", (dialog, which) -> {
                        openImagePicker();
                    })
                    .setNeutralButton("Nhập thủ công", (dialog, which) -> {
                        showManualResolutionDialog();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("🎯 Độ Phân Giải Camera")
                    .setMessage("Chưa phát hiện độ phân giải tự động từ ứng dụng mục tiêu.\n\n" +
                            "💡 Mẹo: Mở camera trong ứng dụng mục tiêu (ví dụ Zalo) lên 1 lần, Shuzuko Camera sẽ tự động bắt kích thước!\n\n" +
                            "Bạn có thể nhập độ phân giải thủ công bên dưới:")
                    .setPositiveButton("Nhập thủ công", (dialog, which) -> {
                        showManualResolutionDialog();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void showManualResolutionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎯 Cài Đặt Độ Phân Giải Thủ Công");
        
        // Create custom view with EditTexts
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        TextView info = new TextView(this);
        info.setText("Nhập độ phân giải camera mong muốn\n(Ví dụ: 1080x1920 hoặc 1080x2400):\n");
        info.setTextSize(14);
        layout.addView(info);
        
        EditText widthInput = new EditText(this);
        widthInput.setHint("Chiều rộng (Width, VD: 1080)");
        widthInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        widthInput.setText(String.valueOf(targetWidth));
        layout.addView(widthInput);
        
        EditText heightInput = new EditText(this);
        heightInput.setHint("Chiều cao (Height, VD: 2400)");
        heightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        heightInput.setText(String.valueOf(targetHeight));
        layout.addView(heightInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Chọn ảnh", (dialog, which) -> {
            try {
                targetWidth = Integer.parseInt(widthInput.getText().toString());
                targetHeight = Integer.parseInt(heightInput.getText().toString());
                openImagePicker();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Độ phân giải không hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Hủy", null);
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

        saveInternalCopy(targetFile, "1000.bmp");

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





