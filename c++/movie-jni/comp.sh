#!/bin/bash
set -e  # エラーが出たら即終了

cd "$(dirname "$0")"

JAVA_HOME="/usr/lib/jvm/java-25-openjdk"

echo "[BUILD] Using JAVA_HOME = $JAVA_HOME"

# FFmpeg の include / libs
FFMPEG_INC="/usr/include/ffmpeg"
FFMPEG_LIBS="-lavformat -lavcodec -lavutil -lswscale"

# PortAudio の libs
PORTAUDIO_LIBS="-lportaudio"

clang++ \
    -std=c++17 \
    -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -I"$FFMPEG_INC" \
    -shared -fPIC \
    jni_onload.cpp \
    player_native.cpp \
    dvd_player_native.cpp \
    $FFMPEG_LIBS \
    $PORTAUDIO_LIBS \
    -o movie_native.so

echo "[BUILD] movie_native.so generated successfully."
