package cbsp.lab06;

import javax.sound.midi.*;

public final class Sustain {
    private Sustain() { }

    public static boolean held(int cc64) {
        Music.range(cc64, 0, 127, "CC64");
        // TODO (Task 6): implement the pedal threshold.
        throw new UnsupportedOperationException("TODO (Task 6): implement the pedal threshold.");
    }

    public static void pedal(Track track, int value, long upTick) {
        Music.range(value, 0, 127, "CC64");
        if (upTick <= 0) throw new IllegalArgumentException("Pedal-up position must be positive.");
        // TODO (Task 6): set the requested pedal state, then explicitly release it.
        throw new UnsupportedOperationException("TODO (Task 6): set the requested pedal state, then explicitly release it.");
    }
}
