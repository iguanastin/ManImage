package manimage.main;

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import manimage.common.ImageDatabase;
import manimage.common.ImageDatabaseUpdateListener;
import manimage.common.ImageInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DatabaseImageGridPane extends GridPane implements ImageDatabaseUpdateListener {

    private final ArrayList<GridImageView> imageViews = new ArrayList<>();
    private final ArrayList<GridImageView> selected = new ArrayList<>();

    private final ContextMenu contextMenu;

    private String orderBy = ImageDatabase.SQL_IMAGE_TIME_ADDED + " DESC, " + ImageDatabase.SQL_IMAGE_ID + " DESC";
    private int pageLength = 100;
    private int pageNum = 0;

    private ImageDatabase db;

    private PreviewListener previewListener;


    //----------------- Constructors -----------------------------------------------------------------------------------

    public DatabaseImageGridPane() {

        //--------------------- Context Menu ---------------------------------------------------------------------------

        MenuItem[] items = new MenuItem[10];
        items[0] = new MenuItem("Add Tag");
        items[0].setOnAction(event -> {
            //TODO: Implement tag adding
        });

        items[1] = new MenuItem("View Info");
        //TODO: Implement info viewing

        items[2] = new Menu("Set Rating...");
        MenuItem r1 = new MenuItem("★");
        r1.setOnAction(event -> setSelectedRating((byte) 1));
        MenuItem r2 = new MenuItem("★★");
        r2.setOnAction(event -> setSelectedRating((byte) 2));
        MenuItem r3 = new MenuItem("★★★");
        r3.setOnAction(event -> setSelectedRating((byte) 3));
        MenuItem r4 = new MenuItem("★★★★");
        r4.setOnAction(event -> setSelectedRating((byte) 4));
        MenuItem r5 = new MenuItem("★★★★★");
        r5.setOnAction(event -> setSelectedRating((byte) 5));
        ((Menu) items[2]).getItems().addAll(r1, r2, r3, r4, r5);

        items[3] = new SeparatorMenuItem();

        items[4] = new MenuItem("Open");
        items[4].setOnAction(event -> {
            if (getFirstSelected() != null) {
                try {
                    Desktop.getDesktop().open(new File(getFirstSelected().getInfo().getPath()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[5] = new MenuItem("View in Folder");
        items[5].setOnAction(event -> {
            if (!selected.isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + getFirstSelected().getInfo().getPath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[6] = new SeparatorMenuItem();

        items[7] = new MenuItem("Remove");
        items[7].setOnAction(event -> removeSelected());

        items[8] = new SeparatorMenuItem();

        items[9] = new MenuItem("Delete File");
        items[9].setOnAction(event -> deleteSelected());

        contextMenu = new ContextMenu(items);

        //----------------- Setup database -----------------------------------------------------------------------------

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

    GridImageView getFirstSelected() {
        if (!selected.isEmpty()) {
            return selected.get(0);
        } else {
            return null;
        }
    }

    GridImageView getLastSelected() {
        if (!selected.isEmpty()) {
            return selected.get(selected.size() - 1);
        } else {
            return null;
        }
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

    ImageDatabase getImageDatabase() {
        return db;
    }

    private int getCurrentSelectedIndex() {
        int index = 0;
        if (getLastSelected() != null) index = imageViews.indexOf(getLastSelected());
        else if (getFirstSelected() != null) index = imageViews.indexOf(getFirstSelected());
        return index;
    }

    public int getPage() {
        return pageNum;
    }

    public int getPageLength() {
        return pageLength;
    }

    //------------------ Setters ---------------------------------------------------------------------------------------

    void setPreviewListener(PreviewListener previewListener) {
        this.previewListener = previewListener;
    }

    void setDatabase(ImageDatabase db) {
        if (this.db != null) {
            this.db.removeChangeListener(this);
        }

        this.db = db;
        if (db != null) db.addChangeListener(this);
    }

    void setPage(int pageNum) {
        if (this.pageNum != pageNum) {
            this.pageNum = pageNum;
            updateView();
        }
    }

    void setPageLength(int pageLength) {
        if (this.pageLength != pageLength) {
            this.pageLength = pageLength;
            updateView();
        }
    }

    //---------------------- Checkers ----------------------------------------------------------------------------------

    boolean areAllSelected() {
        return selected.containsAll(imageViews);
    }

    //------------------ Operators -------------------------------------------------------------------------------------

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
            boolean reselect = true;
            if (selected.size() == 1 && view.isSelected()) reselect = false;

            selected.clear();

            if (reselect) selected.add(view);
        }

        updateSelected();
    }

    void unselectAll() {
        selected.clear();
        updateSelected();
    }

    void selectLeft(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return;
        int index = getCurrentSelectedIndex() - 1;

        if (index >= 0) {
            select(imageViews.get(index), shiftDown, controlDown);
        }
    }

    void selectRight(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return;
        int index = getCurrentSelectedIndex() + 1;

        if (index < imageViews.size()) {
            select(imageViews.get(index), shiftDown, controlDown);
        }
    }

    void selectDown(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return;
        int index = getCurrentSelectedIndex() + columnWidth();

        if (index >= imageViews.size()) index = imageViews.size() - 1;

        if (!imageViews.get(index).isSelected() || selected.size() > 1) select(imageViews.get(index), shiftDown, controlDown);
    }

    void selectUp(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return;
        int index = getCurrentSelectedIndex() - columnWidth();

        if (index < 0) index = 0;

        if (!imageViews.get(index).isSelected() || selected.size() > 1) select(imageViews.get(index), shiftDown, controlDown);
    }

    void selectAll() {
        selected.clear();
        selected.addAll(imageViews);

        updateSelected();
    }

    private void removeSelected() {
        if (db == null || !db.isConnected()) return;
        if (!selected.isEmpty()) {
            selected.forEach(view -> view.getInfo().setToBeDeleted());
            unselectAll();
            try {
                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setSelectedRating(byte newRating) {
        if (db == null || !db.isConnected()) return;
        if (!selected.isEmpty()) {
            selected.forEach(view -> view.getInfo().setRating(newRating));
            try {
                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void deleteSelected() {
        if (db == null || !db.isConnected()) return;
//        selected.forEach(view -> {
//            new File(view.getInfo().getPath()).delete();
//        });
        removeSelected();
    }

    //------------------------ Updaters --------------------------------------------------------------------------------

    private void updateSelected() {
        for (GridImageView view : imageViews) {
            view.setSelected(selected.contains(view));
        }
    }

    void updateVisibleThumbnails() {
        if (getScene() == null) return;
        ScrollPane scrollPane = (ScrollPane) getScene().lookup("#gridScrollPane");
        Bounds scrollPaneBounds = scrollPane.localToScene(scrollPane.getLayoutBounds());
        int updates = 0;

        layout();

        for (GridImageView n : imageViews) {
            Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());
            if (!n.isThumbnailLoaded() && scrollPaneBounds.intersects(nodeBounds)) {
                n.loadThumbnail(true);
                updates++;
            }
        }

        if (updates > 0) System.out.println("UpdateVisibleThumbs:\t" + updates);
    }

    void updateView() {
        if (db == null || !db.isConnected()) return;
        int added = 0, removed = 0, updated = 0;

        try {
            String query = "SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " ORDER BY " + orderBy + " OFFSET " + pageLength * pageNum + " LIMIT " + pageLength;
            ArrayList<ImageInfo> images = db.getImages(query);

            int i = 0;
            for (ImageInfo image : images) {
                if (i >= imageViews.size()) {
                    createNewGridView(i, image);
                    added++;
                } else {
                    GridImageView view = imageViews.get(i);
                    view.unloadThumbnail();

                    view.setInfo(image);
                    updated++;
                }

                i++;
            }

            for (int k = imageViews.size() - i; k > 0; k--) {
                GridImageView view = imageViews.remove(imageViews.size() - 1);
                getChildren().remove(view);
                removed++;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        System.out.println("GridUpdateView:\t\tAdded " + added + ", Removed " + removed + ", Updated " + updated);

        updateVisibleThumbnails();
    }

    private GridImageView createNewGridView(int index, ImageInfo image) {
        GridImageView view = new GridImageView(image);
        view.setOnContextMenuRequested(event -> contextMenu.show(view, event.getScreenX(), event.getScreenY()));
        view.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY || !view.isSelected()) {
                select(view, event.isShiftDown(), event.isControlDown());
            }

            event.consume();
        });
        view.setOnMouseEntered(event -> {
            if (previewListener != null) previewListener.preview(view.getInfo());
        });
        imageViews.add(view);

        //Add new row if not enough present
        if (getRowConstraints().size() <= index / columnWidth())
            getRowConstraints().add(new RowConstraints(150, 150, 150, Priority.NEVER, VPos.CENTER, true));

        add(view, index % columnWidth(), index / columnWidth());

        return view;
    }

    @Override
    public void databaseUpdated() {
        updateView();
    }

}
