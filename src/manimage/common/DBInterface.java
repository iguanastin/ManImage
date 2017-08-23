package manimage.common;


import com.sun.istack.internal.NotNull;

import java.io.File;
import java.lang.ref.SoftReference;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

public class DBInterface {

    private static final String SQL_INITIALIZE_TABLES = "CREATE TABLE imgs(img_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, img_path NVARCHAR(1024) UNIQUE, img_src NVARCHAR(1024), img_added LONG NOT NULL, img_tags NVARCHAR(1024));";

    private final Connection connection;
    private final Statement statement;

    private final ArrayList<ImageDatabaseUpdateListener> listeners = new ArrayList<>();

    final private ArrayList<SoftReference<ImgInfo>> imgs = new ArrayList<>();


    public DBInterface(String path, String username, String password) throws SQLException {
        System.out.println("Attempting to connect to: [jdbc:h2:" + path + "] With user/password: " + username + "/" + password);
        connection = DriverManager.getConnection("jdbc:h2:" + path, username, password);
        statement = connection.createStatement();
        System.out.println("Connected successfully");

        verifyTables();
    }

    private synchronized void verifyTables() throws SQLException {
        System.out.println("Attempting to verify table...");
        if (!tableExists("imgs", "img_id", "img_path", "img_src", "img_added", "img_tags")) {
            dropTables();
            System.out.println("Initializing tables...");
            statement.executeUpdate(SQL_INITIALIZE_TABLES);
        }
        System.out.println("Tables successfully verified");
    }

    private synchronized void dropTables() throws SQLException {
        System.out.println("Attempting to drop tables...");
        statement.executeUpdate("DROP TABLE IF EXISTS imgs;");
    }

