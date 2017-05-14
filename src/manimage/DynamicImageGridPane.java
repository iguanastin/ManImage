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
import java.util.List;


public class DynamicImageGridPane extends GridPane {

    private final List<GridImageView> contents = new ArrayList<>();
    private final List<GridImageView> selected = new ArrayList<>();

    private final ContextMenu contextMenu;


    public DynamicImageGridPane() {

        //--------------------- Context Menu ---------------------------------------------------------------------------

        MenuItem[] items = new MenuItem[6];
        items[0] = new MenuItem("Edit Tags");

        items[1] = new MenuItem("View");
        items[1].setOnAction(event -> {
            try {
                Desktop.getDesktop().open(getFirstSelected().getInfo().getFile());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        items[2] = new MenuItem("View Info");

        items[3] = new MenuItem("Show in Folder");
        items[3].setOnAction(event -> {
            if (!selected.isEmpty()) {
                try {
                    Runtime.getRuntime().exec("explorer.exe /select, " + getFirstSelected().getInfo().getFile().getAbsolutePath());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        items[4] = new SeparatorMenuItem();

        items[5] = new MenuItem("Delete");

        contextMenu = new ContextMenu(items);

        //------------------ Listeners ---------------------------------------------------------------------------------

        setOnMouseClicked(event -> clearSelected());
        setOnScroll(event -> updateVisibleThumbnails());
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

    boolean add(File file, boolean recurse) {
        if (!file.exists()) return false;

        if (Main.IMAGE_FILTER.accept(file)) {
            if (getCount() / columnWidth() >= rowHeight()) getRowConstraints().add(new RowConstraints(150, 150, 150));

            GridImageView view = new GridImageView(new ImageInfo(file));
            add(view, getCount() % columnWidth(), getCount() / columnWidth());
            contents.add(view);
            view.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY || selected.isEmpty()) select(view, event.isShiftDown(), event.isControlDown());
            });
            view.setOnContextMenuRequested(event -> {
                if (!view.isSelected()) select(view, false, false);
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
            });

            updateVisibleThumbnails();

            return true;
        } else {
            return file.isDirectory() && addAll(file.listFiles(Main.IMAGE_AND_DIRECTORY_FILTER), recurse);
        }
    }

    boolean addAll(File[] files, boolean recurse) {
        boolean result = false;

        if (files == null || files.length == 0) return false;

        for (File file : files) {
            if (recurse || !file.isDirectory()) {
                if (add(file, recurse)) {
                    result = true;
                }
            }
        }

        return result;
    }

    private int getCount() {
        return contents.size();
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

    private void select(GridImageView view, boolean shiftDown, boolean ctrlDown) {
        if (view == null) {
            clearSelected();
        } else if (shiftDown && !selected.isEmpty()) {
            GridImageView first = getFirstSelected();
            clearSelected();
            selected.addAll(getContentsInRange(getIndex(first), getIndex(view)));
        } else if (ctrlDown) {
            if (!view.isSelected()) {
                selected.add(view);
            } else {
                selected.remove(view);
            }
        } else {
            clearSelected();

            if (!view.isSelected()) {
                selected.add(view);
            }
        }

        updateSelected();
    }

    private List<GridImageView> getContentsInRange(int start, int end) {
        List<GridImageView> result = new ArrayList<>();

        if (start == end) {
            result.add(contents.get(start));
        }

        if (start < end) {
            for (; start <= end; start++) {
                result.add(contents.get(start));
            }
        } else {
            for (; start >= end; start--) {
                result.add(contents.get(start));
            }
        }

        return result;
    }

    private void updateSelected() {
        for (GridImageView view : contents) {
            view.setSelected(selected.contains(view));
        }
    }

    private GridImageView getFirstSelected() {
        return getFirstSelected();
    }

    private GridImageView getLastSelected() {
        return selected.get(selected.size()-1);
    }

    private void clearSelected() {
        selected.clear();
        updateSelected();
    }

}
