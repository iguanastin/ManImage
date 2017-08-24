package manimage.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends Application {

    static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };
    static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif");

    private static boolean isIDE = false;

    //TODO: Redesign this GUI entirely
    /*Things to take into account:
        Batched editing
        Quick previews
        Complex searches
        Limited space
        Modular pieces
            Duplicate checking
            Comic reading
            Mass editor
            Slideshow
        Other forms of media?
            Video
            Text
        Inline editors?
        CSS Styling?
        Pages
    */

    static boolean getUserConfirmation(String title, String header, String content) {
        Alert d = new Alert(Alert.AlertType.CONFIRMATION);
        d.setTitle(title);
        d.setHeaderText(header);
        d.setContentText(content);
        return d.showAndWait().get() == ButtonType.OK;
    }

    static void showErrorMessage(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(header);
        a.setContentText(content);
        a.showAndWait();
    }

    @Override
    public void start(Stage mainStage) throws Exception {

        //------------ Build main stage --------------------------------------------------------------------------------

        FXMLLoader loader;
        if (isIDE) loader = new FXMLLoader(getClass().getResource("../fxml/application.fxml"));
        else loader = new FXMLLoader(new URL("file:" + new File("fxml/application.fxml").getAbsolutePath()));
        Parent mainRoot = loader.load();
        mainStage.setTitle("ManImage");
        mainStage.setScene(new Scene(mainRoot, 1600, 900));
        mainStage.show();

        MainController mainController = loader.getController();
        mainController.setStage(mainStage);
        mainController.grid.updateSearchContents();
        if (mainController.grid.getCount() > 0) {
            mainController.grid.select((GridImageView) mainController.grid.getChildren().get(0), false, false);
            mainController.preview(mainController.grid.getLastSelected().getInfo());
        }
    }

    public static void main(String[] args) throws MalformedURLException {
        for (String arg : args) {
            if (arg.equalsIgnoreCase("ide")) {
                isIDE = true;
                break;
            }
        }

        launch(args);
//        System.exit(0);
    }

}
