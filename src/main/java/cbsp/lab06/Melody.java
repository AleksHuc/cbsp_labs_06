package cbsp.lab06;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;
import cbsp.lab06.Music.Note;

public final class Melody {
    /** Prepared transcription of 6MIDI.pdf, slide 14, relative to F4 (65). */
    public static final List<Integer> OFFSETS = List.of(0, 0, 0, -5, -3, -3, -5, 4, 4, 2, 2, 0);
    public static final List<Double> QUARTERS = List.of(1., 1., 1., 1., 1., 1., 2., 1., 1., 1., 1., 2.);
    public static final int LENGTH_QUARTERS = 14;
    private Melody() { }

    public static long ticks(double quarters, int ppq) {
        if (!Double.isFinite(quarters) || quarters < 0 || ppq <= 0 || quarters * ppq > Long.MAX_VALUE)
            throw new IllegalArgumentException("Invalid musical duration or PPQ.");
        // TODO (Task 2): convert quarter-note units to the nearest tick.
        throw new UnsupportedOperationException("TODO (Task 2): convert quarter-note units to the nearest tick.");
    }

    public static double seconds(long ticks, int ppq, double bpm) {
        if (ticks < 0 || ppq <= 0 || !Double.isFinite(bpm) || bpm <= 0)
            throw new IllegalArgumentException("Invalid constant-tempo conversion.");
        // TODO (Task 2): convert ticks to seconds at a constant quarter-note tempo.
        throw new UnsupportedOperationException("TODO (Task 2): convert ticks to seconds at a constant quarter-note tempo.");
    }

    public static List<Note> notes(int root, int channel, int velocity, int ppq) {
        Music.range(root, 5, 123, "melody root");
        // TODO (Task 2): accumulate the prepared durations into absolute start ticks.
        throw new UnsupportedOperationException("TODO (Task 2): accumulate the prepared durations into absolute start ticks.");
    }

    public static void add(Track track, List<Note> notes) {
        // TODO (Task 2): store a Note On and a matching Note Off for each note.
        throw new UnsupportedOperationException("TODO (Task 2): store a Note On and a matching Note Off for each note.");
    }
}
