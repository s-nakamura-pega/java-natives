package sn.tools.natives.swing.api.component;

import sn.tools.natives.object.NativeObject;

public interface NativeComponent extends NativeObject {

	// ★ Rust が呼ぶ
	void initCanvas(int width, int height);

	void repaintCallback();

	int getFrame(byte[] buffer);

}
