package com.allywingz.convertingimagetolayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Objects;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {
    LinearLayout mLayout;
    Bitmap bitmap;
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLayout = (LinearLayout) findViewById(R.id.mylayout);
    }

    /*
    * This Method is Used to Check Permission
    * To Convert Layout to Image, We Need two Types of Permissions listed below
    * */
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(), READ_EXTERNAL_STORAGE);

        return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED;
    }

    /*
     * This Method is Used to Check Permission
     * To Convert Layout to Image, We Need two Types of Permissions listed below
     * */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    /*
     * On Allowing Permissions, the next work flow will be handled here
     * */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean writeAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean readAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (writeAccepted && readAccepted)
                    convertToImage(mLayout);
                else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                            showMessageOKCancel(
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE},
                                                    PERMISSION_REQUEST_CODE);
                                        }
                                    });
                        }
                    }

                }
            }
        }
    }

    /*
     * This Method is Used to Alert the User Regarding Runtime Permissions
     * */
    private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("You need to allow access to Both the permissions")
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /*
     * Firstly below method Converts the Layout to Bitmap
     * Then we will draw Canvas to the bitmap with its height and width
     * The next step is to Save Image we are doing it in an AsyncTask Thread
     * */
    private void convertToImage(LinearLayout mContImage) {
        try {
            mContImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            bitmap = Bitmap.createBitmap(mContImage.getMeasuredWidth(), mContImage.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            mContImage.layout(0, 0, mContImage.getMeasuredWidth(), mContImage.getMeasuredHeight());
            mContImage.draw(canvas);

            new AsyncTaskExample().execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * AsyncTask thread which performs the Action of Saving the Image File
     * */
    @SuppressLint("StaticFieldLeak")
    private class AsyncTaskExample extends AsyncTask<String, String, Bitmap> {
        Bitmap icon = null;
        ByteArrayOutputStream bytes;
        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                OutputStream fos;
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LayoutImage");

                    String fileName = System.currentTimeMillis() + "_" + ".Jpg";
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");

                    Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                    Log.i("imageUri", "===>" + imageUri.toString());
                    fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                } else {
                    String ImageDir;
                    String fileName = System.currentTimeMillis() + "_" + ".Jpg";
                    ImageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/LayoutImage";
                    File image = new File(ImageDir, fileName);
                    fos = new FileOutputStream(image);
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                assert fos != null;
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            setAlert();
        }
    }

    private void setAlert() {
        final AlertDialog.Builder alertbox = new AlertDialog.Builder(MainActivity.this);
        alertbox.setMessage("Your Image is Saved in Picture/LayoutImage folder, Please check");
        alertbox.setTitle("Picture Path");

        alertbox.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Write your Stuff
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            }
        });
        alertbox.show();
    }

    public void LayoutToImage(View view) {
        try {
            if (!checkPermission()) {
                requestPermission();
            } else {
                convertToImage(mLayout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}