package sn.tools.natives.swing.canvas;

public interface NativeMovieCanvas extends NativeComponent {

	int setData(byte[] data);

	void start();

	void stop();

	void movePoint(int ms);

	boolean isDecodeReady();

	public boolean isStarted();

	public int getCurrentPoint();

	public void setCurrentPoint(int currentPoint);

}
