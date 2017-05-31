package manimage.common;

import com.sun.istack.internal.NotNull;

public class ComicInfo extends DatabaseInfo {

    private final long timeAdded;
    private String name;
    private String source;

    private boolean nameChanged = false, sourceChanged = false;

    //TODO: Implement tags
    //TODO: Implement pages


    public ComicInfo(int id, String name, String source, long timeAdded) {
        super(id, true);
        this.name = name;
        this.source = source;
        this.timeAdded = timeAdded;
    }

    public ComicInfo(int id, String name) {
        super(id, false);
        this.name = name;
        this.source = null;
        this.timeAdded = System.currentTimeMillis();
        setToBeInserted();
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    public String getSource() {
        return source;
    }

    public String getSQLSafeSource() {
        if (source == null) {
            return null;
        } else {
            return '\'' + source.replace("'", "''") + '\'';
        }
    }

    public String getName() {
        return name;
    }

    public String getSQLSafeName() {
        if (name == null) {
            return null;
        } else {
            return '\'' + name.replace("'", "''") + '\'';
        }
    }

    public long getTimeAdded() {
        return timeAdded;
    }

    @Override
    public int buildSQLUpdates(StringBuilder query) {
        if (isSynchronized()) return 0;

        if (isToBeDeleted()) {
            query.append("DELETE FROM ").append(ImageDatabase.SQL_COMICS_TABLE).append(" WHERE ").append(ImageDatabase.SQL_COMIC_ID).append('=').append(getId()).append(";\n");
        } else if (isToBeInserted()) {
            query.append("INSERT INTO ").append(ImageDatabase.SQL_COMICS_TABLE).append(" (").append(ImageDatabase.SQL_COMIC_ID).append(',').append(ImageDatabase.SQL_COMIC_NAME).append(',');
            query.append(ImageDatabase.SQL_COMIC_SOURCE).append(',').append(ImageDatabase.SQL_COMIC_TIME_ADDED).append(") VALUES (").append(getId()).append(',').append(getSQLSafeName()).append(',');
            query.append(getSQLSafeSource()).append(',').append(getTimeAdded()).append(");\n");
        } else if (isModified()) {
            query.append("UPDATE ").append(ImageDatabase.SQL_COMICS_TABLE).append(" SET ");
            boolean comma = false;
            if (isNameChanged()) {
                query.append(ImageDatabase.SQL_COMIC_NAME).append('=').append(getSQLSafeName());
                comma = true;
            }
            if (isSourceChanged()) {
                if (comma) query.append(',');
                query.append(ImageDatabase.SQL_COMIC_SOURCE).append('=').append(getSQLSafeSource());
            }
            query.append(" WHERE ").append(ImageDatabase.SQL_COMIC_ID).append('=').append(getId()).append(";\n");
        }

        return 1;
    }

    //------------------ Checkers --------------------------------------------------------------------------------------

    public boolean isNameChanged() {
        return nameChanged;
    }

    public boolean isSourceChanged() {
        return sourceChanged;
    }

    boolean isModified() {
        return nameChanged || sourceChanged;
    }

    //---------------------- Setters -----------------------------------------------------------------------------------

    public void setSource(String source) {
        if (this.source == null && source != null) sourceChanged = true;
        else if (this.source != null && !this.source.equals(source)) sourceChanged = true;
        this.source = source;
    }

    @NotNull
    public void setName(String name) {
        if (!this.name.equals(name)) nameChanged = true;
        this.name = name;
    }

    @Override
    public synchronized void markAsCommitted() {
        super.markAsCommitted();
        sourceChanged = nameChanged = false;
    }

}
