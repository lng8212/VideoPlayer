cmake_minimum_required(VERSION 3.10.2)
project("VideoPlayer")

# Add FFmpeg headers
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/ffmpeg)

# Import FFmpeg shared libraries
add_library(libavcodec SHARED IMPORTED)
add_library(libavformat SHARED IMPORTED)
add_library(libavutil SHARED IMPORTED)
add_library(libswscale SHARED IMPORTED)

# Set paths to the .so files
set_target_properties(libavcodec PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/lib/${ANDROID_ABI}/libavcodec.so)

set_target_properties(libavformat PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/lib/${ANDROID_ABI}/libavformat.so)

set_target_properties(libavutil PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/lib/${ANDROID_ABI}/libavutil.so)

set_target_properties(libswscale PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/cpp/lib/${ANDROID_ABI}/libswscale.so)

# Your native code
add_library(native-lib SHARED src/main/cpp/native-lib.cpp)

# Link libraries
target_link_libraries(native-lib
        libavcodec
        libavformat
        libavutil
        libswscale
        android
        log
)
