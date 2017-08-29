package manimage.main;


import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import manimage.common.ImageInfo;
import manimage.common.SimilarPair;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class DuplicateResolverController {

    public ImageView leftImageView;
    public ImageView rightImageView;
    public Label similarityLabel;
    public Label rightInfoLabel;
    public Label leftInfoLabel;
    public Label countLabel;
    public Label leftPathLabel;
    public Label rightPathLabel;
    public HBox imageHBox;
    public SplitPane root;

    private int index = -1;
    private final ArrayList<SimilarPair> dataset = new ArrayList<>();

    @FXML
    public void initialize() {
        ArrayList<SimilarPair> pairs = new ArrayList<>();
        pairs.add(new SimilarPair(new ImageInfo(1, 1, null, "D:\\(cjdrfr)\\cj\\New folder\\waif.png", null), new ImageInfo(2, 2, null, "D:\\(cjdrfr)\\cj\\New folder\\Loli\\2043185 - Diamond_Tiara Friendship_is_Magic Maniacpaint My_Little_Pony Silver_Spoon.jpg", null), 81.3243242));
        setDataset(pairs);

        Platform.runLater(() -> {
            leftImageView.getScene().getWindow().widthProperty().addListener((observable, oldValue, newValue) -> {
                final int width = newValue.intValue();
                updateImageWidth(width);
                updatePathLabelWidth(width);
            });
            imageHBox.heightProperty().addListener((observable, oldValue, newValue) -> updateImageHeight(newValue.intValue()));
            updateImageWidth((int) imageHBox.getWidth());
            updateImageHeight((int) imageHBox.getHeight());
            updatePathLabelWidth((int) imageHBox.getWidth());
        });
    }

    private void setDataset(Iterable<SimilarPair> set) {
        dataset.clear();
        set.forEach(dataset::add);
        index = 0;
        if (!dataset.isEmpty()) display(index);
        else clearDisplay();
    }

    private void clearDisplay() {
        similarityLabel.setText("0%");
        leftImageView.setImage(null);
        rightImageView.setImage(null);
        leftPathLabel.setText("Path...");
        rightPathLabel.setText("Path...");
        leftInfoLabel.setText("...");
        rightInfoLabel.setText("...");
    }

    private void display(int i) {
        SimilarPair pair = dataset.get(i);
        similarityLabel.setText(new DecimalFormat("#.##").format(pair.getSimilarity()) + "%");
        leftImageView.setImage(pair.getImage1().getImage(false));
        rightImageView.setImage(pair.getImage2().getImage(false));
        leftPathLabel.setText(pair.getImage1().getPath().getAbsolutePath());
        rightPathLabel.setText(pair.getImage2().getPath().getAbsolutePath());

        double size = pair.getImage1().getPath().length()/1024.0;
        String sizeString = new DecimalFormat("#.##").format(size) + "KB";
        if (size > 1024) sizeString = new DecimalFormat("#.##").format(size/1024.0) + "MB";
        leftInfoLabel.setText(sizeString + " (" + (int) pair.getImage1().getImage(false).getWidth() + "x" + (int) pair.getImage1().getImage(false).getHeight() + ")");
        size = pair.getImage2().getPath().length()/1024.0;
        sizeString = new DecimalFormat("#.##").format(size) + "KB";
        if (size > 1024) sizeString = new DecimalFormat("#.##").format(size/1024.0) + "MB";
        rightInfoLabel.setText(sizeString + " (" + (int) pair.getImage2().getImage(false).getWidth() + "x" + (int) pair.getImage2().getImage(false).getHeight() + ")");
    }

    private void updatePathLabelWidth(int width) {
        leftPathLabel.setPrefWidth((width - similarityLabel.getWidth())/2 - 50);
        rightPathLabel.setPrefWidth((width - similarityLabel.getWidth())/2 - 50);
    }

    private void updateImageWidth(int width) {
        leftImageView.setFitWidth(width / 2);
        rightImageView.setFitWidth(width / 2);
    }

    private void updateImageHeight(int height) {
        if (leftImageView.getImage() != null && rightImageView.getImage() != null) {
            if (leftImageView.getImage().getHeight() > height) leftImageView.setFitHeight(height);
            else leftImageView.setFitHeight(leftImageView.getImage().getHeight());
            if (rightImageView.getImage().getHeight() > height) rightImageView.setFitHeight(height);
            else rightImageView.setFitHeight(rightImageView.getImage().getHeight());
        }
    }

    public void onDeleteRightAction(ActionEvent event) {
        //TODO: Implement
    }

    public void onDeleteLeftAction(ActionEvent event) {
        //TODO: Implement
    }

    public void onPreviousAction(ActionEvent event) {
        if (index > 0) display(index--);
    }

    public void onNextAction(ActionEvent event) {
        if (index < dataset.size() - 1) display(index++);
    }

}
