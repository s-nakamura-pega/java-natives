package sn.tools.natives.swing.canvas;

import java.awt.BorderLayout;
import java.util.function.Consumer;

import javax.swing.JComponent;

import sn.tools.natives.swing.api.movie.DiscPlayer;

public class DVDCanvasComponent extends JComponent implements DiscPlayer {

    private static final long serialVersionUID = 1L;

    private DVDCanvas canvas;

    public DVDCanvasComponent() {
        super();
        canvas = new DVDCanvas();
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
    }

    public DVDCanvas getCanvas() {
        return canvas;
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
    public void movePoint(int ms) {
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
    public int getCurrentPoint() {
        return canvas.getCurrentPoint();
    }

    @Override
    public void setCurrentPoint(int currentPoint) {
        canvas.setCurrentPoint(currentPoint);
    }

    @Override
    public void setPointRenderer(Consumer<Integer> pointRenderer) {
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

}
