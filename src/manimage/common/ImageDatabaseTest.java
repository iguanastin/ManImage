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
    void getCachedImages() {
        try {
            ImageInfo[] created = db.queueCreateImages("image1", "image2", "image3", "image4", "image5");
            db.commitChanges();
            ArrayList<ImageInfo> retrieved = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " ORDER BY " + ImageDatabase.SQL_IMAGE_ID + " OFFSET 0 LIMIT 5");
            Assertions.assertEquals(5, retrieved.size());
            Assertions.assertEquals(created[0], retrieved.get(0));
            Assertions.assertEquals(created[1], retrieved.get(1));
            Assertions.assertEquals(created[2], retrieved.get(2));
            Assertions.assertEquals(created[3], retrieved.get(3));
            Assertions.assertEquals(created[4], retrieved.get(4));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void getImagesUncached() {
        try {
            ImageInfo created = db.queueCreateImage("image");
            db.commitChanges();
            db.clearCachedImages();
            ArrayList<ImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE);
            Assertions.assertEquals(created.getId(), images.get(0).getId());
            Assertions.assertEquals(created.getPath(), images.get(0).getPath());
            Assertions.assertEquals(created.getSource(), images.get(0).getSource());
            Assertions.assertEquals(created.getTimeAdded(), images.get(0).getTimeAdded());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void getComics() {
        try {
            ComicInfo[] created = db.queueCreateComics("comic1", "comic2", "comic3", "comic4", "comic5");
            db.commitChanges();
            ArrayList<ComicInfo> infos = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE + " ORDER BY " + ImageDatabase.SQL_COMIC_ID + " OFFSET 0 LIMIT 5");
            Assertions.assertEquals(5, infos.size());
            Assertions.assertEquals(created[0], infos.get(0));
            Assertions.assertEquals(created[1], infos.get(1));
            Assertions.assertEquals(created[2], infos.get(2));
            Assertions.assertEquals(created[3], infos.get(3));
            Assertions.assertEquals(created[4], infos.get(4));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void getComicsUncached() {
        try {
            ComicInfo created = db.queueCreateComic("comic");
            db.commitChanges();
            db.clearCachedComics();
            ArrayList<ComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE);
            Assertions.assertEquals(created.getId(), comics.get(0).getId());
            Assertions.assertEquals(created.getName(), comics.get(0).getName());
            Assertions.assertEquals(created.getSource(), comics.get(0).getSource());
            Assertions.assertEquals(created.getTimeAdded(), comics.get(0).getTimeAdded());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void createImage() {
        try {
            ImageInfo created = db.queueCreateImage("image");
            Assertions.assertFalse(created.isInserted());
            Assertions.assertTrue(created.isToBeInserted());
            db.commitChanges();
            Assertions.assertTrue(created.isInserted());
            ArrayList<ImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " WHERE " + ImageDatabase.SQL_IMAGE_ID + "=" + created.getId());
            Assertions.assertEquals(created, images.get(0));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void createComic() {
        try {
            ComicInfo created = db.queueCreateComic("comic");
            Assertions.assertFalse(created.isInserted());
            Assertions.assertTrue(created.isToBeInserted());
            db.commitChanges();
            Assertions.assertTrue(created.isInserted());
            ArrayList<ComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE + " WHERE " + ImageDatabase.SQL_COMIC_ID + "=" + created.getId());
            Assertions.assertEquals(created, comics.get(0));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void deleteImages() {
        try {
            ImageInfo[] created = db.queueCreateImages("image1", "image2", "image3", "image4", "image5");
            db.commitChanges();
            created[0].setToBeDeleted(true);
            created[1].setToBeDeleted(true);
            created[2].setToBeDeleted(true);
            Assertions.assertTrue(created[0].isToBeDeleted());
            Assertions.assertTrue(created[1].isToBeDeleted());
            Assertions.assertTrue(created[2].isToBeDeleted());
            Assertions.assertFalse(created[3].isToBeDeleted());
            Assertions.assertFalse(created[4].isToBeDeleted());

            db.commitChanges();
            Assertions.assertFalse(created[0].isInserted());
            Assertions.assertFalse(created[1].isInserted());
            Assertions.assertFalse(created[2].isInserted());
            Assertions.assertTrue(created[3].isInserted());
            Assertions.assertTrue(created[4].isInserted());

            ArrayList<ImageInfo> infos = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE + " ORDER BY " + ImageDatabase.SQL_IMAGE_ID);
            Assertions.assertEquals(2, infos.size());
            Assertions.assertEquals(created[3], infos.get(0));
            Assertions.assertEquals(created[4], infos.get(1));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void deleteComics() {
        try {
            ComicInfo[] created = db.queueCreateComics("c1", "c2", "c3", "c4", "c5");
            db.commitChanges();
            created[0].setToBeDeleted(true);
            created[1].setToBeDeleted(true);
            created[2].setToBeDeleted(true);
            Assertions.assertTrue(created[0].isToBeDeleted());
            Assertions.assertTrue(created[1].isToBeDeleted());
            Assertions.assertTrue(created[2].isToBeDeleted());
            Assertions.assertFalse(created[3].isToBeDeleted());
            Assertions.assertFalse(created[4].isToBeDeleted());

            db.commitChanges();
            Assertions.assertFalse(created[0].isInserted());
            Assertions.assertFalse(created[1].isInserted());
            Assertions.assertFalse(created[2].isInserted());
            Assertions.assertTrue(created[3].isInserted());
            Assertions.assertTrue(created[4].isInserted());

            ArrayList<ComicInfo> infos = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE + " ORDER BY " + ImageDatabase.SQL_COMIC_ID);
            Assertions.assertEquals(2, infos.size());
            Assertions.assertEquals(created[3], infos.get(0));
            Assertions.assertEquals(created[4], infos.get(1));
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

    @Test
    void uncommittedChanges() {
        try {
            final String imagePath = "image";
            final String comicName = "comic";
            ImageInfo i = db.queueCreateImage(imagePath);
            ComicInfo c = db.queueCreateComic(comicName);
            db.commitChanges();
            i.setPath("changed image");
            c.setName("changed comic");
            db.clearCachedImages();
            db.clearCachedComics();
            ArrayList<ImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE);
            ArrayList<ComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE);
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
            ImageInfo i = db.queueCreateImage("image");
            ComicInfo c = db.queueCreateComic("comic");
            db.commitChanges();
            i.setPath(newPath);
            c.setName(newName);
            db.commitChanges();
            ArrayList<ImageInfo> images = db.getImages("SELECT * FROM " + ImageDatabase.SQL_IMAGES_TABLE);
            ArrayList<ComicInfo> comics = db.getComics("SELECT * FROM " + ImageDatabase.SQL_COMICS_TABLE);
            Assertions.assertEquals(newPath, images.get(0).getPath());
            Assertions.assertEquals(newName, comics.get(0).getName());
        } catch (SQLException ex) {
            Assertions.fail(ex);
        }
    }

}