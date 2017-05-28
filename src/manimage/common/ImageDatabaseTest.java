package manimage.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;

class ImageDatabaseTest {

    private ImageDatabase db;

    @BeforeEach
    void setUp() {
        try {
            db = new ImageDatabase("C:\\temp\\h2db_test", "sa", "", true);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @AfterEach
    void tearDown() {
        try {
            db.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        File dbFile = new File("C:\\temp\\h2db_test.mv.db");
        if (dbFile.exists()) dbFile.delete();
    }

    @Test
    void getImages() {
        try {
            DBImageInfo i1 = db.createImage("image1");
            DBImageInfo i2 = db.createImage("image2");
            DBImageInfo i3 = db.createImage("image3");
            DBImageInfo i4 = db.createImage("image4");
            DBImageInfo i5 = db.createImage("image5");
            ArrayList<DBImageInfo> infos = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " ORDER BY " + ImageDatabase.SQL_IMAGE_ID + " DESC OFFSET 0 LIMIT 5");
            Assertions.assertEquals(5, infos.size());
            Assertions.assertEquals(i5, infos.get(0));
            Assertions.assertEquals(i4, infos.get(1));
            Assertions.assertEquals(i3, infos.get(2));
            Assertions.assertEquals(i2, infos.get(3));
            Assertions.assertEquals(i1, infos.get(4));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void getImagesUncached() {
        try {
            DBImageInfo c = db.createImage("image");
            db.clearCachedImages();
            ArrayList<DBImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE);
            Assertions.assertEquals(c.getId(), images.get(0).getId());
            Assertions.assertEquals(c.getPath(), images.get(0).getPath());
            Assertions.assertEquals(c.getSource(), images.get(0).getSource());
            Assertions.assertEquals(c.getTimeAdded(), images.get(0).getTimeAdded());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void getComics() {
        try {
            DBComicInfo c1 = db.createComic("comic1");
            DBComicInfo c2 = db.createComic("comic2");
            DBComicInfo c3 = db.createComic("comic3");
            DBComicInfo c4 = db.createComic("comic4");
            DBComicInfo c5 = db.createComic("comic5");
            ArrayList<DBComicInfo> infos = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE + " ORDER BY " + ImageDatabase.SQL_COMIC_ID + " DESC OFFSET 0 LIMIT 5");
            Assertions.assertEquals(5, infos.size());
            Assertions.assertEquals(c5, infos.get(0));
            Assertions.assertEquals(c4, infos.get(1));
            Assertions.assertEquals(c3, infos.get(2));
            Assertions.assertEquals(c2, infos.get(3));
            Assertions.assertEquals(c1, infos.get(4));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void getComicsUncached() {
        try {
            DBComicInfo c = db.createComic("comic1");
            db.clearCachedComics();
            ArrayList<DBComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE);
            Assertions.assertEquals(c.getId(), comics.get(0).getId());
            Assertions.assertEquals(c.getName(), comics.get(0).getName());
            Assertions.assertEquals(c.getSource(), comics.get(0).getSource());
            Assertions.assertEquals(c.getTimeAdded(), comics.get(0).getTimeAdded());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void createImage() {
        try {
            DBImageInfo info = db.createImage("image");
            ArrayList<DBImageInfo> infos = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " WHERE " + ImageDatabase.SQL_IMAGE_ID + "=" + info.getId());
            Assertions.assertEquals(info, infos.get(0));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void createComic() {
        try {
            DBComicInfo info = db.createComic("comic");
            ArrayList<DBComicInfo> infos = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE + " WHERE " + ImageDatabase.SQL_COMIC_ID + "=" + info.getId());
            Assertions.assertEquals(info, infos.get(0));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void deleteImages() {
        try {
            DBImageInfo i1 = db.createImage("image1");
            DBImageInfo i2 = db.createImage("image2");
            DBImageInfo i3 = db.createImage("image3");
            DBImageInfo i4 = db.createImage("image4");
            DBImageInfo i5 = db.createImage("image5");
            Assertions.assertEquals(3, db.deleteImages(i1, i2, i3));
            ArrayList<DBImageInfo> infos = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " ORDER BY " + ImageDatabase.SQL_IMAGE_ID);
            Assertions.assertEquals(2, infos.size());
            Assertions.assertEquals(i4, infos.get(0));
            Assertions.assertEquals(i5, infos.get(1));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void deleteComics() {
        try {
            DBComicInfo c1 = db.createComic("comic1");
            DBComicInfo c2 = db.createComic("comic2");
            DBComicInfo c3 = db.createComic("comic3");
            DBComicInfo c4 = db.createComic("comic4");
            DBComicInfo c5 = db.createComic("comic5");
            Assertions.assertEquals(3, db.deleteComics(c1, c2, c3));
            ArrayList<DBComicInfo> infos = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE + " ORDER BY " + ImageDatabase.SQL_COMIC_ID);
            Assertions.assertEquals(2, infos.size());
            Assertions.assertEquals(c4, infos.get(0));
            Assertions.assertEquals(c5, infos.get(1));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void uncommittedChanges() {
        try {
            final String imagePath = "image";
            final String comicName = "comic";
            DBImageInfo i = db.createImage(imagePath);
            DBComicInfo c = db.createComic(comicName);
            i.setPath("changed image");
            c.setName("changed comic");
            db.clearCachedImages();
            db.clearCachedComics();
            ArrayList<DBImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE);
            ArrayList<DBComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE);
            Assertions.assertEquals(imagePath, images.get(0).getPath());
            Assertions.assertEquals(comicName, comics.get(0).getName());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void commitChanges() {
        try {
            final String newPath = "new path";
            final String newName = "new name";
            DBImageInfo i = db.createImage("image");
            DBComicInfo c = db.createComic("comic");
            i.setPath(newPath);
            c.setName(newName);
            db.commitChanges();
            ArrayList<DBImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE);
            ArrayList<DBComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE);
            Assertions.assertEquals(newPath, images.get(0).getPath());
            Assertions.assertEquals(newName, comics.get(0).getName());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

}