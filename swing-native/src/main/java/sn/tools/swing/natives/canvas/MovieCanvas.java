package sn.tools.swing.natives.canvas;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.swing.JComponent;

public class MovieCanvas extends JComponent implements NativeMovieCanvas {

	private static final long serialVersionUID = 1L;
	private static final Path PROPERTIES_PATH = Paths.get("configs", "native.properties");
	static {
		try {
			Properties props = new Properties();
			try (InputStream in = Files.newInputStream(PROPERTIES_PATH)) {
				props.load(in);
			}

			String libName = props.getProperty("canvas.movie.library");
			System.loadLibrary(libName);

		} catch (Exception e) {
			throw new RuntimeException("Native library load failed", e);
		}
	}

	private int w, h;
	private byte[] frameBuffer;
	private BufferedImage img;

	public MovieCanvas() {
		super();
		init(this);
	}

	@Override
	public native void init(NativeCanvas self);

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
			img.getRaster().setDataElements(0, 0, w, h, frameBuffer);
			repaint();
		}
	}

	@Override
	public native void setData(String path);

	@Override
	public native void start();

	@Override
	public native void stop();

	@Override
	public native void rewind(int ms);

	@Override
	public native void forward(int ms);

	@Override
	public native boolean isDecodeReady();

	@Override
	protected void paintComponent(Graphics g) {
		g.drawImage(img, 0, 0, null);
	}

}
