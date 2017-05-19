package manimage.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.h2.jdbcx.JdbcDataSource;

import java.io.FileFilter;
import java.sql.Connection;
import java.sql.SQLException;

public class Main extends Application {

    public static Stage mainStage;
    public static MainController mainController;
    public static Main MAIN;
    public static Connection dbConnection;
    private Stage singleEditorStage;

    private static final String DATABASE_PATH = "jdbc:h2:C:\\Users\\Austin\\h2db";
    private static final String DATABASE_USER = "sa";
    private static final String DATABASE_PASSWORD = "sa";

    private static final String SQL_CLEAN_DATABASE = "DROP TABLE IF EXISTS images;\n" +
            "DROP TABLE IF EXISTS tags;\n" +
            "DROP TABLE IF EXISTS image_tagged;\n" +
            "DROP TABLE IF EXISTS comic_tagged;\n" +
            "DROP TABLE IF EXISTS comics;\n" +
            "DROP TABLE IF EXISTS comic_pages;\n";
    private static final String SQL_INIT_DATABASE = "CREATE TABLE images(image_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, file_path VARCHAR(1024), source_url VARCHAR(1024), rating INT NOT NULL DEFAULT(0), date_added DATE NOT NULL DEFAULT(NOW()));\n" +
            "CREATE TABLE tags(tag_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, tag_name VARCHAR(128) NOT NULL);\n" +
            "CREATE TABLE image_tagged(image_id INT NOT NULL, tag_id INT NOT NULL, PRIMARY KEY(tag_id, image_id), FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE, FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE);\n" +
            "INSERT INTO tags (tag_name) VALUES ('tagme');\n" +
            "CREATE TABLE comics(comic_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, comic_name VARCHAR(512) NOT NULL, comic_source VARCHAR(1024));\n" +
            "CREATE TABLE comic_pages(comic_id INT NOT NULL, image_id INT NOT NULL, page_num INT NOT NULL, FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE, FOREIGN KEY (comic_id) REFERENCES comics(comic_id) ON DELETE CASCADE, PRIMARY KEY (comic_id, image_id));\n" +
            "CREATE TABLE comic_tagged(comic_id INT NOT NULL, tag_id INT NOT NULL, PRIMARY KEY(tag_id, comic_id), FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE, FOREIGN KEY (comic_id) REFERENCES comics(comic_id) ON DELETE CASCADE);";

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
        //--------------- Build Database connection --------------------------------------------------------------------

        try {
            JdbcDataSource source = new JdbcDataSource();
            source.setUser(DATABASE_USER);
            source.setPassword(DATABASE_PASSWORD);
            source.setURL(DATABASE_PATH);

            dbConnection = source.getConnection();
        } catch (SQLException ex) {
            ex.printStackTrace();
            exit();
        }
        //TODO: Implement database loading/selecting by user
        //TODO: Implement database checking/initializing on connect

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

    public static void exit() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        //TODO: Refactor all exits to go through this route to ensure safe disconnects from database

        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }

}
