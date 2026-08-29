package sn.tools.demo.movie.frame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;

import sn.tools.natives.swing.canvas.MovieCanvas;

public class MovieFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private final MovieCanvas canvas;
	private final JButton playButton;
	private final JButton stopButton;
	private final JButton loadButton;
	private final JSlider seekBar;

	public MovieFrame() {
		super("Movie Player");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());

		// --- center: MovieCanvas ---
		canvas = new MovieCanvas();
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

		seekBar = new JSlider(0, 1000, 0);
		seekBar.setPreferredSize(new java.awt.Dimension(300, 30));

		controlPanel.add(playButton);
		controlPanel.add(stopButton);
		controlPanel.add(seekBar);

		add(controlPanel, BorderLayout.SOUTH);

		// 初期状態では再生不可
		playButton.setEnabled(false);

		// --- ファイル読み込みイベント ---
		loadButton.addActionListener(_ -> {
			JFileChooser chooser = new JFileChooser();
			int result = chooser.showOpenDialog(MovieFrame.this);
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
			int ms = seekBar.getValue();
			canvas.movePoint(ms);
			canvas.setCurrentPoint(ms);
		});

		// MovieCanvas → seekBar の同期
		canvas.setPointRenderer(point -> seekBar.setValue(point));

		// --- 状態監視タイマー（200msごとにボタン状態を更新） ---
		Timer stateTimer = new Timer(200, _ -> updatePlayButtonState());
		stateTimer.start();

		setSize(800, 600);
		setLocationRelativeTo(null);
	}

	private void loadMovieFile(File file) {
		long length = canvas.setFile(file.getAbsolutePath());
		if (length > 0) {
			seekBar.setMaximum((int) length);
			seekBar.setValue(0);
			updatePlayButtonState();
			System.out.println("動画読み込み成功: " + file.getName());
		} else {
			System.err.println("動画読み込み失敗");
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

}
