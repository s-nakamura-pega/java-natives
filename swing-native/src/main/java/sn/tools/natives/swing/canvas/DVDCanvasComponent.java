package sn.tools.natives.swing.canvas;

import java.util.function.Consumer;

import sn.tools.natives.swing.api.movie.DiscPlayer;

public class DVDCanvasComponent extends AbstractCanvasComponent<DVDCanvas> implements DiscPlayer {

    private static final long serialVersionUID = 1L;

    public DVDCanvasComponent() {
        super(new DVDCanvas());
    }

    @Override
    public long setFile(String file) {
        return canvas.setFile(file);
    }

    @Override
    public long setData(byte[] data) {
        return canvas.setData(data);
    }

    @Override
    public void start() {
        canvas.start();
    }

    @Override
    public void stop() {
        canvas.stop();
    }

    @Override
    public void movePoint(long ms) {
        canvas.movePoint(ms);
    }

    @Override
    public boolean isDecodeReady() {
        return canvas.isDecodeReady();
    }

    @Override
    public boolean isStarted() {
        return canvas.isStarted();
    }

    @Override
    public long getCurrentPoint() {
        return canvas.getCurrentPoint();
    }

    @Override
    public void setCurrentPoint(long currentPoint) {
        canvas.setCurrentPoint(currentPoint);
    }

    @Override
    public void setPointRenderer(Consumer<Long> pointRenderer) {
        canvas.setPointRenderer(pointRenderer);
    }

    @Override
    public void skip(boolean isForward) {
        canvas.skip(isForward);
    }

    @Override
    public void sendKey(int key) {
        canvas.sendKey(key);
    }

    @Override
    public void changeDuration(long duration) {
        canvas.changeDuration(duration);
    }

    @Override
    public void setChangeDurationRenderer(Consumer<Long> durationRenderer) {
        canvas.setChangeDurationRenderer(durationRenderer);
    }

    @Override
    public void sendMouseEvent(int x, int y, boolean isClick) {
        canvas.sendMouseEvent(x, y, isClick);
    }

}
