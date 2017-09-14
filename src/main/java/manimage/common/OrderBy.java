package manimage.common;


public class OrderBy {

    public String primaryOrder = "img_added";
    public boolean primaryDescending = true;

    public String secondaryOrder = "img_id";
    public boolean secondaryDescending = true;

    public OrderBy(String primary, boolean primaryDescending, String secondary, boolean secondaryDescending) {
        this.primaryOrder = primary;
        this.primaryDescending = primaryDescending;
        this.secondaryOrder = secondary;
        this.secondaryDescending = secondaryDescending;
    }

    public OrderBy() {

    }

    @Override
    public String toString() {
        String result = primaryOrder;
        if (primaryDescending) result += " DESC";
        result += ", " + secondaryOrder;
        if (secondaryDescending) result += " DESC";

        return result;
    }

}
