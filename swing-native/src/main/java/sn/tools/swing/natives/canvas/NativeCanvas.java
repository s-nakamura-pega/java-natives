package sn.tools.swing.natives.canvas;

public interface NativeCanvas {

	void init(NativeCanvas self);

	void initCanvas(int width, int height); // ★ Rust が呼ぶ

	void repaintCallback();

	int getFrame(byte[] buffer);

}
