package co.bgcs.neterraproxy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


class Utils {

    static String getPlayLink(String jsonBody) {
        JsonObject channel = new JsonParser().parse(jsonBody).getAsJsonObject();
        String playLink = channel.get("play_link").getAsString();

        //TODO: Verify playback without cleaning url. New backend changes might have fixed the issues.
        //Cleanup DVR features in live stream that were causing problems for some channels
        //playLink = playLink.replace(":443", "");
        //playLink = playLink.replace("/dvr", "/live");
        //playLink = playLink.replace("DVR&", "");
        return playLink;
    }

    static String generatePlaylist(String contentJson, JsonObject channelsJson, String host, int port) {
        JsonArray neterraContentArray = new JsonParser().parse(contentJson).getAsJsonObject()
                .get("tv_choice_result").getAsJsonArray();

        StringBuilder m3u8 = new StringBuilder("#EXTM3U\n");
        for (int i = 0; i < neterraContentArray.size(); i++) {
            JsonObject channel = neterraContentArray.get(i).getAsJsonArray().get(0).getAsJsonObject();
            String chanId = channel.get("issues_id").getAsString();
            String chanName = channel.get("issues_name").getAsString();
            String tvgId = "";
            String tvgName = "";
            String group = "";
            String logo = "";

            JsonObject definedChannel  = channelsJson.getAsJsonObject(chanId);
            if (definedChannel != null) {
                chanName = definedChannel.get("name").getAsString();
                tvgId = definedChannel.get("tvg-id").getAsString();
                tvgName = definedChannel.get("tvg-name").getAsString();
                group = definedChannel.get("group").getAsString();
                logo = definedChannel.get("logo").getAsString();
            }
            String encodedChanName = null;
            try {
                encodedChanName = URLEncoder.encode(chanName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            m3u8.append(String.format("#EXTINF:-1 tvg-id=\"%s\" tvg-name=\"%s\" tvg-logo=\"%s\" " +
                            "group-title=\"%s\",%s\nhttp://%s:%s/playlist.m3u8?ch=%s&name=%s\n",
                    tvgId, tvgName, logo, group, chanName, host, port, chanId, encodedChanName));
        }

        return m3u8.toString();
    }
}
