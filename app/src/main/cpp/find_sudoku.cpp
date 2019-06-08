#include "find_sudoku.h"

#include <algorithm>
#include <array>
#include <iostream>
#include <vector>

#include <opencv2/imgproc.hpp>

using namespace cv;
using namespace std;

static const int thresh = 50;
static const int N = 128;

static double angle(const Point& pt1, const Point& pt2, const Point& pt0) {
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;

    return (dx1 * dx2 + dy1 * dy2) / sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2));
}

static void filterCells(const Rect& puzzle, vector<Rect>& cells) {
    double puzzleArea = puzzle.area();
    double minArea = 0.8 * puzzleArea / 81;
    double maxArea = 1.25 * puzzleArea / 81;

    cells.erase(remove_if(cells.begin(), cells.end(),
                          [minArea, maxArea](Rect& a) {
                              double sqArea = a.area();
                              return sqArea < minArea || sqArea > maxArea;
                          }),
                cells.end());
}

static void pointsToRect(const vector<Point>& points, Rect& square) {
    int minX = -1;
    int maxX = -1;
    int minY = -1;
    int maxY = -1;

    for (const Point& p : points) {
        minX = minX < 0 ? p.x : min(minX, p.x);
        minY = minY < 0 ? p.y : min(minY, p.y);
        maxX = maxX < 0 ? p.x : max(maxX, p.x);
        maxY = maxY < 0 ? p.y : max(maxY, p.y);
    }

    square = Rect(minX, minY, maxX - minX, maxY - minY);
}

void findSudoku(const Mat& image, Rect& puzzle, vector<Rect>& squares) {
    squares.clear();

    double minPuzzleArea = 0.64 * image.size().area();
    double currentPuzzleArea = 0;
    Mat pyr;
    Mat timg;
    Mat gray;

    pyrDown(image, pyr);
    pyrUp(pyr, timg, image.size());
    vector<vector<Point>> contours;

    array<Mat, 3> channels;
    split(timg, channels);

    for (Mat& gray0 : channels) {
        for (int l = 0; l < N; l++) {
            if (l == 0) {
                Canny(gray0, gray, 0, thresh, 5);
                dilate(gray, gray, Mat());
            } else {
                threshold(gray0, gray, (l + 1) * 255.0 / N, 255, THRESH_BINARY);
            }

            findContours(gray, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

            vector<Point> approx;

            for (vector<Point>& contour : contours) {
                approxPolyDP(contour, approx, arcLength(contour, true) * 0.02, true);

                if (approx.size() == 4 && isContourConvex(approx)) {
                    double maxCosine = 0;

                    for (int j = 0; j < 4; j++) {
                        double cosine = abs(angle(approx[(j + 2) % 4], approx[j], approx[(j + 1) % 4]));
                        maxCosine = max(maxCosine, cosine);
                    }

                    if (maxCosine < 0.2) {
                        double currentArea = abs(contourArea(approx));
                        if (currentArea >= minPuzzleArea && (puzzle.empty() || currentPuzzleArea > currentArea)) {
                            pointsToRect(approx, puzzle);
                            currentPuzzleArea = currentArea;
                        } else if (currentArea < minPuzzleArea) {
                            Rect square;
                            pointsToRect(approx, square);
                            squares.push_back(square);
                        }
                    }
                }
            }
        }
    }

    filterCells(puzzle, squares);
}
