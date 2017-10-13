package co.bgcs.neterraproxy;

import android.content.Context;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.JsonObject;


import java.io.IOException;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

class NeterraProxy extends NanoHTTPD {
    private final String host;
    private final int port;
    private final Pipe pipe;
    private String username;
    private String password;
    private long expireTime;
    private ClearableCookieJar cookieJar;
    private OkHttpClient client;
    private JsonObject channelsJson;

    NeterraProxy(String host, int port, Pipe pipe) {
        super(host, port);
        this.host = host;
        this.port = port;
        this.pipe = pipe;
    }

    void init(String username, String password, Context context) {
        this.username = username;
        this.password = password;
        cookieJar = new PersistentCookieJar(new SetCookieCache(),
                new SharedPrefsCookiePersistor(context));
        client = new OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .build();
        expireTime = 0;
    }

    void initAssets(JsonObject channelsJson) {
        this.channelsJson = channelsJson;
    }

    @Override
    public Response serve(IHTTPSession session) {
        Response res = super.serve(session);

        String uri = session.getUri();
        if (uri.equals("/epg.xml")) {
            pipe.setNotification("Now serving: EPG");
            res = newFixedLengthResponse(Response.Status.REDIRECT, "application/xml", null);
            res.addHeader("Location", "http://epg.kodibg.org/dl.php");
        } else if (uri.startsWith("/playlist.m3u8")) {
            List<String> ch = session.getParameters().get("ch");
            List<String> chName = session.getParameters().get("name");

            if (ch == null) {
                // Fresh authentication every time playlist is served
                if(authenticate()) {
                    pipe.setNotification("Now serving: Playlist");
                    res = newFixedLengthResponse(Response.Status.OK, "application/x-mpegURL", getM3U8());
                    res.addHeader("Content-Disposition", "attachment; filename=\"playlist.m3u8\"");
                } else {
                    pipe.setNotification("Failed to login. Check username and password.");
                }
            } else {
                pipe.setNotification("Now serving: Channel " + chName.get(0));
                res = newFixedLengthResponse(Response.Status.REDIRECT, "application/x-mpegURL", null);
                res.addHeader("Location", getStream(ch.get(0)));
            }
        }
        return res;
    }

    private String getStream(String issueId) {
        checkAuthentication();
        String playLinkJson = "";

        RequestBody formBody = new FormBody.Builder()
                .add("issue_id", issueId)
                .add("quality", "0")
                .add("type", "live")
                .build();
        Request request = new Request.Builder()
                .url("http://www.neterra.tv/content/get_stream")
                .post(formBody)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            playLinkJson = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Utils.getPlayLink(playLinkJson);
    }

    private String getM3U8() {
        String neterraContentJsonString = "";
        Request request = new Request.Builder()
                .url("http://www.neterra.tv/content/live")
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            neterraContentJsonString = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Utils.generatePlaylist(neterraContentJsonString, channelsJson, host, port);
    }

    private void checkAuthentication() {
        long NOW = System.currentTimeMillis();

        // Check if authentication is needed
        if (NOW > expireTime) {
            authenticate();
        }
    }

    private boolean authenticate() {
        boolean logged = false;
        cookieJar.clear();
        RequestBody formBody = new FormBody.Builder()
                .add("login_username", username)
                .add("login_password", password)
                .add("login", "1")
                .add("login_type", "1")
                .build();
        Request request = new Request.Builder()
                .url("http://www.neterra.tv/user/login_page")
                .post(formBody)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            logged = response.body().string().contains("var LOGGED = '1'");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(logged) {
            expireTime = System.currentTimeMillis() + 28800000;
        }
        return logged;
    }

}
