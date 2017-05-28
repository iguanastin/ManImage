package manimage.common;


import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

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
    public static final String SQL_COMICS_TAGGED_TABLE = "comics_tagged";
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

    private final ArrayList<ImageDatabaseUpdateListener> changeListeners = new ArrayList<>();

    private final ArrayList<DBImageInfo> imageInfos = new ArrayList<>();
    private final ArrayList<DBComicInfo> comicInfos = new ArrayList<>();
    private final Connection connection;
    private final Statement statement;

    private int nextImageID;
    private int nextComicID;


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

        nextImageID = getHighestImageID() + 1;
        nextComicID = getHighestComicID() + 1;
    }

    private int getHighestImageID() {
        try {
            ResultSet rs = statement.executeQuery("SELECT TOP 1 * " + SQL_IMAGE_ID + " FROM " + SQL_IMAGES_TABLE + " ORDER BY " + SQL_IMAGE_ID + " DESC");
            return rs.getInt(SQL_IMAGE_ID);
        } catch (SQLException ex) {
            return 0;
        }
    }

    private int getHighestComicID() {
        try {
            ResultSet rs = statement.executeQuery("SELECT TOP 1 * " + SQL_COMIC_ID + " FROM " + SQL_COMICS_TABLE + " ORDER BY " + SQL_COMIC_ID + " DESC");
            return rs.getInt(SQL_COMIC_ID);
        } catch (SQLException ex) {
            return 0;
        }
    }

    private void cleanAndInitialize() throws SQLException {
        statement.executeUpdate(SQL_DROP_TABLES + SQL_INITIALIZE_TABLES);
    }

    public ArrayList<DBImageInfo> getImages(String query) throws SQLException {
        final ArrayList<DBImageInfo> results = new ArrayList<>();

        ResultSet rs = statement.executeQuery(query);

        while (rs.next()) {
            final int id = rs.getInt(SQL_IMAGE_ID);
            DBImageInfo info = getCachedImage(id);

            if (info == null) {
                info = new DBImageInfo(id, rs.getNString(SQL_IMAGE_PATH), rs.getNString(SQL_IMAGE_SOURCE), rs.getByte(SQL_IMAGE_RATING), rs.getLong(SQL_IMAGE_TIME_ADDED));
            }

            results.add(info);
        }

        rs.close();

        //TODO: Bug. Modified, but uncommitted, images are returned as part of the results

        return results;
    }

    public ArrayList<DBComicInfo> getComics(String query) throws SQLException {
        final ArrayList<DBComicInfo> results = new ArrayList<>();

        ResultSet rs = statement.executeQuery(query);

        while (rs.next()) {
            final int id = rs.getInt(SQL_COMIC_ID);
            DBComicInfo info = getCachedComic(id);

            if (info == null) {
                info = new DBComicInfo(rs.getInt(SQL_COMIC_ID), rs.getNString(SQL_COMIC_NAME), rs.getNString(SQL_COMIC_SOURCE), rs.getLong(SQL_COMIC_TIME_ADDED));
            }

            results.add(info);
        }

        //TODO: Bug. Modified, but uncommitted, comics are returned as part of the results

        return results;
    }

    private DBImageInfo getCachedImage(int id) {
        for (DBImageInfo info : imageInfos) {
            if (info.getId() == id) {
                return info;
            }
        }

        return null;
    }

    private DBComicInfo getCachedComic(int id) {
        for (DBComicInfo info : comicInfos) {
            if (info.getId() == id) {
                return info;
            }
        }

        return null;
    }

    public synchronized DBImageInfo[] queueCreateImages(String... paths) {
        //TODO: Mark new images with 'tagme' tag

        final DBImageInfo[] results = new DBImageInfo[paths.length];

        for (int i = 0; i < paths.length; i++) {
            DBImageInfo image = new DBImageInfo(nextImageID, paths[i]);
            nextImageID++;
            imageInfos.add(image);
            results[i] = image;
        }

        return results;
    }

    public synchronized DBImageInfo queueCreateImage(String path) {
        return queueCreateImages(path)[0];
    }

    public synchronized DBComicInfo[] queueCreateComics(String... names) {
        //TODO: Mark new comics with 'tagme' tag

        final DBComicInfo[] results = new DBComicInfo[names.length];

        for (int i = 0; i < names.length; i++) {
            DBComicInfo comic = new DBComicInfo(nextComicID, names[i]);
            nextComicID++;
            comicInfos.add(comic);
            results[i] = comic;
        }

        return results;
    }

    public synchronized DBComicInfo queueCreateComic(String name) {
        return queueCreateComics(name)[0];
    }

    public synchronized void queueDeleteImages(DBImageInfo... images) {
        for (DBImageInfo image : images) {
            image.setToBeDeleted(true);
        }
    }

    public synchronized void queueDeleteComics(DBComicInfo... comics) {
        for (DBComicInfo comic : comics) {
            comic.setToBeDeleted(true);
        }
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
        //Test images table
        statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + "," + SQL_IMAGE_PATH + "," + SQL_IMAGE_SOURCE + "," + SQL_IMAGE_RATING + "," + SQL_IMAGE_TIME_ADDED + " FROM " + SQL_IMAGES_TABLE);

        //Test tags table
        statement.executeQuery("SELECT TOP 1 " + SQL_TAG_ID + "," + SQL_TAG_NAME + " FROM " + SQL_TAGS_TABLE);

        //Test image_tagged table
        statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + "," + SQL_TAG_ID + " FROM " + SQL_IMAGE_TAGGED_TABLE);

        //Test comics table
        statement.executeQuery("SELECT TOP 1 " + SQL_COMIC_ID + "," + SQL_COMIC_NAME + "," + SQL_COMIC_SOURCE + "," + SQL_COMIC_TIME_ADDED + " FROM " + SQL_COMICS_TABLE);

        //Test comic_tagged table
        statement.executeQuery("SELECT TOP 1 " + SQL_COMIC_ID + "," + SQL_TAG_ID + " FROM " + SQL_COMICS_TAGGED_TABLE);

        //Test comic_pages table
        statement.executeQuery("SELECT TOP 1 " + SQL_IMAGE_ID + "," + SQL_COMIC_ID + "," + SQL_COMIC_PAGES_PAGENUM + " FROM " + SQL_COMIC_PAGES_TABLE);
    }

    public int commitChanges() throws SQLException {
        StringBuilder queryBuilder = new StringBuilder();

        for (DBImageInfo image : imageInfos) {
            if (image.isToBeDeleted()) {
                if (image.isInserted()) {
                    buildImageDeleteQuery(queryBuilder, image);
                    image.setInserted(false);
                } else {
                    imageInfos.remove(image);
                }
            } else if (image.isToBeInserted() && !image.isInserted()) {
                buildImageInsertQuery(queryBuilder, image);
                image.setInserted(true);
            } else if (image.isChanged() && image.isInserted()) {
                buildImageUpdateQuery(queryBuilder, image);
            }

            image.setAsUpdated();
        }
        for (DBComicInfo comic : comicInfos) {
            if (comic.isToBeDeleted()) {
                if (comic.isInserted()) {
                    buildComicDeleteQuery(queryBuilder, comic);
                    comic.setInserted(false);
                } else {
                    comicInfos.remove(comic);
                }
            } else if (comic.isToBeInserted()) {
                buildComicInsertQuery(queryBuilder, comic);
                comic.setInserted(true);
            } else if (comic.isChanged()) {
                buildComicUpdateQuery(queryBuilder, comic);
            }

            comic.setAsUpdated();
        }

        final int result = statement.executeUpdate(queryBuilder.toString());
        if (result > 0) changeListeners.forEach(ImageDatabaseUpdateListener::databaseUpdated);
        return result;
    }

    private void buildImageDeleteQuery(StringBuilder sb, DBImageInfo image) {
        sb.append("DELETE FROM ").append(SQL_IMAGES_TABLE).append(" WHERE ").append(SQL_IMAGE_ID).append('=').append(image.getId()).append(";\n");
    }

    private void buildComicDeleteQuery(StringBuilder sb, DBComicInfo comic) {
        sb.append("DELETE FROM ").append(SQL_COMICS_TABLE).append(" WHERE ").append(SQL_COMIC_ID).append('=').append(comic.getId()).append(";\n");
    }

    private void buildComicUpdateQuery(StringBuilder sb, DBComicInfo comic) {
        boolean commaNeeded = false;

        sb.append("UPDATE ").append(SQL_COMICS_TABLE).append(" SET ");

        if (comic.isNameChanged()) {
            sb.append(SQL_COMIC_NAME).append("='").append(comic.getName()).append('\'');
            commaNeeded = true;
        }
        if (comic.isSourceChanged()) {
            if (commaNeeded) sb.append(',');
            sb.append(SQL_COMIC_SOURCE).append("=");
            if (comic.getSource() == null) {
                sb.append("NULL");
            } else {
                sb.append('\'').append(comic.getSource()).append('\'');
            }
        }

        sb.append(" WHERE ").append(SQL_COMIC_ID).append('=').append(comic.getId()).append(";\n");
    }

    private void buildImageUpdateQuery(StringBuilder sb, DBImageInfo image) {
        boolean commaNeeded = false;

        sb.append("UPDATE ").append(SQL_IMAGES_TABLE).append(" SET ");

        if (image.isPathChanged()) {
            sb.append(SQL_IMAGE_PATH).append("=");
            if (image.getPath() == null) {
                sb.append("NULL");
            } else {
                sb.append('\'').append(image.getPath()).append('\'');
            }
            commaNeeded = true;
        }
        if (image.isSourceChanged()) {
            if (commaNeeded) sb.append(',');
            sb.append(SQL_IMAGE_SOURCE).append("=");
            if (image.getSource() == null) {
                sb.append("NULL");
            } else {
                sb.append('\'').append(image.getSource()).append('\'');
            }
            commaNeeded = true;
        }
        if (image.isRatingChanged()) {
            if (commaNeeded) sb.append(',');
            sb.append(SQL_IMAGE_RATING).append('=').append(image.getRating());
        }

        sb.append(" WHERE ").append(SQL_IMAGE_ID).append('=').append(image.getId()).append(";\n");
    }

    private void buildImageInsertQuery(StringBuilder sb, DBImageInfo image) {
        sb.append("INSERT INTO ").append(SQL_IMAGES_TABLE).append(" VALUES (").append(image.getId()).append(',');

        if (image.getPath() == null) {
            sb.append("NULL,");
        } else {
            sb.append('\'').append(image.getPath()).append("',");
        }

        if (image.getSource() == null) {
            sb.append("NULL,");
        } else {
            sb.append('\'').append(image.getSource()).append("',");
        }

        sb.append(image.getRating()).append(",").append(image.getTimeAdded()).append(");\n");
    }

    private void buildComicInsertQuery(StringBuilder sb, DBComicInfo comic) {
        sb.append("INSERT INTO ").append(SQL_COMICS_TABLE).append(" VALUES (").append(comic.getId()).append(",'").append(comic.getName()).append("',");

        if (comic.getSource() == null) {
            sb.append("NULL,");
        } else {
            sb.append('\'').append(comic.getSource()).append("',");
        }

        sb.append(comic.getTimeAdded()).append(");\n");
    }

    public void addChangeListener(ImageDatabaseUpdateListener listener) {
        changeListeners.add(listener);
    }

    public boolean removeChangeListener(ImageDatabaseUpdateListener listener) {
        return changeListeners.remove(listener);
    }

    public void close() throws SQLException {
        statement.close();
        connection.close();
    }

}
