package manimage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class ImageSet {

    private final ArrayList<ImageInfo> images = new ArrayList<>();

    private final ArrayList<ImageSetListener> listeners = new ArrayList<>();


    //------------- Modifiers ------------------------------------------------------------------------------------------

    boolean addImage(ImageInfo info) {
        if (!hasImage(info)) {
            images.add(info);

            listeners.forEach(listener -> listener.onImageAdded(info));

            return true;
        }

        return false;
    }

    boolean removeImage(ImageInfo info) {
        if (hasImage(info)) {
            images.remove(info);

            listeners.forEach(listener -> listener.onImageRemoved(info));

            return true;
        }

        return false;
    }

    boolean initAndAddImage(File file) {
        ImageInfo info = new ImageInfo(file);

        return addImage(info);
    }

    //--------------- Getters ------------------------------------------------------------------------------------------

    ArrayList<ImageInfo> getInfoList() {
        return images;
    }

    ArrayList<ImageInfo> getDisconnectedImageInfos() {
        ArrayList<ImageInfo> result = new ArrayList<>();

        images.forEach(info -> {
            if (!info.getFile().exists()) {
                result.add(info);
            }
        });

        return result;
    }

    ImageInfo getInfoFor(File file) {
        for (ImageInfo info : images) {
            if (info.getFile().equals(file)) {
                return info;
            }
        }

        return null;
    }

    //---------------- Checkers ----------------------------------------------------------------------------------------

    boolean hasImage(ImageInfo info) {
        return images.contains(info);
    }

    //----------------- Listeners --------------------------------------------------------------------------------------

    void addListener(ImageSetListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    boolean removeListener(ImageSetListener listener) {
        return listeners.remove(listener);
    }

    void clearListeners() {
        listeners.clear();
    }

}
