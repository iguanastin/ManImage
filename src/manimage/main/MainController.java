package manimage.main;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import manimage.common.ImageInfo;
import manimage.common.ImageDatabase;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainController {


    public DynamicImageView previewDynamicImageView;
    public Label previewTagsLabel;

    public ScrollPane gridScrollPane;
    public DatabaseImageGridPane grid;

    private File lastFolder;
    private Stage stage;


    //---------------------- Initializers ------------------------------------------------------------------------------

    @FXML
    public void initialize() {
        grid.updateView();
        grid.setPreviewListener(this::preview);
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    private void preview(ImageInfo info) {
        previewDynamicImageView.setImage(info.getImage(true));
        try {
            grid.getImageDatabase().loadTags(info);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        previewTagsLabel.setText(info.getTags().toString());
    }

    //-------------------- Listeners -----------------------------------------------------------------------------------

    public void addFilesClicked(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
        fc.setTitle("Add image(s)");
        fc.setInitialDirectory(lastFolder);
        List<File> files = fc.showOpenMultipleDialog(stage);

        if (files != null) {
            ImageDatabase db = grid.getImageDatabase();

            try {
                files.forEach(file -> {
                    if (Main.IMAGE_FILTER.accept(file)) db.queueCreateImage(file.getAbsolutePath());
                });

                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            lastFolder = files.get(0).getParentFile();
        }
    }

    public void addFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(stage);

        if (folder != null) {
            ImageDatabase db = grid.getImageDatabase();
            getImageFiles(folder, false).forEach(db::queueCreateImage);

            try {
                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            lastFolder = folder.getParentFile();
        }
    }

    public void addRecurseFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder and all subfolders");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(stage);

        if (folder != null) {
            ImageDatabase db = grid.getImageDatabase();
            getImageFiles(folder, true).forEach(db::queueCreateImage);

            try {
                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            lastFolder = folder.getParentFile();
        }
    }

    public void exitClicked(ActionEvent event) {
        Platform.exit();
    }

    public void aboutMenuActivated(ActionEvent event) {

    }

    public void gridScrollPaneClicked(MouseEvent event) {
        grid.unselectAll();
    }

    public void gridScrollPaneKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            grid.selectLeft(event.isShiftDown(), event.isControlDown());
            preview(grid.getLastSelected().getInfo());
            ensureVisible(gridScrollPane, grid.getLastSelected());
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            grid.selectRight(event.isShiftDown(), event.isControlDown());
            preview(grid.getLastSelected().getInfo());
            ensureVisible(gridScrollPane, grid.getLastSelected());
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            grid.selectDown(event.isShiftDown(), event.isControlDown());
            preview(grid.getLastSelected().getInfo());
            ensureVisible(gridScrollPane, grid.getLastSelected());
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            grid.selectUp(event.isShiftDown(), event.isControlDown());
            preview(grid.getLastSelected().getInfo());
            ensureVisible(gridScrollPane, grid.getLastSelected());
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.A) {
            if (grid.areAllSelected()) {
                grid.unselectAll();
            } else {
                grid.selectAll();
            }
            event.consume();
        }
    }

    public void gridScrollPaneScrolled(ScrollEvent event) {
        grid.updateVisibleThumbnails();
    }

    //--------------------- Getters ------------------------------------------------------------------------------------

    private ArrayList<String> getImageFiles(File folder, boolean recurse) {
        final ArrayList<String> results = new ArrayList<>();
        final File[] files = folder.listFiles(Main.IMAGE_AND_DIRECTORY_FILTER);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    results.add(file.getAbsolutePath());
                } else if (recurse) {
                    results.addAll(getImageFiles(file, true));
                }
            }
        }

        return results;
    }

    //-------------------------- Setters -------------------------------------------------------------------------------

    void setStage(Stage stage) {
        this.stage = stage;
    }

    //-------------------- Operators -----------------------------------------------------------------------------------

    private static void ensureVisible(ScrollPane pane, Node node) {
        Bounds viewport = pane.getViewportBounds();
        double contentHeight = pane.getContent().getBoundsInLocal().getHeight();
        double nodeMinY = node.getBoundsInParent().getMinY();
        double nodeMaxY = node.getBoundsInParent().getMaxY();
        double viewportMinY = (contentHeight - viewport.getHeight()) * pane.getVvalue();
        double viewportMaxY = viewportMinY + viewport.getHeight();
        if (nodeMinY < viewportMinY) {
            pane.setVvalue(nodeMinY / (contentHeight - viewport.getHeight()));
        } else if (nodeMaxY > viewportMaxY) {
            pane.setVvalue((nodeMaxY - viewport.getHeight()) / (contentHeight - viewport.getHeight()));
        }
    }

}
