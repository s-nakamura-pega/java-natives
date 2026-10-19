package sn.tools.demo.dvd.icon;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

/**
 * BufferedImageを継承し、トレイが開いてDVDディスクが見えている状態を描画するクラス
 */
public class DVDPlayerImage extends BufferedImage {

    public DVDPlayerImage(int width, int height) {
        super(width, height, BufferedImage.TYPE_INT_ARGB);
        drawOpenDVDPlayer();
    }

    private void drawOpenDVDPlayer() {
        Graphics2D g2d = createGraphics();

        // 描画品質の向上
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. 本体の背景（メタリックなグラデーション）
        GradientPaint bodyGradient = new GradientPaint(
                0, 0, new Color(70, 70, 75),
                0, height, new Color(35, 35, 40));
        g2d.setPaint(bodyGradient);
        g2d.fillRoundRect(10, 10, width - 20, height - 20, 20, 20);

        g2d.setColor(new Color(120, 120, 130));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(10, 10, width - 20, height - 20, 20, 20);

        // 2. 開いたディスクレイ（トレイ）の部分
        // 本体中央から手前に飛び出しているトレイのベース
        int trayX = 40;
        int trayY = 30;
        int trayWidth = width - 80;
        int trayHeight = height - 45;

        // トレイの凹み（ダークグレー）
        g2d.setColor(new Color(25, 25, 28));
        g2d.fillRoundRect(trayX, trayY, trayWidth, trayHeight, 15, 15);
        g2d.setColor(new Color(50, 50, 55));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(trayX, trayY, trayWidth, trayHeight, 15, 15);

        // 3. DVDディスク本体の描画（トレイの中心に配置）
        int discSize = trayHeight - 15;
        int discX = trayX + (trayWidth - discSize) / 2;
        int discY = trayY + (trayHeight - discSize) / 2;

        // ディスクの影（立体感）
        g2d.setColor(new Color(10, 10, 10, 100));
        g2d.fillOval(discX + 3, discY + 3, discSize, discSize);

        // DVDのシルバー＆レインボーの光沢（グラデーションを重ねる）
        // ディスク外周（銀色）
        GradientPaint discBase = new GradientPaint(
                discX, discY, new Color(220, 225, 230),
                discX + discSize, discY + discSize, new Color(140, 145, 155));
        g2d.setPaint(discBase);
        g2d.fillOval(discX, discY, discSize, discSize);

        // DVD特有の虹色の反射（うっすら半透明の円弧やグラデーション）
        GradientPaint rainbowReflect = new GradientPaint(
                discX, discY, new Color(255, 150, 200, 60),
                discX + discSize, discY, new Color(100, 220, 255, 60));
        g2d.setPaint(rainbowReflect);
        g2d.fillOval(discX + 10, discY + 10, discSize - 20, discSize - 20);

        // ディスクのデータ記録面（少し内側の円）
        g2d.setColor(new Color(180, 190, 200, 150));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawOval(discX + 30, discY + 30, discSize - 60, discSize - 60);
        g2d.drawOval(discX + 50, discY + 50, discSize - 100, discSize - 100);

        // ディスク中心の透明なプラスチック部分（ハブ）
        g2d.setColor(new Color(40, 40, 45));
        int hubSize = 36;
        int hubX = discX + (discSize - hubSize) / 2;
        int hubY = discY + (discSize - hubSize) / 2;
        g2d.fillOval(hubX, hubY, hubSize, hubSize);

        // 中央の穴（トレイの底の色が見える）
        g2d.setColor(new Color(25, 25, 28));
        int holeSize = 14;
        g2d.fillOval(hubX + (hubSize - holeSize) / 2, hubY + (hubSize - holeSize) / 2, holeSize, holeSize);

        // 4. 左上のメーカーロゴや「OPEN」表示など
        g2d.setColor(new Color(200, 200, 210));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2d.drawString("OPEN / CLOSE", 55, 52);

        // ディスプレイ部分（"OPEN" 表示）
        int panelWidth = 90;
        int panelHeight = 26;
        int panelX = width - 130;
        int panelY = 38;

        g2d.setColor(new Color(15, 20, 18));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 6, 6);
        g2d.setColor(new Color(60, 80, 70));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 6, 6);

        g2d.setColor(new Color(255, 100, 100)); // オレンジ/レッド系のOPEN表示
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.drawString("OPEN", panelX + 24, panelY + 18);

        g2d.dispose();
    }

    public static void main(String[] args) throws IOException {
        ImageIO.write(new DVDPlayerImage(64, 64), "png", Paths.get("dvd_player_icon.png").toFile());
    }
}