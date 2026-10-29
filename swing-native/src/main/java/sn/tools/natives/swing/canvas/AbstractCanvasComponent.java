package sn.tools.natives.swing.canvas;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public abstract class AbstractCanvasComponent<T extends AbstractCanvas> extends JComponent {

    protected T canvas;
    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> future;

    public AbstractCanvasComponent(T canvas) {
        super();
        this.canvas = canvas;
        setLayout(new BorderLayout());
        add(canvas, BorderLayout.CENTER);
        addComponentListener(new ComponentListener() {

            @Override
            public void componentHidden(ComponentEvent arg0) {
            }

            @Override
            public void componentMoved(ComponentEvent arg0) {
                canvasPaintComponent();
            }

            @Override
            public void componentResized(ComponentEvent arg0) {
                canvasPaintComponent();
            }

            @Override
            public void componentShown(ComponentEvent arg0) {
            }

        });
    }

    private void canvasPaintComponent() {
        if (scheduler == null || scheduler.isShutdown()) {
            return;
        }

        if (future != null) {
            future.cancel(false);
        }

        if (canvas == null) {
            return;
        }

        future = scheduler.schedule(() -> SwingUtilities.invokeLater(() -> canvas.paintComponent()),
                50,
                TimeUnit.MILLISECONDS);
    }

    // @Override
    // protected void paintComponent(Graphics g) {
    // super.paintComponent(g);
    // canvasPaintComponent();
    // }

    @Override
    public void addNotify() {
        super.addNotify();
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

}
