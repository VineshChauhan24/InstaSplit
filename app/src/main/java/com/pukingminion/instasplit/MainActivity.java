package com.pukingminion.instasplit;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int SELECT_PICTURE = 111;
    private static final int SELECT_VIDEO = 112;

    //Grid size of the Square Split. Usually insta follows 3 x 3.
    private static final int SPLIT_NUMBER = 3;
    private static final int NUMBER_OF_FILES = SPLIT_NUMBER * SPLIT_NUMBER;
    private static final String INSTA_SPLIT_ERROR = "InstaSplitError";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.split_image).setOnClickListener(this);
        findViewById(R.id.split_video).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.split_image:
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);
                break;
            case R.id.split_video:
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Video"), SELECT_VIDEO);
                break;

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_PICTURE) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri == null) return;
            String selectedImagePath = getRealPathFromURI(selectedImageUri);
            splitPhotoIntoGrids(selectedImagePath);
        } else if (requestCode == SELECT_VIDEO) {

        }
    }

    private void splitPhotoIntoGrids(String selectedImagePath) {
        // TODO: 03/12/17 If the photo is already 1:1, then proceed otherwise, first send to trimmer fragment.
        File file = new File(selectedImagePath);
        if (!file.exists()) return;
        String fileName = file.getName();
        Bitmap originalBm = BitmapFactory.decodeFile(selectedImagePath);
        if (originalBm == null) {
            makeToast(getString(R.string.bitmap_error));
            return;
        }
        int width = originalBm.getWidth();
        int height = originalBm.getHeight();

        if (width == 0 || height == 0) {
            makeToast(getString(R.string.incorrect_dimensions));
            Log.d(INSTA_SPLIT_ERROR, "height=" + height + " width=" + width);
            return;
        }

        int splitWidth = width / SPLIT_NUMBER;
        int splitHeight = height / SPLIT_NUMBER;

        int count = 0;
        for (int i = 0; i < SPLIT_NUMBER; i++) {
            for (int j = 0; j < SPLIT_NUMBER; j++) {
                count++;
                Bitmap outputBitmap = Bitmap.createBitmap(
                        originalBm,
                        i * splitWidth,
                        j * splitHeight,
                        splitWidth,
                        splitHeight);
                if (outputBitmap != null) {
                    saveImage(outputBitmap, fileName, count);
                } else {
                    makeToast(getString(R.string.error_in_file) + count);
                    // TODO: 03/12/17 Retry again to process, max 5 retries then error
                    return;
                }
            }
        }
    }

    private void saveImage(Bitmap finalBitmap, String image_name, int count) {
        makeToast("Processing image " + count + " of " + NUMBER_OF_FILES);

        if (count == NUMBER_OF_FILES) {
            // TODO: 03/12/17  throw a notification which takes to containing folder
        }
        String root = Environment.getExternalStorageDirectory().toString() + "/InstaSplit/Photos";
        File myDir = new File(root);
        myDir.mkdirs();
        String fname = "Image-" + image_name + "-" + count + ".jpg";
        File file = new File(myDir, fname);
        if (file.exists()) file.delete();
        Log.i("LOAD", root + fname);
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void makeToast(String text) {
        Toast.makeText(
                this,
                text,
                Toast.LENGTH_SHORT)
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String getRealPathFromURI(Uri contentUri) {
        String filePath = null;
        if (DocumentsContract.isDocumentUri(this, contentUri)) {
            String wholeID = DocumentsContract.getDocumentId(contentUri);
// Split at colon, use second item in the array
            String[] splits = wholeID.split(":");
            if (splits.length == 2) {
                String id = splits[1];

                String[] column = {MediaStore.Images.Media.DATA};
// where id is equal to
                String sel = MediaStore.Images.Media._ID + "=?";
                Cursor cursor = getContentResolver()
                        .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                column, sel, new String[]{id}, null);
                int columnIndex = cursor.getColumnIndex(column[0]);
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(columnIndex);
                }
                cursor.close();
            }
        } else {
            filePath = contentUri.getPath();
        }
        return filePath;
    }
}
