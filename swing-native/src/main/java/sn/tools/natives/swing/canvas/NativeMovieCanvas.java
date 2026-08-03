package sn.tools.natives.swing.canvas;

public interface NativeMovieCanvas extends NativeCanvas {

	void setData(String path);

	void start();

	void stop();

	void rewind(int ms);

	void forward(int ms);

	boolean isDecodeReady();

}
