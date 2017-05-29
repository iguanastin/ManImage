package manimage.common;


import com.sun.istack.internal.NotNull;
import javafx.scene.image.Image;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;

public class ImageInfo extends DatabaseInfo {

    private WeakReference<Image> image;
    private WeakReference<Image> thumbnail;
    private ImageHistogram histogram;
    private final ArrayList<Tag> tags = new ArrayList<>();

    private String path;
    private String source;
    private byte rating;
    private final long timeAdded;

    private boolean pathChanged = false, sourceChanged = false, ratingChanged = false;

    public static final int THUMBNAIL_SIZE = 140;

    //TODO: Extract superclass from this and ComicInfo


    public ImageInfo(int id, String path, String source, byte rating, long timeAdded) {
        super(id, true);
        this.path = path;
        this.source = source;
        this.rating = rating;
        this.timeAdded = timeAdded;
    }

    public ImageInfo(int id, String path) {
        super(id, false);
        this.path = path;
        this.source = null;
        this.rating = 0;
        this.timeAdded = System.currentTimeMillis();
        setToBeInserted();
    }

    //-------------- Checkers ------------------------------------------------------------------------------------------

    public synchronized boolean isModified() {
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

    @Override
    public synchronized boolean isSynchronized() {
        return super.isSynchronized() && !isModified();
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

    public synchronized long getTimeAdded() {
        return timeAdded;
    }

    public synchronized String getSQLSafePath() {
        if (path == null) {
            return null;
        } else {
            return '\'' + path.replace("'", "''") + '\'';
        }
    }

    public synchronized  String getPath() {
        return path;
    }

    public synchronized String getSQLSafeSource() {
        if (source == null) {
            return null;
        } else {
            return '\'' + source.replace("'", "''") + '\'';
        }
    }

    public synchronized String getSource() {
        return source;
    }

    @Override
    public synchronized String toString() {
        String result = "ImageInfo(" + getId() + ", " + new Date(getTimeAdded()) + "): ";
        if (isSynchronized()) result += "Synchronized";
        else result += "Not Synchronized";

        return result;
    }

    public synchronized ArrayList<Tag> getTags() {
        return tags;
    }

    @Override
    public int buildSQLUpdate(StringBuilder query) {
        if (isSynchronized()) return 0;

        if (isToBeDeleted()) {
            query.append("DELETE FROM ").append(ImageDatabase.SQL_IMAGES_TABLE).append(" WHERE ").append(ImageDatabase.SQL_IMAGE_ID).append('=').append(getId()).append(";\n");
        } else if (isToBeInserted()) {
            query.append("INSERT INTO ").append(ImageDatabase.SQL_IMAGES_TABLE).append(" (").append(ImageDatabase.SQL_IMAGE_ID).append(',').append(ImageDatabase.SQL_IMAGE_PATH).append(',');
            query.append(ImageDatabase.SQL_IMAGE_SOURCE).append(',').append(ImageDatabase.SQL_IMAGE_RATING).append(',').append(ImageDatabase.SQL_IMAGE_TIME_ADDED).append(") VALUES (");
            query.append(getId()).append(',').append(getSQLSafePath()).append(',').append(getSQLSafeSource()).append(',').append(getRating()).append(',').append(getTimeAdded()).append(");\n");
        } else if (isModified()) {
            query.append("UPDATE ").append(ImageDatabase.SQL_IMAGES_TABLE).append(" SET ");
            boolean comma = false;
            if (isPathChanged()) {
                query.append(ImageDatabase.SQL_IMAGE_PATH).append('=').append(getSQLSafePath());
                comma = true;
            }
            if (isSourceChanged()) {
                if (comma) query.append(',');
                query.append(ImageDatabase.SQL_IMAGE_SOURCE).append('=').append(getSQLSafeSource());
                comma = true;
            }
            if (isRatingChanged()) {
                if (comma) query.append(',');
                query.append(ImageDatabase.SQL_IMAGE_RATING).append('=').append(getRating());
            }
            query.append(" WHERE ").append(ImageDatabase.SQL_IMAGE_ID).append('=').append(getId()).append(";\n");
        }

        return 1;
    }

    //------------- Setters --------------------------------------------------------------------------------------------

    @Override
    public synchronized void markAsCommitted() {
        super.markAsCommitted();
        sourceChanged = ratingChanged = pathChanged = false;
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

    public synchronized boolean addTag(Tag tag) {
        if (tags.contains(tag)) return false;
        tags.add(tag);
        return true;
    }

}
