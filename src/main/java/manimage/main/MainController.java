package manimage.main;

import com.sun.jna.Memory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import manimage.common.DBInterface;
import manimage.common.ImageInfo;
import manimage.common.SimilarPair;
import manimage.common.settings.Settings;
import uk.co.caprica.vlcj.component.DirectMediaPlayerComponent;
import uk.co.caprica.vlcj.player.direct.BufferFormat;
import uk.co.caprica.vlcj.player.direct.DirectMediaPlayer;
import uk.co.caprica.vlcj.player.direct.format.RV32BufferFormat;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;

public class MainController {


    public DynamicImageView previewDynamicImageView;
    public ScrollPane gridScrollPane;
    public DatabaseImageGridPane grid;
    public ChoiceBox primaryOrderByChoiceBox;
    public ChoiceBox secondaryOrderByChoiceBox;
    public ToggleButton primaryOrderByDescendingToggle;
    public ToggleButton secondaryOrderByDescendingToggle;
    public TextField searchPathTextfield;
    public TextField searchTagsTextfield;
    public VBox searchVBox;
    public Button prevPageButton;
    public Button nextPageButton;
    public TextField pageNumTextfield;
    public BorderPane rootPane;
    public SplitPane primarySplitPane;
    public Button allInSearchButton;
    public Label tagTabToggleLabel;
    public BorderPane tagTabPane;
    public ListView<String> tagTabListView;

    private DBInterface db;

    private CanvasPlayerComponent mediaPlayerComponent;

//    private File lastFolder;
//    private File lastSaveFolder;

    private String[] orderByMap;

    private final String dbPath;
    private final String dbUser = "sa";
    private final String dbPass = "";

    static final ClipboardContent clipboard = new ClipboardContent();

    private Properties properties;
    private String propertiesFilePath = "manimage.properties";
    private String settingsFilepath = "manimage.settings";

    private ContextMenu allInSearchContextMenu;

    private boolean tagTabToggledOpen = true;

    private Settings settings;


    public MainController() {
        if (System.getProperty("os.name").contains("Windows")) dbPath = System.getProperty("user.home") + "\\manimage";
        else dbPath = System.getProperty("user.home") + "/manimage";

        MenuItem[] items = new MenuItem[5];
        items[0] = new MenuItem("Edit Tags");
        items[1] = new MenuItem("Find Duplicates");
        items[2] = new SeparatorMenuItem();
        items[3] = new MenuItem("Forget Images");
        items[4] = new MenuItem("Delete Files");

        allInSearchContextMenu = new ContextMenu(items);
    }

    //---------------------- Initializers ------------------------------------------------------------------------------

