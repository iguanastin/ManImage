package manimage.main;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import manimage.common.ImageInfo;
import manimage.common.ImageDatabase;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    public DynamicImageView previewDynamicImageView;
    public DynamicImageGridPane grid;
    public Label previewTagsLabel;

    //TODO: Clean up C style handling

    private File lastFolder;


    //---------------------- Initializers ------------------------------------------------------------------------------

    @FXML
    public void initialize() {
        grid.updateView();
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    void preview(ImageInfo info) {
        previewDynamicImageView.setImage(info.getImage(true));
        //TODO: Fix tag label
    }

    //-------------------- Listeners -----------------------------------------------------------------------------------

    public void addFilesClicked(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
        fc.setTitle("Add image(s)");
        fc.setInitialDirectory(lastFolder);
        List<File> files = fc.showOpenMultipleDialog(Main.mainStage);

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
        File folder = dc.showDialog(Main.mainStage);

        if (folder != null) {
            ImageDatabase db = grid.getImageDatabase();
            getImagesFiles(folder, false).forEach(db::queueCreateImage);

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
        File folder = dc.showDialog(Main.mainStage);

        if (folder != null) {
            ImageDatabase db = grid.getImageDatabase();
            getImagesFiles(folder, true).forEach(db::queueCreateImage);

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

    public void gridMouseClicked(MouseEvent event) {

    }

    //--------------------- Getters ------------------------------------------------------------------------------------

    private ArrayList<String> getImagesFiles(File folder, boolean recurse) {
        final ArrayList<String> results = new ArrayList<>();
        final File[] files = folder.listFiles(Main.IMAGE_AND_DIRECTORY_FILTER);

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    results.add(file.getAbsolutePath());
                } else if (recurse) {
                    results.addAll(getImagesFiles(file, true));
                }
            }
        }

        return results;
    }
}
