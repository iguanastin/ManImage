package manimage.common;


import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import manimage.main.DatabaseImageGridPane;
import manimage.main.DynamicImageView;
import manimage.main.GridImageView;
import manimage.main.Main;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import java.awt.image.BufferedImage;
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

    private static final String[] VLC_ARGS = {"--intf", "dummy", "--vout", "dummy", "--no-audio", "--no-osd", "--no-spu", "--no-stats", "--no-sub-autodetect-file", "--no-disable-screensaver", "--no-snapshot-preview"};
//    private static final MediaPlayer thumbnailMediaPlayer = new MediaPlayerFactory(VLC_ARGS).newHeadlessMediaPlayer();

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
            histogram = new ImageHistogram(getImage(false));
        } catch (HistogramReadException e) {
            e.printStackTrace();
        }
        return histogram;
    }

    public Image getThumbnail() {
        if (thumbnail == null || thumbnail.get() == null) {
            if (Main.IMAGE_FILTER.accept(path)) {
                Image img = new Image("file:" + path.getAbsolutePath(), THUMBNAIL_SIZE, THUMBNAIL_SIZE, true, true, true);
                thumbnail = new SoftReference<>(img);
                return img;
            } else if (Main.VIDEO_FILTER.accept(path)) {
//                thumbnailMediaPlayer.prepareMedia("file:" + path.getAbsolutePath());
//                thumbnailMediaPlayer.setPosition(0.3f);
//                BufferedImage bi = thumbnailMediaPlayer.getSnapshot(THUMBNAIL_SIZE, 0);
//                Image img = SwingFXUtils.toFXImage(bi, null);
//                thumbnail = new SoftReference<>(img);
//                return img;
                return null;
            }
        }

        return thumbnail.get();
    }

    public void cancelLoadingThumbnail() {
        if (thumbnail == null || thumbnail.get() == null || thumbnail.get().getProgress() == 1) return;

        thumbnail.get().cancel();
    }

    public Image getImage(boolean backgroundLoading) {
        if (image == null || image.get() == null) {
            Image img = new Image("file:" + path.getAbsolutePath(), backgroundLoading);
            image = new SoftReference<>(img);
            img.exceptionProperty().addListener((observable, oldValue, newValue) -> {
                newValue.printStackTrace();
                //TODO: Improve this error reporting
            });
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
