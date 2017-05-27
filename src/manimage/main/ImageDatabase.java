package manimage.main;


import manimage.common.DBImageInfo;

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
    private static final String SQL_INITIALIZE_TABLES = "CREATE TABLE images(image_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, file_path NVARCHAR(1024) UNIQUE, source_url NVARCHAR(1024), rating BYTE NOT NULL DEFAULT(0), image_time_added LONG NOT NULL);\n" +
            "CREATE TABLE tags(tag_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, tag_name NVARCHAR(128) NOT NULL UNIQUE);\n" +
            "CREATE TABLE image_tagged(image_id INT NOT NULL, tag_id INT NOT NULL, PRIMARY KEY(tag_id, image_id), FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE, FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE);\n" +
            "INSERT INTO tags (tag_name) VALUES ('tagme');\n" +
            "CREATE TABLE comics(comic_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, comic_name NVARCHAR(512) NOT NULL, comic_source NVARCHAR(1024), comic_time_added LONG NOT NULL);\n" +
            "CREATE TABLE comic_pages(comic_id INT NOT NULL, image_id INT NOT NULL, page_num INT NOT NULL, FOREIGN KEY (image_id) REFERENCES images(image_id) ON DELETE CASCADE, FOREIGN KEY (comic_id) REFERENCES comics(comic_id) ON DELETE CASCADE, PRIMARY KEY (comic_id, image_id));\n" +
            "CREATE TABLE comic_tagged(comic_id INT NOT NULL, tag_id INT NOT NULL, PRIMARY KEY(tag_id, comic_id), FOREIGN KEY (tag_id) REFERENCES tags(tag_id) ON DELETE CASCADE, FOREIGN KEY (comic_id) REFERENCES comics(comic_id) ON DELETE CASCADE);";

    public static final String SQL_IMAGES_TABLE = "images";
    public static final String SQL_TAGS_TABLE = "tags";
    public static final String SQL_IMAGE_TAGGED_TABLE = "image_tagged";
    public static final String SQL_COMICS_TABLE = "comics";
    public static final String SQL_COMICS_TAGGED_TABLE = "comics_tagged";

    public static final String SQL_IMAGE_ID = "image_id";
    public static final String SQL_IMAGE_PATH = "file_path";
    public static final String SQL_IMAGE_SOURCE = "source_url";
    public static final String SQL_IMAGE_RATING = "rating";
    public static final String SQL_IMAGE_TIME_ADDED = "image_time_added";

    public static final String SQL_TAG_ID = "tag_id";
    public static final String SQL_TAG_NAME = "tag_name";

    public static final String SQL_COMIC_ID = "comic_id";
    public static final String SQL_COMIC_NAME = "comic_name";
    public static final String SQL_COMIC_TIME_ADDED = "comic_time_added";

    public static final String SQL_COMIC_PAGES_PAGENUM = "page_num";

    private final ArrayList<DBImageInfo> imageInfos = new ArrayList<>();
    private final Connection connection;
    private final Statement statement;


    public ImageDatabase(String path, String username, String password, boolean cleanAndInitalize) throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:" + path, username, password);
        statement = connection.createStatement();

        if (cleanAndInitalize) statement.executeUpdate(SQL_DROP_TABLES + "\n" + SQL_INITIALIZE_TABLES);
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

        //TODO: Test

        return results;
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

    public DBImageInfo getCachedImage(int id) {
        for (DBImageInfo info : imageInfos) {
            if (info.getId() == id) {
                return info;
            }
        }

        return null;
    }

    public int commitChangedImages() throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (DBImageInfo info : imageInfos) {
            if (info.isChanged()) {
                boolean commaNeeded = false;

                sb.append("UPDATE ").append(SQL_IMAGES_TABLE).append(" SET ");

                if (info.isPathChanged()) {
                    sb.append(SQL_IMAGE_PATH).append('=').append(info.getPath());
                    commaNeeded = true;
                }
                if (info.isSourceChanged()) {
                    if (commaNeeded) sb.append(',');
                    sb.append(SQL_IMAGE_SOURCE).append('=').append(info.getSource());
                    commaNeeded = true;
                }
                if (info.isRatingChanged()) {
                    if (commaNeeded) sb.append(',');
                    sb.append(SQL_IMAGE_RATING).append('=').append(info.getRating());
                }

                info.markChangeApplied();

                sb.append(" WHERE ").append(SQL_IMAGE_ID).append('=').append(info.getId()).append(";\n");
            }
        }

        //TODO: Test

        return statement.executeUpdate(sb.toString());
    }

    public void close() throws SQLException {
        statement.close();
        connection.close();
    }

}
