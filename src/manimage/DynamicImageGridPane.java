package manimage;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class DynamicImageGridPane extends GridPane {

    private final List<GridImageView> imageViews = new ArrayList<>();
    private final List<GridImageView> selected = new ArrayList<>();

    private ImageSet imageSet;
    private Comparator<ImageInfo> sortMethod;

    private final ContextMenu contextMenu;

    private final ImageSetListener imageSetListener = new ImageSetListener() {
        @Override
        public void onImageAdded(ImageInfo info) {
            updateImageViews();
        }

        @Override
        public void onImageRemoved(ImageInfo info) {
            updateImageViews();
        }
    };


    //----------------- Constructors -----------------------------------------------------------------------------------

    public DynamicImageGridPane() {

        //--------------------- Context Menu ---------------------------------------------------------------------------

        MenuItem[] items = new MenuItem[7];
        items[0] = new MenuItem("Edit Tags");

        items[1] = new MenuItem("View Info");

        items[2] = new SeparatorMenuItem();

        items[3] = new MenuItem("Open");
        items[3].setOnAction(event -> {
            if (getFirstSelected() != null) {
                try {
                    Desktop.getDesktop().open(getFirstSelected().getInfo().getFile());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[4] = new MenuItem("View in Folder");
        items[4].setOnAction(event -> {
            if (!selected.isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + getFirstSelected().getInfo().getFile().getAbsolutePath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[5] = new SeparatorMenuItem();

        items[6] = new MenuItem("Delete");

        contextMenu = new ContextMenu(items);

        //------------------ Listeners ---------------------------------------------------------------------------------

        setOnMouseClicked(event -> clearSelected());
        setOnScroll(event -> updateVisibleThumbnails());
    }

    //------------------------ Getters ---------------------------------------------------------------------------------

    private int getCount() {
        return imageViews.size();
    }

    private int columnWidth() {
        return getColumnConstraints().size();
    }

    private int rowHeight() {
        return getRowConstraints().size();
    }

    private int getIndex(Node n) {
        return columnWidth() * getRowIndex(n) + getColumnIndex(n);
    }

    public ImageSet getImageSet() {
        return imageSet;
    }

    private GridImageView getFirstSelected() {
        if (!selected.isEmpty()) {
            return selected.get(0);
        } else {
            return null;
        }
    }

    private GridImageView getLastSelected() {
        return selected.get(selected.size() - 1);
    }

    private List<GridImageView> getContentsInRange(int start, int end) {
        List<GridImageView> result = new ArrayList<>();

        if (start == end) {
            result.add(imageViews.get(start));
        }

        if (start < end) {
            for (; start <= end; start++) {
                result.add(imageViews.get(start));
            }
        } else {
            for (; start >= end; start--) {
                result.add(imageViews.get(start));
            }
        }

        return result;
    }

    //------------------ Modifiers -------------------------------------------------------------------------------------

    private void select(GridImageView view, boolean shiftDown, boolean ctrlDown) {
        if (view == null) {
            selected.clear();
        } else if (shiftDown && !selected.isEmpty()) {
            GridImageView first = getFirstSelected();
            selected.clear();
            selected.addAll(getContentsInRange(getIndex(first), getIndex(view)));
        } else if (ctrlDown) {
            if (!view.isSelected()) {
                selected.add(view);
            } else {
                selected.remove(view);
            }
        } else {
            selected.clear();

            if (!view.isSelected()) {
                selected.add(view);
            }
        }

        updateSelected();
    }

    private void clearSelected() {
        selected.clear();
        updateSelected();
    }

    //---------------------- Setters -----------------------------------------------------------------------------------

    void setSortMethod(Comparator<ImageInfo> c) {
        this.sortMethod = c;

        updateImageViews();
    }

    void setImageSet(ImageSet imageSet) {
        if (this.imageSet != null) {
            this.imageSet.removeListener(imageSetListener);
        }

        this.imageSet = imageSet;

        if (imageSet != null) {
            imageSet.addListener(imageSetListener);
        }
    }

    //------------------------ Updaters --------------------------------------------------------------------------------

    private void updateSelected() {
        for (GridImageView view : imageViews) {
            view.setSelected(selected.contains(view));
        }
    }

    private void updateImageViews() {
        ArrayList<ImageInfo> infoList = (ArrayList<ImageInfo>) imageSet.getInfoList().clone();
        if (sortMethod != null) infoList.sort(sortMethod);

        int i = 0;
        for (ImageInfo info : infoList) {
            GridImageView existingView = null;
            if (i < getChildrenUnmodifiable().size()) existingView = (GridImageView) getChildrenUnmodifiable().get(i);

            if (existingView != null) {
                existingView.setInfo(info);
            } else {
                if (getCount() / columnWidth() >= rowHeight())
                    getRowConstraints().add(new RowConstraints(150, 150, 150));

                GridImageView view = new GridImageView(info);
                add(view, getCount() % columnWidth(), getCount() / columnWidth());
                imageViews.add(view);
                view.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY || selected.isEmpty())
                        select(view, event.isShiftDown(), event.isControlDown());
                });
                view.setOnContextMenuRequested(event -> {
                    if (!view.isSelected()) select(view, false, false);
                    contextMenu.show(this, event.getScreenX(), event.getScreenY());
                });
            }

            i++;
        }

        if (i < getChildren().size()) {
            //Remove all unused ImageViews
            getChildren().removeAll(getChildren().subList(i + 1, getChildren().size()));
        }

        updateVisibleThumbnails();
    }

    private void updateVisibleThumbnails() {
        ScrollPane scrollPane = (ScrollPane) getScene().lookup("#gridScrollPane");
        Bounds scrollPaneBounds = scrollPane.localToScene(scrollPane.getLayoutBounds());

        for (Node n : getChildrenUnmodifiable()) {
            Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());

            if (n instanceof GridImageView && !((GridImageView) n).isThumbnailLoaded() && scrollPaneBounds.intersects(nodeBounds)) {
                ((GridImageView) n).loadThumbnail(true);

                System.out.println(getRowIndex(n));
            }
        }
    }

}
