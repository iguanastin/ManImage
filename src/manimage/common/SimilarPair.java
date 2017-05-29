package manimage.common;


public class SimilarPair {

    private final double similarity;
    private final ImageInfo first, second;

    public SimilarPair(ImageInfo first, ImageInfo second, double similarity) {
        this.first = first;
        this.second = second;
        this.similarity = similarity;
    }

    public double getSimilarity() {
        return similarity;
    }

    public ImageInfo getFirst() {
        return first;
    }

    public ImageInfo getSecond() {
        return second;
    }

}
