package sn.tools.natives.swing.api.movie;

import java.util.function.Consumer;

public interface Movie {

    long setFile(String file);

    long setData(byte[] data);

    void start();

    void stop();

    void movePoint(int ms);

    boolean isDecodeReady();

    public boolean isStarted();

    public int getCurrentPoint();

    public void setCurrentPoint(int currentPoint);

    public void setPointRenderer(Consumer<Integer> pointRenderer);

}