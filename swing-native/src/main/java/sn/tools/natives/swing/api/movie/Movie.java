package sn.tools.natives.swing.api.movie;

import java.util.function.Consumer;

public interface Movie {

    long setFile(String file);

    long setData(byte[] data);

    void start();

    void stop();

    void movePoint(long ms);

    boolean isDecodeReady();

    boolean isStarted();

    long getCurrentPoint();

    void setCurrentPoint(long currentPoint);

    void setPointRenderer(Consumer<Long> pointRenderer);

}