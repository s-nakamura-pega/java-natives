package sn.tools.demo.dvd.system;

import javax.swing.SwingUtilities;

import sn.tools.demo.dvd.frame.DVDMovieFrame;

public class SystemMain {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			DVDMovieFrame frame = new DVDMovieFrame();
			frame.setVisible(true);
		});
	}

}
