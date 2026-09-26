#!/bin/bash
set -e
cd "$(dirname "$0")"

JAVA_HOME="/usr/lib/jvm/java-25-openjdk"
echo "[BUILD] Using JAVA_HOME = $JAVA_HOME"

FFMPEG_INC="/usr/include/ffmpeg"
PORTAUDIO_INC="/usr/include"
DVDNAV_INC="/usr/include"
DVDREAD_INC="/usr/include"
LIBVLC_INC="/usr/include/vlc"

FFMPEG_LIBS="-lavcodec -lavformat -lavutil -lswscale -lswresample"
PORTAUDIO_LIBS="-lportaudio"
DVDNAV_LIBS="-ldvdnav"
DVDREAD_LIBS="-ldvdread"
LIBVLC_LIBS="-lvlc"

clang++ -std=c++17 \
    -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -I"$FFMPEG_INC" \
    -I"$PORTAUDIO_INC" \
    -I"$DVDNAV_INC" \
    -I"$DVDREAD_INC" \
    -I"$LIBVLC_INC" \
    -shared -fPIC \
    jni_onload.cpp \
    player_native.cpp \
    dvd_player_native.cpp \
    $FFMPEG_LIBS \
    $PORTAUDIO_LIBS \
    $DVDNAV_LIBS \
    $DVDREAD_LIBS \
    $LIBVLC_LIBS \
    -o movie_native.so

echo "[BUILD] movie_native.so generated successfully."
