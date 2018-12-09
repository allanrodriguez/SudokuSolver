#include <sudokuocrreader.h>

namespace SudokuSolver {
    SudokuOcrReader::SudokuOcrReader(const char* path_to_image,
                                     const char* path_to_tessdata_parent)
    {
        if (path_to_image == NULL || !strcmp(path_to_image, "")) {
            throw std::invalid_argument("path_to_image is NULL or an empty string.");
        }

        if (path_to_tessdata_parent == NULL || !strcmp(path_to_tessdata_parent, "")) {
            throw std::invalid_argument("path_to_tessdata_parent is NULL or an empty string.");
        }

        _path_to_image = path_to_image;
        _path_to_tessdata_parent = path_to_tessdata_parent;

#ifdef DEBUG
        __android_log_print(ANDROID_LOG_DEBUG, k_name, "Path to image is %s", _path_to_image);
        __android_log_print(ANDROID_LOG_DEBUG, k_name, "Path to tessdata parent directory is %s", _path_to_tessdata_parent);
#endif
    }

    SudokuOcrReader::SudokuOcrReader(const cv::Mat& image,
                                     const char* path_to_tessdata_parent)
    {
        if (image.empty()) {
            throw std::invalid_argument("Image is empty.");
        }

        if (path_to_tessdata_parent == NULL || !strcmp(path_to_tessdata_parent, "")) {
            throw std::invalid_argument("path_to_tessdata_parent is NULL or an empty string.");
        }

        _image = image;
        _path_to_tessdata_parent = path_to_tessdata_parent;

#ifdef DEBUG
        cv::Size image_size = image.size();

        __android_log_print(ANDROID_LOG_DEBUG, k_name, "Image dimensions are %d x %d", image_size.width, image_size.height);
        __android_log_print(ANDROID_LOG_DEBUG, k_name, "Path to tessdata parent directory is %s", _path_to_tessdata_parent);
#endif
    }

