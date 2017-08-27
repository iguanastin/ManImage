package manimage.common;


import com.sun.istack.internal.NotNull;

import java.io.File;
import java.lang.ref.SoftReference;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ListIterator;

public class DBInterface {

    private static final String SQL_INITIALIZE_TABLES = "CREATE TABLE imgs(img_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, img_path NVARCHAR(1024) UNIQUE, img_src NVARCHAR(1024), img_added LONG NOT NULL, img_tags NVARCHAR(1024));";

    private final Connection connection;

    private final ArrayList<ImageDatabaseUpdateListener> listeners = new ArrayList<>();

    final private ArrayList<SoftReference<ImageInfo>> imgs = new ArrayList<>();


    public DBInterface(String path, String username, String password) throws SQLException {
        System.out.println("Attempting to connect to: [jdbc:h2:" + path + "] With user/password: " + username + "/" + password);
        connection = DriverManager.getConnection("jdbc:h2:" + path, username, password);
        System.out.println("Connected successfully");

        verifyTables();
    }

    private synchronized void verifyTables() throws SQLException {
        System.out.println("Attempting to verify table...");
        if (!tableExists("imgs", "img_id", "img_path", "img_src", "img_added", "img_tags")) {
            dropTables();
            System.out.println("Initializing tables...");
            Statement state = connection.createStatement();
            state.executeUpdate(SQL_INITIALIZE_TABLES);
            state.close();
        }
        System.out.println("Tables successfully verified");
    }

    private synchronized void dropTables() throws SQLException {
        System.out.println("Attempting to drop tables...");
        Statement state = connection.createStatement();
        state.executeUpdate("DROP TABLE IF EXISTS imgs;");
        state.close();
    }

