package wireless.gesture;

/**
 * Created by zhhsp on 10/24/2015.
 */
public class Gesture {
    public double data[][];
    public int length;

    public Gesture(int n, int d) {
        length = n;
        data = new double[n][d];
    }

    public Gesture() {
        length = 0;
    }
}
