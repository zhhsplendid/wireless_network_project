package wireless.gesture;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * Created by zhhsp on 10/24/2015.
 */
public class DTWLib {
//how to use this class:
//1. Set "recordFlag" to true to record gestures in a file as template. In this mode, input gestures will be written to files named "$index.umv"
//and become templates for recognition. The number of templates is fixed and defined as "NUM_TEMPLATES".
//Set "recordFlag" to false to enable recognition. Recognition result will be returned by endGesture() as the index of the template.
//
//2. When a gesture begins, call beginGesture(); when the gesture finishes, call endGesture();
//put the acc samples during a gesture into accBuffer like the following(x, y, z are the acceleration in g):
//	accBuffer[accIndex][0] = x*EARTH_GRAVITY;
//	accBuffer[accIndex][1] = y*EARTH_GRAVITY;
//	accBuffer[accIndex][2] = (z-1)*EARTH_GRAVITY;
//	accIndex++;
//Make sure (accIndex < MAX_ACC_LEN)!!!!!!!!!!!!!!!!!!!!
//beginGesture() allocates memory and initializes variables; endGesture() record/recognize the input gesture and output results
//
    public boolean recordFlag = true;
    public static final double EARTH_GRAVITY = 9.8;
    public static int NUM_TEMPLATES = 20;

    public static final int DIMENSION = 3;
    final int QUAN_WIN_SIZE = 8;
    final int QUAN_MOV_STEP = 4;
    final int MAX_ACC_LEN = 500;

    public double accBuffer[][];
    public int accIndex;
    int tempIndex = 0;

    public void recordMode() {
        recordFlag = true;
    }

    public void detectMode() {
        recordFlag = false;
    }

    public void beginGesture() {
        accBuffer = new double[MAX_ACC_LEN][DIMENSION];
        accIndex = 0;
    }

    public void addAccerelation(double acce[]) {
        //We ignore too many accerelations.
        if(accIndex < MAX_ACC_LEN) {
            for (int i = 0; i < DIMENSION; ++i) {
                accBuffer[accIndex][i] = acce[i];
            }
            ++accIndex;
        }
    }

    public int endGesture() {
        int ret = -1;
        if(recordFlag) {
            writeFile(accBuffer, accIndex, tempIndex);
            ++tempIndex;
        }
        else {
            accIndex = quantizeAcc(accBuffer, accIndex);
            Gesture templates[] = new Gesture[NUM_TEMPLATES];
            for(int i = 0; i < NUM_TEMPLATES; ++i) {
                templates[i] = readFile(i);
                templates[i].length = quantizeAcc(templates[i].data, templates[i].length);
            }
            ret = detectGesture(accBuffer, accIndex, templates, NUM_TEMPLATES);
        }
        return ret;
    }

