package sn.tools.demo.movie.icon;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * BufferedImageを継承し、白ベースのモダンなムービープレイヤーを描画するクラス
 */
public class MoviePlayerImage extends BufferedImage {

    public MoviePlayerImage(int width, int height) {
        super(width, height, BufferedImage.TYPE_INT_ARGB);
        drawMoviePlayer();
    }

    private void drawMoviePlayer() {
        Graphics2D g2d = createGraphics();

        // 描画品質の最大化（アンチエイリアス有効化）
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        // 1. 空のグラデーション（夜空のディープパープルから夕暮れのレッド・オレンジへ）
        GradientPaint skyGradient = new GradientPaint(
                0, 0, new Color(15, 10, 35), // 上部：夜空の深い紺色
                0, height * 0.65f, new Color(230, 95, 50) // 下部：夕焼けの鮮やかなオレンジ
        );
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, width, height);

        // 2. 太陽または月の発光（ラジアルグラデーション）
        RadialGradientPaint sunGlow = new RadialGradientPaint(
                new Point2D.Float(width * 0.75f, height * 0.42f),
                90,
                new float[] { 0.0f, 0.4f, 1.0f },
                new Color[] {
                        new Color(255, 255, 200, 220),
                        new Color(255, 150, 50, 100),
                        new Color(0, 0, 0, 0)
                });
        g2d.setPaint(sunGlow);
        g2d.fill(new Ellipse2D.Float(width * 0.75f - 90, height * 0.42f - 90, 180, 180));

        // 3. 遠景の山（シルエット）
        g2d.setColor(new Color(45, 28, 55));
        Path2D mountainDistant = new Path2D.Float();
        mountainDistant.moveTo(0, height * 0.7);
        mountainDistant.lineTo(width * 0.22, height * 0.48);
        mountainDistant.lineTo(width * 0.5, height * 0.65);
        mountainDistant.lineTo(width * 0.78, height * 0.42);
        mountainDistant.lineTo(width, height * 0.58);
        mountainDistant.lineTo(width, height);
        mountainDistant.lineTo(0, height);
        mountainDistant.closePath();
        g2d.fill(mountainDistant);

        // 4. 近景の山・丘（シルエット）
        g2d.setColor(new Color(20, 12, 30));
        Path2D mountainClose = new Path2D.Float();
        mountainClose.moveTo(0, height * 0.76);
        mountainClose.lineTo(width * 0.15, height * 0.62);
        mountainClose.lineTo(width * 0.42, height * 0.82);
        mountainClose.lineTo(width * 0.7, height * 0.55);
        mountainClose.lineTo(width, height * 0.72);
        mountainClose.lineTo(width, height);
        mountainClose.lineTo(0, height);
        mountainClose.closePath();
        g2d.fill(mountainClose);

        // 5. 映画のシグネチャーである上下の黒帯（レターボックス）
        int barHeight = (int) (height * 0.13);
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, width, barHeight); // 上の帯
        g2d.fillRect(0, height - barHeight, width, barHeight); // 下の帯

        // 6. 映画風の字幕・クレジットテキスト
        g2d.setColor(new Color(220, 220, 230, 190));
        g2d.setFont(new Font("Serif", Font.ITALIC | Font.BOLD, 14));
        g2d.drawString("－ A CINEMATIC MOMENT －", 35, height - barHeight - 12);

        g2d.dispose();
    }

    public static void main(String[] args) throws IOException {
        ImageIO.write(new MoviePlayerImage(64, 64), "png", Paths.get("movie_player_icon.png").toFile());
    }
}