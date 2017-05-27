package manimage.common;

public class DBComicInfo {

    private final int id;
    private final long timeAdded;
    private String name;
    private String source;

    private boolean nameChanged = false, sourceChanged = false;


    public DBComicInfo(int id, String name, String source, long timeAdded) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.timeAdded = timeAdded;
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    public String getSource() {
        return source;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getTimeAdded() {
        return timeAdded;
    }

    //------------------ Checkers --------------------------------------------------------------------------------------

    public boolean isChanged() {
        return nameChanged || sourceChanged;
    }

    public boolean isNameChanged() {
        return nameChanged;
    }

    public boolean isSourceChanged() {
        return sourceChanged;
    }

    //---------------------- Setters -----------------------------------------------------------------------------------

    public void setSource(String source) {
        if (!this.source.equals(source)) sourceChanged = true;
        this.source = source;
    }

    public void setName(String name) {
        if (!this.name.equals(name)) nameChanged = true;
        this.name = name;
    }

    public void markChangesCommitted() {
        nameChanged = sourceChanged = false;
    }

}
