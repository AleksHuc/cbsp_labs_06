package cbsp.lab06;

import javax.sound.midi.*;

public final class Programs {
    private Programs() { }

    public static ShortMessage change(int channel, int transmittedProgram) {
        Music.range(transmittedProgram, 0, 127, "program");
        // TODO (Task 3): Program Change has one data byte (not the list number).
        throw new UnsupportedOperationException("TODO (Task 3): Program Change has one data byte (not the list number).");
    }

    public static void direct(MidiChannel channel, ShortMessage message) {
        if (channel == null) throw new IllegalArgumentException("Missing MIDI channel.");
        if (message.getCommand() != ShortMessage.PROGRAM_CHANGE
                && message.getCommand() != ShortMessage.NOTE_ON
                && message.getCommand() != ShortMessage.NOTE_OFF)
            throw new IllegalArgumentException("Only program and note messages belong to Task 3.");
        // TODO (Task 3): perform the corresponding direct MidiChannel operation.
        throw new UnsupportedOperationException("TODO (Task 3): perform the corresponding direct MidiChannel operation.");
    }
}
