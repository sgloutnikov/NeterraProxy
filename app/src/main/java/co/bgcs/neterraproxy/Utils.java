package co.bgcs.neterraproxy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.bgcs.neterraproxy.pojo.VODSeries;
import co.bgcs.neterraproxy.pojo.VODSeriesItem;


class Utils {

    static String getPlayLink(String jsonBody) {
        JsonObject channel = new JsonParser().parse(jsonBody).getAsJsonObject();
        String playLink = channel.get("link").getAsString();
        return playLink;
    }

    static String generateLivePlaylist(String contentHTML, JsonObject channelsJson, String host, int port) {
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

    static String generateVODPlaylist(List<VODSeries> seriesList, String host, int port) {
        StringBuilder m3u8 = new StringBuilder("#EXTM3U\n");

        for (VODSeries series : seriesList) {
            String group = series.getName();
            String tag = series.getTag();

            for (VODSeriesItem item : series.getVodSeriesItemList()) {
                String title = item.getTitle();
                // Strip commas for playlist title entries
                title = title.replaceAll(",", "");
                String dataId = item.getDataId();

                String encodedTitle = null;
                try {
                    encodedTitle = URLEncoder.encode(title, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                m3u8.append(String.format("#EXTINF:-1 group-title=\"%s\",%s\nhttp://%s:%s/vod.m3u8?id=%s&tag=%s&name=%s\n",
                        group, title, host, port, dataId, tag, encodedTitle));
            }
        }
        return m3u8.toString();
    }

    static String generateTimeShiftPlaylist(JsonObject channelsJson, String host, int port) {
        // Iterate defined channels and add to playlist channels with dvr
        String group = "Neterra Time Shift";
        StringBuilder m3u8 = new StringBuilder("#EXTM3U\n");
        Set<Map.Entry<String, JsonElement>> definedChannels = channelsJson.entrySet();

        for(Map.Entry<String,JsonElement> channelEntry : definedChannels){
            JsonObject channel = channelEntry.getValue().getAsJsonObject();
            if (channel.get("dvr").getAsBoolean()) {
                String chanId = channelEntry.getKey();
                String name = channel.get("name").getAsString() + " (TS)";
                String logo = channel.get("logo").getAsString();

                m3u8.append(String.format("#EXTINF:-1 group-title=\"%s\" tvg-id=\"none\" tvg-name=\"none\" tvg-logo=\"%s\",%s\nhttp://%s:%s/timeshift.m3u8?ch=%s\n",
                        group, logo, name, host, port, chanId));
            }
        }
        return m3u8.toString();
    }

    static List<VODSeries> getVODSeriesList(String vodJSONString) {
        List<VODSeries> seriesList = new ArrayList<>();
        JsonArray seriesJsonArray = new JsonParser().parse(vodJSONString).getAsJsonArray();

        for (JsonElement jsonElement : seriesJsonArray) {
            JsonObject seriesJsonObject = jsonElement.getAsJsonObject();
            String name = seriesJsonObject.get("name").getAsString();
            String tag = seriesJsonObject.get("tag").getAsString();
            VODSeries series = new VODSeries(name, tag);
            seriesList.add(series);
        }
        return seriesList;
    }
}
