package manimage.common;


public class SimilarPair {

    private final ImageInfo img1, img2;
    private final double similarity;

    public SimilarPair(ImageInfo img1, ImageInfo img2, double similarity) {
        this.img1 = img1;
        this.img2 = img2;
        this.similarity = similarity;
    }

    public ImageInfo getImage1() {
        return img1;
    }

    public ImageInfo getImage2() {
        return img2;
    }

    public double getSimilarity() {
        return similarity;
    }

}
