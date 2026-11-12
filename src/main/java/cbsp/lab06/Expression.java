package cbsp.lab06;

import javax.sound.midi.*;

public final class Expression {
    private Expression() { }

    public static void velocity(Track track, int note, int ppq) {
        // TODO (Task 5): three equal-length attacks, velocities 32/80/120, all else fixed.
        throw new UnsupportedOperationException("TODO (Task 5): three equal-length attacks, velocities 32/80/120, all else fixed.");
    }

    public static void volume(Track track, int value) {
        Music.range(value, 0, 127, "volume");
        // TODO (Task 5): set the initial channel balance using CC7.
        throw new UnsupportedOperationException("TODO (Task 5): set the initial channel balance using CC7.");
    }

    public static void phrase(Track track, int ppq) {
        // TODO (Task 5): CC11 values at beats 0/1/2/3/4/5; one attack is held throughout.
        throw new UnsupportedOperationException("TODO (Task 5): CC11 values at beats 0/1/2/3/4/5; one attack is held throughout.");
    }
}
