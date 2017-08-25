package manimage.main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import manimage.common.DBInterface;
import manimage.common.ImageInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.*;

public class MainController {


    public DynamicImageView previewDynamicImageView;
    public Label previewTagsLabel;

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
    public SplitPane rootPane;

    private DBInterface db;
    private Stage stage;

    private File lastFolder;
    private File lastSaveFolder;
    private String[] orderByMap;

    private final String dbPath;
    private final String dbUser = "sa";
    private final String dbPass = "";

    static final ClipboardContent clipboard = new ClipboardContent();


    public MainController() {
        if (System.getProperty("os.name").contains("Windows")) dbPath = System.getProperty("user.home") + "\\manimage";
        else dbPath = System.getProperty("user.home") + "/manimage";
    }

    //---------------------- Initializers ------------------------------------------------------------------------------

    @FXML
    public void initialize() {

        backupDatabase();

        try {
            db = new DBInterface(dbPath, dbUser, dbPass);
        } catch (SQLException ex) {
            ex.printStackTrace();
            Main.showErrorMessage("Error!", "Database is already in use and cannot be opened", ex.getLocalizedMessage());
            Platform.exit();
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

        rootPane.setOnDragOver(event -> {
            if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
                event.acceptTransferModes(TransferMode.ANY);
            }
            event.consume();
        });
        rootPane.setOnDragDropped(event -> {
            List<File> files = event.getDragboard().getFiles();
            String url = event.getDragboard().getUrl();
            Platform.runLater(() -> {
                if (files != null && !files.isEmpty()) {
                    addFiles(files);
                } else if (url != null && !url.isEmpty()) {
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Save Image As");
                    fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
                    fc.setInitialFileName(url.substring(url.lastIndexOf("/") + 1));
                    fc.setInitialDirectory(lastSaveFolder);
                    File target = fc.showSaveDialog(stage);

                    if (target != null) {
                        try {
                            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                            conn.addRequestProperty("User-Agent", "Mozilla/4.0");
                            ReadableByteChannel rbc = Channels.newChannel(conn.getInputStream());
                            FileOutputStream fos = new FileOutputStream(target);
                            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                            lastSaveFolder = target.getParentFile();

                            try {
                                db.addImage(target.getAbsolutePath());
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Main.showErrorMessage("Error!", "Error saving file from URL", e.getMessage());
                        }
                    }
                }
            });
            event.consume();
        });

        Platform.runLater(() -> {

            grid.updateSearchContents();
            if (grid.getCount() > 0) {
                grid.select(grid.getImageViews().get(0), false, false);
                preview(grid.getLastSelected().getInfo());
            }
        });
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    void preview(ImageInfo info) {
        if (info != null) {
            previewDynamicImageView.setImage(info.getImage());
            previewTagsLabel.setText(String.join(", ", info.getTags()));
        } else {
            previewDynamicImageView.setImage(null);
            previewTagsLabel.setText(null);
        }
    }

    private void applySearchFilter() {
        //Set order
        grid.setOrderBy(orderByMap[primaryOrderByChoiceBox.getItems().indexOf(primaryOrderByChoiceBox.getValue())], primaryOrderByDescendingToggle.isSelected(), orderByMap[secondaryOrderByChoiceBox.getItems().indexOf(secondaryOrderByChoiceBox.getValue())], secondaryOrderByDescendingToggle.isSelected());
        //Set tags
        grid.setSearchTags(searchTagsTextfield.getText().split(" "));
        //Set filepath
        grid.setSearchFilePath(searchPathTextfield.getText());
        //Set page
        setPage(0);

        grid.updateSearchContents();
        grid.requestFocus();
        showImage(grid.selectFirst(false, false));
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
                if (file.exists() && Main.IMAGE_FILTER.accept(file)) work.add(file);
            });
            DBInterface db = grid.getDatabase();

            try {
                db.addBatchImages(work);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            lastFolder = files.get(0).getParentFile();
        }
    }

