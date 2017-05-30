package manimage.common;


import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;

public class ImageDatabase {

    private static final String SQL_DROP_TABLES = "DROP TABLE IF EXISTS images;\n" +
            "DROP TABLE IF EXISTS tags;\n" +
            "DROP TABLE IF EXISTS image_tagged;\n" +
            "DROP TABLE IF EXISTS comic_tagged;\n" +
            "DROP TABLE IF EXISTS comics;\n" +
            "DROP TABLE IF EXISTS comic_pages;";
    private static final String SQL_INITIALIZE_TABLES = "CREATE TABLE images(image_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, file_path NVARCHAR(1024) UNIQUE, source_url NVARCHAR(1024), rating TINYINT NOT NULL DEFAULT(0), image_time_added LONG NOT NULL);\n" +
            "CREATE TABLE tags(tag_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, tag_name NVARCHAR(128) NOT NULL UNIQUE);\n" +
            "CREATE TABLE image_tagged(image_id INT NOT NULL, tag_id INT NOT NULL, PRIMARY KEY(tag_id, image_id), FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE, FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE);\n" +
            "INSERT INTO tags (tag_name) VALUES ('tagme');\n" +
            "CREATE TABLE comics(comic_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, comic_name NVARCHAR(512) NOT NULL UNIQUE, comic_source NVARCHAR(1024), comic_time_added LONG NOT NULL);\n" +
            "CREATE TABLE comic_pages(comic_id INT NOT NULL, image_id INT NOT NULL, page_num INT NOT NULL, FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE, FOREIGN KEY (comic_id) REFERENCES comics(comic_id) ON DELETE CASCADE, PRIMARY KEY (comic_id, image_id));\n" +
            "CREATE TABLE comic_tagged(comic_id INT NOT NULL, tag_id INT NOT NULL, PRIMARY KEY(tag_id, comic_id), FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE, FOREIGN KEY (comic_id) REFERENCES comics(comic_id) ON DELETE CASCADE);";

    public static final String SQL_IMAGES_TABLE = "images";
    public static final String SQL_TAGS_TABLE = "tags";
    public static final String SQL_IMAGE_TAGGED_TABLE = "image_tagged";
    public static final String SQL_COMICS_TABLE = "comics";
    public static final String SQL_COMIC_TAGGED_TABLE = "comic_tagged";
    public static final String SQL_COMIC_PAGES_TABLE = "comic_pages";

    public static final String SQL_IMAGE_ID = "image_id";
    public static final String SQL_IMAGE_PATH = "file_path";
    public static final String SQL_IMAGE_SOURCE = "source_url";
    public static final String SQL_IMAGE_RATING = "rating";
    public static final String SQL_IMAGE_TIME_ADDED = "image_time_added";

    public static final String SQL_TAG_ID = "tag_id";
    public static final String SQL_TAG_NAME = "tag_name";

    public static final String SQL_COMIC_ID = "comic_id";
    public static final String SQL_COMIC_NAME = "comic_name";
    public static final String SQL_COMIC_SOURCE = "comic_source";
    public static final String SQL_COMIC_TIME_ADDED = "comic_time_added";

    public static final String SQL_COMIC_PAGES_PAGENUM = "page_num";

    private final static int TAGME_TAG_ID = 1;

    private final ArrayList<ImageDatabaseUpdateListener> changeListeners = new ArrayList<>();

    private final ArrayList<ImageInfo> imageInfos = new ArrayList<>();
    private final ArrayList<ComicInfo> comicInfos = new ArrayList<>();
    private final ArrayList<TagInfo> tags = new ArrayList<>();
    private final Connection connection;
    private final Statement statement;

    private int nextImageID;
    private int nextComicID;
    private int nextTagID;


    public ImageDatabase(String path, String username, String password, boolean forceClean) throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:" + path, username, password);
        statement = connection.createStatement();

        if (forceClean) {
            cleanAndInitialize();
        } else {
            try {
                test();
            } catch (SQLException ex) {
                cleanAndInitialize();
            }
        }

        loadTags();

