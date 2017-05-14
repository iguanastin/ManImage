package manimage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ImageSet {

    private final ArrayList<ImageInfo> images = new ArrayList<>();

    private final ArrayList<ImageSetListener> listeners = new ArrayList<>();


    //------------------ Loader/Savers ---------------------------------------------------------------------------------

    void loadXML(File file) throws IOException {
        //TODO: Implement XML loading
    }

    void saveXML(File file) throws IOException {
        //TODO: Implement XML saving
    }

    //------------- Modifiers ------------------------------------------------------------------------------------------

    boolean add(ImageInfo info) {
        if (!hasImage(info)) {
            images.add(info);

            listeners.forEach(listener -> listener.onImageAdded(info));

            return true;
        }

        return false;
    }

    void addAll(ArrayList<ImageInfo> infos) {
        infos.forEach(this::add);
    }

    boolean remove(ImageInfo info) {
        if (hasImage(info)) {
            images.remove(info);

            listeners.forEach(listener -> listener.onImageRemoved(info));

            return true;
        }

        return false;
    }

    boolean initAndAdd(File file) {
        if (!Main.IMAGE_FILTER.accept(file)) return false;

        ImageInfo info = new ImageInfo(file);

        return add(info);
    }

    void initAndAddAll(List<File> files) {
        files.forEach(this::initAndAdd);
    }

    void initAndAddSubfiles(File folder, boolean recurse) {
        if (folder.isDirectory()) {
            File[] files;

            if (recurse) {
                 files = folder.listFiles(Main.IMAGE_AND_DIRECTORY_FILTER);
            } else {
                 files = folder.listFiles(Main.IMAGE_FILTER);
            }

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        initAndAddSubfiles(file, recurse);
                    } else {
                        initAndAdd(file);
                    }
                }
            }
        }
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
