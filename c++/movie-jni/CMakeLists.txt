cmake_minimum_required(VERSION 3.10)
project(game_native)

set(CMAKE_CXX_STANDARD 17)

# JNI
find_package(JNI REQUIRED)

include_directories(
    ${JNI_INCLUDE_DIRS}
    ${JNI_INCLUDE_DIRS}/linux
)

# FFmpeg (pkg-config)
find_package(PkgConfig REQUIRED)
pkg_check_modules(FFMPEG REQUIRED
    libavformat
    libavcodec
    libavutil
    libswscale
    PORTAUDIO REQUIRED portaudio-2.0
)

# ★ pkg-config が返した include パスを使う
include_directories(
    ${FFMPEG_INCLUDE_DIRS}   # ← /usr/include/ffmpeg が入る
    ${PORTAUDIO_INCLUDE_DIRS}
    )

link_directories(
    ${FFMPEG_LIBRARY_DIRS}
    ${PORTAUDIO_LIBRARY_DIRS}
)

add_library(game_native SHARED
    jni_onload.cpp
    player_native.cpp
)

target_link_libraries(game_native
    ${JNI_LIBRARIES}
    ${FFMPEG_LIBRARIES}
    ${PORTAUDIO_LIBRARIES}
)
