package cbsp.lab06;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.midi.*;

/** Prepared PNG plots using Java2D; no display or audio device required. */
public final class Plot {
    static { System.setProperty("java.awt.headless", "true"); }
    private static final int W = 1200, LEFT = 95, RIGHT = 1140;
    private static final Color[] COLORS = {new Color(30, 94, 174), new Color(198, 75, 45),
            new Color(38, 132, 85), new Color(137, 68, 166)};
    private Plot() { }

    public static void write(Sequence sequence, Diagnostics.Analysis analysis, Path prefix) throws IOException {
        piano(sequence, analysis, prefix);
        controls(sequence, analysis, prefix);
        time(sequence, prefix);
    }

    private static BufferedImage canvas(int height) {
        BufferedImage image = new BufferedImage(W, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, W, height); g.dispose();
        return image;
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        return g;
    }

    private static int x(double quarter, double length) {
        return LEFT + (int) Math.round(quarter / length * (RIGHT - LEFT));
    }

    private static void grid(Graphics2D g, int top, int bottom, double length) {
        double step = length <= 8 ? 0.5 : Math.ceil(length / 16);
        for (double quarter = 0; quarter <= length + 1e-9; quarter += step) {
            int x = x(quarter, length);
            g.setColor(new Color(225, 229, 234)); g.drawLine(x, top, x, bottom);
            g.setColor(Color.DARK_GRAY); g.drawString(String.format(java.util.Locale.ROOT, "%.1f", quarter), x - 10, bottom + 20);
        }
        g.setColor(Color.DARK_GRAY); g.drawRect(LEFT, top, RIGHT - LEFT, bottom - top);
    }

    private static double length(Sequence sequence) {
        return Math.max(1, (double) sequence.getTickLength() / sequence.getResolution());
    }

    private static void piano(Sequence sequence, Diagnostics.Analysis analysis, Path prefix) throws IOException {
        BufferedImage image = canvas(570); Graphics2D g = graphics(image);
        g.setColor(Color.BLACK); g.drawString(prefix.getFileName() + " - MIDI key-held durations", LEFT, 28);
        g.drawString("Color = channel; pale extension = pedal-deferred release, not an audio envelope", LEFT, 52);
        int low = analysis.spans().stream().mapToInt(Diagnostics.Span::note).min().orElse(60) - 2;
        int high = analysis.spans().stream().mapToInt(Diagnostics.Span::note).max().orElse(72) + 2;
        int top = 85, bottom = 470;
        double scale = (bottom - top) / (double) (high - low + 1), end = length(sequence);
        for (int note = low; note <= high; note++) {
            int y = bottom - (int) Math.round((note - low + 1) * scale);
            g.setColor((note % 12 == 1 || note % 12 == 3 || note % 12 == 6 || note % 12 == 8 || note % 12 == 10)
                    ? new Color(243, 245, 248) : Color.WHITE);
            g.fillRect(LEFT, y, RIGHT - LEFT, (int) Math.ceil(scale));
            if (high - low < 28 || note % 12 == 0) {
                g.setColor(Color.DARK_GRAY); g.drawString(note + " " + Diagnostics.name(note), 15, y + (int) scale / 2 + 4);
            }
        }
        grid(g, top, bottom, end);
        for (Diagnostics.Span span : analysis.spans()) {
            int y = bottom - (int) Math.round((span.note() - low + 1) * scale) + 2;
            int on = x((double) span.on() / sequence.getResolution(), end);
            int off = x((double) span.off() / sequence.getResolution(), end);
            int held = x((double) span.heldUntil() / sequence.getResolution(), end);
            int height = Math.max(3, (int) scale - 4);
            Color color = COLORS[span.channel() % COLORS.length];
            if (held > off) {
                g.setColor(new Color((color.getRed() + 510) / 3, (color.getGreen() + 510) / 3, (color.getBlue() + 510) / 3));
                g.fillRect(off, y, held - off, height);
            }
            g.setColor(color); g.fillRect(on, y, Math.max(2, off - on), height);
        }
        g.setColor(Color.DARK_GRAY); g.drawString("Position [quarter notes from start]", 450, 520);
        int legendX = LEFT;
        for (int ch : new java.util.TreeSet<>(analysis.channels())) {
            g.setColor(COLORS[ch % COLORS.length]); g.fillRect(legendX, 542, 15, 12);
            g.setColor(Color.DARK_GRAY); g.drawString("Channel " + (ch + 1), legendX + 22, 553); legendX += 145;
        }
        g.dispose(); ImageIO.write(image, "png", Path.of(prefix + "-piano-roll.png").toFile());
    }

