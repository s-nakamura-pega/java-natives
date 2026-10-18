package sn.tools.natives.swing.canvas;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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

	private KeyListener keyListener = new KeyListener() {

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
	}

	@Override
	public void removeNotify() {
		removeKeyListener(keyListener);
		super.removeNotify();
	}

	protected native long create();

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

}
