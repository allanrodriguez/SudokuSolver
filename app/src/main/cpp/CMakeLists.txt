cmake_minimum_required(VERSION 3.4.1)

find_library(bitmap-lib jnigraphics)
find_library(log-lib log)

add_library(sudoku_ocr SHARED sudoku_ocr.cpp find_sudoku.cpp read_cells.cpp log.cpp)

include_directories(include opencv/include)

add_library(opencv_core SHARED IMPORTED)
set_target_properties(opencv_core
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/opencv/lib/${ANDROID_ABI}/libopencv_core.so)

add_library(opencv_imgcodecs SHARED IMPORTED)
set_target_properties(opencv_imgcodecs
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/opencv/lib/${ANDROID_ABI}/libopencv_imgcodecs.so)

add_library(opencv_imgproc SHARED IMPORTED)
set_target_properties(opencv_imgproc
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/opencv/lib/${ANDROID_ABI}/libopencv_imgproc.so)

add_library(opencv_ml SHARED IMPORTED)
set_target_properties(opencv_ml
                        PROPERTIES IMPORTED_LOCATION
                        ${CMAKE_SOURCE_DIR}/opencv/lib/${ANDROID_ABI}/libopencv_ml.so)

target_link_libraries(sudoku_ocr opencv_core opencv_imgcodecs opencv_imgproc opencv_ml ${bitmap-lib}
                        ${log-lib})
