package cbsp.lab06;

import javax.sound.midi.*;

/** Task 1: implement the receiver and note message constructors marked TODO. */
public final class NoteMessages {
    private NoteMessages() { }

    public static Receiver connect(Synthesizer synthesizer) throws MidiUnavailableException {
        // TODO (Task 1): obtain the receiver of the already opened synthesizer.
        throw new UnsupportedOperationException("TODO (Task 1): obtain the receiver of the already opened synthesizer.");
    }

    public static ShortMessage on(int channel, int note, int velocity) {
        Music.range(velocity, 1, 127, "attack velocity");
        // TODO (Task 1): construct a Note On on the requested channel.
        throw new UnsupportedOperationException("TODO (Task 1): construct a Note On on the requested channel.");
    }

    public static ShortMessage off(int channel, int note, int releaseVelocity) {
        // TODO (Task 1): release exactly the same channel/note pair.
        throw new UnsupportedOperationException("TODO (Task 1): release exactly the same channel/note pair.");
    }

    public static ShortMessage zeroVelocityOff(int channel, int note) {
        // TODO (Task 1): encode the alternate Note Off representation.
        throw new UnsupportedOperationException("TODO (Task 1): encode the alternate Note Off representation.");
    }
}
