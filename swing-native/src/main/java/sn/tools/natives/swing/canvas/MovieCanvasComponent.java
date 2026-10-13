package sn.tools.natives.swing.canvas;

import javax.swing.JComponent;

import sn.tools.natives.swing.api.movie.Movie;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.util.function.Consumer;

public class MovieCanvasComponent extends JComponent implements Movie {

    private static final long serialVersionUID = 1L;

    private MovieCanvas movieCanvas;

    public MovieCanvasComponent() {
        super();
        movieCanvas = new MovieCanvas();
        setLayout(new BorderLayout());
        add(movieCanvas, BorderLayout.CENTER);
    }

    @Override
    public long setFile(String file) {
        return movieCanvas.setFile(file);
    }

    @Override
    public long setData(byte[] data) {
        return movieCanvas.setData(data);
    }

    @Override
    public void start() {
        movieCanvas.start();
    }

    @Override
    public void stop() {
        movieCanvas.stop();
    }

    @Override
    public void movePoint(long ms) {
        movieCanvas.movePoint(ms);
    }

    @Override
    public boolean isDecodeReady() {
        return movieCanvas.isDecodeReady();
    }

    @Override
    public boolean isStarted() {
        return movieCanvas.isStarted();
    }

    @Override
    public long getCurrentPoint() {
        return movieCanvas.getCurrentPoint();
    }

    @Override
    public void setCurrentPoint(long currentPoint) {
        movieCanvas.setCurrentPoint(currentPoint);
    }

    @Override
    public void setPointRenderer(Consumer<Long> pointRenderer) {
        movieCanvas.setPointRenderer(pointRenderer);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        movieCanvas.paintComponent();
    }

}
