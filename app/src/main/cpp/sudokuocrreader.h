#ifndef SUDOKUSOLVER_SUDOKUOCRREADER_H
#define SUDOKUSOLVER_SUDOKUOCRREADER_H

#define SUDOKU_SIZE 9

#include <array>
#include <cstring>
#include <utility>

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
    template<typename T>
    struct Rectangle {
        T left;
        T top;
        T width;
        T height;
    };

    class SudokuOcrReader {
    public:
        const char *k_name = "SudokuOcrReader";
        const int k_num_iterations = 11;
        const double k_thresh = 50.0;

        SudokuOcrReader(const cv::Mat &image, const char *path_to_tessdata_parent);

        ~SudokuOcrReader();

        bool parse_image(
                std::array<std::array<int, SUDOKU_SIZE>, SUDOKU_SIZE> &parsed_sudoku);

    private:
        cv::Mat _image;
        cv::Size _image_size;
        const char *_path_to_tessdata_parent = nullptr;

        inline double angle(cv::Point point1, cv::Point point2, cv::Point point0);

        void find_squares(std::vector<std::vector<cv::Point> > &squares);

        void generate_rectangle_vector(
                const std::vector<std::vector<cv::Point> > &squares,
                std::array<std::array<Rectangle<int>, SUDOKU_SIZE>, SUDOKU_SIZE>
                &rectangles);

        inline std::pair<int, int> get_coordinates(const Rectangle<int> &square);

        inline void get_midpoint(const cv::Point &point1, const cv::Point &point2,
                                 cv::Point &midpoint);

        inline bool is_digit(char c);

        void parse_numbers_in_rectangles(
                const std::array<std::array<Rectangle<int>, SUDOKU_SIZE>, SUDOKU_SIZE>
                &squares,
                PIX *image,
                std::array<std::array<int, SUDOKU_SIZE>, SUDOKU_SIZE> &parsed_sudoku);

        int parse_rectangle(char *rectangle_text);

        PIX *prepare_tesseract_image(cv::Mat &image);
    };
}  // namespace SudokuSolver

#endif
