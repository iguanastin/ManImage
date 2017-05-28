package manimage.common;


import com.sun.istack.internal.NotNull;
import javafx.scene.image.Image;

import java.lang.ref.WeakReference;

public class ImageInfo {

    private WeakReference<Image> image;
    private WeakReference<Image> thumbnail;
    private ImageHistogram histogram;

    private String path;
    private String source;
    private byte rating;
    private final int id;
    private final long timeAdded;

    private boolean pathChanged = false, sourceChanged = false, ratingChanged = false;
    private boolean inserted = false;
    private boolean toBeInserted = false;
    private boolean toBeDeleted = false;

    public static final int THUMBNAIL_SIZE = 140;


    public ImageInfo(int id, String path, String source, byte rating, long timeAdded) {
        this.id = id;
        this.path = path;
        this.source = source;
        this.rating = rating;
        this.timeAdded = timeAdded;
        inserted = true;
        toBeDeleted = false;
        toBeInserted = false;
    }

    public ImageInfo(int id, String path) {
        this.id = id;
        this.path = path;
        this.source = null;
        this.rating = 0;
        this.timeAdded = System.currentTimeMillis();
        inserted = false;
        toBeDeleted = false;
        toBeInserted = true;
    }

    //-------------- Checkers ------------------------------------------------------------------------------------------

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

    public boolean isInserted() {
        return inserted;
    }

    public boolean isToBeDeleted() {
        return toBeDeleted;
    }

    public boolean isToBeInserted() {
        return toBeInserted;
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    public synchronized Image getThumbnail(boolean backgroundLoading) {
        if (thumbnail == null || thumbnail.get() == null)
            thumbnail = new WeakReference<>(new Image("file:" + path, THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, backgroundLoading));

        return thumbnail.get();
    }

    public synchronized Image getImage(boolean backgroundLoading) {
        if (image == null || image.get() == null) {
            image = new WeakReference<>(new Image("file:" + path, backgroundLoading));
        }

        return image.get();
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

    public synchronized void setAsUpdated() {
        pathChanged = sourceChanged = ratingChanged = toBeDeleted = toBeInserted = false;
    }

    public synchronized void setSource(String source) {
        if (this.source == null && source != null) sourceChanged = true;
        else if (this.source != null && !this.source.equals(source)) sourceChanged = true;

        this.source = source;
    }

    public synchronized void setRating(byte rating) {
        if (this.rating != rating) ratingChanged = true;
        this.rating = rating;
    }

    @NotNull
    public synchronized void setPath(String path) {
        if (!this.path.equals(path)) pathChanged = true;
        this.path = path;
    }

    public void setInserted(boolean b) {
        inserted = b;
    }

    public void setToBeDeleted(boolean toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

    public void setToBeInserted(boolean toBeInserted) {
        this.toBeInserted = toBeInserted;
    }

}
