package manimage.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import manimage.common.HistogramReadException;
import manimage.common.ImageHistogram;
import manimage.editors.SingleEditorController;

import java.io.FileFilter;
import java.io.IOException;

public class Main extends Application {

    public static Stage mainStage;
    public static MainController mainController;

    private Stage singleEditorStage;

    public static Main MAIN;

    public static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };

    public static final FileFilter IMAGE_AND_DIRECTORY_FILTER = file -> file.isDirectory() || IMAGE_FILTER.accept(file);

    public static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif)", "*.png", "*.jpg", "*.jpeg", "*.gif");


    public Main() {
        MAIN = this;
    }

    public void openSingleEditor() {
        singleEditorStage.show();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //------------ Build main stage --------------------------------------------------------------------------------

        mainStage = primaryStage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("application.fxml"));
        Parent mainRoot = loader.load();
        mainController = loader.getController();

        primaryStage.setTitle("Test");
        primaryStage.setOnCloseRequest(event -> System.out.println("Exiting via window control"));
        primaryStage.setScene(new Scene(mainRoot, 1600, 900));

        primaryStage.show();

        //-------------- Build SingleEditor stage ----------------------------------------------------------------------

        singleEditorStage = new Stage();

        Parent singleEditorRoot = FXMLLoader.load(getClass().getResource("../editors/singleeditor.fxml"));

        singleEditorStage.setScene(new Scene(singleEditorRoot));
        singleEditorStage.setTitle("Edit Image");

    }

    public static void main(String[] args) {
//        launch(args);

        try {
            ImageHistogram hist1 = ImageHistogram.getHistogram(new Image("file:C:\\Users\\Austin\\Documents\\(cjdrfr)\\cj\\New folder\\Loli\\1486060110154.png"));
            ImageHistogram hist2 = ImageHistogram.getHistogram(new Image("file:C:\\Users\\Austin\\Documents\\(cjdrfr)\\cj\\New folder\\Loli\\1486060110154 - Copy.png"));

            System.out.println(hist1.getSimilarity(hist2));
            System.out.println(hist1.isSimilar(hist2, 0.05));
        } catch (HistogramReadException ex) {
            ex.printStackTrace();
        }

        System.exit(0);
    }

}
