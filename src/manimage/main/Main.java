package manimage.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileFilter;

public class Main extends Application {

    public static Stage mainStage;
    public static MainController mainController;
    public static Main MAIN;
    private Stage singleEditorStage;

    public static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };
    public static final FileFilter IMAGE_AND_DIRECTORY_FILTER = file -> file.isDirectory() || IMAGE_FILTER.accept(file);
    public static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif");


    public Main() {
        MAIN = this;

        //TODO: Change design to not require poor singleton Main
    }

    public void openSingleEditor() {
        singleEditorStage.show();

        //TODO: Implement targeting of specific ImageInfo
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //------------ Build main stage --------------------------------------------------------------------------------

        mainStage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent mainRoot = loader.load();
        mainController = loader.getController();

        primaryStage.setTitle("Test");
        primaryStage.setScene(new Scene(mainRoot, 1600, 900));

        primaryStage.show();

        //-------------- Build SingleEditor stage ----------------------------------------------------------------------

        singleEditorStage = new Stage();

        Parent singleEditorRoot = FXMLLoader.load(getClass().getResource("../editors/singleeditor.fxml"));

        //TODO: Refactor fxml file locations to be consistent

        singleEditorStage.setScene(new Scene(singleEditorRoot));
        singleEditorStage.setTitle("Edit Image");
    }

    public static void main(String[] args) {
        launch(args);
    }

}