    int writeFile(double data[][], int length, int index) {
        String fileName = Integer.toString(index) + ".uwv";
        try {
            BufferedWriter fout = new BufferedWriter(new FileWriter(fileName));
            for(int i = 0; i < length; ++i) {
                for(int j = 0; j < DIMENSION; ++j) {
                    fout.write(data[i][j] + " ");
                }
                fout.newLine();
            }
            fout.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    Gesture readFile(int index) {
        String fileName = Integer.toString(index) + ".uwv";
        double data[][] = new double[MAX_ACC_LEN][DIMENSION];
        int dCount = 0;
        try {
            BufferedReader fin = new BufferedReader(new FileReader(fileName));
            String line;
            String numberStrs[];
            do {
                line = fin.readLine();
                numberStrs = line.trim().split("\\s+");
                for(int i = 0; i < DIMENSION; ++i) {
                    data[dCount][i] = Double.valueOf(numberStrs[i]);
                }
                ++dCount;
            } while(line != null);
            fin.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        Gesture ret = new Gesture(dCount, DIMENSION);
        for(int i = 0; i < dCount; ++i) {
            for(int j = 0; j < DIMENSION; ++j) {
                ret.data[i][j] = data[i][j];
            }
        }
        return ret;
    }

    int quantizeAcc(double accData[][], int length) {
        int i = 0, j, k = 0, l, window = QUAN_WIN_SIZE, sum;
        double temp[][] = new double[length / QUAN_MOV_STEP + 1][DIMENSION];
        //take moving window average

        while(i < length) {
            if( i + window > length)
                window = length - i;
            for( l = 0; l < DIMENSION; l++) {
                sum = 0;
                for( j = i; j < window+i; j++)
                    sum += accData[j][l];
                temp[k][l] = sum * 1.0/window;
            }
            k++;
            i += QUAN_MOV_STEP;
        }//while
        //nonlinear quantization and copy quantized value to original buffer
        for( i = 0; i < k; i++)
            for( l = 0; l < DIMENSION; l++) {
                if( temp[i][l] > 10 ) {
                    if( temp[i][l] > 20)
                        temp[i][l] = 16;
                    else
                        temp[i][l] = 10 + (temp[i][l]-10)/10*5;
                } else if( temp[i][l] < -10) {
                    if( temp[i][l] < -20)
                        temp[i][l] = -16;
                    else
                        temp[i][l] = -10 + (temp[i][l] + 10)/10*5;
                }
                accData[i][l] = temp[i][l];
            }
        return k;
    }

    public int detectGesture(double input[][], int length, Gesture templates[], int templateNum) {
        if( length <= 0)
            return -1;
        int ret = 0;

        int distances[] = new int [NUM_TEMPLATES];
        //int table[MAX_ACC_LEN/QUAN_MOV_STEP*MAX_ACC_LEN/QUAN_MOV_STEP];
        int table[];
        for(int i = 0; i < templateNum; ++i) {
            table = new int[length * templates[i].length];
            for(int j = 0; j < table.length; ++j) {
                table[j] = -1;
            }
            distances[i] = DTWdistance(input, length, templates[i].data, templates[i].length, length - 1, templates[i].length - 1, table);
            distances[i] /= (length + templates[i].length);
        }

        for(int i = 1; i < templateNum; i++) {
            if( distances[i] < distances[ret]) {
                ret = i;
            }
        }
        return ret;
    }

    int DTWdistance(double sample1[][], int length1, double sample2[][], int length2, int i, int j, int table[]) {

        if( i < 0 || j < 0)
            return 100000000;
        int tableWidth = length2;
        int localDistance = 0;
        int k;
        for( k = 0; k < DIMENSION; k++)
            localDistance += ((sample1[i][k]-sample2[j][k])*(sample1[i][k]-sample2[j][k]));

        int sdistance, s1, s2, s3;

        if( i == 0 && j == 0) {
            if( table[i*tableWidth+j] < 0)
                table[i*tableWidth+j] = localDistance;
            return localDistance;
        } else if( i==0) {
            if( table[i*tableWidth+(j-1)] < 0)
                sdistance = DTWdistance(sample1, length1, sample2, length2, i, j-1, table);
            else
                sdistance = table[i*tableWidth+j-1];
        } else if( j==0) {
            if( table[(i-1)*tableWidth+ j] < 0)
                sdistance = DTWdistance(sample1, length1, sample2, length2, i-1, j, table);
            else
                sdistance = table[(i-1)*tableWidth+j];
        } else {
            if( table[i*tableWidth+(j-1)] < 0)
                s1 = DTWdistance(sample1, length1, sample2, length2, i, j-1, table);
            else
                s1 = table[i*tableWidth+(j-1)];
            if( table[(i-1)*tableWidth+ j] < 0)
                s2 = DTWdistance(sample1, length1, sample2, length2, i-1, j, table);
            else
                s2 = table[(i-1)*tableWidth+ j];
            if( table[(i-1)*tableWidth+ j-1] < 0)
                s3 = DTWdistance(sample1, length1, sample2, length2, i-1, j-1, table);
            else
                s3 = table[(i-1)*tableWidth+ j-1];
            sdistance = s1 < s2 ? s1:s2;
            sdistance = sdistance < s3 ? sdistance:s3;
        }
        table[i*tableWidth+j] = localDistance + sdistance;
        return table[i*tableWidth+j];
    }

}
