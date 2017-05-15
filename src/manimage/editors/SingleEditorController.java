package manimage.editors;


import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TextField;

public class SingleEditorController {

    public TextField sourceTextField;
    public TextField tagTextField;
    public ComboBox ratingComboBox;

    @FXML
    public void initialize() {
        ratingComboBox.getItems().addAll("No rating", "★", "★★", "★★★", "★★★★", "★★★★★");
        ratingComboBox.getSelectionModel().select(0);
    }

    public void addTagActivated(ActionEvent event) {

    }

    public void acceptActivated(ActionEvent event) {

    }

    public void cancelActivated(ActionEvent event) {

    }

}
