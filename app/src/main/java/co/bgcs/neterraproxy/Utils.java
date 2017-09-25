package co.bgcs.neterraproxy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


class Utils {

    static String getPlayLink(String jsonBody) {
        JsonObject channel = new JsonParser().parse(jsonBody).getAsJsonObject();
        return channel.get("play_link").getAsString();
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
            m3u8.append(String.format("#EXTINF:-1 tvg-id=\"%s\" tvg-name=\"%s\" tvg-logo=\"%s\" " +
                            "group-title=\"%s\",%s\nhttp://%s:%s/playlist.m3u8?ch=%s\n",
                    tvgId, tvgName, logo, group, chanName, host, port, chanId));
        }

        return m3u8.toString();
    }
}