    private void addFolder(File folder) {
        if (folder != null) {
            DBInterface db = grid.getDatabase();
            File[] files = folder.listFiles(Main.IMAGE_FILTER);
            if (files == null) return;
            try {
                db.addBatchImages(Arrays.asList(files));
            } catch (SQLException e) {
                e.printStackTrace();
            }

            lastFolder = folder.getParentFile();
        }
    }

    private void addRecurseFolder(File folder) {
        if (folder != null) {
            final DBInterface db = grid.getDatabase();

            final ArrayList<File> files = new ArrayList<>();
            for (File fldr : getSubFolders(folder)) {
                files.addAll(Arrays.asList(fldr.listFiles(Main.IMAGE_FILTER)));
            }
            try {
                db.addBatchImages(files);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            lastFolder = folder.getParentFile();
        }
    }

    private void showImage(GridImageView last) {
        if (last != null) {
            preview(last.getInfo());
            ensureVisible(gridScrollPane, last);
            grid.updateVisibleThumbnails();
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

    //-------------------------- Setters -------------------------------------------------------------------------------

    void setStage(Stage stage) {
        this.stage = stage;
    }

    //-------------------- Listeners -----------------------------------------------------------------------------------

    public void addFilesClicked(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
        fc.setTitle("Add image(s)");
        fc.setInitialDirectory(lastFolder);
        List<File> files = fc.showOpenMultipleDialog(stage);

        addFiles(files);
    }

    public void addFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(stage);

        addFolder(folder);
    }

    public void addRecurseFolderClicked(ActionEvent event) {
        final DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder and all subfolders");
        dc.setInitialDirectory(lastFolder);
        final File folder = dc.showDialog(stage);

        addRecurseFolder(folder);
    }

    public void exitClicked(ActionEvent event) {
        Platform.exit();
    }

    public void aboutMenuActivated(ActionEvent event) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText("a\nb\nc\nd\ne");
        a.setTitle("About");
        a.setHeaderText("ManImage");
        a.showAndWait();
    }

    public void gridScrollPaneClicked(MouseEvent event) {
        grid.unselectAll();
    }

    public void gridScrollPaneKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            showImage(grid.selectLeft(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            showImage(grid.selectRight(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            showImage(grid.selectDown(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            showImage(grid.selectUp(1, event.isShiftDown(), event.isControlDown()));
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE) {
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
            for (GridImageView view : grid.getSelected()) {
                System.out.println(view.getInfo().getHistogram().getSimilarity(grid.getLastSelected().getInfo().getHistogram()));
            }
            event.consume();
        }
    }

    public void gridScrolled(ScrollEvent event) {
        grid.updateVisibleThumbnails();
    }

    public void clearAllOnAction(ActionEvent event) {
        Alert d = new Alert(Alert.AlertType.CONFIRMATION);
        d.setTitle("Clear Database");
        d.setHeaderText("Erase all data in database?");
        d.setContentText(" All data will be lost!");
        Optional result = d.showAndWait();
        if (result.get() == ButtonType.OK) {
            try {
                db.cleanDB();
                preview(null);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
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
            Platform.exit();
        } else if (event.isControlDown() && event.getCode() == KeyCode.R) {
            searchTagsTextfield.requestFocus();
        }
    }

    public void hotkeysMenuActivated(ActionEvent event) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Help");
        a.setHeaderText("Hotkeys");
        a.setContentText("Ctrl+E\t\tEdit tags of the selected images\n" +
                "Ctrl+Q\t\tQuit ManImage\n" +
                "Ctrl+R\t\tFocus the tag searchbar\n" +
                "Ctrl+A\t\tSelect all/no images\n" +
                "Ctrl+PgDwn\tGo to next page\n" +
                "Ctrl+PgUp\tGo to previous page");
        a.showAndWait();
    }

    public void searchPaneKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            grid.requestFocus();
            event.consume();
        }
    }

}
