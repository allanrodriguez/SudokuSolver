#ifndef READ_CELLS_H
#define READ_CELLS_H

#include "core.h"

void prepareCellsForOcr(const cv::Rect& puzzle, const std::vector<cv::Rect>& squares, std::array<Cell, 81>& cells);

void readCells(const cv::Mat& image, const std::string& trainDataFilePath, std::array<Cell, 81>& cells);

#endif
