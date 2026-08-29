package sn.tools.demo.movie.system;

import javax.swing.SwingUtilities;

import sn.tools.demo.movie.frame.MovieFrame;

public class SystemMain {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			MovieFrame frame = new MovieFrame();
			frame.setVisible(true);
		});
	}

}
