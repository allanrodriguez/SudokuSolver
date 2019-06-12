#ifndef CORE_H
#define CORE_H

#include <opencv2/core.hpp>

struct Cell {
    bool isSquareSet = false;
    int value = 0;
    cv::Rect square;
};

enum class LogPriority { debug, error, info, verbose, warn };

void log(const LogPriority& priority, const std::string& tag, const std::string& message);

#endif
