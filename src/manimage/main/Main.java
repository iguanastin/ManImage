package manimage.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import manimage.common.HistogramReadException;
import manimage.common.ImageHistogram;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

import java.io.File;
import java.io.FileFilter;
import java.net.MalformedURLException;
import java.net.URL;

public class Main extends Application {

    static final FileFilter IMAGE_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif");
    };
    static final FileFilter VIDEO_FILTER = file -> {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".webm") || name.endsWith(".flv") || name.endsWith(".wmv") || name.endsWith(".3gp") || name.endsWith(".mov") || name.endsWith(".mpg");
    };
    static final FileFilter IMG_VID_FILTER = file -> IMAGE_FILTER.accept(file) || VIDEO_FILTER.accept(file);
    static final FileChooser.ExtensionFilter EXTENSION_FILTER = new FileChooser.ExtensionFilter("Image and Video Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.mp4", "*.avi", "*.webm", "*.flv", "*.wmv", "*.3gp", "*.mov", "*.mpg");

    static boolean supportVideo = true;


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

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        //--------------- Discover VLC natives -------------------------------------------------------------------------

        if (new NativeDiscovery().discover()) {
            System.out.println("Native VLCLibs Version: " + LibVlc.INSTANCE.libvlc_get_version());
        } else {
            supportVideo = false;
        }

        //------------ Build main stage --------------------------------------------------------------------------------

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/manimage/fxml/application.fxml"));
        Parent mainRoot = loader.load();
        mainStage.setTitle("ManImage");
        mainStage.setScene(new Scene(mainRoot, screen.getWidth() * 0.8, screen.getHeight() * 0.8));
        mainStage.show();

//        Parent mainRoot = FXMLLoader.load(getClass().getResource("/manimage/fxml/duplicateresolver.fxml"));
//        mainStage.setScene(new Scene(mainRoot, screen.getWidth() * 0.8, screen.getHeight() * 0.8));
//        mainStage.show();
    }

    public static void main(String[] args) {
        launch(args);
//        System.exit(0);
    }

}
