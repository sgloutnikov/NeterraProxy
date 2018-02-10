package co.bgcs.neterraproxy.pojo;

public class VODSeriesItem {

    private String title;
    private String dataId;

    public VODSeriesItem(String title, String dataId) {
        this.title = title;
        this.dataId = dataId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDataId() {
        return dataId;
    }

    public void setDataId(String dataId) {
        this.dataId = dataId;
    }

}
