package manimage.common;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

public final class ImageHistogram {

    private final double[] alpha = new double[256];
    private final double[] red = new double[256];
    private final double[] green = new double[256];
    private final double[] blue = new double[256];

    public ImageHistogram(final Image image) throws HistogramReadException {
        if (image.isBackgroundLoading() && image.getProgress() != 1) {
            Object lock = new Object();
            synchronized (lock) {
                image.progressProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue.equals(1)) lock.notifyAll();
                });
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        final long pixelCount = (long) (image.getWidth() * image.getHeight());

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
            alpha[i] /= pixelCount;
            red[i] /= pixelCount;
            green[i] /= pixelCount;
            blue[i] /= pixelCount;
        }
    }

    public ImageHistogram(String parse) {
        for (int i = 0; i < 256; i++) {
            alpha[i] = Double.parseDouble(parse.substring(0, parse.indexOf(' ')));
            parse = parse.substring(parse.indexOf(' ') + 1);
        }
        for (int i = 0; i < 256; i++) {
            red[i] = Double.parseDouble(parse.substring(0, parse.indexOf(' ')));
            parse = parse.substring(parse.indexOf(' ') + 1);
        }
        for (int i = 0; i < 256; i++) {
            green[i] = Double.parseDouble(parse.substring(0, parse.indexOf(' ')));
            parse = parse.substring(parse.indexOf(' ') + 1);
        }
        for (int i = 0; i < 256; i++) {
            blue[i] = Double.parseDouble(parse.substring(0, parse.indexOf(' ')));
            parse = parse.substring(parse.indexOf(' ') + 1);
        }
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ImageHistogram) {
            ImageHistogram hist = (ImageHistogram) obj;
            if (hist == this) return true;

            for (int i = 0; i < alpha.length; i++) {
                if (hist.alpha[i] != alpha[i] || hist.red[i] != red[i] || hist.green[i] != green[i] || hist.blue[i] != blue[i]) return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (double d : alpha) sb.append(d).append(" ");
        for (double d : red) sb.append(d).append(" ");
        for (double d : green) sb.append(d).append(" ");
        for (double d : blue) sb.append(d).append(" ");
        return sb.toString();
    }

}
