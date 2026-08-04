package sn.tools.natives.swing.canvas;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import sn.tools.natives.object.NativeObject;
import sn.tools.natives.util.NativeLoader;

public class MovieCanvas extends JComponent implements NativeMovieCanvas {

	private static final long serialVersionUID = 1L;

	private long handleId = 0;

	private int w, h;
	private byte[] frameBuffer;
	private BufferedImage img;
	private int currentPoint;

	private Consumer<Integer> pointRenderer = _ -> {
	};

	public MovieCanvas() {
		super();
		NativeLoader.load("swing.component.movie");
		handleId = create();
	}

	@Override
	public void initCanvas(int width, int height) {
		this.w = width;
		this.h = height;
		this.frameBuffer = new byte[w * h * 3];
		this.img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
	}

	@Override
	public native int getFrame(byte[] buffer);

	@Override
	public void repaintCallback() {
		int size = getFrame(frameBuffer);
		if (size > 0) {
			SwingUtilities.invokeLater(() -> {
				img.getRaster().setDataElements(0, 0, w, h, frameBuffer);
				repaint();
			});
		}
	}

	@Override
	public native int setData(byte[] data);

	@Override
	public native void start();

	@Override
	public native void stop();

	@Override
	public native void movePoint(int ms);

	@Override
	public native boolean isDecodeReady();

	@Override
	protected void paintComponent(Graphics g) {
		if (img != null) {
			g.drawImage(img, 0, 0, null);
		}
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		NativeObject.close(this);
	}

	@Override
	public long getHandleId() {
		return handleId;
	}

	private native long create();

	private native void destroy();

	@Override
	public void close() {
		destroy();
		handleId = 0;
	}

	@Override
	public native boolean isStarted();

	@Override
	public int getCurrentPoint() {
		return currentPoint;
	}

	@Override
	public void setCurrentPoint(int currentPoint) {
		this.currentPoint = currentPoint;
		SwingUtilities.invokeLater(() -> pointRenderer.accept(currentPoint));
	}

	public void setPointRenderer(Consumer<Integer> pointRenderer) {
		this.pointRenderer = pointRenderer;
	}

}