    @FXML
    public void initialize() {

        backupDatabase();

        try {
            db = new DBInterface(dbPath, dbUser, dbPass);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.showErrorMessage("Error!", "Database could not be opened", ex.getLocalizedMessage());
            closeWindow();
        }

        grid.setPreviewListener(this::preview);
        grid.setDatabase(db);
        grid.setOrderBy("img_added", true, "img_id", true);

        ObservableList<String> items = FXCollections.observableArrayList("ID", "File path", "Time added", "Source URL");
        orderByMap = new String[]{"img_id", "img_path", "img_added", "img_src"};
        primaryOrderByChoiceBox.setItems(items);
        primaryOrderByChoiceBox.setValue(items.get(2));
        primaryOrderByDescendingToggle.setSelected(grid.isPrimaryOrderDescending());
        secondaryOrderByChoiceBox.setItems(items);
        secondaryOrderByChoiceBox.setValue(items.get(0));
        secondaryOrderByDescendingToggle.setSelected(grid.isSecondaryOrderDescending());

        tagTabPane.heightProperty().addListener((observable, oldValue, newValue) -> tagTabToggleLabel.setPrefHeight(newValue.doubleValue()));
        tagTabListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        rootPane.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        rootPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();
            if (files != null && !files.isEmpty()) {
                addFiles(files);
            } else if (url != null && !url.isEmpty()) {
                String filename = URI.create(url).getPath().replaceAll("^.*/", "");
                Platform.runLater(() -> {
                    if (!settings.getBoolean("auto_add_dropped", false) || settings.getString("last_folder", null) == null || !new File(settings.getString("last_folder", null)).exists()) {
                        FileChooser fc = new FileChooser();
                        fc.setTitle("Save Image As");
                        fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
                        fc.setInitialFileName(filename);
                        String path = settings.getString("last_folder", null);
                        if (path != null) fc.setInitialDirectory(new File(path));
                        File target = fc.showSaveDialog(rootPane.getScene().getWindow());

                        if (target != null) {
                            downloadImageAndSave(url, target);
                        }
                    } else {
                        String path = settings.getString("last_folder", null);
                        if (!path.endsWith("/")) path += "/";
                        File target = new File(path + filename);

                        int i = 1;
                        while (target.exists()) {
                            target = new File(path + filename.substring(0, filename.lastIndexOf('.')) + " (" + i + ')' + filename.substring(filename.lastIndexOf('.')));
                            i++;
                        }

                        downloadImageAndSave(url, target);
                    }
                });
            }
            event.consume();
        });

        gridScrollPane.viewportBoundsProperty().addListener((observable, oldValue, newValue) -> grid.updateWidth(newValue.getWidth()));

        initProperties();

        Platform.runLater(() -> {
            rootPane.getScene().getWindow().setOnCloseRequest(event -> {
                closeWindow();
            });
            grid.updateSearchContents();
            if (grid.getCount() > 0) {
                grid.select(grid.getImageViews().get(0), false, false);
                preview(grid.getLastSelected().getInfo());
            }
            grid.requestFocus();
        });

    }

    private void downloadImageAndSave(String url, File target) {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.addRequestProperty("User-Agent", "Mozilla/4.0");
                ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
                FileOutputStream fos = new FileOutputStream(target);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                settings.setString("last_folder", target.getParentFile().getAbsolutePath());

                Platform.runLater(() -> {
                    try {
                        db.addImage(target.getAbsolutePath());
                    } catch (SQLException e) {
                        e.printStackTrace();
                        Main.showErrorMessage("Unexpected Error", "Error adding image to database", e.getLocalizedMessage());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error saving file from URL", e.getLocalizedMessage());
            }
        }).start();
    }

    private void initProperties() {
        try {
            settings = new Settings(new File(settingsFilepath));
        } catch (FileNotFoundException e) {
            settings = new Settings();
        }

        Platform.runLater(() -> {
            rootPane.getScene().getWindow().setX(settings.getDouble("window_x", 0));
            rootPane.getScene().getWindow().setY(settings.getDouble("window_y", 0));
            rootPane.getScene().getWindow().setWidth(settings.getDouble("window_width", 800));
            rootPane.getScene().getWindow().setHeight(settings.getDouble("window_height", 600));
            ((Stage) rootPane.getScene().getWindow()).setMaximized(settings.getBoolean("window_maximized", false));
            primarySplitPane.setDividerPosition(0, settings.getDouble("window_split_percent", 0.33));
        });
    }

    private void saveProperties() throws FileNotFoundException {
        settings.setBoolean("window_maximized", ((Stage) rootPane.getScene().getWindow()).isMaximized());
        settings.setDouble("window_x", rootPane.getScene().getWindow().getX());
        settings.setDouble("window_y", rootPane.getScene().getWindow().getY());
        settings.setDouble("window_width", rootPane.getScene().getWindow().getWidth());
        settings.setDouble("window_height", rootPane.getScene().getWindow().getHeight());
        settings.setDouble("window_split_percent", primarySplitPane.getDividerPositions()[0]);

        settings.save(new File(settingsFilepath));
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    private void preview(ImageInfo info) {
        if (Main.supportVideo && mediaPlayerComponent != null) {
            mediaPlayerComponent.getMediaPlayer().stop();
            mediaPlayerComponent.getMediaPlayer().release();
            mediaPlayerComponent.release();
            mediaPlayerComponent = null;
        }

        if (info != null) {
            if (Main.IMAGE_FILTER.accept(info.getPath())) {
                previewImage(info);
            } else if (Main.supportVideo && Main.VIDEO_FILTER.accept(info.getPath())) {
                previewVideo(info);
            } else {
                Main.showErrorMessage("Error", "Unsupported file extension", info.getPath().getAbsolutePath());
                preview(null);
            }
        } else {
            previewDynamicImageView.setImage(null);
            tagTabListView.getItems().clear();
        }
    }

    private void previewVideo(ImageInfo info) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        WritableImage img = new WritableImage((int) screen.getWidth(), (int) screen.getHeight());
        //TODO: Fix video viewing aspect ratio
        mediaPlayerComponent = new CanvasPlayerComponent(img);
        previewDynamicImageView.setImage(img);
        mediaPlayerComponent.getMediaPlayer().prepareMedia(info.getPath().getAbsolutePath());
        mediaPlayerComponent.getMediaPlayer().start();
        mediaPlayerComponent.getMediaPlayer().setRepeat(true);
        tagTabListView.getItems().clear();
        tagTabListView.getItems().addAll(info.getTags());
        Collections.sort(tagTabListView.getItems());
    }

    private void previewImage(ImageInfo info) {
        previewDynamicImageView.setImage(info.getImage(true));
        try {
            Map<String, Integer> tags = db.getTags();
            tagTabListView.getItems().clear();
            for (String tag : info.getTags()) {
                tagTabListView.getItems().add(tag + " (" + tags.get(tag) + ")");
            }
            Collections.sort(tagTabListView.getItems());
        } catch (SQLException e) {
            e.printStackTrace();
            Main.showErrorMessage("Unexpected Error", "Error retrieving tags list from database", e.getLocalizedMessage());
        }
    }

    private void applySearchFilter() {
        //Set order
        grid.setOrderBy(orderByMap[primaryOrderByChoiceBox.getItems().indexOf(primaryOrderByChoiceBox.getValue())], primaryOrderByDescendingToggle.isSelected(), orderByMap[secondaryOrderByChoiceBox.getItems().indexOf(secondaryOrderByChoiceBox.getValue())], secondaryOrderByDescendingToggle.isSelected());
        //Set tags
        grid.setSearchTags(searchTagsTextfield.getText().split(" "));
        //Set filepath
        grid.setSearchFilePath(searchPathTextfield.getText());
        grid.requestFocus();
        //Set page
        setPage(0);
    }

    private void closeWindow() {
        try {
            saveProperties();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Main.showErrorMessage("Unexpected Error", "Error saving settings to file", e.getLocalizedMessage());
        }

        Platform.exit();
        System.exit(0);
    }

    private static void ensureVisible(ScrollPane pane, Node node) {
        Bounds viewport = pane.getViewportBounds();
        double contentHeight = pane.getContent().getBoundsInLocal().getHeight();
        double nodeMinY = node.getBoundsInParent().getMinY();
        double nodeMaxY = node.getBoundsInParent().getMaxY();
        double viewportMinY = (contentHeight - viewport.getHeight()) * pane.getVvalue();
        double viewportMaxY = viewportMinY + viewport.getHeight();
        if (nodeMinY < viewportMinY) {
            pane.setVvalue(nodeMinY / (contentHeight - viewport.getHeight()));
        } else if (nodeMaxY > viewportMaxY) {
            pane.setVvalue((nodeMaxY - viewport.getHeight()) / (contentHeight - viewport.getHeight()));
        }
    }

    private void backupDatabase() {
        try {
            File cur = new File(dbPath + ".mv.db");
            File bak1 = new File(dbPath + ".mv.db.bak");
            File bak2 = new File(dbPath + ".mv.db-2.bak");
            File bak3 = new File(dbPath + ".mv.db-3.bak");
            //Copy bak2 to bak3 if it exists
            if (bak2.exists()) Files.copy(bak2.toPath(), bak3.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //Copy bak1 to bak2 if it exists
            if (bak1.exists()) Files.copy(bak1.toPath(), bak2.toPath(), StandardCopyOption.REPLACE_EXISTING);
            //Copy cur to bak1
            Files.copy(cur.toPath(), bak1.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backed up the database to: " + dbPath + ".mv.db.bak");
        } catch (IOException e) {
            System.err.println("Unable to backup database: " + e.getLocalizedMessage());
        }
    }

    private void addFiles(List<File> files) {
        if (files != null && !files.isEmpty()) {
            ArrayList<File> work = new ArrayList<>();
            files.forEach(file -> {
                if (file.exists() && Main.IMG_VID_FILTER.accept(file)) work.add(file);
            });
            DBInterface db = grid.getDatabase();

            try {
                db.addBatchImages(work);
            } catch (SQLException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error adding batch images to database", e.getLocalizedMessage());
            }

            settings.setString("last_folder", files.get(0).getParentFile().getAbsolutePath());
        }
    }

    private void addFolder(File folder) {
        if (folder != null) {
            DBInterface db = grid.getDatabase();
            File[] files = folder.listFiles(Main.IMG_VID_FILTER);
            if (files == null) return;
            try {
                db.addBatchImages(Arrays.asList(files));
            } catch (SQLException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error adding batch images to database", e.getLocalizedMessage());
            }

            settings.setString("last_folder", folder.getParentFile().getAbsolutePath());
        }
    }

    private void addRecurseFolder(File folder) {
        if (folder != null) {
            final DBInterface db = grid.getDatabase();

            final ArrayList<File> files = new ArrayList<>();
            for (File fldr : getSubFolders(folder)) {
                files.addAll(Arrays.asList(fldr.listFiles(Main.IMG_VID_FILTER)));
            }
            try {
                db.addBatchImages(files);
            } catch (SQLException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error adding batch images to database", e.getLocalizedMessage());
            }

            settings.setString("last_folder", folder.getParentFile().getAbsolutePath());
        }
    }

    private void showImage(GridImageView last) {
        if (last != null) {
            preview(last.getInfo());
            ensureVisible(gridScrollPane, last);
            grid.updateVisibleThumbnails();
        } else {
            preview(null);
        }
    }

    private void nextPage() {
        setPage(grid.getPage() + 1);
    }

    private void previousPage() {
        setPage(grid.getPage() - 1);
    }

    private void setPage(int i) {
        if (i >= 0) {
            grid.setPage(i);
            pageNumTextfield.setText(grid.getPage() + "");
            grid.unselectAll();
            showImage(grid.selectFirst(false, false));
        }
    }

    //--------------------- Getters ------------------------------------------------------------------------------------

    private ArrayList<File> getSubFolders(File folder) {
        File[] folders = folder.listFiles();
        ArrayList<File> results = new ArrayList<>();

        if (folders == null) return results;

        for (File file : folders) {
            if (file.isDirectory()) {
                results.add(file);
                results.addAll(getSubFolders(file));
            }
        }

        return results;
    }

//------------------- Private classes ------------------------------------------------------------------------------

    private class CanvasPlayerComponent extends DirectMediaPlayerComponent {

        WritableImage img;
        WritablePixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();


        CanvasPlayerComponent(WritableImage img) {
            super((i, i1) -> new RV32BufferFormat((int) Screen.getPrimary().getVisualBounds().getWidth(), (int) Screen.getPrimary().getVisualBounds().getHeight()));
            this.img = img;
        }

        @Override
        public void display(DirectMediaPlayer mediaPlayer, Memory[] nativeBuffers, BufferFormat bufferFormat) {
            if (img == null) {
                return;
            }
            Platform.runLater(() -> {
                Memory[] lock = mediaPlayer.lock();
                try {
                    if (lock != null) {
                        Memory nativeBuffer = lock[0];
                        ByteBuffer byteBuffer = nativeBuffer.getByteBuffer(0, nativeBuffer.size());
                        img.getPixelWriter().setPixels(0, 0, bufferFormat.getWidth(), bufferFormat.getHeight(), pixelFormat, byteBuffer, bufferFormat.getPitches()[0]);
                    }
                } finally {
                    mediaPlayer.unlock();
                }
            });
        }

    }

    //-------------------- Listeners -----------------------------------------------------------------------------------

    private void requestFilesToAdd() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
        fc.setTitle("Add image(s)");
        String path = settings.getString("last_folder", null);
        if (path != null) fc.setInitialDirectory(new File(path));
        List<File> files = fc.showOpenMultipleDialog(rootPane.getScene().getWindow());

        addFiles(files);
    }

    private void requestFolderToAdd() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder");
        String path = settings.getString("last_folder", null);
        if (path != null) dc.setInitialDirectory(new File(path));
        File folder = dc.showDialog(rootPane.getScene().getWindow());

        addFolder(folder);
    }

    private void requestRecursiveFolderToAdd() {
        final DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder and all subfolders");
        String path = settings.getString("last_folder", null);
        if (path != null) dc.setInitialDirectory(new File(path));
        final File folder = dc.showDialog(rootPane.getScene().getWindow());

        addRecurseFolder(folder);
    }

    public void gridScrollPaneClicked(MouseEvent event) {
        grid.unselectAll();
    }

    public void gridScrollPaneKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.DELETE) {
            if (event.isControlDown()) {
                if (Main.getUserConfirmation("Forget Files", "Remove these files from the database permanently?", "This action cannot be undone!")) {
                    grid.removeSelected();
                }
            } else {
                if (Main.getUserConfirmation("Delete Files", "Delete these files permanently?", "This action cannot be undone!")) {
                    grid.deleteSelected();
                }
            }
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.A) {
            if (grid.areAllSelected()) {
                grid.unselectAll();
            } else {
                grid.selectAll();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.HOME) {
            showImage(grid.selectFirst(event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.END) {
            showImage(grid.selectLast(event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.PAGE_DOWN) {
            nextPage();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.PAGE_UP) {
            previousPage();
            event.consume();
        } else if (event.getCode() == KeyCode.PAGE_DOWN) {
            showImage(grid.selectDown(3, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.PAGE_UP) {
            showImage(grid.selectUp(3, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.E) {
            grid.openTagEditorDialog();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.H) {
            List<SimilarPair> pairs = new ArrayList<>();
            for (int i = 0; i < grid.getSelected().size(); i++) {
                for (int j = i + 1; j < grid.getSelected().size(); j++) {
                    final ImageInfo img1 = grid.getSelected().get(i).getInfo();
                    final ImageInfo img2 = grid.getSelected().get(j).getInfo();
                    if (!img1.getPath().getName().toLowerCase().endsWith(".gif") && !img2.getPath().getName().toLowerCase().endsWith(".gif")) {
                        final double similarity = img1.getHistogram().getSimilarity(img2.getHistogram());
                        final double confidence = 0.9;
                        if (similarity >= confidence) {
                            pairs.add(new SimilarPair(img1, img2, similarity));
                        }
                    }
                }
            }
            try {
                Stage stage = new Stage();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/duplicateresolver.fxml"));
                stage.setScene(new Scene(loader.load(), Screen.getPrimary().getVisualBounds().getWidth() * 0.8, Screen.getPrimary().getVisualBounds().getHeight() * 0.8));
                ((DuplicateResolverController) loader.getController()).setDataset(db, pairs);
                stage.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error loading FXML template", e.getLocalizedMessage());
            }
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.S) {
            try {
                Stage stage = new Stage();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
                stage.setScene(new Scene(loader.load()));
                ((SettingsController) loader.getController()).setSettings(settings);
                stage.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
                Main.showErrorMessage("Unexpected Error", "Error loading FXML template", e.getLocalizedMessage());
            }
            event.consume();
        } else if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) {
            showImage(grid.selectLeft(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) {
            showImage(grid.selectRight(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) {
            showImage(grid.selectDown(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W) {
            showImage(grid.selectUp(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        }
    }

    public void gridScrolled(ScrollEvent event) {
        grid.updateVisibleThumbnails();
    }

    public void onSearchButtonAction(ActionEvent event) {
        applySearchFilter();
    }

    public void searchTagsTextFieldOnAction(ActionEvent event) {
        applySearchFilter();
    }

    public void searchPathTextFieldOnAction(ActionEvent event) {
        applySearchFilter();
    }

    public void pageNumTextfieldOnAction(ActionEvent event) {
        int page;
        try {
            page = Integer.parseInt(pageNumTextfield.getText());
        } catch (NumberFormatException ex) {
            return;
        }
        setPage(page);
    }

    public void nextPageButtonOnAction(ActionEvent event) {
        nextPage();
    }

    public void prevPageButtonOnAction(ActionEvent event) {
        previousPage();
    }

    public void rootPaneKeyPressed(KeyEvent event) {
        if (event.isControlDown() && event.getCode() == KeyCode.Q) {
            closeWindow();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.F) {
            searchTagsTextfield.requestFocus();
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.I) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Folder or Files");
            a.setContentText("Import folder or files?");
            ButtonType folderButton = new ButtonType("Folder", ButtonBar.ButtonData.YES);
            ButtonType filesButton = new ButtonType("Files", ButtonBar.ButtonData.NO);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            a.getButtonTypes().setAll(folderButton, filesButton, cancel);
            a.showAndWait().ifPresent(type -> {
                if (type == folderButton) {
                    Alert a2 = new Alert(Alert.AlertType.CONFIRMATION);
                    a2.setTitle("Recursive");
                    a2.setContentText("Recursively add sub-folders?");
                    ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                    ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                    a2.getButtonTypes().setAll(yesButton, noButton, cancel);
                    a2.showAndWait().ifPresent(type2 -> {
                        if (type2 == yesButton) {
                            requestRecursiveFolderToAdd();
                        } else if (type2 == noButton) {
                            requestFolderToAdd();
                        }
                    });
                } else if (type == filesButton) {
                    requestFilesToAdd();
                }
            });
            event.consume();
        }
    }

    public void searchPaneKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            grid.requestFocus();
            event.consume();
        }
    }

    public void allInSearchButtonOnAction(ActionEvent event) {
        Point2D p = allInSearchButton.localToScreen(allInSearchButton.getLayoutX(), allInSearchButton.getLayoutY());
        allInSearchContextMenu.show(searchVBox, p.getX(), p.getY());
    }

    public void tagTabMouseEntered(MouseEvent event) {
        if (!tagTabToggledOpen) {
            tagTabPane.setPrefWidth(200);
            tagTabListView.setVisible(true);
            event.consume();
        }
    }

    public void tagTabMouseExited(MouseEvent event) {
        if (!tagTabToggledOpen) {
            tagTabPane.setPrefWidth(tagTabToggleLabel.getWidth());
            tagTabListView.setVisible(false);
            event.consume();
        }
    }

    public void tagTabToggleClicked(MouseEvent event) {
        if (tagTabToggledOpen) {
            tagTabPane.setPrefWidth(tagTabToggleLabel.getWidth());
            tagTabListView.setVisible(false);
            tagTabToggleLabel.setText(">");
            tagTabToggledOpen = false;
        } else {
            tagTabPane.setPrefWidth(200);
            tagTabListView.setVisible(true);
            tagTabToggleLabel.setText("<");
            tagTabToggledOpen = true;
        }
        event.consume();
    }

}
