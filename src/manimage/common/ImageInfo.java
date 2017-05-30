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
    private ArrayList<TagInfo> tags;
    private ArrayList<TagInfo> tagsToRemove;
    private ArrayList<TagInfo> tagsToAdd;

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

    public synchronized boolean tagsChanged() {
        return (tagsToAdd != null && !tagsToAdd.isEmpty()) || (tagsToRemove != null && !tagsToRemove.isEmpty());
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

    public synchronized String getPath() {
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

    public synchronized ArrayList<TagInfo> getTags() {
        return tags;
    }

    @Override
    public int buildSQLUpdates(StringBuilder query) {
        if (isSynchronized()) return 0;

        int updates = 0;
        if (isToBeDeleted()) {
            updates += buildDeleteSQLUpdates(query);
        } else if (isToBeInserted()) {
            updates += buildInsertSQLUpdates(query);
        } else {
            if (isModified()) {
                updates += buildChangeSQLUpdates(query);
            }
            if (tagsChanged()) {
                updates += buildTagsSQLUpdates(query);
            }
        }

        return updates;
    }

    private int buildDeleteSQLUpdates(StringBuilder query) {
        query.append("DELETE FROM ").append(ImageDatabase.SQL_IMAGES_TABLE).append(" WHERE ").append(ImageDatabase.SQL_IMAGE_ID).append('=').append(getId()).append(";\n");

        return 1;
    }

    private int buildInsertSQLUpdates(StringBuilder query) {
        query.append("INSERT INTO ").append(ImageDatabase.SQL_IMAGES_TABLE).append(" (").append(ImageDatabase.SQL_IMAGE_ID).append(',').append(ImageDatabase.SQL_IMAGE_PATH).append(',');
        query.append(ImageDatabase.SQL_IMAGE_SOURCE).append(',').append(ImageDatabase.SQL_IMAGE_RATING).append(',').append(ImageDatabase.SQL_IMAGE_TIME_ADDED).append(") VALUES (");
        query.append(getId()).append(',').append(getSQLSafePath()).append(',').append(getSQLSafeSource()).append(',').append(getRating()).append(',').append(getTimeAdded()).append(");\n");

        return 1;
    }

    private int buildChangeSQLUpdates(StringBuilder query) {
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

        return 1;
    }

    private int buildTagsSQLUpdates(StringBuilder query) {
        int updates = 0;
        if (tagsToRemove != null) {
            for (TagInfo tag : tagsToRemove) {
                query.append("DELETE FROM ").append(ImageDatabase.SQL_IMAGE_TAGGED_TABLE).append(" WHERE ").append(ImageDatabase.SQL_TAG_ID).append('=').append(tag.getId()).append(" AND ");
                query.append(ImageDatabase.SQL_IMAGE_ID).append('=').append(getId()).append(";\n");
                updates++;
            }
        }

        if (tagsToAdd != null) {
            for (TagInfo tag : tagsToAdd) {
                query.append("INSERT INTO ").append(ImageDatabase.SQL_IMAGE_TAGGED_TABLE).append(" (").append(ImageDatabase.SQL_IMAGE_ID).append(',').append(ImageDatabase.SQL_TAG_ID);
                query.append(") VALUES (").append(getId()).append(',').append(tag.getId()).append(");\n");
                updates++;
            }
        }

        return updates;
    }

    //------------- Setters --------------------------------------------------------------------------------------------

    @Override
    public synchronized void markAsCommitted() {
        if (tags != null) {
            if (tagsToRemove != null) {
                tags.removeAll(tagsToRemove);
                tagsToRemove.clear();
            }
            if (tagsToAdd != null) {
                tags.addAll(tagsToAdd);
                tagsToAdd.clear();
            }
            if (isToBeDeleted()) {
                tags.clear();
            }
        }

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

    public synchronized boolean addTag(TagInfo tag) {
        if (tag == null) return false;

        if (tags == null) tags = new ArrayList<>();
        if (tagsToAdd == null) tagsToAdd = new ArrayList<>();

        if (tagsToAdd.contains(tag) || tags.contains(tag)) return false;
        tagsToAdd.add(tag);
        return true;
    }

    public synchronized boolean removeTag(TagInfo tag) {
        if (tag == null) return false;

        if (tags == null) tags = new ArrayList<>();
        if (tagsToRemove == null) tagsToRemove = new ArrayList<>();

        if (tagsToRemove.contains(tag) || !tags.contains(tag)) return false;
        tagsToRemove.add(tag);
        return true;
    }

}
