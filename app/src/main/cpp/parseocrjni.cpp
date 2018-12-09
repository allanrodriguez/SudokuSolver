#include <android/bitmap.h>
#include <android/log.h>
#include <jni.h>
#include <string>

#include <sudokuocrreader.h>

const char *k_tag = "parseocrjni";

jobjectArray get_java_array(JNIEnv *env, std::array<std::array<int, 9>, 9> &sudoku)
{
    auto result = env->NewObjectArray(9, env->FindClass("[I"), NULL);

    for (jint i = 0; i < 9; ++i) {
        auto row = env->NewIntArray(9);

        for (jint j = 0; j < 9; ++j) {
            env->SetIntArrayRegion(row, j, 1, &sudoku[i][j]);
        }

        env->SetObjectArrayElement(result, i, row);
        env->DeleteLocalRef(row);
    }

    return result;
}

cv::Mat read_bitmap(JNIEnv *env, jobject bitmap)
{
    AndroidBitmapInfo info;
    void *pixels = NULL;
    int ret;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, k_tag, "AndroidBitmap_getInfo() failed! error=%d",
                            ret);
        return cv::Mat();
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        __android_log_print(ANDROID_LOG_ERROR, k_tag, "Bitmap format is not RGBA_8888!");
        return cv::Mat();
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, k_tag, "AndroidBitmap_lockPixels() failed! error=%d",
                            ret);
        return cv::Mat();
    }

    cv::Mat mat(cv::Size(info.width, info.height), CV_8UC3);

    uint32_t *src = (uint32_t *) pixels;
    uint32_t srcWpl = info.stride / 4;
    uint32_t *src_line = NULL;

    uint8_t *pixel_bgra = NULL;

    for (int y = 0; y < info.height; ++y) {
        src_line = src + (y * srcWpl);

        for (int x = 0; x < info.width; ++x) {
            pixel_bgra = (uint8_t *) src_line;

            uint8_t b = pixel_bgra[0];
            uint8_t g = pixel_bgra[1];
            uint8_t r = pixel_bgra[2];

            // Set pixel
            mat.at<cv::Vec3b>(y, x) = cv::Vec3b(b, g, r);

            // Move to the next pixel
            ++src_line;
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    return mat;
}

extern "C"
{
    JNIEXPORT jobjectArray JNICALL
    Java_com_allanrodriguez_sudokusolver_fragments_ParseOcrFragment_00024ParseOcrAsyncTask_parseSudokuFromImage(
            JNIEnv *env,
            jobject,
            jobject image,
            jstring path_to_trained_data)
    {
        const char *char_path_to_trained_data = env->GetStringUTFChars(path_to_trained_data, NULL);

        SudokuSolver::SudokuOcrReader reader(read_bitmap(env, image), char_path_to_trained_data);

        std::array<std::array<int, 9>, 9> sudoku_puzzle;
        bool success = reader.parse_image(sudoku_puzzle);

        env->ReleaseStringUTFChars(path_to_trained_data, char_path_to_trained_data);

        return success ? get_java_array(env, sudoku_puzzle) : NULL;
    }
}