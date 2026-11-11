package sn.tools.demo.players.system;

import java.nio.file.Paths;

import javax.swing.SwingUtilities;

import sn.tools.demo.players.frame.PlayerFrame;

public class SystemMain {

	public static void main(String[] args) {
		System.out.println(Paths.get(""));
		SwingUtilities.invokeLater(() -> {
			PlayerFrame frame = new PlayerFrame();
			frame.setVisible(true);
		});
	}

}
