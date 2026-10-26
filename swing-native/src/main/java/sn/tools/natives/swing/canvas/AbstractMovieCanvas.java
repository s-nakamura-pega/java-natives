package sn.tools.natives.swing.canvas;

import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import sn.tools.natives.object.NativeObject;

abstract class AbstractMovieCanvas extends Canvas implements NativeCanvas {

	private static final long serialVersionUID = 1L;

	private long handleId = 0;

	private BufferStrategy bufferStrategy;
	private int movieWidth, movieHeight;
	private byte[] frameBuffer;
	private BufferedImage img;
	private long currentPoint;
	private boolean isUseRasterTransfer;

	private int drawX, drawY, draWidth, drawHeight;

	private Consumer<Long> pointRenderer = _ -> {
	};

	@Override
	public synchronized void initCanvas(int width, int height, boolean isUseRasterTransfer) {
		this.movieWidth = width;
		this.movieHeight = height;
		this.isUseRasterTransfer = isUseRasterTransfer;
		this.img = new BufferedImage(movieWidth, movieHeight, BufferedImage.TYPE_3BYTE_BGR);
		this.frameBuffer = isUseRasterTransfer ? new byte[width * height * 3]
				: ((DataBufferByte) (img.getRaster().getDataBuffer())).getData();
	}

	@Override
	public synchronized void display(ByteBuffer buffer) {
		buffer.rewind();
		buffer.get(frameBuffer);
		if (frameBuffer.length > 0) {
			if (isUseRasterTransfer) {
				img.getRaster().setDataElements(0, 0, movieWidth, movieHeight, frameBuffer);
			}
			paintComponent();
		}
	}

	@Override
	public void close() {
		destroy();
		handleId = 0;
	}

	@Override
	public synchronized void paintComponent() {
		if (bufferStrategy == null || !isDisplayable() || getParent() == null) {
			return;
		}
		if (img != null) {
			Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
			g.setRenderingHint(
					RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			// g.setRenderingHint(
			// RenderingHints.KEY_RENDERING,
			// RenderingHints.VALUE_RENDER_QUALITY);

			// g.setRenderingHint(
			// RenderingHints.KEY_ALPHA_INTERPOLATION,
			// RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			try {
				int cw = getWidth();
				int ch = getHeight();

				// 元の動画サイズ
				double iw = movieWidth;
				double ih = movieHeight;

				// コンポーネントのアスペクト比
				double aspectCanvas = (double) cw / ch;
				// 画像のアスペクト比
				double aspectImage = iw / ih;

				int drawW, drawH;

				if (aspectCanvas > aspectImage) {
					// コンポーネントの方が横に広い → 高さに合わせる
					drawH = ch;
					drawW = (int) (ch * aspectImage);
				} else {
					// コンポーネントの方が縦に広い → 幅に合わせる
					drawW = cw;
					drawH = (int) (cw / aspectImage);
				}

				// 中央に配置
				int x = (cw - drawW) / 2;
				int y = (ch - drawH) / 2;

				g.drawImage(img, x, y, drawW, drawH, null);

				this.drawX = x;
				this.drawY = y;
				this.draWidth = drawW;
				this.drawHeight = drawH;
			} finally {
				g.dispose();
			}
			bufferStrategy.show();
			Toolkit.getDefaultToolkit().sync();
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		SwingUtilities.invokeLater(() -> {
			createBufferStrategy(2);
			bufferStrategy = getBufferStrategy();
		});
		handleId = create();
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		NativeObject.close(this);
		if (bufferStrategy != null) {
			bufferStrategy.dispose();
			bufferStrategy = null;
		}
	}

	@Override
	public long getHandleId() {
		return handleId;
	}

	public long getCurrentPoint() {
		return currentPoint;
	}

	public synchronized void setCurrentPoint(long currentPoint) {
		this.currentPoint = currentPoint;
		SwingUtilities.invokeLater(() -> pointRenderer.accept(currentPoint));
	}

	public void setPointRenderer(Consumer<Long> pointRenderer) {
		this.pointRenderer = pointRenderer;
	}

	@Override
	public void setHandleId(long handleId) {
		this.handleId = handleId;
	}

	public int getMovieWidth() {
		return movieWidth;
	}

	public int getMovieHeight() {
		return movieHeight;
	}

	public boolean isUseRasterTransfer() {
		return isUseRasterTransfer;
	}

	public int getDrawX() {
		return drawX;
	}

	public int getDrawY() {
		return drawY;
	}

	public int getDraWidth() {
		return draWidth;
	}

	public int getDrawHeight() {
		return drawHeight;
	}

	protected abstract long create();

	protected abstract void destroy();

}
