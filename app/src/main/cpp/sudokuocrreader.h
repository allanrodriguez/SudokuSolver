#ifndef SUDOKUSOLVER_SUDOKUOCRREADER_H
#define SUDOKUSOLVER_SUDOKUOCRREADER_H

#include <array>
#include <cstring>
#include <exception>

#ifdef DEBUG
#include <sstream>
#endif

#include <android/log.h>

#include <leptonica/allheaders.h>

#include <opencv2/core/mat.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#include <tesseract/baseapi.h>

namespace SudokuSolver {
    template <typename T>
    struct Rectangle {
        T left;
        T top;
        T width;
        T height;
    };

    class SudokuOcrReader {

    public:
        const char* k_name = "SudokuOcrReader";
        const int k_num_iterations = 11;
        const double k_thresh = 50.0;

        SudokuOcrReader(const char* path_to_image, const char* path_to_tessdata_parent);
        SudokuOcrReader(const cv::Mat& image, const char* path_to_tessdata_parent);

        ~SudokuOcrReader();

        bool parse_image(std::array<std::array<int, 9>, 9>& parsed_sudoku);

    private:
        cv::Mat _image;
        const char* _path_to_image = NULL;
        const char* _path_to_tessdata_parent = NULL;

        inline double angle(cv::Point point1, cv::Point point2, cv::Point point0);

        int best_candidate_for_cell(std::array<int, 10> candidates);

        void find_squares(const cv::Mat& image, std::vector< std::vector<cv::Point> >& squares);

        void generate_rectangle_vector(const std::vector< std::vector<cv::Point> >& squares,
                                       std::vector< Rectangle<int> >& rectangles);

        inline void get_midpoint(const cv::Point& point1,
                                 const cv::Point& point2,
                                 cv::Point& midpoint);

        inline bool is_digit(char c);

        void parse_numbers_in_rectangles(const std::vector< Rectangle<int> >& squares,
                                         PIX* image,
                                         std::array<std::array<int, 9>, 9>& parsed_sudoku);

        int parse_rectangle(char* rectangle_text);

        PIX* prepare_tesseract_image(cv::Mat& image);
    };
}

#endif
