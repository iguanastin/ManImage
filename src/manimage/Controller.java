package manimage;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class Controller {

    public DynamicImageView previewDynamicImageView;

    public DynamicImageGridPane grid;
    public Label previewTagsLabel;
    private ImageInfo currentPreview;

    private File lastFolder;

    @FXML
    public void initialize() {

    }

    void preview(ImageInfo info) {
        if (currentPreview != null) currentPreview.unloadImage();
        currentPreview = info;

        previewDynamicImageView.setImage(info.getImage(true));
        previewTagsLabel.setText(info.getTags().toString());
    }

    public void addFileClicked(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
        fc.setTitle("Add image(s)");
        fc.setInitialDirectory(lastFolder);
        List<File> files = fc.showOpenMultipleDialog(Main.mainStage);

        if (files == null || files.isEmpty()) {
            //Canceled
        } else {
            grid.addAll(files.toArray(new File[files.size()]), false);

            lastFolder = files.get(0).getParentFile();
        }
    }

    public void addFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(Main.mainStage);

        if (folder == null) {
            //Canceled
        } else {
            grid.add(folder, false);

            lastFolder = folder.getParentFile();
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
            grid.add(folder, true);

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
