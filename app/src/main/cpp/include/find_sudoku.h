#ifndef FIND_SUDOKU_H
#define FIND_SUDOKU_H

#include <opencv2/core.hpp>

void findSudoku(const cv::Mat& image, cv::Rect& puzzle, std::vector<cv::Rect>& squares);

#endif
