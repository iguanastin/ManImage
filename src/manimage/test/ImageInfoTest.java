package manimage.test;

import manimage.common.ImageInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ImageInfoTest {

    @Test
    void isChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isModified());

        image.setPath("newpath");
        Assertions.assertTrue(image.isModified());
        image.markAsCommitted();
        Assertions.assertFalse(image.isModified());

        image.setSource("newsource");
        Assertions.assertTrue(image.isModified());
        image.markAsCommitted();
        Assertions.assertFalse(image.isModified());

        image.setRating((byte) 2);
        Assertions.assertTrue(image.isModified());
        image.markAsCommitted();
        Assertions.assertFalse(image.isModified());

    }

    @Test
    void isPathChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isPathChanged());
        image.setPath("newpath");
        Assertions.assertTrue(image.isPathChanged());
        image.markAsCommitted();

        image.setPath("newpath");
        Assertions.assertFalse(image.isPathChanged());
    }

    @Test
    void isPathChangedSame() {
        ImageInfo image = new ImageInfo(1, "path");
        image.setPath("path");
        Assertions.assertFalse(image.isPathChanged());
    }

    @Test
    void isRatingChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isRatingChanged());
        image.setRating((byte) 2);
        Assertions.assertTrue(image.isRatingChanged());
    }

    @Test
    void isRatingChangedSame() {
        ImageInfo image = new ImageInfo(1, "path");
        image.setRating((byte) 2);
        image.markAsCommitted();
        image.setRating((byte) 2);
        Assertions.assertFalse(image.isRatingChanged());
    }

    @Test
    void isSourceChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isSourceChanged());
        image.setSource("newsource");
        Assertions.assertTrue(image.isSourceChanged());
    }

    @Test
    void isSourceChangedNull() {
        ImageInfo image = new ImageInfo(1, "path");
        image.setSource("newsource");
        image.markAsCommitted();
        image.setSource(null);
        Assertions.assertTrue(image.isSourceChanged());
    }

    @Test
    void isSourceChangedSame() {
        ImageInfo image = new ImageInfo(1, "path");
        image.setSource("source");
        image.markAsCommitted();
        Assertions.assertFalse(image.isSourceChanged());
        image.setSource("source");
        Assertions.assertFalse(image.isSourceChanged());
    }

    @Test
    void setAsUpdated() {
        ImageInfo image = new ImageInfo(1, "path");
        image.setToBeInserted();
        image.setToBeDeleted();
        image.setPath("newpath");
        image.setSource("newsource");
        image.setRating((byte) 2);
        Assertions.assertTrue(image.isModified());
        Assertions.assertTrue(image.isToBeInserted());
        Assertions.assertTrue(image.isToBeDeleted());
        image.markAsCommitted();
        Assertions.assertFalse(image.isModified());
        Assertions.assertFalse(image.isToBeInserted());
        Assertions.assertFalse(image.isToBeDeleted());
    }

}