package sn.tools.swing.natives.canvas;

public interface NativeMovieCanvas extends NativeCanvas {

	void setData(String path);

	void start();

	void stop();

	void rewind(int ms);

	void forward(int ms);

	boolean isDecodeReady();

}
