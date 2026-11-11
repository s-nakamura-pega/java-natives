package sn.tools.demo.players.panel;

import java.io.File;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import sn.tools.natives.swing.api.movie.Movie;
import sn.tools.natives.swing.canvas.MovieCanvasComponent;

public class MoviePanel extends JPanel implements PlayerPanel {

    private static final int SEEK_BAR_MAX = 1000;

    private final MovieCanvasComponent canvas;
    private final JButton playButton;
    private final JButton stopButton;
    private final JButton loadButton;
    private final JSlider seekBar;
    private final JLabel seekLabel = new JLabel("0:00:00 / 0:00:00");
    private boolean isSeekInnerUpdate = false;
    private boolean isMovingPoint = false;

    private volatile long endPoint = 0;
    private double seekValueScale = 0.0;

    private final Timer stateTimer;

    public MoviePanel() {
        setLayout(new BorderLayout());

        // --- center: MovieCanvas ---
        canvas = new MovieCanvasComponent();
        add(canvas, BorderLayout.CENTER);

        // --- north: ファイル読み込み ---
        JPanel filePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadButton = new JButton("📂 ファイルを開く");
        filePanel.add(loadButton);
        add(filePanel, BorderLayout.NORTH);

        // --- south: 操作パネル ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        playButton = new JButton("▶ 再生");
        stopButton = new JButton("■ 停止");

        seekBar = new JSlider(0, SEEK_BAR_MAX, 0);
        seekBar.setPreferredSize(new java.awt.Dimension(300, 30));

        controlPanel.add(playButton);
        controlPanel.add(stopButton);
        controlPanel.add(seekBar);
        controlPanel.add(seekLabel);
        add(controlPanel, BorderLayout.SOUTH);

        // 初期状態では再生不可
        playButton.setEnabled(false);

        // --- ファイル読み込みイベント ---
        loadButton.addActionListener(_ -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(window);
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                loadMovieFile(file);
            }
        });

        // --- 再生 ---
        playButton.addActionListener(_ -> {
            canvas.start();
            updatePlayButtonState();
        });

        // --- 停止 ---
        stopButton.addActionListener(_ -> {
            canvas.stop();
            updatePlayButtonState();
        });

        // --- シークバー操作 ---
        seekBar.addChangeListener(_ -> {
            if (isSeekInnerUpdate) {
                return;
            }
            if (!seekBar.getValueIsAdjusting()) {
                isMovingPoint = true;
                int ms = seekBar.getValue();
                canvas.movePoint((long) (ms * seekValueScale));
                isMovingPoint = false;
            }
        });

        // MovieCanvas → seekBar の同期
        canvas.setPointRenderer(point -> {
            if (isMovingPoint || seekBar.getValueIsAdjusting()) {
                return;
            }
            String text = PlayerPanel.formatTime(point) + " / " + PlayerPanel.formatTime(endPoint);
            if (!seekLabel.getText().equals(text)) {
                seekLabel.setText(text);
                isSeekInnerUpdate = true;
                seekBar.setValue((int) (point / seekValueScale));
                isSeekInnerUpdate = false;
            }
        });

        // --- 状態監視タイマー（200msごとにボタン状態を更新） ---
        stateTimer = new Timer(200, _ -> updatePlayButtonState());
        stateTimer.start();

        setMinimumSize(new Dimension(850, 600));
    }

    private void loadMovieFile(File file) {
        long length = canvas.setFile(file.getAbsolutePath());
        if (length > 0) {
            endPoint = length;
            seekValueScale = (double) length / SEEK_BAR_MAX;
            seekBar.setValue(0);
            seekLabel.setText(PlayerPanel.formatTime(0) + " / " + PlayerPanel.formatTime(endPoint));
            updatePlayButtonState();
            System.out.println("動画読み込み成功: " + file.getName());
            canvas.start();
        } else {
            System.err.println("動画読み込み失敗");
            JOptionPane.showMessageDialog(this, "動画の読み込みに失敗しました。", "エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 再生ボタンの活性/非活性を更新する
     */
    private void updatePlayButtonState() {
        boolean ready = canvas.isDecodeReady();
        boolean started = canvas.isStarted();

        // 再生可能条件：デコード準備完了 && 再生中ではない
        playButton.setEnabled(ready && !started);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        stateTimer.start();
    }

    @Override
    public void removeNotify() {
        stateTimer.stop();
        super.removeNotify();
    }

    @Override
    public Movie getMovieComponent() {
        return canvas;
    }

}
