package sn.tools.natives.swing.api.movie;

public interface DiscPlayer extends Movie {

	void skip(boolean isForward);

	void sendKey(int key);

}