    private synchronized boolean tableExists(String name, String... columns) {
        try {
            Statement state = connection.createStatement();
            state.executeQuery("SELECT TOP 1 " + String.join(",", columns) + " FROM " + name);
            state.close();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized ArrayList<ImageInfo> getImages(int limit, int offset, OrderBy order, String[] tags, String pathContains) throws SQLException {
        StringBuilder query = new StringBuilder("SELECT imgs.* FROM imgs ");

        final ArrayList<String> whereParts = new ArrayList<>();

        if (tags != null && tags.length > 0) {
            tags = tags.clone();
            final String[] sqlTagArr = tags.clone();
            for (int i = 0; i < sqlTagArr.length; i++) {
                if (sqlTagArr[i].charAt(0) == '-') {
                    sqlTagArr[i] = "img_tags NOT LIKE ?";
                    tags[i] = tags[i].substring(1);
                }
                else sqlTagArr[i] = "img_tags LIKE ?";
            }
            whereParts.add(String.join(" AND ", sqlTagArr));
        }

        if (pathContains != null && !pathContains.isEmpty()) {
            whereParts.add("img_path LIKE ?");
        }

        if (!whereParts.isEmpty()) {
            query.append("WHERE ").append(String.join(" AND ", whereParts));
        }

        if (order != null) query.append(" ORDER BY ").append(order);
        if (limit > 0) query.append(" LIMIT ").append(limit);
        if (offset > 0) query.append(" OFFSET ").append(offset);
        query.append(";");

        PreparedStatement state = connection.prepareStatement(query.toString());
        int i = 1;

        if (tags != null && tags.length > 0) {
            for (String tag : tags) {
                state.setNString(i, "% " + tag + " %");
                i++;
            }
        }

        if (pathContains != null && !pathContains.isEmpty()) {
            state.setNString(i, "%" + pathContains + "%");
        }

        final ResultSet rs = state.executeQuery();
        final ArrayList<ImageInfo> results = new ArrayList<>();
        while (rs.next()) {
            ImageInfo img = getCachedImg(rs.getInt("img_id"));
            if (img == null) {
                img = new ImageInfo(rs.getInt("img_id"), rs.getLong("img_added"), rs.getNString("img_src"), rs.getNString("img_path"), imgTagsStringToArray(rs.getNString("img_tags")));
            } else {
                img.update(rs.getNString("img_src"), rs.getNString("img_path"), imgTagsStringToArray(rs.getNString("img_tags")));
            }
            results.add(img);
        }
        state.close();

        return results;
    }

    private synchronized void addImage(@NotNull String path, String src, String tags, boolean isBatch) throws SQLException {
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<String> strings = new ArrayList<>();

        if (path != null && !path.isEmpty()) {
            PreparedStatement state = connection.prepareStatement("SELECT TOP 1 img_id FROM imgs WHERE img_path=?");
            state.setNString(1, path);
            ResultSet rs = state.executeQuery();
            if (rs.next()) {
                return;
            }
            state.close();
            columns.add("img_path");
            strings.add(path);
        }
        if (src != null && !src.isEmpty()) {
            columns.add("img_src");
            strings.add(src);
        }
        if (tags != null && !tags.isEmpty()) {
            columns.add("img_tags");
            strings.add(tags);
        }
        columns.add("img_added");

        PreparedStatement state = connection.prepareStatement("INSERT INTO imgs (" + String.join(",", columns) + ") VALUES (" + String.join(",", Collections.nCopies(strings.size() + 1, "?")) + ")");
        int i = 1;
        for (String str : strings) {
            state.setNString(i, str);
            i++;
        }
        state.setLong(i, System.currentTimeMillis());
        state.executeUpdate();
        state.close();

        if (!isBatch) notifyChangeListeners();
    }

    public synchronized void addImage(@NotNull String path) throws SQLException {
        addImage(path, null, " tagme ", false);
    }

    public synchronized void addBatchImages(Iterable<File> files) throws SQLException {
        for (File file : files) {
            addImage(file.getAbsolutePath(), null, " tagme ", true);
        }

        notifyChangeListeners();
    }

    public synchronized void addTag(Iterable<ImageInfo> imgs, String tag, boolean isBatch) throws SQLException {
        PreparedStatement state = connection.prepareStatement("UPDATE imgs SET img_tags=? WHERE img_id=?;");
        for (ImageInfo img : imgs) {
            if (!Arrays.asList(img.getTags()).contains(tag)) {
                img.addTag(tag);
                state.setNString(1, imgTagsArrayToString(img.getTags()));
                state.setInt(2, img.getId());
                state.executeUpdate();
            }
        }
        state.close();

        if (!isBatch) notifyChangeListeners();
    }

    public synchronized void removeTag(Iterable<ImageInfo> imgs, String tag, boolean isBatch) throws SQLException {
        PreparedStatement state = connection.prepareStatement("UPDATE imgs SET img_tags=? WHERE img_id=?;");
        for (ImageInfo img : imgs) {
            if (Arrays.asList(img.getTags()).contains(tag)) {
                img.removeTag(tag);
                state.setNString(1, imgTagsArrayToString(img.getTags()));
                state.setInt(2, img.getId());
                state.executeUpdate();
            }
        }
        state.close();

        if (!isBatch) notifyChangeListeners();
    }

    public static String imgTagsArrayToString(String[] tags) {
        return (" " + String.join(" ", tags) + " ").toLowerCase();
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

    private synchronized ImageInfo getCachedImg(int id) {
        ListIterator<SoftReference<ImageInfo>> iter = imgs.listIterator();
        while (iter.hasNext()) {
            ImageInfo info = iter.next().get();
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

    public synchronized void removeImgs(Iterable<ImageInfo> imgs) throws SQLException {
        PreparedStatement state = connection.prepareStatement("DELETE FROM imgs WHERE img_id=?");
        for (ImageInfo img : imgs) {
            state.setInt(1, img.getId());
            state.executeUpdate();
        }
        state.close();
        notifyChangeListeners();
    }

    public synchronized int getNumImages() throws SQLException {
        Statement state = connection.createStatement();
        ResultSet rs = state.executeQuery("SELECT count(img_id) AS count FROM imgs;");
        if (rs.next()) {
            int count = rs.getInt("count");
            state.close();
            return count;
        } else {
            return 0;
        }
    }

}
