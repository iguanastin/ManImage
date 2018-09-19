package manimage.main;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import manimage.common.settings.Settings;

import java.io.File;
import java.nio.file.Path;

public class SettingsController {

    public TextField lastFolderTextField;
    public Button lastFolderBrowseButton;
    public CheckBox autoAddDroppedCheckbox;
    private Settings settings = null;


    public void setSettings(Settings settings) {
        this.settings = settings;

        lastFolderTextField.setText(settings.getString("last_folder", null));
        autoAddDroppedCheckbox.setSelected(settings.getBoolean("auto_add_dropped", false));
        Platform.runLater(this::updateAutoAddDroppedDisable);
    }

    private void updateAutoAddDroppedDisable() {
        if (autoAddDroppedCheckbox.isSelected()) {
            lastFolderBrowseButton.setDisable(false);
            lastFolderTextField.setDisable(false);
        } else {
            lastFolderBrowseButton.setDisable(true);
            lastFolderTextField.setDisable(true);
        }
    }

    public void autoAddDroppedOnAction(ActionEvent event) {
        updateAutoAddDroppedDisable();
        event.consume();
    }

    public void acceptButtonOnAction(ActionEvent event) {
        settings.setString("last_folder", lastFolderTextField.getText());
        settings.setBoolean("auto_add_dropped", autoAddDroppedCheckbox.isSelected());
        ((Stage)autoAddDroppedCheckbox.getScene().getWindow()).close();
        event.consume();
    }

    public void cancelButtonOnAction(ActionEvent event) {
        ((Stage)autoAddDroppedCheckbox.getScene().getWindow()).close();
        event.consume();
    }

    public void lastFolderBrowseButtonOnAction(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Folder to auto-add to");
        String path = settings.getString("last_folder", null);
        if (path != null) dc.setInitialDirectory(new File(path));
        File directory = dc.showDialog(lastFolderBrowseButton.getScene().getWindow());
        if (directory != null) {
            settings.setString("last_folder", directory.getAbsolutePath());
            lastFolderTextField.setText(directory.getAbsolutePath());
        }
        event.consume();
    }

}
