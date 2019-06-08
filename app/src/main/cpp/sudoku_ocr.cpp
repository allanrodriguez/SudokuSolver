#include "find_sudoku.h"
#include "read_cells.h"

#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <array>

using namespace cv;
using namespace std;

const char *TAG = "sudoku_ocr";

static void getJavaArray(JNIEnv &env, const array<Cell, 81> &cells, jintArray javaArray) {
    jint sudoku[81] = {};

    for (int i = 0; i < 81; ++i) {
        sudoku[i] = cells[i].value;
    }

    env.SetIntArrayRegion(javaArray, 0, 81, sudoku);
}

static void readBitmap(JNIEnv *env, const jobject &bitmap, Mat &outMat) {
    AndroidBitmapInfo info;
    void *pixels = nullptr;
    int ret;

    ret = AndroidBitmap_getInfo(env, bitmap, &info);
    if (ret < 0) {
        log(LogPriority::error, TAG, "AndroidBitmap_getInfo() failed! error=" + to_string(ret));
        outMat = Mat();
        return;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        log(LogPriority::error, TAG, "Bitmap format is not RGBA_8888!");
        outMat = Mat();
        return;
    }

    ret = AndroidBitmap_lockPixels(env, bitmap, &pixels);
    if (ret < 0) {
        log(LogPriority::error, TAG, "AndroidBitmap_lockPixels() failed! error=" + to_string(ret));
        outMat = Mat();
        return;
    }

    outMat = Mat(Size(info.width, info.height), CV_8UC3);

    auto *src = (uint32_t *)pixels;
    uint32_t src_wpl = info.stride / 4;
    uint32_t *src_line = nullptr;

    uint8_t *pixel_bgra = nullptr;

    for (int y = 0; y < info.height; ++y) {
        src_line = src + (y * src_wpl);

        for (int x = 0; x < info.width; ++x) {
            pixel_bgra = (uint8_t *)src_line;

            uint8_t b = pixel_bgra[0];
            uint8_t g = pixel_bgra[1];
            uint8_t r = pixel_bgra[2];

            // Set pixel
            outMat.at<Vec3b>(y, x) = Vec3b(b, g, r);

            // Move to the next pixel
            ++src_line;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}

extern "C" {
JNIEXPORT void JNICALL Java_com_allanrodriguez_sudokusolver_fragments_ParseOcrFragment_parseSudokuFromImage(
    JNIEnv *env, jobject thiz, jobject image, jstring pathToTrainData, jintArray sudoku) {
    auto charPathToTrainData = env->GetStringUTFChars(pathToTrainData, nullptr);
    string trainData = charPathToTrainData;
    Mat bitmap;
    Rect puzzle;
    vector<Rect> squares = {};
    array<Cell, 81> cells = {};

    readBitmap(env, image, bitmap);

    log(LogPriority::debug, TAG, "Finding sudoku puzzle and cell candidates...");
    findSudoku(bitmap, puzzle, squares);

    log(LogPriority::debug, TAG, "Filtering out cell candidates...");
    prepareCellsForOcr(puzzle, squares, cells);

    log(LogPriority::debug, TAG, "Attempting to parse cell values...");
    readCells(bitmap, charPathToTrainData, cells);
    env->ReleaseStringUTFChars(pathToTrainData, charPathToTrainData);

    getJavaArray(*env, cells, sudoku);

    log(LogPriority::debug, TAG, "Done!");
}
}
