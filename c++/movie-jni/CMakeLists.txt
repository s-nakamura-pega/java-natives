cmake_minimum_required(VERSION 3.10)
project(movie_native)

set(CMAKE_CXX_STANDARD 17)

# ----------------------------------------
# JNI
# ----------------------------------------
find_package(JNI REQUIRED)

include_directories(
    ${JNI_INCLUDE_DIRS}
    ${JNI_INCLUDE_DIRS}/linux
)

# ----------------------------------------
# pkg-config
# ----------------------------------------
find_package(PkgConfig REQUIRED)

# ----------------------------------------
# FFmpeg
# ----------------------------------------
pkg_check_modules(FFMPEG REQUIRED
    libavformat
    libavcodec
    libavutil
    libswscale
    libswresample
)

# ----------------------------------------
# PortAudio
# ----------------------------------------
pkg_check_modules(PORTAUDIO REQUIRED portaudio-2.0)

# ----------------------------------------
# DVD (libdvdnav + libdvdread)
# ----------------------------------------
pkg_check_modules(DVDNAV REQUIRED dvdnav)
pkg_check_modules(DVDREAD REQUIRED dvdread)

include_directories(
    ${FFMPEG_INCLUDE_DIRS}
    ${PORTAUDIO_INCLUDE_DIRS}
    ${DVDNAV_INCLUDE_DIRS}
    ${DVDREAD_INCLUDE_DIRS}
)

link_directories(
    ${FFMPEG_LIBRARY_DIRS}
    ${PORTAUDIO_LIBRARY_DIRS}
    ${DVDNAV_LIBRARY_DIRS}
    ${DVDREAD_LIBRARY_DIRS}
)

# ----------------------------------------
# ソース
# ----------------------------------------
add_library(movie_native SHARED
    jni_onload.cpp
    player_native.cpp
    dvd_player_native.cpp
)

# ----------------------------------------
# リンク
# ----------------------------------------
target_link_libraries(movie_native
    ${JNI_LIBRARIES}
    ${FFMPEG_LIBRARIES}
    ${PORTAUDIO_LIBRARIES}
    ${DVDNAV_LIBRARIES}
    ${DVDREAD_LIBRARIES}
)
