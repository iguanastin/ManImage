package manimage.main;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import manimage.common.ImageInfo;
import manimage.common.ImageSet;
import manimage.common.ImageSetListener;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class DynamicImageGridPane extends GridPane {

    private final List<GridImageView> imageViews = new ArrayList<>();
    //TODO: Centralize this array instead of getChildren()
    private final List<GridImageView> selected = new ArrayList<>();

    private ImageSet imageSet;
    //TODO: Update to use database instead
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
            clearSelected();
        }
    };

    private final int THUMBNAIL_SIZE = 140;


    //----------------- Constructors -----------------------------------------------------------------------------------

    public DynamicImageGridPane() {

        //--------------------- Context Menu ---------------------------------------------------------------------------

        MenuItem[] items = new MenuItem[8];
        items[0] = new MenuItem("Edit");
        items[0].setOnAction(event -> {
            if (selected.size() == 1) {
                Main.MAIN.openSingleEditor();
                //TODO: Implement single-image editor
            } else if (selected.size() > 1) {
                //TODO: Create multi-image editor
                //TODO: Implement multi-image editor
            }
        });

        items[1] = new MenuItem("View Info");
        //TODO: Implement info viewing

        items[2] = new Menu("Set Rating...");
        ((Menu)items[2]).getItems().addAll(new MenuItem("★"), new MenuItem("★★"), new MenuItem("★★★"), new MenuItem("★★★★"), new MenuItem("★★★★★"));
        //TODO: Implement rating edit

        items[3] = new SeparatorMenuItem();

        items[4] = new MenuItem("Open");
        items[4].setOnAction(event -> {
            if (getFirstSelected() != null) {
                try {
                    Desktop.getDesktop().open(getFirstSelected().getInfo().getFile());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[5] = new MenuItem("View in Folder");
        items[5].setOnAction(event -> {
            if (!selected.isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + getFirstSelected().getInfo().getFile().getAbsolutePath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[6] = new SeparatorMenuItem();

        items[7] = new MenuItem("Delete");
        items[7].setOnAction(event -> {
            final ArrayList<ImageInfo> infoList = new ArrayList<>();

            selected.forEach(select -> infoList.add(select.getInfo()));

            infoList.forEach(info -> getImageSet().remove(info));

            //TODO: Actual delete/move to recycle bin
        });

        contextMenu = new ContextMenu(items);

        //------------------ Listeners ---------------------------------------------------------------------------------

        setOnMouseClicked(event -> clearSelected()); //TODO: Make this actually work; event is never fired
        setOnScroll(event -> updateVisibleThumbnails());
        setOnKeyTyped(event -> {
            if (event.getCode().isArrowKey()) {
                int index = 0;
                if (getLastSelected() != null) index = imageViews.indexOf(getLastSelected());
                else if (getFirstSelected() != null) index = imageViews.indexOf(getFirstSelected());

                if (event.getCode() == KeyCode.RIGHT) index++;
                if (event.getCode() == KeyCode.LEFT) index--;
                if (event.getCode() == KeyCode.DOWN) index += 4;
                if (event.getCode() == KeyCode.UP) index -= 4;

                if (index < 0) index = 0;
                if (index >= getCount()) index = getCount() - 1;

                select(imageViews.get(index), event.isShiftDown(), event.isControlDown());
            }

            //TODO: Figure out where keyevents actually happen
        });
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

    private List<GridImageView> getViewsInRange(int start, int end) {
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
            selected.addAll(getViewsInRange(getIndex(first), getIndex(view)));
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
        if (getImageSet() == null) {
            getChildren().clear();
            return;
        }

        ArrayList<ImageInfo> infoList = (ArrayList<ImageInfo>) getImageSet().getInfoList().clone();
        if (sortMethod != null) infoList.sort(sortMethod);
        //TODO: Improve efficiency of sorting image list

        int i = 0;
        for (ImageInfo info : infoList) {
            GridImageView existingView = null;
            if (i < getCount()) existingView = imageViews.get(i);

            if (existingView != null) {
                existingView.setInfo(info);
                existingView.loadThumbnail(true);
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
            getChildren().remove(i, getChildren().size());
            imageViews.removeAll(imageViews.subList(i, imageViews.size()));
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
            }
        }
    }

}
