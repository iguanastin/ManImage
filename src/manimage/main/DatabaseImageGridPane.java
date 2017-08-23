package manimage.main;

import javafx.event.*;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import manimage.common.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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

        items[1] = new MenuItem("Add Tag");
        items[1].setOnAction(event -> {
            openTagEditorDialog();
        });
        //TODO: Implement info viewing

        items[2] = new MenuItem("View in Folder");
        items[2].setOnAction(event -> {
            if (!selected.isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + getFirstSelected().getInfo().getPath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[3] = new SeparatorMenuItem();

        items[4] = new MenuItem("Forget");
        items[4].setOnAction(event -> {
            Alert d = new Alert(Alert.AlertType.CONFIRMATION);
            d.setTitle("Forget Files");
            d.setHeaderText("Remove these files from the database permanently?");
            d.setContentText("This action cannot be undone!");
            Optional result = d.showAndWait();
            if (result.get() == ButtonType.OK) {
                removeSelected();
            }
        });

        items[5] = new SeparatorMenuItem();

        items[6] = new MenuItem("Delete Files");
        items[6].setOnAction(event -> {
            Alert d = new Alert(Alert.AlertType.CONFIRMATION);
            d.setTitle("Delete Files");
            d.setHeaderText("Delete these files permanently?");
            d.setContentText("This action cannot be undone!");
            Optional result = d.showAndWait();
            if (result.get() == ButtonType.OK) {
                deleteSelected();
            }
        });

        contextMenu = new ContextMenu(items);

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

    DBInterface getDatabase() {
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

    void setPageLength(int pageLength) {
        this.pageLength = pageLength;
    }

    void setPrimaryOrder(String primaryOrder) {
        this.primaryOrder = primaryOrder;
    }

    void setPrimaryOrderDescending(boolean primaryOrderDescending) {
        this.primaryOrderDescending = primaryOrderDescending;
    }

    void setSecondaryOrderDescending(boolean secondaryOrderDescending) {
        this.secondaryOrderDescending = secondaryOrderDescending;
    }

    void setSecondaryOrder(String secondaryOrder) {
        this.secondaryOrder = secondaryOrder;
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

        if (!imageViews.get(index).isSelected() || selected.size() > 1)
            select(imageViews.get(index), shiftDown, controlDown);
    }

    void selectUp(boolean shiftDown, boolean controlDown) {
        if (imageViews.isEmpty()) return;
        int index = getCurrentSelectedIndex() - columnWidth();

        if (index < 0) index = 0;

        if (!imageViews.get(index).isSelected() || selected.size() > 1)
            select(imageViews.get(index), shiftDown, controlDown);
    }

    void selectAll() {
        selected.clear();
        selected.addAll(imageViews);

        updateSelected();
    }

    void removeSelected() {
        if (db == null || !db.isConnected()) return;
        if (!selected.isEmpty()) {
            final ArrayList<ImgInfo> imgs = new ArrayList<>();
            selected.forEach(img -> imgs.add(img.getInfo()));
            try {
                db.removeImgs(imgs);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            unselectAll();
        }
    }

    void deleteSelected() {
        if (db == null || !db.isConnected()) return;
        selected.forEach(view -> {
            view.getInfo().getPath().delete();
        });
        removeSelected();
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
            ArrayList<ImgInfo> imgs = new ArrayList<>();
            for (GridImageView view : selected) {
                imgs.add(view.getInfo());
            }
            for (String tag : lastTagString.split(" ")) {
                if (tag.charAt(0) == '-') {
                    try {
                        db.removeTag(imgs, tag.substring(1), true);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        db.addTag(imgs, tag, true);
                    } catch (SQLException e) {
                        e.printStackTrace();
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
        Bounds scrollPaneBounds = scrollPane.localToScene(scrollPane.getLayoutBounds());
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
        int added = 0, removed = 0, updated = 0;

        try {
            ArrayList<ImgInfo> images = db.getImages(pageLength, pageLength*pageNum, new OrderBy(primaryOrder, primaryOrderDescending, secondaryOrder, secondaryOrderDescending), searchTags, searchFilePath);

            int i = 0;
            for (ImgInfo image : images) {
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

//        System.out.println("GridUpdateView:\t\tAdded " + added + ", Removed " + removed + ", Updated " + updated);

        updateVisibleThumbnails();
    }

    private GridImageView createNewGridView(int index, ImgInfo image) {
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
            Dragboard db = view.startDragAndDrop(TransferMode.ANY);
            ArrayList<File> files = new ArrayList<>();
            selected.forEach(item -> files.add(item.getInfo().getPath()));
            MainController.clipboard.putFiles(files);
            db.setContent(MainController.clipboard);
            event.consume();
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