        nextImageID = getHighestImageID() + 1;
        nextComicID = getHighestComicID() + 1;
        nextTagID = getHighestTagID() + 1;
    }

    private synchronized void loadTags() throws SQLException {
        tags.clear();
        ResultSet rs = statement.executeQuery("SELECT * FROM " + SQL_TAGS_TABLE);
        while (rs.next()) {
            tags.add(new TagInfo(rs.getInt(SQL_TAG_ID), rs.getNString(SQL_TAG_NAME), true));
        }
    }

    private synchronized int getHighestImageID() {
        try {
            ResultSet rs = statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + " FROM " + SQL_IMAGES_TABLE + " ORDER BY " + SQL_IMAGE_ID + " DESC");
            if (rs.next()) {
                return rs.getInt(SQL_IMAGE_ID);
            } else {
                return 0;
            }
        } catch (SQLException ex) {
            return 0;
        }
    }

    private synchronized int getHighestComicID() {
        try {
            ResultSet rs = statement.executeQuery("SELECT TOP 1 " + SQL_COMIC_ID + " FROM " + SQL_COMICS_TABLE + " ORDER BY " + SQL_COMIC_ID + " DESC");
            if (rs.next()) {
                return rs.getInt(SQL_COMIC_ID);
            } else {
                return 0;
            }
        } catch (SQLException ex) {
            return 0;
        }
    }

    private synchronized int getHighestTagID() {
        try {
            ResultSet rs = statement.executeQuery("SELECT TOP 1 " + SQL_TAG_ID + " FROM " + SQL_TAGS_TABLE + " ORDER BY " + SQL_TAG_ID + " DESC");
            if (rs.next()) {
                return rs.getInt(SQL_TAG_ID);
            } else {
                return 0;
            }
        } catch (SQLException ex) {
            return 0;
        }
    }

    private synchronized void cleanAndInitialize() throws SQLException {
        statement.executeUpdate(SQL_DROP_TABLES + SQL_INITIALIZE_TABLES);
    }

    public synchronized boolean isConnected() {
        try {
            return !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    public synchronized ArrayList<ImageInfo> getImages(String query) throws SQLException {
        final ArrayList<ImageInfo> results = new ArrayList<>();

        ResultSet rs = statement.executeQuery(query);

        while (rs.next()) {
            final int id = rs.getInt(SQL_IMAGE_ID);
            ImageInfo image = getCachedImage(id);

            if (image == null) {
                image = new ImageInfo(id, rs.getNString(SQL_IMAGE_PATH), rs.getNString(SQL_IMAGE_SOURCE), rs.getByte(SQL_IMAGE_RATING), rs.getLong(SQL_IMAGE_TIME_ADDED));
                imageInfos.add(image);
            }

            results.add(image);
        }

        rs.close();

        //TODO: Issue. Modified, but uncommitted images are returned as part of the results

        return results;
    }

    public synchronized ArrayList<ComicInfo> getComics(String query) throws SQLException {
        final ArrayList<ComicInfo> results = new ArrayList<>();

        ResultSet rs = statement.executeQuery(query);

        while (rs.next()) {
            final int id = rs.getInt(SQL_COMIC_ID);
            ComicInfo comic = getCachedComic(id);

            if (comic == null) {
                comic = new ComicInfo(rs.getInt(SQL_COMIC_ID), rs.getNString(SQL_COMIC_NAME), rs.getNString(SQL_COMIC_SOURCE), rs.getLong(SQL_COMIC_TIME_ADDED));
                comicInfos.add(comic);
            }

            results.add(comic);
        }

        //TODO: Issue. Modified, but uncommitted comics are returned as part of the results

        return results;
    }

    public synchronized TagInfo getTag(int id) {
        for (TagInfo tag : tags) {
            if (tag.getId() == id) {
                return tag;
            }
        }

        return null;
    }

    public synchronized TagInfo getTag(String name) {
        for (TagInfo tag : tags) {
            if (tag.getName().equalsIgnoreCase(name)) {
                return tag;
            }
        }

        return null;
    }

    private synchronized ImageInfo getCachedImage(int id) {
        for (ImageInfo info : imageInfos) {
            if (info.getId() == id) {
                return info;
            }
        }

        return null;
    }

    private synchronized ComicInfo getCachedComic(int id) {
        for (ComicInfo info : comicInfos) {
            if (info.getId() == id) {
                return info;
            }
        }

        return null;
    }

    public synchronized ImageInfo[] queueCreateImages(String... paths) {
        final ImageInfo[] results = new ImageInfo[paths.length];

        for (int i = 0; i < paths.length; i++) {
            ImageInfo image = new ImageInfo(nextImageID, paths[i]);
            image.addTag(getTag("tagme"));
            nextImageID++;
            imageInfos.add(image);
            results[i] = image;
        }

        return results;
    }

    public synchronized ImageInfo queueCreateImage(String path) {
        return queueCreateImages(path)[0];
    }

    public synchronized ComicInfo[] queueCreateComics(String... names) {
        final ComicInfo[] results = new ComicInfo[names.length];

        for (int i = 0; i < names.length; i++) {
            ComicInfo comic = new ComicInfo(nextComicID, names[i]);
            nextComicID++;
            comicInfos.add(comic);
            results[i] = comic;
        }

        return results;
    }

    public synchronized ComicInfo queueCreateComic(String name) {
        return queueCreateComics(name)[0];
    }

    public synchronized TagInfo[] queueCreateTags(String... names) {
        final TagInfo[] results = new TagInfo[names.length];

        for (int i = 0; i < names.length; i++) {
            results[i] = new TagInfo(nextTagID, names[i], false);
            results[i].setToBeInserted();
            nextTagID++;
            tags.add(results[i]);
        }

        return results;
    }

    public synchronized TagInfo queueCreateTag(String name) {
        return queueCreateTags(name)[0];
    }

    public synchronized void clearCachedImages() {
        imageInfos.clear();
    }

    public synchronized void clearCachedComics() {
        comicInfos.clear();
    }

    /**
     * Tests the basic architecture of the database
     *
     * @throws SQLException When basic architecture is unexpected
     */
    public void test() throws SQLException {
        //TODO: Modify test to test database and make updates if necessary

        statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + "," + SQL_IMAGE_PATH + "," + SQL_IMAGE_SOURCE + "," + SQL_IMAGE_RATING + "," + SQL_IMAGE_TIME_ADDED + " FROM " + SQL_IMAGES_TABLE);
        statement.executeQuery("SELECT TOP 1 " + SQL_TAG_ID + "," + SQL_TAG_NAME + " FROM " + SQL_TAGS_TABLE);
        statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + "," + SQL_TAG_ID + " FROM " + SQL_IMAGE_TAGGED_TABLE);
        statement.executeQuery("SELECT TOP 1 " + SQL_COMIC_ID + "," + SQL_COMIC_NAME + "," + SQL_COMIC_SOURCE + "," + SQL_COMIC_TIME_ADDED + " FROM " + SQL_COMICS_TABLE);
        statement.executeQuery("SELECT TOP 1 " + SQL_COMIC_ID + "," + SQL_TAG_ID + " FROM " + SQL_COMIC_TAGGED_TABLE);
        statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + "," + SQL_COMIC_ID + "," + SQL_COMIC_PAGES_PAGENUM + " FROM " + SQL_COMIC_PAGES_TABLE);
    }

    public synchronized int commitChanges() throws SQLException {
        int updates = commitChangesWithoutNotify();
        if (updates > 0) notifyChangeListeners();
        return updates;
    }

    public void notifyChangeListeners() {
        changeListeners.forEach(ImageDatabaseUpdateListener::databaseUpdated);
    }

    public synchronized int commitChangesWithoutNotify() throws SQLException {
        int updates = 0;
        StringBuilder queryBuilder = new StringBuilder();

        //TODO: Make uncommitted updates reversible

        Iterator<TagInfo> tagIter = tags.listIterator();
        while (tagIter.hasNext()) {
            TagInfo tag = tagIter.next();
            if (!tag.isSynchronized()) {
                updates += tag.buildSQLUpdates(queryBuilder);
            }

            if (tag.isToBeDeleted()) tagIter.remove();
            tag.markAsCommitted();
        }

        Iterator<ImageInfo> imageIter = imageInfos.listIterator();
        while (imageIter.hasNext()) {
            ImageInfo image = imageIter.next();
            if (!image.isSynchronized()) {
                updates += image.buildSQLUpdates(queryBuilder);
            }

            if (image.isToBeDeleted()) imageIter.remove();
            image.markAsCommitted();
        }

        Iterator<ComicInfo> comicIter = comicInfos.listIterator();
        while (comicIter.hasNext()) {
            ComicInfo comic = comicIter.next();
            if (!comic.isSynchronized()) {
                updates += comic.buildSQLUpdates(queryBuilder);
            }

            if (comic.isToBeDeleted()) comicIter.remove();
            comic.markAsCommitted();
        }

        statement.executeUpdate(queryBuilder.toString());

        System.out.println("DatabaseUpdated:\t" + updates);

        return updates;
    }

    public void addChangeListener(ImageDatabaseUpdateListener listener) {
        changeListeners.add(listener);
    }

    public boolean removeChangeListener(ImageDatabaseUpdateListener listener) {
        return changeListeners.remove(listener);
    }

    public synchronized void close() throws SQLException {
        statement.close();
        connection.close();
    }

}
