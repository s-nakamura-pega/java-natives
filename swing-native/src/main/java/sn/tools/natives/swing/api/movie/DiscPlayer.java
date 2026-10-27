package sn.tools.natives.swing.api.movie;

import java.util.function.Consumer;

public interface DiscPlayer extends Movie {

	void skip(boolean isForward);

	void sendKey(int key);

	void sendMouseEvent(int x, int y, boolean isClick);

	void changeDuration(long duration);

	void setChangeDurationRenderer(Consumer<Long> durationRenderer);

}
