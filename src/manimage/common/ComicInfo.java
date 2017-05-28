package manimage.common;

import com.sun.istack.internal.NotNull;

public class ComicInfo {

    private final int id;
    private final long timeAdded;
    private String name;
    private String source;

    private boolean nameChanged = false, sourceChanged = false;
    private boolean inserted = false;
    private boolean toBeInserted = false;
    private boolean toBeDeleted = false;


    public ComicInfo(int id, String name, String source, long timeAdded) {
        this.id = id;
        this.name = name;
        this.source = source;
        this.timeAdded = timeAdded;
        inserted = true;
        toBeInserted = false;
        toBeDeleted = false;
    }

    public ComicInfo(int id, String name) {
        this.id = id;
        this.name = name;
        this.source = null;
        this.timeAdded = System.currentTimeMillis();
        inserted = false;
        toBeInserted = true;
        toBeDeleted = false;
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

    public boolean isInserted() {
        return inserted;
    }

    public boolean isToBeInserted() {
        return toBeInserted;
    }

    public boolean isToBeDeleted() {
        return toBeDeleted;
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

    public void setAsUpdated() {
        nameChanged = sourceChanged = toBeDeleted = toBeInserted = false;
    }

    public void setInserted(boolean b) {
        inserted = b;
    }

    public void setToBeInserted(boolean toBeInserted) {
        this.toBeInserted = toBeInserted;
    }

    public void setToBeDeleted(boolean toBeDeleted) {
        this.toBeDeleted = toBeDeleted;
    }

}
