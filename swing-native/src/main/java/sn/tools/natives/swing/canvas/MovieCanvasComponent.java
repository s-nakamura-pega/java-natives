package sn.tools.natives.swing.canvas;

import sn.tools.natives.swing.api.movie.Movie;
import java.util.function.Consumer;

public class MovieCanvasComponent extends AbstractCanvasComponent<MovieCanvas> implements Movie {

    private static final long serialVersionUID = 1L;

    public MovieCanvasComponent() {
        super(new MovieCanvas());
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

}
