package manimage.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageInfoTest {

    @Test
    void isChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isChanged());
        image.setPath("newpath");
        Assertions.assertTrue(image.isChanged());
        image.setAsUpdated();
        Assertions.assertFalse(image.isChanged());
        image.setSource("newsource");
        Assertions.assertTrue(image.isChanged());
        image.setAsUpdated();
        Assertions.assertFalse(image.isChanged());
        image.setRating((byte) 2);
        Assertions.assertTrue(image.isChanged());
        image.setAsUpdated();
        Assertions.assertFalse(image.isChanged());

    }

    @Test
    void isPathChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isPathChanged());
        image.setPath("newpath");
        Assertions.assertTrue(image.isPathChanged());
        image.setAsUpdated();

        image.setPath("newpath");
        Assertions.assertFalse(image.isPathChanged());
    }

    @Test
    void isRatingChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isRatingChanged());
        image.setRating((byte) 2);
        Assertions.assertTrue(image.isRatingChanged());
        image.setAsUpdated();

        image.setRating((byte) 2);
        Assertions.assertFalse(image.isRatingChanged());
    }

    @Test
    void isSourceChanged() {
        ImageInfo image = new ImageInfo(1, "path");
        Assertions.assertFalse(image.isSourceChanged());
        image.setSource("newsource");
        Assertions.assertTrue(image.isSourceChanged());
        image.setAsUpdated();

        image.setSource("newsource");
        Assertions.assertFalse(image.isSourceChanged());
    }

    @Test
    void setAsUpdated() {
        ImageInfo image = new ImageInfo(1, "path");
        image.setPath("newpath");
        image.setSource("newsource");
        image.setRating((byte) 2);
        image.setAsUpdated();
        Assertions.assertFalse(image.isChanged());
    }

}