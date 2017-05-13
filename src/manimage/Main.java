package manimage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.FileFilter;

public class Main extends Application {

    static Stage mainStage;
    static Controller controller;

    static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };

    static final FileFilter IMAGE_AND_DIRECTORY_FILTER = file -> file.isDirectory() || IMAGE_FILTER.accept(file);

    static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif");


    @Override
    public void start(Stage primaryStage) throws Exception {
        mainStage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));;
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.setTitle("Test");
        primaryStage.setScene(new Scene(root, 1600, 900));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> System.out.println("Exiting via window control"));
    }

    public static void main(String[] args) {
        launch(args);
    }

}