    private static void controls(Sequence sequence, Diagnostics.Analysis analysis, Path prefix) throws IOException {
        BufferedImage image = canvas(930); Graphics2D g = graphics(image);
        g.setColor(Color.BLACK); g.drawString(prefix.getFileName() + " - attacks and channel state (values are not dB)", LEFT, 28);
        String[] labels = {"Attack velocity", "CC7 Volume", "CC11 Expression", "CC64 Pedal", "Pitch Bend"};
        double end = length(sequence);
        for (int panel = 0; panel < labels.length; panel++) {
            int top = 70 + panel * 166, bottom = top + 112;
            int max = panel == 4 ? 16383 : 127;
            g.setColor(Color.DARK_GRAY); g.drawString(labels[panel], LEFT, top - 10);
            g.drawString(Integer.toString(max), 32, top + 5); g.drawString("0", 58, bottom);
            grid(g, top, bottom, end);
            if (panel == 3 || panel == 4) {
                int value = panel == 3 ? 64 : 8192;
                int y = bottom - (int) Math.round((double) value / max * (bottom - top));
                g.setColor(new Color(170, 170, 170)); g.drawLine(LEFT, y, RIGHT, y);
                g.drawString(Integer.toString(value), 32, y);
            }
            for (int ch : new java.util.TreeSet<>(analysis.channels())) {
                g.setColor(COLORS[ch % COLORS.length]);
                int lastX = LEFT;
                int lastValue = switch (panel) { case 1 -> 100; case 2 -> 127; case 4 -> 8192; default -> 0; };
                int lastY = bottom - (int) Math.round((double) lastValue / max * (bottom - top));
                for (Diagnostics.Row row : analysis.rows()) {
                    if (!(row.event().message() instanceof ShortMessage m) || m.getStatus() >= 0xf0 || m.getChannel() != ch) continue;
                    int at = x((double) row.event().tick() / sequence.getResolution(), end);
                    if (panel == 0) {
                        if (MidiSupport.isOn(m)) {
                            int y = bottom - (int) Math.round((double) m.getData2() / max * (bottom - top));
                            g.drawLine(at, bottom, at, y); g.fillOval(at - 3, y - 3, 6, 6);
                        }
                    } else {
                        int value = switch (panel) { case 1 -> row.volume(); case 2 -> row.expression(); case 3 -> row.pedal(); default -> row.bend(); };
                        int y = bottom - (int) Math.round((double) value / max * (bottom - top));
                        g.drawLine(lastX, lastY, at, lastY); g.drawLine(at, lastY, at, y);
                        lastX = at; lastY = y;
                    }
                }
                if (panel != 0) g.drawLine(lastX, lastY, RIGHT, lastY);
            }
        }
        g.setColor(Color.DARK_GRAY); g.drawString("Position [quarter notes from start]; channel colors match piano roll", LEFT, 920);
        g.dispose(); ImageIO.write(image, "png", Path.of(prefix + "-controllers.png").toFile());
    }

    private static void time(Sequence sequence, Path prefix) throws IOException {
        BufferedImage image = canvas(440); Graphics2D g = graphics(image);
        int ppq = sequence.getResolution(), top = 65, bottom = 350;
        List<Music.Tempo> tempos = MidiSupport.tempos(sequence);
        double seconds = Math.max(0.1, MidiSupport.time(sequence.getTickLength(), ppq, tempos));
        double end = length(sequence);
        g.setColor(Color.BLACK); g.drawString(prefix.getFileName() + " - musical position to elapsed time", LEFT, 28);
        grid(g, top, bottom, end);
        for (int i = 0; i <= 4; i++) {
            int y = bottom - (bottom - top) * i / 4;
            g.setColor(new Color(225, 229, 234)); g.drawLine(LEFT, y, RIGHT, y);
            g.setColor(Color.DARK_GRAY); g.drawString(String.format(java.util.Locale.ROOT, "%.2f s", seconds * i / 4), 20, y + 5);
        }
        int lastX = LEFT, lastY = bottom;
        g.setColor(COLORS[0]); g.setStroke(new BasicStroke(2));
        java.util.TreeSet<Long> positions = new java.util.TreeSet<>();
        for (Music.Tempo tempo : tempos) positions.add(tempo.tick());
        positions.add(sequence.getTickLength());
        for (long tick : positions) {
            int nextX = x((double) tick / ppq, end);
            int nextY = bottom - (int) Math.round(MidiSupport.time(tick, ppq, tempos) / seconds * (bottom - top));
            g.drawLine(lastX, lastY, nextX, nextY); lastX = nextX; lastY = nextY;
        }
        g.setColor(Color.DARK_GRAY); g.drawString("Position [quarter notes from start]", 450, 407);
        g.dispose(); ImageIO.write(image, "png", Path.of(prefix + "-time.png").toFile());
    }
}
