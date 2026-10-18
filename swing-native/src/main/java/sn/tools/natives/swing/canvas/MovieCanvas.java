package sn.tools.natives.swing.canvas;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import sn.tools.natives.swing.api.movie.Movie;
import sn.tools.natives.util.NativeLoader;

class MovieCanvas extends AbstractMovieCanvas implements Movie {

	public MovieCanvas() {
		super();
		NativeLoader.load("swing.component.movie");
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
	public long getHandleId() {
		return handleId;
	}

	protected native long create();

	protected native void destroy();

	@Override
	public native boolean isStarted();

	@Override
	public native long setFile(String filePath);

}
