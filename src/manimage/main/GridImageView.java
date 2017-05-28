package manimage.main;


import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import manimage.common.DBImageInfo;
import manimage.common.ImageInfo;

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
        setMinSize(ImageInfo.thumbnailSize, ImageInfo.thumbnailSize);
        setMaxSize(ImageInfo.thumbnailSize, ImageInfo.thumbnailSize);

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
        info.unloadThumbnail();
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
