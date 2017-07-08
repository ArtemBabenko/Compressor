package compresor.compressor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.yovenny.videocompress.MediaController;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity {
    private static final int TYPE_VIDEO = 1;
    private static final int RESULT_CODE_COMPRESS_VIDEO = 3;
    public static final int READ_EXTERNAL_STORAGE = 0, MULTIPLE_PERMISSIONS = 10;
    private static final String TAG = "MainActivity";
    private TextView editTextFrom;
    private TextView editTextTo;
    private ImageView btnWriteVideo;
    private ProgressBar progressBar;

    String[] permissions = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,};

    File directory;
    Uri urlForVideo = null;

    public static final String APP_DIR = "Compressor";

    public static final String COMPRESSED_VIDEOS_DIR = "/Compressed Videos/";

    public static final String TEMP_DIR = "/Temp/";


    public static void try2CreateCompressDir() {
        File f = new File(Environment.getExternalStorageDirectory(), File.separator + APP_DIR);
        f.mkdirs();
        f = new File(Environment.getExternalStorageDirectory(), File.separator + APP_DIR + COMPRESSED_VIDEOS_DIR);
        f.mkdirs();
        f = new File(Environment.getExternalStorageDirectory(), File.separator + APP_DIR + TEMP_DIR);
        f.mkdirs();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createDirectory();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        editTextFrom = (TextView) findViewById(R.id.editTextFrom);
        editTextTo = (TextView) findViewById(R.id.editTextTo);
        btnWriteVideo = (ImageView) findViewById(R.id.btnWriteVideo);
        btnWriteVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    onClickVideo();
                }
            }
        });

    }

    private void onClickVideo() {
        Uri outputFileUri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName() + ".provider", generateFileUri(TYPE_VIDEO));
        System.out.println(outputFileUri);
        System.out.println(urlForVideo);

        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, urlForVideo);
        cameraIntent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(Intent.createChooser(cameraIntent, "Select an Image"), RESULT_CODE_COMPRESS_VIDEO);
    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    compress();
                    return;
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onClickVideo();
                }
            }
        }
    }


    private File generateFileUri(int type) {
        File file = null;
        switch (type) {
            case TYPE_VIDEO:
                file = new File(directory.getPath() + "/" + "video_"
                        + System.currentTimeMillis() + ".mp4");
                break;
        }
        Log.d(TAG, "fileName = " + file);
        urlForVideo = Uri.fromFile(file);
        return file;
    }

    private void createDirectory() {
        directory = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "MyFolder");
        if (!directory.exists())
            directory.mkdirs();
    }

    //for Compress video
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void onActivityResult(int reqCode, int resCode, Intent data) {
        if (reqCode == RESULT_CODE_COMPRESS_VIDEO) {
            if (resCode == RESULT_OK) {
                if (checkPermissions()) {
                    editTextFrom.setText(urlForVideo.getPath());
                    compress();
                }
            } else if (resCode == RESULT_CANCELED) {
                Log.d(TAG, "Canceled");
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void compress() {
        try2CreateCompressDir();
        String outPath = Environment.getExternalStorageDirectory()
                + File.separator
                + APP_DIR
                + COMPRESSED_VIDEOS_DIR
                + "VIDEO_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
        editTextTo.setText(outPath);
        new VideoCompressor().execute(urlForVideo.getPath(), outPath);
    }

    class VideoCompressor extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Start video compression");
        }

        @Override
        protected Boolean doInBackground(String... params) {
            return MediaController.getInstance().convertVideo(params[0], params[1]);
        }

        @Override
        protected void onPostExecute(Boolean compressed) {
            super.onPostExecute(compressed);
            progressBar.setVisibility(View.GONE);
            if (compressed) {
                Log.d(TAG, "Compression successfully!");
            }
        }
    }

}