    private synchronized boolean tableExists(String name, String... columns) {
        try {
            statement.executeQuery("SELECT TOP 1 " + String.join(",", columns) + " FROM " + name);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized ArrayList<ImgInfo> getImages(int limit, int offset, OrderBy order, String[] tags, String pathContains) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT imgs.* FROM imgs ");

        String ratingSQL = null;
        String tagsSQL = null;
        String pathSQL = null;

        //Tags search
        if (tags != null && tags.length > 0) {
            tagsSQL = "(";

            boolean and = false;
            for (String tag : tags) {
                if (!tag.isEmpty()) {
                    if (and) tagsSQL += " AND ";
                    tagsSQL += "img_tags ";
                    if (tag.startsWith("-")) {
                        tagsSQL += "NOT ";
                        tag = tag.substring(1);
                    }
                    tagsSQL += "LIKE '% " + tag + " %'";
                    and = true;
                }
            }

            tagsSQL += ")";
        }

        if (pathContains != null && !pathContains.isEmpty()) {
            pathSQL = "(img_path LIKE '%" + pathContains + "%')";
        }

        final ArrayList<String> whereParts = new ArrayList<>();
        if (ratingSQL != null) whereParts.add(ratingSQL);
        if (tagsSQL != null) whereParts.add(tagsSQL);
        if (pathSQL != null) whereParts.add(pathSQL);
        if (!whereParts.isEmpty()) query.append(" WHERE ");
        query.append(String.join(" AND ", whereParts));

        //Order and offset
        if (order != null) query.append(" ORDER BY ").append(order);
        if (limit > 0) query.append(" LIMIT ").append(limit);
        if (offset > 0) query.append(" OFFSET ").append(offset);
        query.append(";");
        ResultSet rs = statement.executeQuery(query.toString());

        ArrayList<ImgInfo> results = new ArrayList<>();
        while (rs.next()) {
            ImgInfo img = getCachedImg(rs.getInt("img_id"));
            if (img == null) {
                img = new ImgInfo(rs.getInt("img_id"), rs.getLong("img_added"), rs.getNString("img_src"), rs.getNString("img_path"), imgTagsStringToArray(rs.getNString("img_tags")));
            } else {
                img.update(rs.getNString("img_src"), rs.getNString("img_path"), imgTagsStringToArray(rs.getNString("img_tags")));
            }
            results.add(img);
        }

        return results;
    }

    private synchronized void addImage(@NotNull String path, String src, int rating, String tags, boolean isBatch) throws SQLException {
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();

        if (path != null && !path.isEmpty()) {
            if (path.contains("'")) path = path.replaceAll("'", "''");
            ResultSet rs = statement.executeQuery("SELECT TOP 1 img_id FROM imgs WHERE img_path='" + path + "'");
            if (rs.next()) {
                return;
            }
            columns.add("img_path");
            values.add("'" + path + "'");
        }
        if (src != null && !src.isEmpty()) {
            if (src.contains("'")) src = src.replaceAll("'", "''");
            columns.add("img_src");
            values.add("'" + src + "'");
        }
        columns.add("img_rating");
        values.add(Integer.toString(rating));
        columns.add("img_added");
        values.add(Long.toString(System.currentTimeMillis()));
        if (tags != null && !tags.isEmpty()) {
            columns.add("img_tags");
            values.add("'" + tags + "'");
        }

        statement.executeUpdate("INSERT INTO imgs (" + String.join(",", columns) + ") VALUES (" + String.join(",", values) + ");");

        if (!isBatch) notifyChangeListeners();
    }

    public synchronized void addImage(@NotNull String path) throws SQLException {
        addImage(path, null, 0, " tagme ", false);
    }

    public synchronized void addBatchImages(Iterable<File> files) throws SQLException {
        for (File file : files) {
            addImage(file.getAbsolutePath(), null, 0, " tagme ", true);
        }

        notifyChangeListeners();
    }

    public synchronized void addTag(Iterable<ImgInfo> imgs, String tag, boolean isBatch) throws SQLException {
        for (ImgInfo img : imgs) {
            if (!Arrays.asList(img.getTags()).contains(tag)) {
                img.addTag(tag);
                statement.addBatch("UPDATE imgs SET img_tags='" + imgTagsArrayToString(img.getTags()) + "' WHERE img_id=" + img.getId() + ";");
            }
        }

        statement.executeBatch();
        if (!isBatch) notifyChangeListeners();
    }

    public synchronized void removeTag(Iterable<ImgInfo> imgs, String tag, boolean isBatch) throws SQLException {
        for (ImgInfo img : imgs) {
            if (Arrays.asList(img.getTags()).contains(tag)) {
                img.removeTag(tag);
                statement.addBatch("UPDATE imgs SET img_tags='" + imgTagsArrayToString(img.getTags()) + "' WHERE img_id=" + img.getId() + ";");
            }
        }

        statement.executeBatch();
        if (!isBatch) notifyChangeListeners();
    }

    public static String imgTagsArrayToString(String[] tags) {
        return " " + String.join(" ", tags) + " ";
    }

    public static String[] imgTagsStringToArray(String img_tags) {
        return img_tags.trim().split(" ");
    }

    public synchronized void disconnect() throws SQLException {
        connection.close();
    }

    public synchronized void cleanDB() throws SQLException {
        dropTables();
        verifyTables();
        notifyChangeListeners();
    }

    public synchronized boolean isConnected() {
        try {
            return !connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void addChangeListener(ImageDatabaseUpdateListener listener) {
        listeners.add(listener);
    }

    public synchronized void removeChangeListener(ImageDatabaseUpdateListener listener) {
        listeners.remove(listener);
    }

    public synchronized void notifyChangeListeners() {
        listeners.forEach(ImageDatabaseUpdateListener::databaseUpdated);
    }

    private synchronized ImgInfo getCachedImg(int id) {
        ListIterator<SoftReference<ImgInfo>> iter = imgs.listIterator();
        while (iter.hasNext()) {
            ImgInfo info = iter.next().get();
            if (info != null) {
                if (info.getId() == id) {
                    return info;
                }
            } else {
                iter.remove();
            }
        }

        return null;
    }

    public synchronized void removeImgs(Iterable<ImgInfo> imgs) throws SQLException {
        for (ImgInfo img : imgs) {
            statement.addBatch("DELETE FROM imgs WHERE img_id=" + img.getId());
        }
        statement.executeBatch();
        notifyChangeListeners();
    }

    public synchronized int getNumImages() throws SQLException {
        ResultSet rs = statement.executeQuery("SELECT count(img_id) AS count FROM imgs;");
        if (rs.next()) {
            return rs.getInt("count");
        } else {
            return 0;
        }
    }

}
