package sn.tools.demo.players.panel;

import javax.swing.JPanel;

import sn.tools.natives.swing.canvas.AbstractCanvasComponent;

public abstract class PlayerPanel<T extends AbstractCanvasComponent<?>> extends JPanel {

    protected final T canvasComponent;

    public PlayerPanel(T canvasComponent) {
        this.canvasComponent = canvasComponent;
    }

    public void dispose() {
        canvasComponent.close();
    }

    public abstract void displayControlPanel(boolean isDisplay);

    public static String formatTime(long time) {
        long hours = time / 3600000;
        long minutes = (time % 3600000) / 60000;
        long seconds = (time % 60000) / 1000;
        return String.format("%2d:%02d:%02d", hours, minutes, seconds);
    }

}
