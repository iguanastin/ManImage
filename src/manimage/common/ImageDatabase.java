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

    public DBImageInfo createImage(String path) throws SQLException {
        //TODO: Mark new images with 'tagme' tag

        statement.executeUpdate("INSERT INTO " + SQL_IMAGES_TABLE + " (" + SQL_IMAGE_PATH + ", " + SQL_IMAGE_TIME_ADDED + ") VALUES ('" + path + "'," + System.currentTimeMillis() + ")");

        //Get created image info from database
        ResultSet rs = statement.executeQuery("SELECT * FROM " + SQL_IMAGES_TABLE + " WHERE " + SQL_IMAGE_PATH + "='" + path + "'");
        if (rs.next()) {
            DBImageInfo info = new DBImageInfo(rs.getInt(SQL_IMAGE_ID), rs.getNString(SQL_IMAGE_PATH), rs.getNString(SQL_IMAGE_SOURCE), rs.getByte(SQL_IMAGE_RATING), rs.getLong(SQL_IMAGE_TIME_ADDED));
            imageInfos.add(info);
            return info;
        } else {
            return null;
        }
    }

    public DBComicInfo createComic(String name) throws SQLException {
        //TODO: Mark new comics with 'tagme' tag

        statement.executeUpdate("INSERT INTO " + SQL_COMICS_TABLE + " (" + SQL_COMIC_NAME + "," + SQL_COMIC_TIME_ADDED + ") VALUES ('" + name + "'," + System.currentTimeMillis() + ")");

        //Get created comic info from database
        ResultSet rs = statement.executeQuery("SELECT * FROM " + SQL_COMICS_TABLE + " WHERE " + SQL_COMIC_NAME + "='" + name + "'");
        if (rs.next()) {
            DBComicInfo info = new DBComicInfo(rs.getInt(SQL_COMIC_ID), rs.getNString(SQL_COMIC_NAME), rs.getNString(SQL_COMIC_SOURCE), rs.getLong(SQL_COMIC_TIME_ADDED));
            comicInfos.add(info);
            return info;
        } else {
            return null;
        }
    }

    public int deleteImages(DBImageInfo... infos) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(SQL_IMAGE_ID).append('=').append(infos[0].getId());
        for (int i = 1; i < infos.length; i++) {
            sb.append(" OR ").append(SQL_IMAGE_ID).append('=').append(infos[i].getId());
        }

        //TODO: Test

        imageInfos.removeAll(Arrays.asList(infos));
        return statement.executeUpdate("DELETE FROM " + SQL_IMAGES_TABLE + " WHERE " + sb);
    }

    public int deleteComics(DBComicInfo... infos) throws SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append(SQL_COMIC_ID).append('=').append(infos[0].getId());
        for (int i = 1; i < infos.length; i++) {
            sb.append(" OR ").append(SQL_COMIC_ID).append('=').append(infos[i].getId());
        }

        //TODO: Test

        comicInfos.removeAll(Arrays.asList(infos));
        return statement.executeUpdate("DELETE FROM " + SQL_COMICS_TABLE + " WHERE " + sb);
    }

    public void clearCachedImages() {
        imageInfos.clear();
    }

    public void clearCachedComics() {
        comicInfos.clear();
    }

    /**
     * Tests the basic architecture of the database
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
        StringBuilder sb = new StringBuilder();
        for (DBImageInfo info : imageInfos) {
            if (info.isChanged()) {
                boolean commaNeeded = false;

                sb.append("UPDATE ").append(SQL_IMAGES_TABLE).append(" SET ");

                if (info.isPathChanged()) {
                    sb.append(SQL_IMAGE_PATH).append("='").append(info.getPath()).append('\'');
                    commaNeeded = true;
                }
                if (info.isSourceChanged()) {
                    if (commaNeeded) sb.append(',');
                    sb.append(SQL_IMAGE_SOURCE).append("='").append(info.getSource()).append('\'');
                    commaNeeded = true;
                }
                if (info.isRatingChanged()) {
                    if (commaNeeded) sb.append(',');
                    sb.append(SQL_IMAGE_RATING).append('=').append(info.getRating());
                }

                info.markChangeCommitted();

                sb.append(" WHERE ").append(SQL_IMAGE_ID).append('=').append(info.getId()).append(";\n");
            }
        }
        for (DBComicInfo info : comicInfos) {
            if (info.isChanged()) {
                boolean commaNeeded = false;

                sb.append("UPDATE ").append(SQL_COMICS_TABLE).append(" SET ");

                if (info.isNameChanged()) {
                    sb.append(SQL_COMIC_NAME).append("='").append(info.getName()).append('\'');
                    commaNeeded = true;
                }
                if (info.isSourceChanged()) {
                    if (commaNeeded) sb.append(',');
                    sb.append(SQL_COMIC_SOURCE).append("='").append(info.getSource()).append('\'');
                }

                info.markChangesCommitted();

                sb.append(" WHERE ").append(SQL_COMIC_ID).append('=').append(info.getId()).append(";\n");
            }
        }

        //TODO: Test

        final int result = statement.executeUpdate(sb.toString());

        if (result > 0) changeListeners.forEach(ImageDatabaseUpdateListener::databaseUpdated);

        return result;
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
