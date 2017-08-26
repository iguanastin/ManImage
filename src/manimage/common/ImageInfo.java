package manimage.common;


import javafx.scene.image.Image;

import java.io.File;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;

public class ImageInfo {

    private int id;
    private long added;
    private URL src;
    private File path;
    private String[] tags;

    private SoftReference<Image> image;
    private SoftReference<Image> thumbnail;
    private ImageHistogram histogram;

    public static final int THUMBNAIL_SIZE = 150;

    public ImageInfo(int id, long added, String src, String path, String[] tags) {
        this.id = id;
        this.added = added;
        if (path != null) this.path = new File(path);
        if (src != null) try { this.src = new URL(src); } catch (MalformedURLException ex) { ex.printStackTrace(); }
        this.tags = tags;
    }

    public void update(String src, String path, String[] tags) {
        if (path == null) {
            this.path = null;
        } else {
            this.path = new File(path);
        }
        if (src == null) {
            this.src = null;
        } else {
            try {
                this.src = new URL(src);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        this.tags = tags;
    }

    public ImageHistogram getHistogram() {
        if (histogram != null) return histogram;

        try {
            histogram = new ImageHistogram(getImage());
        } catch (HistogramReadException e) {
            e.printStackTrace();
        }
        return histogram;
    }

    public Image getThumbnail() {
        if (thumbnail == null || thumbnail.get() == null) {
            Image img = new Image("file:" + path.getAbsolutePath(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
            thumbnail = new SoftReference<>(img);
            return img;
        }

        return thumbnail.get();
    }

    public boolean cancelLoadingThumbnail() {
        if (thumbnail == null || thumbnail.get() == null || thumbnail.get().getProgress() == 1) {
            return false;
        }

        thumbnail.get().cancel();

        return true;
    }

    public Image getImage() {
        if (image == null || image.get() == null) {
            Image img = new Image("file:" + path.getAbsolutePath(), true);
            image = new SoftReference<>(img);
            return img;
        }

        return image.get();
    }

    public File getPath() {
        return path;
    }

    public String[] getTags() {
        return tags;
    }

    public void addTag(String tag) {
        tags = Arrays.copyOf(tags, tags.length + 1);
        tags[tags.length - 1] = tag;
    }

    public void removeTag(String tag) {
        String[] work = new String[tags.length - 1];
        int i = 0;
        for (String t : tags) {
            if (!t.equalsIgnoreCase(tag)) {
                work[i] = t;
                i++;
            }
        }
        tags = work;
    }

    public int getId() {
        return id;
    }

    public URL getSrc() {
        return src;
    }

    @Override
    public String toString() {
        return new Date(added) + " - " + id + ": " + path;
    }

}
