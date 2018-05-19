package co.bgcs.neterraproxy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;


public class MainService extends Service implements Pipe {
    private NeterraProxy proxy;
    private final IBinder binder = new LocalBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // LocalIP Bind Testing
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String localIp = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        proxy = new NeterraProxy(localIp, 8889, this);
        System.out.println("Binding to: " + localIp);

        try {
            proxy.start();
            startForeground(1, getNotification("Ready to serve."));
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        setNotification("Binding to: " + localIp);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        proxy.stop();
        stopForeground(true);
    }

    @Override
    public void setNotification(String text) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, getNotification(text));
    }

    private Notification getNotification(String text) {
        return new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("NeterraProxy")
                .setContentText(text)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();
    }

    void loadPreferences(SharedPreferences preferences) {
        proxy.init(
                preferences.getString("username", null),
                preferences.getString("password", null),
                getApplicationContext()
        );
    }

    void loadAssets() {
        String channelsJsonString = "";
        try {
            InputStream inputStream = getAssets().open("channels.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            channelsJsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonObject channelsJson = new Gson().fromJson(channelsJsonString, JsonObject.class);
        proxy.initAssets(channelsJson);
    }


    class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }
}