    inline double SudokuOcrReader::angle(cv::Point point1, cv::Point point2, cv::Point point0)
    {
        double dx1 = point1.x - point0.x;
        double dy1 = point1.y - point0.y;
        double dx2 = point2.x - point0.x;
        double dy2 = point2.y - point0.y;
                            
        return (dx1 * dx2 + dy1 * dy2) / sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    int SudokuOcrReader::best_candidate_for_cell(std::array<int, 10> candidates)
    {
        int choice = 0;
        int num_appearances = 0;

        int candidates_size = candidates.size();

        for (int i = 0; i < candidates_size; ++i) {
            if (candidates[i] > num_appearances) {
                num_appearances = candidates[i];
                choice = i;
            }
        }

        return choice;
    }

    void SudokuOcrReader::find_squares(const cv::Mat& image, std::vector< std::vector<cv::Point> >& squares)
    {
        double image_area = image.rows * image.cols;

        cv::Mat pyr;
        cv::Mat t_img;
        cv::Mat gray0(image.size(), CV_8U);
        cv::Mat gray;
        cv::Mat kernel;

        cv::pyrDown(image, pyr, cv::Size(image.cols / 2, image.rows / 2));
        cv::pyrUp(pyr, t_img, image.size());

        int ch[] = { 0, 0 };

        std::vector< std::vector<cv::Point> > contours;

        for (int c = 0; c < 3; ++c) {
            ch[0] = c;
            cv::mixChannels(&t_img, 1, &gray0, 1, ch, 1);

            for (int l = 0; l < k_num_iterations; ++l) {
                if (l) {
                    cv::threshold(gray0, gray, (l + 1.0) * 255.0 / k_num_iterations, 255.0, cv::THRESH_BINARY);
                } else {
                    cv::Canny(gray0, gray, 0.0, k_thresh, 5);
                    cv::dilate(gray, gray, kernel);
                }

                cv::findContours(gray, contours, cv::RETR_LIST, cv::CHAIN_APPROX_SIMPLE);

                std::vector<cv::Point> approx;

                for (std::vector<cv::Point> contour : contours) {
                    cv::approxPolyDP(contour, approx, 0.1 * cv::arcLength(contour, true), true);

                    if (approx.size() == 4 &&
                        cv::contourArea(approx) > image_area / 144.0 &&
                        cv::contourArea(approx) <= image_area / 81.0 &&
                        cv::isContourConvex(approx)) {
                        double max_cos = 0.0;
                        double cos;

                        for (int j = 2; j < 5; ++j) {
                            cos = std::fabs(angle(approx[j % 4], approx[j - 2], approx[j - 1]));
                            max_cos = std::max(max_cos, cos);
                        }

                        if (max_cos < 0.3) {
                            squares.push_back(approx);
                        }
                    }
                }
            }
        }
    }

    void SudokuOcrReader::generate_rectangle_vector(const std::vector< std::vector<cv::Point> >& squares, std::vector< Rectangle<int> >& rectangles)
    {
        std::vector<cv::Point> square_copy;
        cv::Point midpoint;

        for (std::vector<cv::Point> square : squares) {
            int rightmost = 0;
            int leftmost = INT_MAX;
            int topmost = INT_MAX;
            int bottommost = 0;

            square_copy = square;

            while (!square_copy.empty()) {
                cv::Point vertex = square_copy.back();
                square_copy.pop_back();

                for (cv::Point p : square_copy) {
                    get_midpoint(vertex, p, midpoint);

                    leftmost = std::min(leftmost, midpoint.x);
                    topmost = std::min(topmost, midpoint.y);
                    rightmost = std::max(rightmost, midpoint.x);
                    bottommost = std::max(bottommost, midpoint.y);
                }
            }

            Rectangle<int> rect;
            rect.left = leftmost;
            rect.top = topmost;

            rect.width = rightmost - rect.left;
            rect.height = bottommost - rect.top;

            if (rect.width > 0 && rect.height > 0) {
                rectangles.push_back(rect);
            } else {
                __android_log_print(ANDROID_LOG_WARN, k_name, "Rectangle with negative dimensions found.");
            }
        }
    }

    inline void SudokuOcrReader::get_midpoint(const cv::Point& point1, const cv::Point& point2, cv::Point& midpoint)
    {
        midpoint.x = (int) std::round((point1.x + point2.x) / 2.0);
        midpoint.y = (int) std::round((point1.y + point2.y) / 2.0);
    }

    inline bool SudokuOcrReader::is_digit(char c)
    {
        auto i = (int) c - 48;

        return i >= 0 && i < 10;
    }

    bool SudokuOcrReader::parse_image(std::array<std::array<int, 9>, 9>& parsed_sudoku)
    {
        cv::Mat image = _path_to_image == NULL ? _image : cv::imread(_path_to_image);
        if (image.empty()) {
            __android_log_print(ANDROID_LOG_ERROR, k_name, "Could not load image at %s", _path_to_image);
            return false;
        }

        std::vector< std::vector<cv::Point> > squares_list;
        std::vector< Rectangle<int> > rectangles_list;

        find_squares(image, squares_list);

        generate_rectangle_vector(squares_list, rectangles_list);

#ifdef DEBUG
        for (Rectangle<int> r : rectangles_list) {
            __android_log_print(ANDROID_LOG_DEBUG, k_name, "(%d, %d, %d, %d)", r.left, r.top, r.width, r.height);
        }

        __android_log_print(ANDROID_LOG_DEBUG, k_name, "%u square(s) found.", rectangles_list.size());
#endif

        PIX* pix_image = prepare_tesseract_image(image);

        if (!pix_image) {
            __android_log_print(ANDROID_LOG_ERROR, k_name, "Could not create Tesseract image.");
            return false;
        }

        parse_numbers_in_rectangles(rectangles_list, pix_image, parsed_sudoku);

        pixDestroy(&pix_image);

#ifdef DEBUG
        std::stringstream puzzle_string;
        puzzle_string << "filler so that the puzzle looks nice" << std::endl;

        for (std::array<int, 9> row : parsed_sudoku) {
            for (int col : row) {
                if (col) {
                    puzzle_string << col;
                } else {
                    puzzle_string << '_';
                }
                puzzle_string << ' ';
            }
            puzzle_string << std::endl;
        }

        __android_log_print(ANDROID_LOG_DEBUG, k_name, "%s", puzzle_string.str().c_str());
#endif

        return true;
    }

    void SudokuOcrReader::parse_numbers_in_rectangles(const std::vector< Rectangle<int> >& squares,
                                                      PIX* image,
                                                      std::array<std::array<int, 9>, 9>& parsed_sudoku)
    {
        auto api = tesseract::TessBaseAPI();

        if (api.Init(_path_to_tessdata_parent, NULL, tesseract::OEM_TESSERACT_ONLY)) {
            __android_log_print(ANDROID_LOG_ERROR, k_name, "Could not initialize Tesseract");
            return;
        }

        // Set so that only numbers are recognized
        api.SetVariable("tessedit_char_whitelist", "123456789");

        int imageWidth = pixGetWidth(image);
        int imageHeight = pixGetHeight(image);

#ifdef DEBUG
        if (imageWidth <= 0 || imageHeight <= 0)
        {
            __android_log_print(ANDROID_LOG_ERROR, k_name, "Image width and/or height is 0, OCR will fail.");
        }
#endif

        api.SetImage(image);

        int num = 0;
        int row = 0;
        int col = 0;
        std::array<std::array<std::array<int, 10>, 9>, 9> candidates { };

        for (Rectangle<int> square : squares) {
            api.SetRectangle(3 * square.left, 3 * square.top, 3 * square.width, 3 * square.height);

            num = parse_rectangle(api.GetUTF8Text());
            col = (int) ((27 * square.left + 13.5 * square.width) / imageWidth);
            row = (int) ((27 * square.top + 13.5 * square.height) / imageHeight);
            ++candidates[row][col][num];
            __android_log_print(ANDROID_LOG_DEBUG, k_name, "(%d, %d): %s", col, row, api.GetUTF8Text());
        }

        api.End();

        for (int i = 0; i < 9; ++i) {
            for (int j = 0; j < 9; ++j) {
                parsed_sudoku[i][j] = best_candidate_for_cell(candidates[i][j]);
            }
        }
    }

    int SudokuOcrReader::parse_rectangle(char* rectangle_text)
    {
        int num = 0;

        if (rectangle_text == NULL) {
            return -1;
        }

        for (int i = 0; rectangle_text[i] != '\0'; ++i) {
            if (is_digit(rectangle_text[i])) {
                if (num) {
                    return 0;
                }
                num = (int) rectangle_text[i] - 48;
            }
        }

        return num;
    }

    PIX* SudokuOcrReader::prepare_tesseract_image(cv::Mat& image)
    {
        cv::Mat cv_gray;
        PIX* pix_gray = NULL;

        cv::resize(image, cv_gray, cv::Size(3 * image.cols, 3 * image.rows));

        cv::cvtColor(cv_gray, cv_gray, cv::COLOR_BGR2GRAY);
        cv::threshold(cv_gray, cv_gray, 0.0, 255.0, cv::THRESH_BINARY | cv::THRESH_OTSU);

        cv::Size cv_gray_size = cv_gray.size();
        pix_gray = pixCreate(cv_gray_size.width, cv_gray_size.height, 8);

        for (int y = 0; y < cv_gray.rows; ++y)
        {
            for (int x = 0; x < cv_gray.cols; ++x)
            {
                pixSetPixel(pix_gray, x, y, (l_uint32) cv_gray.at<uchar>(y, x));
            }
        }

        return pix_gray;
    }

    SudokuOcrReader::~SudokuOcrReader()
    {
    }
}
