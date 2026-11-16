package sn.tools.demo.players.frame;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import sn.tools.demo.players.icon.PlayerImage;
import sn.tools.demo.players.panel.DVDPanel;
import sn.tools.demo.players.panel.MoviePanel;

public class PlayerFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final DVDPanel dvdPanel;
    private final MoviePanel moviePanel;

    public PlayerFrame() {
        setTitle("Player - " + "DVD");
        setIconImage(new PlayerImage(64, 64));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        dvdPanel = new DVDPanel();
        moviePanel = new MoviePanel();
        setContentPane(dvdPanel);

        setJMenuBar(createMenuBar());

        setMinimumSize(new Dimension(1024, 700));
        pack();
        setLocationRelativeTo(null);
    }

    private void showPanel(String name) {
        switch (name) {
            case "DVD" -> {
                moviePanel.getMovieComponent().stop();
                SwingUtilities.invokeLater(() -> {
                    setContentPane(dvdPanel);
                    revalidate();
                    repaint();
                });
            }
            case "Movie" -> {
                dvdPanel.getMovieComponent().stop();
                SwingUtilities.invokeLater(() -> {
                    setContentPane(moviePanel);
                    revalidate();
                    repaint();
                });
            }
            default -> throw new IllegalArgumentException("Unknown panel name: " + name);
        }
        setTitle("Player - " + name);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu changePanelMenu = new JMenu("Change Player");
        JMenuItem dvdMenuItem = new JMenuItem("DVD");
        JMenuItem movieMenuItem = new JMenuItem("Movie");
        changePanelMenu.add(dvdMenuItem).addActionListener(e -> showPanel("DVD"));
        changePanelMenu.add(movieMenuItem).addActionListener(e -> showPanel("Movie"));
        changePanelMenu.setMnemonic('C');
        dvdMenuItem.setMnemonic('D');
        movieMenuItem.setMnemonic('M');
        menuBar.add(changePanelMenu);

        JMenu controlPanelMenu = new JMenu("Control Panel");
        JMenuItem display = new JMenuItem("Display");
        display.addActionListener(_ -> {
            dvdPanel.displayControlPanel(true);
            moviePanel.displayControlPanel(true);
            revalidate();
            repaint();
        });
        JMenuItem hidden = new JMenuItem("Hidden");
        hidden.addActionListener(_ -> {
            dvdPanel.displayControlPanel(false);
            moviePanel.displayControlPanel(false);
            revalidate();
            repaint();
        });
        controlPanelMenu.add(display);
        controlPanelMenu.add(hidden);
        menuBar.add(controlPanelMenu);
        controlPanelMenu.setMnemonic('P');
        display.setMnemonic('D');
        hidden.setMnemonic('H');

        return menuBar;
    }

    @Override
    public void dispose() {
        dvdPanel.getMovieComponent().stop();
        moviePanel.getMovieComponent().stop();
        super.dispose();
        System.exit(0);
    }

}
