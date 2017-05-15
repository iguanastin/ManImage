package manimage.common;


import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import org.apache.commons.math3.stat.inference.ChiSquareTest;

import java.util.Arrays;

public final class ImageHistogram {

    private final long[] red = new long[256];
    private final long[] green = new long[256];
    private final long[] blue = new long[256];


    private ImageHistogram(Image image) throws HistogramReadException {
        for (int i = 0; i < 256; i++) {
            red[i] = green[i] = blue[i] = 0;
        }

        PixelReader pixelReader = image.getPixelReader();
        if (pixelReader == null) {
            throw new HistogramReadException();
        }

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = pixelReader.getArgb(x, y);
                int r = (0xff & (argb >> 16));
                int g = (0xff & (argb >> 8));
                int b = (0xff & argb);

                red[r]++;
                green[g]++;
                blue[b]++;
            }
        }
    }

    public static ImageHistogram getHistogram(Image image) throws HistogramReadException {
        if (image == null) return null;

        return new ImageHistogram(image);
    }

    public double getSimilarity(ImageHistogram other) {
        for (int i = 0; i < 256; i++) {
            if (red[i] == 0 && other.red[i] == 0) red[i] = other.red[i] = 1;
            if (green[i] == 0 && other.green[i] == 0) green[i] = other.green[i] = 1;
            if (blue[i] == 0 && other.blue[i] == 0) blue[i] = other.blue[i] = 1;
        }

        ChiSquareTest test = new ChiSquareTest();
        double pRed = test.chiSquareTestDataSetsComparison(red, other.red);
        double pGreen = test.chiSquareTestDataSetsComparison(green, other.green);
        double pBlue = test.chiSquareTestDataSetsComparison(blue, other.blue);

        return (pRed + pGreen + pBlue)/3;
    }

    public boolean isSimilar(ImageHistogram other, double alpha) {
        return getSimilarity(other) <= alpha;
    }

}
