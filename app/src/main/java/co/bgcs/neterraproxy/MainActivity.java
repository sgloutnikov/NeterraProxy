package co.bgcs.neterraproxy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private MainService mainService;
    private boolean isBound;
    private EditText usernameEditText;
    private EditText passwordEditText;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mainService = ((MainService.LocalBinder) service).getService();
            mainService.loadPreferences(sharedPreferences);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        editor.commit();

        if (isBound) mainService.loadPreferences(sharedPreferences);
        Toast.makeText(getApplicationContext(), "Preferences saved.", Toast.LENGTH_SHORT).show();
    }
}
