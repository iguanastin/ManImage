package manimage.common;


public class TagInfo extends DatabaseInfo {

    private final String name;

    public TagInfo(int id, String name, boolean isInserted) {
        super(id, isInserted);
        this.name = name;
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

    @Override
    public int buildSQLUpdates(StringBuilder query) {
        if (isSynchronized()) return 0;

        if (isToBeDeleted()) {
            query.append("INSERT INTO ").append(ImageDatabase.SQL_TAGS_TABLE).append(" (").append(ImageDatabase.SQL_TAG_ID).append(',').append(ImageDatabase.SQL_TAG_NAME).append(") VALUES (");
            query.append(getId()).append(',').append(getSQLSafeName()).append(");\n");
        } else if (isToBeInserted()) {
            query.append("DELETE FROM ").append(ImageDatabase.SQL_TAGS_TABLE).append(" WHERE ").append(ImageDatabase.SQL_TAG_ID).append('=').append(getId()).append(";\n");
        }

        return 1;
    }

}
