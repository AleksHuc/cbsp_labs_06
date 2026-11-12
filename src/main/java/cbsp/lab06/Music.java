package cbsp.lab06;

import java.util.List;

/** Prepared data and validation. Times are absolute ticks, not delta times. */
public final class Music {
    public static final int PPQ = 480;
    public static final int PIANO = 0, VIOLIN = 40, FLUTE = 73;
    private Music() { }

    public record Note(int channel, int number, long start, long duration, int velocity) {
        public Note {
            range(channel, 0, 15, "channel");
            range(number, 0, 127, "note");
            range(velocity, 1, 127, "attack velocity");
            if (start < 0 || duration <= 0 || start > Long.MAX_VALUE - duration)
                throw new IllegalArgumentException("Invalid note position/duration.");
        }
        public long end() { return start + duration; }
    }

    public record Tempo(long tick, int microsPerQuarter) {
        public Tempo {
            if (tick < 0) throw new IllegalArgumentException("Negative tempo position.");
            range(microsPerQuarter, 1, 0xffffff, "microseconds per quarter");
        }
    }

    public static void range(int value, int min, int max, String label) {
        if (value < min || value > max)
            throw new IllegalArgumentException(label + " must be in " + min + ".." + max);
    }

    public static void tempoMap(int ppq, List<Tempo> map) {
        if (ppq <= 0 || map.isEmpty() || map.get(0).tick() != 0)
            throw new IllegalArgumentException("Positive PPQ and a tempo at tick zero are required.");
        for (int i = 1; i < map.size(); i++)
            if (map.get(i).tick() <= map.get(i - 1).tick())
                throw new IllegalArgumentException("Tempo positions must be strictly increasing.");
    }
}
