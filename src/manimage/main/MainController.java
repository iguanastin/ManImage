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
import manimage.common.ImgInfo;

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
    private String dbUser = "sa";
    private String dbPass = "";

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
        }

        grid.setPreviewListener(this::preview);
        grid.setDatabase(db);
        grid.setOrderBy("img_added", true, "img_id", true);

        ObservableList<String> items = FXCollections.observableArrayList("ID", "Rating", "File path", "Time added", "Source URL");
        orderByMap = new String[]{"img_id", "img_rating", "img_path", "img_added", "img_src"};
        primaryOrderByChoiceBox.setItems(items);
        primaryOrderByChoiceBox.setValue(items.get(3));
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
            if (event.getDragboard().hasFiles()) {
                addFiles(event.getDragboard().getFiles());
            } else if (event.getDragboard().hasUrl()) {
                FileChooser fc = new FileChooser();
                fc.setTitle("Save Image As");
                fc.getExtensionFilters().add(Main.EXTENSION_FILTER);
                fc.setInitialFileName(event.getDragboard().getUrl().substring(event.getDragboard().getUrl().lastIndexOf("/") + 1));
                fc.setInitialDirectory(lastSaveFolder);
                File target = fc.showSaveDialog(stage);

                if (target != null) {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(event.getDragboard().getUrl()).openConnection();
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
                        Alert a = new Alert(Alert.AlertType.ERROR);
                        a.setContentText("Error saving file from url: " + e.getLocalizedMessage());
                        a.showAndWait();
                    }
                }
            }
            event.consume();
        });
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    void preview(ImgInfo info) {
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
        grid.setPage(0);
        pageNumTextfield.setText("0");
        gridScrollPane.vvalueProperty().setValue(0);

        grid.updateSearchContents();
        grid.requestFocus();
        if (grid.getCount() > 0) {
            grid.select((GridImageView) grid.getChildren().get(0), false, false);
            preview(grid.getLastSelected().getInfo());
        }
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
            grid.selectLeft(event.isShiftDown(), event.isControlDown());
            if (grid.getLastSelected() != null) {
                preview(grid.getLastSelected().getInfo());
                ensureVisible(gridScrollPane, grid.getLastSelected());
                grid.updateVisibleThumbnails();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            grid.selectRight(event.isShiftDown(), event.isControlDown());
            if (grid.getLastSelected() != null) {
                preview(grid.getLastSelected().getInfo());
                ensureVisible(gridScrollPane, grid.getLastSelected());
                grid.updateVisibleThumbnails();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.DOWN) {
            grid.selectDown(event.isShiftDown(), event.isControlDown());
            if (grid.getLastSelected() != null) {
                preview(grid.getLastSelected().getInfo());
                ensureVisible(gridScrollPane, grid.getLastSelected());
                grid.updateVisibleThumbnails();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.UP) {
            grid.selectUp(event.isShiftDown(), event.isControlDown());
            if (grid.getLastSelected() != null) {
                preview(grid.getLastSelected().getInfo());
                ensureVisible(gridScrollPane, grid.getLastSelected());
                grid.updateVisibleThumbnails();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE) {
            if (event.isControlDown()) {
                Alert d = new Alert(Alert.AlertType.CONFIRMATION);
                d.setTitle("Forget Files");
                d.setHeaderText("Remove these files from the database permanently?");
                d.setContentText("This action cannot be undone!");
                Optional result = d.showAndWait();
                if (result.get() == ButtonType.OK) {
                    grid.removeSelected();
                }
            } else {
                Alert d = new Alert(Alert.AlertType.CONFIRMATION);
                d.setTitle("Delete Files");
                d.setHeaderText("Delete these files permanently?");
                d.setContentText("This action cannot be undone!");
                Optional result = d.showAndWait();
                if (result.get() == ButtonType.OK) {
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
            if (!grid.getChildren().isEmpty()) {
                grid.select((GridImageView) grid.getChildren().get(0), event.isShiftDown(), event.isControlDown());
                if (grid.getLastSelected() != null) {
                    preview(grid.getLastSelected().getInfo());
                    ensureVisible(gridScrollPane, grid.getLastSelected());
                    grid.updateVisibleThumbnails();
                }
                event.consume();
            }
        } else if (event.getCode() == KeyCode.END) {
            if (!grid.getChildren().isEmpty()) {
                grid.select((GridImageView) grid.getChildren().get(grid.getChildren().size() - 1), event.isShiftDown(), event.isControlDown());
                if (grid.getLastSelected() != null) {
                    preview(grid.getLastSelected().getInfo());
                    ensureVisible(gridScrollPane, grid.getLastSelected());
                    grid.updateVisibleThumbnails();
                }
                event.consume();
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.PAGE_DOWN) {
            gridScrollPane.setVvalue(0);
            grid.unselectAll();
            grid.setPage(grid.getPage() + 1);
            pageNumTextfield.setText(grid.getPage() + "");
            event.consume();
        } else if (event.isControlDown() && event.getCode() == KeyCode.PAGE_UP) {
            if (grid.getPage() > 0) {
                gridScrollPane.setVvalue(0);
                grid.unselectAll();
                grid.setPage(grid.getPage() - 1);
                pageNumTextfield.setText(grid.getPage() + "");
                event.consume();
            }
        } else if (event.getCode() == KeyCode.PAGE_DOWN) {
            if (!grid.getChildren().isEmpty()) {
                int index = grid.getChildren().indexOf(grid.getLastSelected()) + 12;
                if (index > grid.getChildren().size()) index = grid.getChildren().size() - 1;
                if (grid.getLastSelected() != grid.getChildren().get(index)) {
                    grid.select((GridImageView) grid.getChildren().get(index), event.isShiftDown(), event.isControlDown());
                    if (grid.getLastSelected() != null) {
                        preview(grid.getLastSelected().getInfo());
                        ensureVisible(gridScrollPane, grid.getLastSelected());
                        grid.updateVisibleThumbnails();
                    }
                }
                event.consume();
            }
        } else if (event.getCode() == KeyCode.PAGE_UP) {
            if (!grid.getChildren().isEmpty()) {
                int index = grid.getChildren().indexOf(grid.getLastSelected()) - 12;
                if (index < 0) index = 0;
                if (grid.getLastSelected() != grid.getChildren().get(index)) {
                    grid.select((GridImageView) grid.getChildren().get(index), event.isShiftDown(), event.isControlDown());
                    if (grid.getLastSelected() != null) {
                        preview(grid.getLastSelected().getInfo());
                        ensureVisible(gridScrollPane, grid.getLastSelected());
                        grid.updateVisibleThumbnails();
                    }
                }
                event.consume();
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.E) {
            grid.openTagEditorDialog();
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
        int numImages = 0;
        try {
            numImages = db.getNumImages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (page < 0) {
            page = 0;
        } else if (page > numImages / grid.getPageLength()) {
            page = numImages / grid.getPageLength();
        }
        grid.setPage(page);
        pageNumTextfield.setText(Integer.toString(page));
        if (!grid.getChildren().isEmpty()) ensureVisible(gridScrollPane, grid.getChildren().get(0));
    }

    public void nextPageButtonOnAction(ActionEvent event) {
        int page = grid.getPage() + 1;
        int numImages = 0;
        try {
            numImages = db.getNumImages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (page < 0) {
            page = 0;
        } else if (page > numImages / grid.getPageLength()) {
            page = numImages / grid.getPageLength();
        }
        grid.setPage(page);
        pageNumTextfield.setText(Integer.toString(page));
        if (!grid.getChildren().isEmpty()) ensureVisible(gridScrollPane, grid.getChildren().get(0));
        grid.updateVisibleThumbnails();
    }

    public void prevPageButtonOnAction(ActionEvent event) {
        int page = grid.getPage() - 1;
        int numImages = 0;
        try {
            numImages = db.getNumImages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (page < 0) {
            page = 0;
        } else if (page > numImages / grid.getPageLength()) {
            page = numImages / grid.getPageLength();
        }
        grid.setPage(page);
        pageNumTextfield.setText(Integer.toString(page));
        if (!grid.getChildren().isEmpty()) ensureVisible(gridScrollPane, grid.getChildren().get(0));
        grid.updateVisibleThumbnails();
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
