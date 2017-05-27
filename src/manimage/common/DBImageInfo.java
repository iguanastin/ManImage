package manimage.common;


import javafx.scene.image.Image;
import manimage.main.ImageDatabase;
import manimage.main.Main;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBImageInfo {

    private Image image;
    private Image thumbnail;
    private ImageHistogram histogram;

    private String path;
    private String source;
    private final int id;
    private byte rating;
    private long timeAdded;

    boolean pathChanged = false, sourceChanged = false, ratingChanged = false;


    public DBImageInfo(int id, String path, String source, byte rating, long timeAdded) {
        this.id = id;
        this.path = path;
        this.source = source;
        this.rating = rating;
        this.timeAdded = timeAdded;
    }

    //------------------- Operators ------------------------------------------------------------------------------------

    public synchronized boolean unloadImage() {
        if (image == null) return false;

        image = null;
        return true;
    }

    public synchronized boolean unloadThumbnail() {
        if (thumbnail == null) return false;

        thumbnail = null;
        return true;
    }

    public synchronized boolean unloadAll() {
        return unloadImage() || unloadThumbnail();
    }

    //-------------- Checkers ------------------------------------------------------------------------------------------

    public synchronized boolean isImageLoaded() {
        return image != null;
    }

    public synchronized boolean isThumbnailLoaded() {
        return thumbnail != null;
    }

    public synchronized boolean isChanged() {
        return pathChanged || sourceChanged || ratingChanged;
    }

    public synchronized boolean isPathChanged() {
        return pathChanged;
    }

    public synchronized boolean isRatingChanged() {
        return ratingChanged;
    }

    public synchronized boolean isSourceChanged() {
        return sourceChanged;
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    public synchronized Image getThumbnail(boolean backgroundLoading, int thumbnailSize) {
        if (thumbnail == null)
            thumbnail = new Image("file:" + path, thumbnailSize, thumbnailSize, true, true, backgroundLoading);

        return thumbnail;
    }

    public synchronized Image getImage(boolean backgroundLoading) {
        if (image == null) {
            image = new Image("file:" + path, backgroundLoading);
        }

        return image;
    }

    public synchronized ImageHistogram getHistogram() {
        try {
            if (histogram == null) histogram = ImageHistogram.getHistogram(getImage(false));
        } catch (HistogramReadException ex){
        }

        return histogram;
    }

    public synchronized byte getRating() {
        return rating;
    }

    public synchronized int getId() {
        return id;
    }

    public synchronized long getTimeAdded() {
        return timeAdded;
    }

    public synchronized String getPath() {
        return path;
    }

    public synchronized String getSource() {
        return source;
    }

    //------------- Setters --------------------------------------------------------------------------------------------

    public synchronized void markChangeApplied() {
        pathChanged = sourceChanged = ratingChanged = false;
    }

    public synchronized void setSource(String source) {
        if (!this.source.equals(source)) sourceChanged = true;
        this.source = source;
    }

    public synchronized void setRating(byte rating) {
        if (this.rating != rating) ratingChanged = true;
        this.rating = rating;
    }

    public synchronized void setPath(String path) {
        if (!this.path.equals(path)) pathChanged = true;
        this.path = path;
    }

}
