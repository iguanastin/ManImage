package manimage.main;


import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import manimage.common.DBImageInfo;

public class GridImageView extends BorderPane {

    private DBImageInfo info;

    private DynamicImageView view;

    private boolean selected = false;

    private static final String selectedBackgroundColor = "5588AA";
    private static final String backgroundColor = "DDDDDD";


    public GridImageView(DBImageInfo info) {
        this.info = info;

        setCenter(view = new DynamicImageView());
        setMargin(view, new Insets(5));
        setMinSize(DBImageInfo.THUMBNAIL_SIZE, DBImageInfo.THUMBNAIL_SIZE);
        setMaxSize(DBImageInfo.THUMBNAIL_SIZE, DBImageInfo.THUMBNAIL_SIZE);

        setOnMouseEntered(event -> Main.mainController.preview(getInfo()));

        setSelected(false);
    }

    DBImageInfo getInfo() {
        return info;
    }

    void setInfo(DBImageInfo info) {
        this.info = info;
    }

    void loadThumbnail(boolean backgroundLoading) {
        view.setImage(info.getThumbnail(backgroundLoading));
    }

    void unloadThumbnail() {
        view.setImage(null);
    }

    boolean isThumbnailLoaded() {
        return view.getImage() != null;
    }

    void setSelected(boolean n) {
        selected = n;

        if (!selected) {
            setStyle("-fx-background-color: #" + backgroundColor + ";");
        } else {
            setStyle("-fx-background-color: #" + selectedBackgroundColor + ";");
        }
    }

    boolean isSelected() {
        return selected;
    }

}
