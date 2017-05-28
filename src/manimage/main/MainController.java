package manimage.main;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import manimage.common.DBImageInfo;
import manimage.common.ImageDatabase;
import manimage.common.ImageInfo;
import manimage.common.ImageSet;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class MainController {

    public DynamicImageView previewDynamicImageView;
    public DynamicImageGridPane grid;
    public Label previewTagsLabel;

    //TODO: Clean up C style handling

    private DBImageInfo currentPreview;
    private File lastFolder;


    @FXML
    public void initialize() {
//        grid.setImageSet(imageSet);

        //TODO: Swap to using databases
    }

    void preview(DBImageInfo info) {
        if (currentPreview != null) currentPreview.unloadImage();
        currentPreview = info;

        previewDynamicImageView.setImage(info.getImage(true));
//        previewTagsLabel.setText(info.getTags().toString());
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
            File[] imageFiles = folder.listFiles(Main.IMAGE_FILTER);
            if (imageFiles != null) {
                ImageDatabase db = grid.getImageDatabase();

                try {
                    for (File imageFile : imageFiles) {
                        db.queueCreateImage(imageFile.getAbsolutePath());
                    }

                    db.commitChanges();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

                lastFolder = folder.getParentFile();
            }
        }
    }

    public void addRecurseFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder and all subfolders");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(Main.mainStage);

        if (folder == null) {
            //Canceled
        } else {
//            imageSet.initAndAddSubfiles(folder, true);

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
