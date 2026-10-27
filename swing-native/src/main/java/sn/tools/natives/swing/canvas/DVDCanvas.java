package sn.tools.natives.swing.canvas;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import sn.tools.natives.swing.api.movie.DiscPlayer;
import sn.tools.natives.util.NativeLoader;

class DVDCanvas extends AbstractMovieCanvas implements DiscPlayer {

	private final KeyListener keyListener = new KeyListener() {

		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			sendKey(e.getKeyCode());
		}

	};

	private final MouseMotionListener mml = new MouseMotionListener() {

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			Point p = getMoviePoint(e.getX(), e.getY());
			if (p != null) {
				sendMouseEvent(p.x, p.y, false);
			}
		}

	};

	private final MouseListener ml = new MouseListener() {

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
			requestFocusInWindow();
			Point p = getMoviePoint(e.getX(), e.getY());
			if (p != null) {
				sendMouseEvent(p.x, p.y, true);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

	};

	private Point getMoviePoint(int x, int y) {
		int drawX = getDrawX();
		int drawY = getDrawY();
		int drawW = getDrawWidth();
		int drawH = getDrawHeight();
		if (x < drawX || x >= drawX + drawW || y < drawY || y >= drawY + drawH) {
			return null;
		}
		int movieW = getMovieWidth();
		int movieH = getMovieHeight();
		double xRatio = (double) drawW / movieW;
		double yRatio = (double) drawH / movieH;
		int movieX = (int) ((x - drawX) / xRatio);
		int movieY = (int) ((y - drawY) / yRatio);
		return new Point(movieX, movieY);
	}

	private long duration;

	private Consumer<Long> durationRenderer = _ -> {
	};

	public DVDCanvas() {
		super();
		NativeLoader.load("swing.component.movie");
		setFocusable(true);
		SwingUtilities.invokeLater(() -> {
			requestFocusInWindow();
		});
	}

	@Override
	public long setData(byte[] data) {
		String tempDir = System.getProperty("java.io.tmpdir");

		Path dir = Paths.get(tempDir, "moviecanvas", String.valueOf(getHandleId()));
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
		super.close();
		// temp ディレクトリを削除（レコ仕様に合わせてクリーンアップ）
		try {
			Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "moviecanvas", String.valueOf(getHandleId()));
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
	}

	@Override
	public native void start();

	@Override
	public native void stop();

	@Override
	public native void movePoint(long ms);

	@Override
	public native boolean isDecodeReady();

	@Override
	public void addNotify() {
		super.addNotify();
		addKeyListener(keyListener);
		addMouseListener(ml);
		addMouseMotionListener(mml);
	}

	@Override
	public void removeNotify() {
		removeKeyListener(keyListener);
		removeMouseListener(ml);
		removeMouseMotionListener(mml);
		super.removeNotify();
	}

	@Override
	protected native long create();

	@Override
	protected native void destroy();

	@Override
	public native boolean isStarted();

	@Override
	public native long setFile(String filePath);

	@Override
	public native void skip(boolean isForward);

	@Override
	public native void sendKey(int key);

	@Override
	public synchronized void changeDuration(long duration) {
		this.duration = duration;
		SwingUtilities.invokeLater(() -> durationRenderer.accept(duration));
	}

	@Override
	public void setChangeDurationRenderer(Consumer<Long> durationRenderer) {
		this.durationRenderer = durationRenderer;
	}

	@Override
	public native void sendMouseEvent(int x, int y, boolean isClick);

}
