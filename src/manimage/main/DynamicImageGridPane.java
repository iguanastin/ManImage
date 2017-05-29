package manimage.main;

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import manimage.common.ImageDatabase;
import manimage.common.ImageInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class DynamicImageGridPane extends GridPane {

    private final ArrayList<GridImageView> imageViews = new ArrayList<>();
    //TODO: Centralize this array instead of getChildren()
    private final ArrayList<GridImageView> selected = new ArrayList<>();

    private final ContextMenu contextMenu;

    private String orderByPrimary = ImageDatabase.SQL_IMAGE_TIME_ADDED;
    private String orderBySecondary = ImageDatabase.SQL_IMAGE_ID;
    private boolean descendingPrimary = true;
    private boolean descendingSecondary = true;
    private int pageLength = 100;
    private int pageNum = 0;

    private ImageDatabase db;


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
        ((Menu) items[2]).getItems().addAll(new MenuItem("★"), new MenuItem("★★"), new MenuItem("★★★"), new MenuItem("★★★★"), new MenuItem("★★★★★"));
        //TODO: Implement rating edit

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
        items[7].setOnAction(event -> deleteSelected());

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
        }); //TODO: Event is never fired

        //----------------- Setup database -----------------------------------------------------------------------------

        try {
            db = new ImageDatabase("C:\\Users\\Austin\\h2db", "sa", "sa", false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        db.addChangeListener(this::updateView);

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

    public ImageDatabase getImageDatabase() {
        return db;
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    public void select(GridImageView view, boolean shiftDown, boolean ctrlDown) {
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

    private void deleteSelected() {
        if (!selected.isEmpty()) {
            selected.forEach(view -> view.getInfo().setToBeDeleted());
            clearSelected();
            try {
                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    //------------------------ Updaters --------------------------------------------------------------------------------

    private void updateSelected() {
        for (GridImageView view : imageViews) {
            view.setSelected(selected.contains(view));
        }
    }

    private void updateVisibleThumbnails() {
        ScrollPane scrollPane = (ScrollPane) getScene().lookup("#gridScrollPane");
        Bounds scrollPaneBounds = scrollPane.localToScene(scrollPane.getLayoutBounds());

        for (GridImageView n : imageViews) {
            Bounds nodeBounds = n.localToScene(n.getBoundsInLocal());

            if (!n.isThumbnailLoaded() && scrollPaneBounds.intersects(nodeBounds)) {
                n.loadThumbnail(true);
            }
        }
    }

    public void updateView() {
        try {
            String query = "SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " ORDER BY " + orderByPrimary;
            if (descendingPrimary) query += " DESC";
            query += ", " + orderBySecondary;
            if (descendingSecondary) query += " DESC";
            query += " OFFSET " + pageLength * pageNum + " LIMIT " + pageLength;
            ArrayList<ImageInfo> images = db.getImages(query);

            int i = 0;
            for (ImageInfo image : images) {
                if (i >= imageViews.size()) {
                    GridImageView view = new GridImageView(image);
                    view.loadThumbnail(true);
                    view.setOnContextMenuRequested(event -> contextMenu.show(view, event.getScreenX(), event.getScreenY()));
                    view.setOnMouseClicked(event -> {
                        if (event.isPrimaryButtonDown() || !view.isSelected()) select(view, event.isShiftDown(), event.isControlDown());
                    });
                    imageViews.add(view);

                    //Add new row if not enough present
                    if (getRowConstraints().size() <= i / columnWidth())
                        getRowConstraints().add(new RowConstraints(150, 150, 150, Priority.NEVER, VPos.CENTER, true));

                    add(view, i % columnWidth(), i / columnWidth());
                } else {
                    GridImageView view = imageViews.get(i);

                    view.setInfo(image);
                    view.loadThumbnail(true);
                }

                i++;
            }

            for (int k = i; k < imageViews.size(); k++) {
                GridImageView view = imageViews.remove(imageViews.size()-1);
                getChildren().remove(view);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

}
