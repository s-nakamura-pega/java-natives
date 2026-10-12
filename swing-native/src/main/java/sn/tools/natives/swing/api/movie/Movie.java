package sn.tools.natives.swing.api.movie;

import java.util.function.Consumer;

public interface Movie {

    long setFile(String file);

    long setData(byte[] data);

    void start();

    void stop();

    void movePoint(int ms);

    boolean isDecodeReady();

    boolean isStarted();

    int getCurrentPoint();

    void setCurrentPoint(int currentPoint);

    void setPointRenderer(Consumer<Integer> pointRenderer);

}