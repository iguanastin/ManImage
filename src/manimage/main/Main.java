package manimage.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileFilter;

public class Main extends Application {

    public static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };
    public static final FileFilter IMAGE_AND_DIRECTORY_FILTER = file -> file.isDirectory() || IMAGE_FILTER.accept(file);
    public static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif");

    @Override
    public void start(Stage mainStage) throws Exception {
        //------------ Build main stage --------------------------------------------------------------------------------

        FXMLLoader loader = new FXMLLoader(getClass().getResource("../fxml/application.fxml"));
        Parent mainRoot = loader.load();
        mainStage.setTitle("ManImage");
        mainStage.setScene(new Scene(mainRoot, 1600, 900));
        mainStage.show();

        MainController mainController = loader.getController();
        mainController.setStage(mainStage);

        //-------------- Build SingleEditor stage ----------------------------------------------------------------------

        Stage singleEditorStage = new Stage();
        Parent singleEditorRoot = FXMLLoader.load(getClass().getResource("../fxml/singleeditor.fxml"));
        singleEditorStage.setScene(new Scene(singleEditorRoot));
        singleEditorStage.setTitle("Edit Image");
    }

    public static void main(String[] args) {
        launch(args);
    }

}
