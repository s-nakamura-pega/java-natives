package sn.tools.natives.swing.canvas;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	public long setData(byte[] data) {
		String tempDir = System.getProperty("java.io.tmpdir");

		Path dir = Paths.get(tempDir, "moviecanvas", String.valueOf(handleId));
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		// data.bin を毎回上書き（レコが単一ファイルを読む仕様ならこれでOK）
		Path tempFile = dir.resolve("data.bin");

		try {
			Files.write(tempFile, data);
			return setFile(tempFile.toAbsolutePath().toString());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() {
		destroy();

		// temp ディレクトリを削除（レコ仕様に合わせてクリーンアップ）
		try {
			Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "moviecanvas", String.valueOf(handleId));
			if (Files.exists(dir)) {
				Files.walk(dir).sorted((a, b) -> b.compareTo(a)) // ファイル→ディレクトリの順で削除
						.forEach(p -> {
							try {
								Files.deleteIfExists(p);
							} catch (IOException ignored) {
							}
						});
			}
		} catch (IOException ignored) {
		}

		handleId = 0;
	}

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

	@Override
	public native long setFile(String filePath);

}
