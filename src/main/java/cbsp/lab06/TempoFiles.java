package cbsp.lab06;

import java.util.List;
import javax.sound.midi.*;
import cbsp.lab06.Music.Tempo;

public final class TempoFiles {
    private TempoFiles() { }

    public static double secondsAt(long tick, int ppq, List<Tempo> map) {
        Music.tempoMap(ppq, map);
        if (tick < 0) throw new IllegalArgumentException("Negative tick.");
        // TODO (Task 8): integrate each tempo segment up to the requested position.
        throw new UnsupportedOperationException("TODO (Task 8): integrate each tempo segment up to the requested position.");
    }

    public static double duration(long on, long off, int ppq, List<Tempo> map) {
        if (on < 0 || off < on) throw new IllegalArgumentException("Invalid On/Off positions.");
        // TODO (Task 8): subtract two positions, including tempo boundaries inside the note.
        throw new UnsupportedOperationException("TODO (Task 8): subtract two positions, including tempo boundaries inside the note.");
    }

    public static void metadata(Track conductor, int ppq) {
        // TODO (Task 8): name, 4/4, and Set Tempo 120 -> 60 BPM at tick 4*PPQ.
        throw new UnsupportedOperationException("TODO (Task 8): name, 4/4, and Set Tempo 120 -> 60 BPM at tick 4*PPQ.");
    }
}
