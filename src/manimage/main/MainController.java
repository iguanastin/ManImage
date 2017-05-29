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
import java.util.List;

public class MainController {

    public DynamicImageView previewDynamicImageView;
    public DynamicImageGridPane grid;
    public Label previewTagsLabel;

    //TODO: Clean up C style handling

    private File lastFolder;


    @FXML
    public void initialize() {
        grid.updateView();
    }

    void preview(ImageInfo info) {
        previewDynamicImageView.setImage(info.getImage(true));
//        previewTagsLabel.setText(info.getTags().toString());
        //TODO: Fix tag label
    }

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
            insertImagesIntoDatabase(folder, false);

            lastFolder = folder.getParentFile();
        }
    }

    private void insertImagesIntoDatabase(File folder, boolean recurse) {
        File[] files = folder.listFiles(Main.IMAGE_AND_DIRECTORY_FILTER);
        if (files != null) {
            ImageDatabase db = grid.getImageDatabase();

            try {
                for (File file : files) {
                    if (file.isFile()) {
                        db.queueCreateImage(file.getAbsolutePath());
                    } else if (recurse) {
                        insertImagesIntoDatabase(file, true);
                    }
                }

                db.commitChanges();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        }
    }

    public void addRecurseFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder and all subfolders");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(Main.mainStage);

        if (folder != null) {
            insertImagesIntoDatabase(folder, true);

            lastFolder = folder.getParentFile();
        }
    }

    public void exitClicked(ActionEvent event) {
        System.out.println("Exiting via menu item");

        Platform.exit();
    }

    public void aboutMenuActivated(ActionEvent event) {

    }

    public void gridMouseClicked(MouseEvent event) {

    }

}
