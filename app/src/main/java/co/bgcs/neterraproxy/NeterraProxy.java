package co.bgcs.neterraproxy;

import android.content.Context;

import com.franmontiel.persistentcookiejar.ClearableCookieJar;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.JsonObject;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;

import co.bgcs.neterraproxy.pojo.VODSeries;
import co.bgcs.neterraproxy.pojo.VODSeriesItem;
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
    private int timeShiftSeconds;
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

    void setTimeShift(int tsHours, int tsMin) {
        this.timeShiftSeconds = (tsHours * 60 * 60) + (tsMin * 60);
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
            //List<String> chName = session.getParameters().get("name");

            if (ch == null) {
                // Fresh authentication every time playlist is served
                if(authenticate()) {
                    pipe.setNotification("Now serving: Playlist");
                    res = newFixedLengthResponse(Response.Status.OK, "application/x-mpegURL", getLiveM3U8());
                    res.addHeader("Content-Disposition", "attachment; filename=\"playlist.m3u8\"");
                } else {
                    pipe.setNotification("Failed to login. Check username and password.");
                }
            } else {
                //pipe.setNotification("Now serving: " + chName.get(0));
                res = newFixedLengthResponse(Response.Status.REDIRECT, "application/x-mpegURL", null);
                res.addHeader("Location", getLiveStream(ch.get(0)));
            }
        } else if (uri.startsWith("/timeshift.m3u8")) {
            List<String> ch = session.getParameters().get("ch");

            if (ch == null) {
                checkAuthentication();
                pipe.setNotification("Now serving: Time Shift Playlist");
                res = newFixedLengthResponse(Response.Status.OK, "application/x-mpegURL", getTimeShiftM3U8());
                res.addHeader("Content-Disposition", "attachment; filename=\"timeshift.m3u8\"");
            } else {
                res = newFixedLengthResponse(Response.Status.REDIRECT, "application/x-mpegURL", null);
                res.addHeader("Location", getTimeShiftStream(ch.get(0)));
            }
        } else if (uri.startsWith("/vod.m3u8")) {
            List<String> dataId = session.getParameters().get("id");
            List<String> tag = session.getParameters().get("tag");
            List<String> name = session.getParameters().get("name");

            if (dataId == null) {
                checkAuthentication();
                pipe.setNotification("Now serving: VOD Playlist");
                res = newFixedLengthResponse(Response.Status.OK, "application/x-mpegURL", getVODM3U8());
                res.addHeader("Content-Disposition", "attachment; filename=\"vod.m3u8\"");
            } else {
                //pipe.setNotification("Now serving: " + name.get(0));
                res = newFixedLengthResponse(Response.Status.REDIRECT, "application/x-mpegURL", null);
                res.addHeader("Location", getVODStream(tag.get(0), dataId.get(0)));
            }
        }
        return res;
    }

    private String getLiveStream(String chanId) {
        checkAuthentication();
        String playLinkJson = "";
        String playUrl = "https://neterra.tv/live/play/" + chanId + "?quality=25";

        Request request = new Request.Builder()
                .url(playUrl)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            playLinkJson = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Utils.getPlayLink(playLinkJson);
    }

    private String getVODStream(String tag, String dataId) {
        checkAuthentication();
        String playLinkJson = "";
        String playUrl = "https://neterra.tv/videos/" + tag + "/play/" + dataId;

        Request request = new Request.Builder()
                .url(playUrl)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            playLinkJson = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Utils.getPlayLink(playLinkJson);
    }

    private String getTimeShiftStream(String chanId) {
        // Get live stream and replace
        String liveStream = getLiveStream(chanId);
        String timeShiftStream = liveStream.replace("/0/60/",
                "/0/" + this.timeShiftSeconds + "/");
        return timeShiftStream;
    }

    private String getLiveM3U8() {
        String neterraContentHTMLString = "";
        Request request = new Request.Builder()
                .url("https://neterra.tv/live")
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            neterraContentHTMLString = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Utils.generateLivePlaylist(neterraContentHTMLString, channelsJson, host, port);
    }

    private String getVODM3U8() {
        String vodJSONString = "";
        Request request = new Request.Builder()
                // Only favorites for now, maybe grow list to all later
                .url("https://neterra.tv/videos/categories/favorites")
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            vodJSONString = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<VODSeries> seriesList = Utils.getVODSeriesList(vodJSONString);
        for (VODSeries series : seriesList) {
            String tag = series.getTag();
            String vodItemsHTML = "";
            // Get all items per series
            request = new Request.Builder()
                    .url("https://neterra.tv/videos/" + tag)
                    .build();
            try {
                okhttp3.Response response = client.newCall(request).execute();
                vodItemsHTML = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Scrape all items and add to series list
            Elements itemPlaylistElements = Jsoup.parse(vodItemsHTML).selectFirst("ul.playlist-items")
                    .select("li");
            for (Element item : itemPlaylistElements) {
                Element urlElement = item.selectFirst("a[href]");
                String title = urlElement.select("span.playlist-item__title").text();
                String dataId = urlElement.attr("data-id");
                VODSeriesItem vodSeriesItem = new VODSeriesItem(title, dataId);
                series.getVodSeriesItemList().add(vodSeriesItem);
            }
        }

        return Utils.generateVODPlaylist(seriesList, host, port);
    }

    // Placeholder method for possible future changes
    private String getTimeShiftM3U8() {
        return Utils.generateTimeShiftPlaylist(channelsJson, host, port);
    }

    private void checkAuthentication() {
        long NOW = System.currentTimeMillis();

        // Check if authentication is needed
        if (NOW > expireTime) {
            authenticate();
        }
    }

    private boolean authenticate() {
        String token = "";
        boolean logged = false;
        cookieJar.clear();

        // Get CSRF Token
        Request getRequest = new Request.Builder()
                .url("https://neterra.tv/sign-in")
                .build();
        try {
            okhttp3.Response response = client.newCall(getRequest).execute();
            Document loginPageDoc = Jsoup.parse(response.body().string());
            token = loginPageDoc.getElementById("wrapper").selectFirst("input[name=_token]")
                    .attr("value");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Authenticate
        RequestBody formBody = new FormBody.Builder()
                .add("_token", token)
                .add("username", username)
                .add("password", password)
                .build();
        Request request = new Request.Builder()
                .url("https://neterra.tv/sign-in")
                .post(formBody)
                .build();
        try {
            okhttp3.Response response = client.newCall(request).execute();
            // Check for something that only exists when authenticated.
            logged = response.body().string().contains("account.png");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(logged) {
            expireTime = System.currentTimeMillis() + 28800000;
        }
        return logged;
    }

}
