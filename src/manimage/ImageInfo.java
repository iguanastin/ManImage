package manimage;


import javafx.scene.image.Image;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ImageInfo {

    private Image image;
    private Image thumbnail;
    private File file;
    private URL source;
    private final ArrayList<String> tags = new ArrayList<>();
    private int width = -1;
    private int height = -1;
    private int rating = -1;

    public static final int thumbnailSize = 140;


    //------------- Constructors ---------------------------------------------------------------------------------------

    public ImageInfo(File file) {
        setFile(file);

        addTag("tagme");
    }

    //----------- Functionality ----------------------------------------------------------------------------------------

    boolean unloadImage() {
        if (image == null) return false;

        image = null;
        return true;
    }

    boolean unloadThumbnail() {
        if (thumbnail == null) return false;

        thumbnail = null;
        return true;
    }

    boolean unloadAll() {
        return unloadImage() || unloadThumbnail();
    }

    //-------------- Checkers ------------------------------------------------------------------------------------------

    boolean isImageLoaded() {
        return image != null;
    }

    boolean isThumbnailLoaded() {
        return thumbnail != null;
    }

    boolean hasTag(String tag) {
        for (String str : tags) {
            if (str.equalsIgnoreCase(tag)) {
                return true;
            }
        }

        return false;
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    Image getThumbnail(boolean backgroundLoading) {
        if (thumbnail == null) thumbnail = new Image(file.toURI().toASCIIString(), thumbnailSize, thumbnailSize, true, true, backgroundLoading);

        return thumbnail;
    }

    Image getImage(boolean backgroundLoading) {
        if (image == null) {
            image = new Image(file.toURI().toASCIIString(), backgroundLoading);
            width = (int) image.getWidth();
            height = (int) image.getHeight();
        }

        return image;
    }

    int getRating() {
        return rating;
    }

    File getFile() {
        return file;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    URL getSource() {
        return source;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    //------------------- Setters --------------------------------------------------------------------------------------

    void setFile(File file) {
        if (!file.exists()) throw new IllegalArgumentException("File must exist");
        if (!Main.IMAGE_FILTER.accept(file)) throw new IllegalArgumentException("File must be of image type: *.png, *.jpg, *.jpeg, *.gif");

        this.file = file;
    }

    void setRating(int rating) {
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("Rating must be in rage [0, 5]");

        this.rating = rating;
    }

    void setSource(URL source) {
        this.source = source;
    }

    boolean addTag(String tag) {
        if (hasTag(tag)) return false;
        return tags.add(tag);
    }

    boolean removeTag(String tag) {
        if (hasTag(tag)) {
            Iterator<String> tagIterator = tags.iterator();
            while (tagIterator.hasNext()) {
                if (tagIterator.next().equalsIgnoreCase(tag)) {
                    tagIterator.remove();
                    return true;
                }
            }
        }

        return false;
    }

}
