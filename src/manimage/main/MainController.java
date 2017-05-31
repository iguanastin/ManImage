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

    private ImageDatabase db;
    private File lastFolder;
    private Stage stage;

    private String dbPath = "C:\\Users\\Austin\\h2db";
    private String dbUser = "sa";
    private String dbPass = "sa";


    //---------------------- Initializers ------------------------------------------------------------------------------

    @FXML
    public void initialize() {
        try {
            db = new ImageDatabase(dbPath, dbUser, dbPass, false);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        grid.setPreviewListener(this::preview);
        grid.setDatabase(db);
        grid.updateView();
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    private void preview(ImageInfo info) {
        previewDynamicImageView.setImage(info.getImage(true));
//        previewTagsLabel.setText(info.getTags().toString());
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
            File[] files = folder.listFiles(Main.IMAGE_FILTER);
            if (files == null) return;
            for (File file : files) {
                db.queueCreateImage(file.getAbsolutePath());
            }

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

            getSubFolders(folder).forEach(subFolder -> {
                File[] files = subFolder.listFiles(Main.IMAGE_FILTER);
                if (files == null) return;
                for (File file : files) {
                    db.queueCreateImage(file.getAbsolutePath());
                }
                try {
                    db.commitChangesWithoutNotify();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            db.notifyChangeListeners();

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

    public void gridScrolled(ScrollEvent event) {
        grid.updateVisibleThumbnails();
    }

    public void clearAllOnAction(ActionEvent event) {
        try {
            db.cleanAndInitialize();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    //--------------------- Getters ------------------------------------------------------------------------------------

    private ArrayList<File> getSubFolders(File folder) {
        File[] folders = folder.listFiles();
        ArrayList<File> results = new ArrayList<>();

        if (folders == null) return results;

        for (File file : folders) {
            if (file.isDirectory()) {
                results.add(file);
                results.addAll(getSubFolders(file));
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
