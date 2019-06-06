cmake_minimum_required(VERSION 3.4.1)

find_library(bitmap-lib jnigraphics)
find_library(log-lib log)

add_library(parseocrjni SHARED src/main/cpp/parseocrjni.cpp)

include_directories(src/main/cpp jniLibs/include)

add_library(sudokuocrreader SHARED src/main/cpp/sudokuocrreader.cpp)

if(${CMAKE_BUILD_TYPE} STREQUAL Debug)
    target_compile_definitions(sudokuocrreader PRIVATE DEBUG)
endif()

add_library(leptonica SHARED IMPORTED)
set_target_properties(leptonica PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libleptonica.so)

add_library(tesseract SHARED IMPORTED)
set_target_properties(tesseract PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libtesseract.so)

add_library(opencv_core SHARED IMPORTED)
set_target_properties(opencv_core PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_core.so)

add_library(opencv_imgcodecs SHARED IMPORTED)
set_target_properties(opencv_imgcodecs PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_imgcodecs.so)

add_library(opencv_imgproc SHARED IMPORTED)
set_target_properties(opencv_imgproc PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/jniLibs/${ANDROID_ABI}/libopencv_imgproc.so)

target_link_libraries(sudokuocrreader
        leptonica
        tesseract
        opencv_core
        opencv_imgcodecs
        opencv_imgproc
        ${log-lib})

target_link_libraries(parseocrjni sudokuocrreader ${bitmap-lib})