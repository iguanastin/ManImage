package manimage.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;

import java.io.FileFilter;
import java.io.IOException;

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
    public void start(Stage mainStage) {

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        //--------------- Discover VLC natives -------------------------------------------------------------------------

        if (new NativeDiscovery().discover()) {
            System.out.println("Native VLCLibs Version: " + LibVlc.INSTANCE.libvlc_get_version());
        } else {
            supportVideo = false;
        }

        //------------ Build main stage --------------------------------------------------------------------------------

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/application.fxml"));
        Parent mainRoot = null;
        try {
            mainRoot = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            Main.showErrorMessage("Unexpected FATAL Error", "Error loading FXML template", e.getLocalizedMessage());
            Platform.exit();
            System.exit(0);
        }
        mainStage.setTitle("ManImage");
        mainStage.setScene(new Scene(mainRoot, screen.getWidth() * 0.8, screen.getHeight() * 0.8));
        mainStage.show();
    }

    public static void main(String[] args) {
        launch(args);
//        System.exit(0);
    }

}
