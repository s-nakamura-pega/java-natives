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
# FFmpeg（movie 側で使うので残す）
# ----------------------------------------
pkg_check_modules(FFMPEG REQUIRED
    libavformat
    libavcodec
    libavutil
    libswscale
    libswresample
)

# ----------------------------------------
# PortAudio（movie 側で使うので残す）
# ----------------------------------------
pkg_check_modules(PORTAUDIO REQUIRED portaudio-2.0)

# ----------------------------------------
# DVD (libdvdnav + libdvdread)
# ----------------------------------------
pkg_check_modules(DVDNAV REQUIRED dvdnav)
pkg_check_modules(DVDREAD REQUIRED dvdread)

# ----------------------------------------
# mpv（libmpv）
# ----------------------------------------
pkg_check_modules(MPV REQUIRED mpv)

include_directories(
    ${FFMPEG_INCLUDE_DIRS}
    ${PORTAUDIO_INCLUDE_DIRS}
    ${DVDNAV_INCLUDE_DIRS}
    ${DVDREAD_INCLUDE_DIRS}
    ${MPV_INCLUDE_DIRS}       # ★ libmpv include
)

link_directories(
    ${FFMPEG_LIBRARY_DIRS}
    ${PORTAUDIO_LIBRARY_DIRS}
    ${DVDNAV_LIBRARY_DIRS}
    ${DVDREAD_LIBRARY_DIRS}
    ${MPV_LIBRARY_DIRS}       # ★ libmpv library
)

# ----------------------------------------
# ソース
# ----------------------------------------
add_library(movie_native SHARED
    jni_onload.cpp
    player_native.cpp        # FFmpegベースの通常動画プレイヤー
    dvd_player_native.cpp    # ★ mpvベースのDVD専用プレイヤーに書き換え済み
)

# ----------------------------------------
# リンク
# ----------------------------------------
target_link_libraries(movie_native
    ${JNI_LIBRARIES}

    # FFmpeg（movie 側で使う）
    ${AVCODEC_LIBRARIES}
    ${AVFORMAT_LIBRARIES}
    ${AVUTIL_LIBRARIES}
    ${SWSCALE_LIBRARIES}
    ${SWRESAMPLE_LIBRARIES}

    # PortAudio（movie 側で使う）
    ${PORTAUDIO_LIBRARIES}

    # libdvdnav / libdvdread（movie 側で使う）
    ${DVDNAV_LIBRARIES}
    ${DVDREAD_LIBRARIES}

    # mpv（DVD専用プレイヤー）
    ${MPV_LIBRARIES}
)
