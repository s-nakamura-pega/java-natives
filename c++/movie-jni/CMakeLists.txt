cmake_minimum_required(VERSION 3.10)
project(game_native)

set(CMAKE_CXX_STANDARD 17)

# JNI
find_package(JNI REQUIRED)

include_directories(
    ${JNI_INCLUDE_DIRS}
    ${JNI_INCLUDE_DIRS}/linux
)

# pkg-config
find_package(PkgConfig REQUIRED)

# FFmpeg
pkg_check_modules(FFMPEG REQUIRED
    libavformat
    libavcodec
    libavutil
    libswscale
)

# PortAudio（正しい pkg-config 名）
pkg_check_modules(PORTAUDIO REQUIRED portaudio-2.0)

# include path
include_directories(
    ${FFMPEG_INCLUDE_DIRS}
    ${PORTAUDIO_INCLUDE_DIRS}
)

# library path
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
