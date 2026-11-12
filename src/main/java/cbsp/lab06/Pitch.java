package cbsp.lab06;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;
import cbsp.lab06.Music.Note;

public final class Pitch {
    private Pitch() { }

    public static List<Note> transpose(List<Note> notes, int semitones) {
        for (Note note : notes)
            if ((long) note.number() + semitones < 0 || (long) note.number() + semitones > 127)
                throw new IllegalArgumentException("Transposed note is outside MIDI range.");
        // TODO (Task 7): return new notes, preserving channel, positions, duration, velocity.
        throw new UnsupportedOperationException("TODO (Task 7): return new notes, preserving channel, positions, duration, velocity.");
    }

    public static ShortMessage bend(int channel, int value) {
        Music.range(value, 0, 16383, "pitch bend");
        // TODO (Task 7): split the 14-bit value into low then high seven-bit fields.
        throw new UnsupportedOperationException("TODO (Task 7): split the 14-bit value into low then high seven-bit fields.");
    }

    public static void curve(Track track, int channel, int ppq) {
        // TODO (Task 7): go from center to maximum and return before release at beat 4.
        throw new UnsupportedOperationException("TODO (Task 7): go from center to maximum and return before release at beat 4.");
    }
}
