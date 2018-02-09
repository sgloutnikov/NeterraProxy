package co.bgcs.neterraproxy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


class Utils {

    static String getPlayLink(String jsonBody) {
        JsonObject channel = new JsonParser().parse(jsonBody).getAsJsonObject();
        String playLink = channel.get("url").getAsJsonObject().get("play").getAsString();

        return playLink;
    }

    static String generatePlaylist(String contentHTML, JsonObject channelsJson, String host, int port) {
        Element channelsPlaylistElement = Jsoup.parse(contentHTML).selectFirst("ul.playlist-items");
        Elements neterraPlaylist = channelsPlaylistElement.select("li");

        StringBuilder m3u8 = new StringBuilder("#EXTM3U\n");

        for (Element chan : neterraPlaylist) {
            // Parse Channel Name and ID
            String chanName = "";
            String chanId = "";
            Element chanLink = chan.selectFirst("a[href]");
            if (chanLink != null) {
                chanName = chanLink.attr("title");
            } else { continue; }
            chanId = chan.getElementsByClass("js-pl-favorite playlist-item__favorite")
                    .first().attr("data-id");

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
