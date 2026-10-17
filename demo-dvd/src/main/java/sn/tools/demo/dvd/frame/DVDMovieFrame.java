package sn.tools.demo.dvd.frame;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;

import sn.tools.natives.swing.canvas.DVDCanvasComponent;

public class DVDMovieFrame extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final int SEEK_BAR_MAX = 1000;

	private final DVDCanvasComponent canvas;
	private final JButton playButton;
	private final JButton stopButton;
	private final JButton forwardButton;
	private final JButton backButton;
	private final JButton loadButton;
	private final JSlider seekBar;
	private boolean isSeekInnerUpdate = false;
	private boolean isMovingPoint = false;

	private double seekValueScale = 0.0;

	public DVDMovieFrame() {
		super("Movie Player");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());

		// --- center: MovieCanvas ---
		canvas = new DVDCanvasComponent();
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
		backButton = new JButton("|◀◀");
		forwardButton = new JButton("▶▶|");

		seekBar = new JSlider(0, SEEK_BAR_MAX, 0);
		seekBar.setPreferredSize(new Dimension(300, 30));

		controlPanel.add(playButton);
		controlPanel.add(stopButton);
		controlPanel.add(backButton);
		controlPanel.add(forwardButton);
		controlPanel.add(seekBar);

		add(controlPanel, BorderLayout.SOUTH);

		// 初期状態では再生不可
		playButton.setEnabled(false);

		// --- ファイル読み込みイベント ---
		loadButton.addActionListener(_ -> {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int result = chooser.showOpenDialog(DVDMovieFrame.this);
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

		forwardButton.addActionListener(_ -> canvas.skip(true));

		backButton.addActionListener(_ -> canvas.skip(false));

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
			isSeekInnerUpdate = true;
			seekBar.setValue((int) (point / seekValueScale));
			isSeekInnerUpdate = false;
		});

		canvas.setChangeDurationRenderer(duration -> {
			if (isMovingPoint || seekBar.getValueIsAdjusting()) {
				return;
			}
			isSeekInnerUpdate = true;
			seekValueScale = (double) duration / SEEK_BAR_MAX;
			seekBar.setValue(0);
			isSeekInnerUpdate = false;
		});

		// --- 状態監視タイマー（200msごとにボタン状態を更新） ---
		Timer stateTimer = new Timer(200, _ -> updatePlayButtonState());
		stateTimer.start();

		setSize(800, 600);
		setLocationRelativeTo(null);
	}

	private void loadMovieFile(File file) {
		long length = canvas.setFile(file.getAbsolutePath());
		if (length > 0) {
			seekValueScale = (double) length / SEEK_BAR_MAX;
			seekBar.setValue(0);
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
	public void dispose() {
		super.dispose();
		System.exit(0);
	}

}
