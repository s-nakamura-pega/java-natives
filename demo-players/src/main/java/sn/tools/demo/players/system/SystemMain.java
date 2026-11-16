package sn.tools.demo.players.system;

import javax.swing.SwingUtilities;
import sn.tools.demo.players.frame.PlayerFrame;

public class SystemMain {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			PlayerFrame frame = new PlayerFrame();
			frame.setVisible(true);
		});
	}

}
