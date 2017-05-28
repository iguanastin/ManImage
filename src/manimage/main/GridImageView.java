package manimage.main;


import javafx.geometry.Insets;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import manimage.common.ImageInfo;

public class GridImageView extends BorderPane {

    private ImageInfo info;

    private DynamicImageView view;

    private boolean selected = false;

    private static final String selectedBackgroundColor = "5588AA";
    private static final String backgroundColor = "DDDDDD";


    public GridImageView(ImageInfo info) {
        this.info = info;

        setCenter(view = new DynamicImageView());
        setMargin(view, new Insets(5));
        setMinSize(ImageInfo.THUMBNAIL_SIZE, ImageInfo.THUMBNAIL_SIZE);
        setMaxSize(ImageInfo.THUMBNAIL_SIZE, ImageInfo.THUMBNAIL_SIZE);

        setOnMouseEntered(event -> Main.mainController.preview(getInfo()));
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) Main.mainController.grid.select(this, event.isShiftDown(), event.isControlDown());
        });
        view.setOnMouseClicked(getOnMouseClicked());

        setSelected(false);
    }

    ImageInfo getInfo() {
        return info;
    }

    void setInfo(ImageInfo info) {
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
