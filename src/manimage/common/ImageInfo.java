package manimage.common;


import javafx.scene.image.Image;
import manimage.main.Main;

import java.io.File;
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
    private ImageHistogram histogram;

    //TODO: Modify this class to use information from the database and make updates to the database
    //TODO: Add imageID field corresponding to database image_id

    public static final int thumbnailSize = 140;


    //------------- Constructors ---------------------------------------------------------------------------------------

    ImageInfo(File file) {
        setFile(file);

        addTag("tagme");
    }

    //----------- Functionality ----------------------------------------------------------------------------------------

    public boolean unloadImage() {
        if (image == null) return false;

        image = null;
        return true;
    }

    public boolean unloadThumbnail() {
        if (thumbnail == null) return false;

        thumbnail = null;
        return true;
    }

    public boolean unloadAll() {
        return unloadImage() || unloadThumbnail();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ImageInfo && ((ImageInfo) obj).getFile().equals(getFile());

        //TODO: Modify to compare imageID fields
    }

    //-------------- Checkers ------------------------------------------------------------------------------------------

    public boolean isImageLoaded() {
        return image != null;
    }

    public boolean isThumbnailLoaded() {
        return thumbnail != null;
    }

    public boolean isSimilar(ImageInfo other, double alpha) {
        return getHistogram().isSimilar(other.getHistogram(), alpha);
    }

    public boolean hasTag(String tag) {
        for (String str : tags) {
            if (str.equalsIgnoreCase(tag)) {
                return true;
            }
        }

        return false;
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    public Image getThumbnail(boolean backgroundLoading) {
        if (thumbnail == null)
            thumbnail = new Image(file.toURI().toASCIIString(), thumbnailSize, thumbnailSize, true, true, backgroundLoading);

        return thumbnail;
    }

    public Image getImage(boolean backgroundLoading) {
        if (image == null) {
            image = new Image(file.toURI().toASCIIString(), backgroundLoading);
            width = (int) image.getWidth();
            height = (int) image.getHeight();
        }

        return image;
    }

    public ImageHistogram getHistogram() {
        try {
            if (histogram == null) histogram = ImageHistogram.getHistogram(getImage(false));
        } catch (HistogramReadException ex){
            ex.printStackTrace();
        }

        return histogram;
    }

    public int getRating() {
        return rating;
    }

    public File getFile() {
        return file;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public URL getSource() {
        return source;
    }

    public List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    //------------------- Setters --------------------------------------------------------------------------------------

    public void setFile(File file) {
        if (!file.exists()) throw new IllegalArgumentException("File must exist");
        if (!Main.IMAGE_FILTER.accept(file))
            throw new IllegalArgumentException("File must be of image type: *.png, *.jpg, *.jpeg, *.gif");

        this.file = file;
    }

    public void setRating(int rating) {
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("Rating must be in rage [0, 5]");

        this.rating = rating;
    }

    public void setSource(URL source) {
        this.source = source;
    }

    public boolean addTag(String tag) {
        if (hasTag(tag)) return false;
        return tags.add(tag);
    }

    public boolean removeTag(String tag) {
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
