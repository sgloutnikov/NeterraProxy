package co.bgcs.neterraproxy;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.DisableClickListener;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.appupdater.objects.Update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private MainService mainService;
    private AppUpdaterUtils appUpdater;
    private boolean isBound;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText timeShiftHoursEditText;
    private EditText timeShiftMinutesEditText;
    private ProgressDialog mProgressDialog;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mainService = ((MainService.LocalBinder) service).getService();
            mainService.loadPreferences(sharedPreferences);
            mainService.loadAssets();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getPreferences(Context.MODE_PRIVATE);

        usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        passwordEditText = (EditText) findViewById(R.id.passwordEditText);
        timeShiftHoursEditText = (EditText) findViewById(R.id.timeShiftHoursEditText);
        timeShiftHoursEditText.addTextChangedListener(timeShiftTextWatcher);
        timeShiftMinutesEditText = (EditText) findViewById(R.id.timeShiftMinutesEditText);
        timeShiftMinutesEditText.addTextChangedListener(timeShiftTextWatcher);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferences();
            }
        });
        Button exitButton = (Button) findViewById(R.id.exitButton);
        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        startService(new Intent(this, MainService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, MainService.class), mConnection, BIND_AUTO_CREATE);
        usernameEditText.setText(sharedPreferences.getString("username", null));
        passwordEditText.setText(sharedPreferences.getString("password", null));
        timeShiftHoursEditText.setText(Integer.toString(sharedPreferences.getInt("timeShiftHours", 3)));
        timeShiftMinutesEditText.setText(Integer.toString(sharedPreferences.getInt("timeShiftMinutes", 0)));

        final Context context = this;
        appUpdater = new AppUpdaterUtils(context)
                .setUpdateFrom(UpdateFrom.JSON)
                .setUpdateJSON("https://raw.githubusercontent.com/sgloutnikov/NeterraProxy/master/app/update-changelog.json")
                .withListener(new AppUpdaterUtils.UpdateListener() {
                    @Override
                    public void onSuccess(final Update update, Boolean isUpdateAvailable) {
                        if (isUpdateAvailable) {
                            AlertDialog dlg = new AlertDialog.Builder(context)
                                    .setTitle("Нова Версия: " + update.getLatestVersion())
                                    .setMessage(update.getReleaseNotes())
                                    .setPositiveButton("Свали сега", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            URL url = update.getUrlToDownload();
                                            new DownloadNewVersion(context).execute(url);
                                        }
                                    })
                                    .setNegativeButton("Затвори", null)
                                    .setNeutralButton("Не показвай отново", new DisableClickListener(context))
                                    .create();
                            dlg.setCancelable(true);
                            dlg.show();
                        }
                    }

                    @Override
                    public void onFailed(AppUpdaterError appUpdaterError) {

                    }
                });
        appUpdater.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        appUpdater.stop();
        if (isBound) {
            unbindService(mConnection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, MainService.class));
    }

    @Override
    public void onBackPressed() {
        savePreferences();
        moveTaskToBack(true);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", usernameEditText.getText().toString().trim());
        editor.putString("password", passwordEditText.getText().toString().trim());
        editor.putInt("timeShiftHours", Integer.parseInt(timeShiftHoursEditText.getText()
                .toString().trim()));
        editor.putInt("timeShiftMinutes", Integer.parseInt(timeShiftMinutesEditText
                .getText().toString()));
        editor.apply();

        if (isBound) {
            mainService.loadPreferences(sharedPreferences);
            mainService.loadAssets();
        }
        Toast.makeText(getApplicationContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
    }

    private class DownloadNewVersion extends AsyncTask<URL, Integer, Boolean> {
        Context context;


        public DownloadNewVersion(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage("Downloading...");
            mProgressDialog.setMax(100);
            mProgressDialog.setCanceledOnTouchOutside(false);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(URL... urls) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(urls[0]).build();
            try {
                Response response = client.newCall(request).execute();
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        + "/neterraproxy-update.apk");
                file.mkdirs();
                if (file.exists()) {
                    file.delete();
                }

                FileOutputStream fileOutputStream = new FileOutputStream(file);
                InputStream inputStream = response.body().byteStream();
                long fileSize = Long.parseLong(response.header("Content-Length"));

                byte[] data = new byte[512];
                long total = 0;
                int count;
                while ((count = inputStream.read(data)) != -1) {
                    total += count;
                    // Publish download progress
                    publishProgress((int) (total * 100 / fileSize));
                    fileOutputStream.write(data, 0, count);
                }

                fileOutputStream.flush();
                fileOutputStream.close();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();
            // Will not work on Android 24+. Look to update to use modern methods...
            File updateApk = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    + "/neterraproxy-update.apk");
            Intent updateIntent = new Intent(Intent.ACTION_VIEW);
            updateIntent.setDataAndType(Uri.fromFile(updateApk), "application/vnd.android.package-archive");
            context.startActivity(updateIntent);
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // Update the progress dialog
            mProgressDialog.setProgress(progress[0]);
        }
    }

    TextWatcher timeShiftTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (timeShiftHoursEditText.getText().length() > 0) {
                int hours = Integer.parseInt(timeShiftHoursEditText.getText().toString());
                if (hours < 0 || hours > 12) {
                    Toast.makeText(MainActivity.this,
                            "Часовете трябва да са между 0 и 12.",
                            Toast.LENGTH_SHORT).show();
                    timeShiftHoursEditText.setText("3");
                }
            }
            if (timeShiftMinutesEditText.getText().length() > 0) {
                int mins = Integer.parseInt(timeShiftMinutesEditText.getText().toString());
                if (mins < 0 || mins > 60) {
                    Toast.makeText(MainActivity.this,
                            "Минутите трябва да са между 0 и 60.",
                            Toast.LENGTH_SHORT).show();
                    timeShiftMinutesEditText.setText("0");
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

}
