#include "read_cells.h"

#include <opencv2/imgproc.hpp>
#include <opencv2/ml.hpp>

using namespace cv;
using namespace cv::ml;
using namespace std;

static void deskew(const Mat &image, Mat &deskewedImage) {
    Size cellSize = image.size();
    Moments m = moments(image);

    if (abs(m.mu02) < 0.01) {
        deskewedImage = image.clone();
        return;
    }

    auto skew = (float)(m.mu11 / m.mu02);
    float matrixValues[2][3] = {{1, skew, -0.5f * min(cellSize.width, cellSize.height) * skew}, {0, 1, 0}};
    Mat transformationMatrix(Size(3, 2), CV_32F);

    for (int i = 0; i < transformationMatrix.rows; ++i) {
        for (int j = 0; j < transformationMatrix.cols; ++j) {
            transformationMatrix.at<float>(i, j) = matrixValues[i][j];
        }
    }

    warpAffine(image, deskewedImage, transformationMatrix, cellSize, WARP_INVERSE_MAP | INTER_LINEAR);
}

static void binCount(const Mat &x, const Mat &weights, int minLength, vector<double> &bins) {
    double maxXValue = 0;
    minMaxLoc(x, nullptr, &maxXValue);

    bins = vector<double>((unsigned int)max((int)maxXValue, minLength));

    for (int i = 0; i < x.rows; ++i) {
        for (int j = 0; j < x.cols; ++j) {
            bins[x.at<int>(i, j)] += weights.at<float>(i, j);
        }
    }
}

static void preprocessHog(const vector<Mat> &cells, Mat &hog) {
    int numBins = 16;
    int halfCellHeight = 0;
    int halfCellWidth = 0;
    double eps = 1e-7;

    hog = Mat(Size(4 * numBins, (int)cells.size()), CV_32F);

    for (int i = 0; i < cells.size(); ++i) {
        Mat gx;
        Sobel(cells[i], gx, CV_32F, 1, 0);

        Mat gy;
        Sobel(cells[i], gy, CV_32F, 0, 1);

        Mat mag;
        Mat ang;
        cartToPolar(gx, gy, mag, ang);

        Mat bin(ang.size(), CV_32S);

        for (int row = 0; row < ang.rows; ++row) {
            for (int column = 0; column < ang.cols; ++column) {
                bin.at<int>(row, column) = (int)(numBins * ang.at<float>(row, column) / (2 * CV_PI));
            }
        }

        Size cellSize = cells[i].size();
        halfCellHeight = cellSize.height / 2;
        halfCellWidth = cellSize.width / 2;

        Mat binCells[] = {
            bin(Rect(0, 0, halfCellWidth, halfCellHeight)),
            bin(Rect(halfCellWidth, 0, cellSize.width - halfCellWidth, halfCellHeight)),
            bin(Rect(0, halfCellHeight, halfCellWidth, cellSize.height - halfCellHeight)),
            bin(Rect(halfCellWidth, halfCellHeight, cellSize.width - halfCellWidth, cellSize.height - halfCellHeight))};
        Mat magCells[] = {
            mag(Rect(0, 0, halfCellWidth, halfCellHeight)),
            mag(Rect(halfCellWidth, 0, cellSize.width - halfCellWidth, halfCellHeight)),
            mag(Rect(0, halfCellHeight, halfCellWidth, cellSize.height - halfCellHeight)),
            mag(Rect(halfCellWidth, halfCellHeight, cellSize.width - halfCellWidth, cellSize.height - halfCellHeight))};

        vector<double> hist;
        hist.reserve((unsigned int)(4 * numBins));

        for (int q = 0; q < 4; ++q) {
            vector<double> partialHist;
            binCount(binCells[q], magCells[q], numBins, partialHist);
            hist.insert(hist.end(), partialHist.begin(), partialHist.end());
        }

        // transform to Hellinger kernel
        double sum = 0;
        for (double d : hist) {
            sum += d;
        }

        for (double &d : hist) {
            d = sqrt(d / (sum + eps));
        }

        double histNorm = norm(hist);

        for (int h = 0; h < hist.size(); ++h) {
            hog.at<float>(i, h) = (float)(hist[h] / (histNorm + eps));
        }
    }
}

void prepareCellsForOcr(const Rect& puzzle, const vector<Rect>& squares, array<Cell, 81>& cells) {
    Point center;
    int cellWidth = puzzle.width / 9;
    int cellHeight = puzzle.height / 9;
    int row = 0;
    int column = 0;

    for (const Rect& square : squares) {
        center.x = square.x + (square.width + 1) / 2;
        center.y = square.y + (square.height + 1) / 2;

        column = (center.x - puzzle.x) / cellWidth;
        row = (center.y - puzzle.y) / cellHeight;

        Cell& cell = cells[row * 9 + column];
        if (!cell.isSquareSet || cell.square.area() > square.area()) {
            cell.square = square;
            cell.isSquareSet = true;
        }
    }
}

void readCells(const Mat &image, const string &trainDataFilePath, array<Cell, 81> &cells) {
    Mat cellImage;
    Mat grayImage;
    Mat samples;
    int responsesPtr = 0;
    vector<Mat> deskewedImages = {};
    vector<float> responses = {};

    cvtColor(image, grayImage, COLOR_BGR2GRAY);
    threshold(grayImage, grayImage, 64, 255, THRESH_BINARY);

    for (Cell &c : cells) {
        if (c.isSquareSet) {
            Mat deskewedCell;
            cellImage = grayImage(c.square);
            deskew(cellImage, deskewedCell);
            deskewedImages.push_back(deskewedCell);
        }
    }

    preprocessHog(deskewedImages, samples);

    Ptr<SVM> model = SVM::load(trainDataFilePath);
    model->predict(samples, responses);
    model.release();

    for (Cell &c : cells) {
        if (c.isSquareSet) {
            c.value = (int)responses[responsesPtr];
            ++responsesPtr;
        }
    }
}
