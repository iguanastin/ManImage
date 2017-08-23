package manimage.main;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import manimage.common.DBInterface;
import manimage.common.ImgInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
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
    public CheckBox ratingNoneCheckbox;
    public CheckBox ratingOneCheckbox;
    public CheckBox ratingTwoCheckbox;
    public CheckBox ratingThreeCheckbox;
    public CheckBox ratingFourCheckbox;
    public CheckBox ratingFiveCheckbox;
    public VBox searchVBox;
    public Button prevPageButton;
    public Button nextPageButton;
    public TextField pageNumTextfield;

    private DBInterface db;
    private Stage stage;

    private File lastFolder;
    private String[] orderByMap;

    private String dbPath = System.getProperty("user.home") + "\\manimage";
    private String dbUser = "sa";
    private String dbPass = "";


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
    }

    //------------------ Operators -------------------------------------------------------------------------------------

    private void preview(ImgInfo info) {
        if (info != null) {
            previewDynamicImageView.setImage(info.getImage());
            previewTagsLabel.setText(Arrays.toString(info.getTags()));
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
        //Set rating
        ArrayList<Integer> ratings = new ArrayList<>(6);
        if (ratingNoneCheckbox.isSelected()) ratings.add(0);
        if (ratingOneCheckbox.isSelected()) ratings.add(1);
        if (ratingTwoCheckbox.isSelected()) ratings.add(2);
        if (ratingThreeCheckbox.isSelected()) ratings.add(3);
        if (ratingFourCheckbox.isSelected()) ratings.add(4);
        if (ratingFiveCheckbox.isSelected()) ratings.add(5);
        int[] ratingArr = new int[ratings.size()];
        for (int i = 0; i < ratingArr.length; i++) {
            ratingArr[i] = ratings.get(i);
        }
        grid.setSearchRatings(ratingArr);
        //Set page
        grid.setPage(0);
        pageNumTextfield.setText("0");

        grid.updateSearchContents();
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

        if (files != null) {
            DBInterface db = grid.getDatabase();

            try {
                db.addBatchImages(files);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            lastFolder = files.get(0).getParentFile();
        }
    }

    public void addFolderClicked(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder");
        dc.setInitialDirectory(lastFolder);
        File folder = dc.showDialog(stage);

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

    public void addRecurseFolderClicked(ActionEvent event) {
        final DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Add folder and all subfolders");
        dc.setInitialDirectory(lastFolder);
        final File folder = dc.showDialog(stage);

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

    public void exitClicked(ActionEvent event) {
        Platform.exit();
    }

    public void aboutMenuActivated(ActionEvent event) {

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
            Alert d = new Alert(Alert.AlertType.CONFIRMATION);
            d.setTitle("Forget Files");
            d.setHeaderText("Remove these files from the database permanently?");
            d.setContentText("This action cannot be undone!");
            Optional result = d.showAndWait();
            if (result.get() == ButtonType.OK) {
                grid.removeSelected();
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.A) {
            if (grid.areAllSelected()) {
                grid.unselectAll();
            } else {
                grid.selectAll();
            }
            event.consume();
        } else if (event.getCode() == KeyCode.PAGE_DOWN) {
            gridScrollPane.setVvalue(0);
            grid.unselectAll();
            grid.setPage(grid.getPage() + 1);
            pageNumTextfield.setText(grid.getPage() + "");
        } else if (event.getCode() == KeyCode.PAGE_UP) {
            if (grid.getPage() > 0) {
                gridScrollPane.setVvalue(0);
                grid.unselectAll();
                grid.setPage(grid.getPage() - 1);
                pageNumTextfield.setText(grid.getPage() + "");
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.E) {
            grid.openTagEditorDialog();
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
        } else if (page > numImages/grid.getPageLength()) {
            page = numImages/grid.getPageLength();
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
        } else if (page > numImages/grid.getPageLength()) {
            page = numImages/grid.getPageLength();
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
        } else if (page > numImages/grid.getPageLength()) {
            page = numImages/grid.getPageLength();
        }
        grid.setPage(page);
        pageNumTextfield.setText(Integer.toString(page));
        if (!grid.getChildren().isEmpty()) ensureVisible(gridScrollPane, grid.getChildren().get(0));
        grid.updateVisibleThumbnails();
    }

}
