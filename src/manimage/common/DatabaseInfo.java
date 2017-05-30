package manimage.common;


public abstract class DatabaseInfo {

    private final int id;

    private boolean toBeDeleted = false, toBeInserted = false, inserted = false;

    public DatabaseInfo(int id, boolean isInserted) {
        this.id = id;
        this.inserted = isInserted;
    }

    //------------------ Getters ---------------------------------------------------------------------------------------

    public int getId() {
        return id;
    }

    public abstract int buildSQLUpdates(StringBuilder query);

    //------------------- Setters --------------------------------------------------------------------------------------

    public synchronized void setToBeDeleted() {
        this.toBeDeleted = true;
    }

    public synchronized void setToBeInserted() {
        this.toBeInserted = true;
    }

    //----------------- Checkers ---------------------------------------------------------------------------------------

    public synchronized boolean isToBeDeleted() {
        return toBeDeleted;
    }

    public synchronized boolean isToBeInserted() {
        return toBeInserted;
    }

    public synchronized boolean isInserted() {
        return inserted;
    }

    public synchronized boolean isSynchronized() {
        return !toBeDeleted && !toBeInserted && inserted;
    }

    //---------------- Operators ---------------------------------------------------------------------------------------

    public synchronized void markAsCommitted() {
        if (isToBeDeleted()) inserted = false;
        else if (isToBeInserted()) inserted = true;

        toBeDeleted = toBeInserted = false;
    }

}
