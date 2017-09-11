package manimage.main;

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import manimage.common.DBInterface;
import manimage.common.ImageDatabaseUpdateListener;
import manimage.common.ImageInfo;
import manimage.common.OrderBy;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;


public class DatabaseImageGridPane extends GridPane implements ImageDatabaseUpdateListener {

    private final ArrayList<GridImageView> imageViews = new ArrayList<>();
    private final ArrayList<GridImageView> selected = new ArrayList<>();

    private final ContextMenu contextMenu;

    private String lastTagString = "";

    private String[] searchTags;
    private String searchFilePath;
    private String primaryOrder = "img_added";
    private boolean primaryOrderDescending = true;
    private String secondaryOrder = "img_id";
    private boolean secondaryOrderDescending = true;
    private int pageLength = 100;
    private int pageNum = 0;

    private DBInterface db;

    private PreviewListener previewListener;


    //----------------- Constructors -----------------------------------------------------------------------------------

    public DatabaseImageGridPane() {
        //--------------------- Context Menu ---------------------------------------------------------------------------

        MenuItem[] items = new MenuItem[7];

        items[0] = new MenuItem("View Info");
        //TODO: Implement info viewing

        items[1] = new MenuItem("Edit Tags");
        items[1].setOnAction(event -> {
            openTagEditorDialog();
        });

        items[2] = new MenuItem("View in Folder");
        items[2].setOnAction(event -> {
            if (!selected.isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + getFirstSelected().getInfo().getPath());
                } catch (IOException e) {
                    e.printStackTrace();
                    Main.showErrorMessage("Unexpected Error", "Error opening file explorer", e.getLocalizedMessage());
                }
            }
        });

        items[3] = new SeparatorMenuItem();

        items[4] = new MenuItem("Forget");
        items[4].setOnAction(event -> {
            if (Main.getUserConfirmation("Forget Files", "Remove these files from the database permanently?", "This action cannot be undone!")) {
                removeSelected();
            }
        });

        items[5] = new SeparatorMenuItem();

        items[6] = new MenuItem("Delete Files");
        items[6].setOnAction(event -> {
            if (Main.getUserConfirmation("Delete Files", "Delete this files permanently?", "This action cannot be undone!")) {
                deleteSelected();
            }
        });

        contextMenu = new ContextMenu(items);

    }

    //------------------------ Getters ---------------------------------------------------------------------------------

    int getCount() {
        return imageViews.size();
    }

    public ArrayList<GridImageView> getImageViews() {
        return imageViews;
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

    DBInterface getDatabase() {
        return db;
    }

    private int getCurrentSelectedIndex() {
        int index = 0;
        if (getLastSelected() != null) index = imageViews.indexOf(getLastSelected());
        else if (getFirstSelected() != null) index = imageViews.indexOf(getFirstSelected());
        return index;
    }

    int getPage() {
        return pageNum;
    }

    int getPageLength() {
        return pageLength;
    }

    ArrayList<GridImageView> getSelected() {
        return selected;
    }

    //------------------ Setters ---------------------------------------------------------------------------------------

    void setPreviewListener(PreviewListener previewListener) {
        this.previewListener = previewListener;
    }

    void setDatabase(DBInterface db) {
        if (this.db != null) {
            this.db.removeChangeListener(this);
        }

        this.db = db;
        if (db != null) db.addChangeListener(this);
    }

    void setPage(int pageNum) {
        this.pageNum = pageNum;
        updateSearchContents();
        unselectAll();
        updateVisibleThumbnails();
    }

    void setOrderBy(String primaryOrder, boolean primaryDescending, String secondaryOrder, boolean secondaryDescending) {
        this.primaryOrder = primaryOrder;
        this.primaryOrderDescending = primaryDescending;
        this.secondaryOrder = secondaryOrder;
        this.secondaryOrderDescending = secondaryDescending;
    }

    void setSearchFilePath(String searchFilePath) {
        this.searchFilePath = searchFilePath;
    }

    void setSearchTags(String[] searchTags) {
        this.searchTags = searchTags;
        if (searchTags != null) {
            if (searchTags.length == 0 || (searchTags.length == 1 && searchTags[0].isEmpty())) {
                this.searchTags = null;
            }
        }
    }

    //---------------------- Checkers ----------------------------------------------------------------------------------

    boolean areAllSelected() {
        return selected.containsAll(imageViews);
    }

    boolean isPrimaryOrderDescending() {
        return primaryOrderDescending;
    }

    boolean isSecondaryOrderDescending() {
        return secondaryOrderDescending;
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    void select(GridImageView view, boolean shiftDown, boolean ctrlDown) {
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

    GridImageView selectLeft(int distance, boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return null;
        for (int i = 0; i < distance; i++) {
            int index = getCurrentSelectedIndex() - 1;

            if (index >= 0) {
                select(imageViews.get(index), shiftDown, controlDown);
            }
        }
        return getLastSelected();
    }

    GridImageView selectRight(int distance, boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return null;
        for (int i = 0; i < distance; i++) {
            int index = getCurrentSelectedIndex() + 1;

            if (index < imageViews.size()) {
                select(imageViews.get(index), shiftDown, controlDown);
            }
        }

        return getLastSelected();
    }

    GridImageView selectDown(int distance, boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return null;
        for (int i = 0; i < distance; i++) {
            int index = getCurrentSelectedIndex() + columnWidth();

            if (index >= imageViews.size()) index = imageViews.size() - 1;

            if (!imageViews.get(index).isSelected() || selected.size() > 1)
                select(imageViews.get(index), shiftDown, controlDown);
        }

        return getLastSelected();
    }

    GridImageView selectUp(int distance, boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return null;
        for (int i = 0; i < distance; i++) {
            int index = getCurrentSelectedIndex() - columnWidth();

            if (index < 0) index = 0;

            if (!imageViews.get(index).isSelected() || selected.size() > 1)
                select(imageViews.get(index), shiftDown, controlDown);
        }

        return getLastSelected();
    }

    GridImageView selectFirst(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return null;

        select(imageViews.get(0), shiftDown, controlDown);
        return imageViews.get(0);
    }

    GridImageView selectLast(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return null;

        select(imageViews.get(imageViews.size() - 1), shiftDown, controlDown);
        return imageViews.get(imageViews.size() - 1);
    }

    void selectAll() {
        selected.clear();
        selected.addAll(imageViews);

        updateSelected();
    }

    void removeSelected() {
        if (db == null || !db.isConnected()) return;
        if (!selected.isEmpty()) {
            final ArrayList<ImageInfo> imgs = new ArrayList<>();
            selected.forEach(img -> imgs.add(img.getInfo()));
            try {
                db.removeImgs(imgs);
            } catch (SQLException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error removing images from database", e.getLocalizedMessage());
            }
//            unselectAll();
        }
        if (previewListener != null) {
            if (getLastSelected() == null) previewListener.preview(null);
            else previewListener.preview(getLastSelected().getInfo());
        }
    }

    void deleteSelected() {
        if (db == null || !db.isConnected()) return;
        selected.forEach(view -> {
            view.getInfo().getPath().delete();
        });
        removeSelected();
        if (previewListener != null) previewListener.preview(null);
    }

    void openTagEditorDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("List of tags to add separated by spaces. Prepend with '-' character to remove if present.");
        dialog.setTitle("Tag Editor");
        dialog.getEditor().setText(lastTagString);
        dialog.getEditor().selectAll();
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            lastTagString = result.get().trim().replaceAll("( )+", " ");
            ArrayList<ImageInfo> imgs = new ArrayList<>();
            for (GridImageView view : selected) {
                imgs.add(view.getInfo());
            }
            for (String tag : lastTagString.split(" ")) {
                if (tag.charAt(0) == '-') {
                    try {
                        db.removeTag(imgs, tag.substring(1), true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Main.showErrorMessage("Unexpected Error", "Error removing tag from image", e.getLocalizedMessage());
                    }
                } else {
                    try {
                        db.addTag(imgs, tag, true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Main.showErrorMessage("Unexpected Error", "Error adding tag to image", e.getLocalizedMessage());
                    }
                }
            }
            db.notifyChangeListeners();
        }
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
        Bounds scrollPaneBounds = scrollPane.localToScene(scrollPane.getBoundsInLocal());
        int updates = 0;

        layout();

        for (GridImageView n : imageViews) {
            Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());
            if (!n.isThumbnailLoaded() && scrollPaneBounds.intersects(nodeBounds)) {
                n.loadThumbnail();
                updates++;
            }
        }
    }

    void updateSearchContents() {
        if (db == null || !db.isConnected()) return;

        ArrayList<ImageInfo> images;
        try {
            images = db.getImages(pageLength, pageLength * pageNum, new OrderBy(primaryOrder, primaryOrderDescending, secondaryOrder, secondaryOrderDescending), searchTags, searchFilePath);
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Unexpected Error", "Error retrieving images from database", e.getLocalizedMessage());
            return;
        }
        ArrayList<GridImageView> pool = (ArrayList<GridImageView>) imageViews.clone();
        ArrayList<GridImageView> needed = new ArrayList<>();
        imageViews.clear();
        getChildren().clear();

        ListIterator<GridImageView> iter = pool.listIterator();
        while (iter.hasNext()) {
            GridImageView item = iter.next();
            if (images.contains(item.getInfo())) {
                needed.add(item);
                iter.remove();
            }
        }

        int i = 0;
        for (ImageInfo img : images) {
            GridImageView grid = null;
            for (GridImageView view : needed) {
                if (view.getInfo() == img) {
                    needed.remove(view);
                    grid = view;
                    imageViews.add(grid);
                    getChildren().add(grid);
                    break;
                }
            }

            if (grid == null) {
                if (pool.isEmpty()) {
                    createNewGridView(i, img);
                } else {
                    grid = pool.get(0);
                    grid.unloadThumbnail();
                    grid.setInfo(img);
                    pool.remove(0);
                    imageViews.add(grid);
                    getChildren().add(grid);
                }
            }

            i++;
        }

//        int i = 0;
//        for (ImageInfo image : images) {
//            if (i >= imageViews.size()) {
//                createNewGridView(i, image);
//            } else {
//                GridImageView view = imageViews.get(i);
//                if (view.getInfo() != image) {
//                    view.unloadThumbnail();
//
//                    view.setInfo(image);
//                }
//            }
//
//            i++;
//        }
//
//        for (int k = imageViews.size() - i; k > 0; k--) {
//            GridImageView view = imageViews.remove(imageViews.size() - 1);
//            getChildren().remove(view);
//        }

        if (previewListener != null) {
            if (getLastSelected() == null) previewListener.preview(null);
            else previewListener.preview(getLastSelected().getInfo());
        }
        updateVisibleThumbnails();

    }

    void updateWidth(double width) {
        final int cols = columnWidth();
        final int targetCols = (int) (width / 155);
        if (targetCols == cols) return;

        getChildren().clear();
        if (targetCols < cols) {
            for (int i = 0; i < cols-targetCols; i++) {
                getColumnConstraints().remove(getColumnConstraints().size() - 1);
            }
        } else {
            for (int i = 0; i < targetCols-cols; i++) {
                getColumnConstraints().add(new ColumnConstraints());
            }
        }
        int i = 0;
        for (GridImageView view : imageViews) {
            add(view, i % columnWidth(), i / columnWidth());
            i++;
        }
        updateVisibleThumbnails();
    }

    private GridImageView createNewGridView(int index, ImageInfo image) {
        GridImageView view = new GridImageView(image);
        view.setOnContextMenuRequested(event -> contextMenu.show(view, event.getScreenX(), event.getScreenY()));
        view.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY || !view.isSelected()) {
                select(view, event.isShiftDown(), event.isControlDown());
            }
            if (previewListener != null) previewListener.preview(view.getInfo());

            event.consume();
        });
        view.setOnDragDetected(event -> {
            if (event.isPrimaryButtonDown()) {
                if (!view.isSelected()) {
                    select(view, event.isShiftDown(), event.isControlDown());
                    if (previewListener != null) previewListener.preview(view.getInfo());
                }
                Dragboard db = view.startDragAndDrop(TransferMode.ANY);
                ArrayList<File> files = new ArrayList<>();
                selected.forEach(item -> files.add(item.getInfo().getPath()));
                MainController.clipboard.putFiles(files);
                db.setContent(MainController.clipboard);
                event.consume();
            }
        });
        view.setOnDragDone(javafx.event.Event::consume);
        imageViews.add(view);

        //Add new row if not enough present
        if (getRowConstraints().size() <= index / columnWidth())
            getRowConstraints().add(new RowConstraints(150, 150, 150, Priority.NEVER, VPos.CENTER, true));

        add(view, index % columnWidth(), index / columnWidth());

        return view;
    }

    @Override
    public void databaseUpdated() {
        updateSearchContents();
    }

}
