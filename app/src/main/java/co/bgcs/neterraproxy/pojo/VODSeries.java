package co.bgcs.neterraproxy.pojo;

import java.util.ArrayList;
import java.util.List;

public class VODSeries {

    private String name;
    private String tag;
    private List<VODSeriesItem> vodSeriesItemList;

    public VODSeries(String name, String tag) {
        this.name = name;
        this.tag = tag;
        vodSeriesItemList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<VODSeriesItem> getVodSeriesItemList() {
        return vodSeriesItemList;
    }
}
