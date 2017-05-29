package manimage.common;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.ArrayList;

public final class ImageHistogram {

    private final double[] alpha = new double[256];
    private final double[] red = new double[256];
    private final double[] green = new double[256];
    private final double[] blue = new double[256];

    private final long pixelCount;


    private ImageHistogram(Image image) throws HistogramReadException {
        if (image.isBackgroundLoading()) {
            final Thread thisThread = Thread.currentThread();
            image.progressProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals(1)) thisThread.notify(); //Notify this thread as soon as the image finishes loading
            });

            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        pixelCount = (long) (image.getWidth() * image.getHeight());

        for (int i = 0; i < 256; i++) {
            alpha[i] = red[i] = green[i] = blue[i] = 0;
        }

        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            throw new HistogramReadException();
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = pixelReader.getArgb(x, y);
                int a = (0xff & (argb >> 24));
                int r = (0xff & (argb >> 16));
                int g = (0xff & (argb >> 8));
                int b = (0xff & argb);

                alpha[a]++;
                red[r]++;
                green[g]++;
                blue[b]++;
            }
        }

        for (int i = 0; i < 256; i++) {
            alpha[i] /= getPixelCount();
            red[i] /= getPixelCount();
            green[i] /= getPixelCount();
            blue[i] /= getPixelCount();
        }
    }

    public long getPixelCount() {
        return pixelCount;
    }

    public static ImageHistogram getHistogram(Image image) throws HistogramReadException {
        if (image == null) return null;

        return new ImageHistogram(image);
    }

    public double getSimilarity(ImageHistogram other) {
        double da = 0, dr = 0, dg = 0, db = 0;

        for (int i = 0; i < 256; i++) {
            da += Math.abs(alpha[i] - other.alpha[i]);
            dr += Math.abs(red[i] - other.red[i]);
            dg += Math.abs(green[i] - other.green[i]);
            db += Math.abs(blue[i] - other.blue[i]);
        }

        return 1 - (da + dr + dg + db) / 8;
    }

    public boolean isSimilar(ImageHistogram other, double confidence) {
        return getSimilarity(other) >= confidence;
    }

    public static ArrayList<SimilarPair> getDuplicates(ArrayList<ImageInfo> infos, double confidence) {
        ArrayList<SimilarPair> results = new ArrayList<>();

        for (int i = 0; i < infos.size(); i++) {
            for (int j = i + 1; j < infos.size(); j++) {
                ImageHistogram hist1 = infos.get(i).getHistogram(), hist2 = infos.get(j).getHistogram();
                if (hist1 != null && hist2 != null) {
                    double similarity = hist1.getSimilarity(hist2);
                    if (similarity > confidence) {
                        results.add(new SimilarPair(infos.get(i), infos.get(j), similarity));
                    }
                }
            }
        }

        return results;
    }

}
