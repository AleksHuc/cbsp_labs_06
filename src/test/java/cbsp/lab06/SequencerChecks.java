package cbsp.lab06;

import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;

/** Actual sequencer -> transmitter -> capturing receiver; never opens an audio device. */
public final class SequencerChecks {
    private SequencerChecks() { }

    public static void main(String[] args) throws Exception {
        Sequence sequence = MidiSupport.fixed(480, 120);
        Track track = MidiSupport.namedTrack(sequence, "Device-free transport fixture");
        MidiSupport.add(track, Programs.change(2, Music.VIOLIN), 0);
        MidiSupport.add(track, NoteMessages.on(2, 69, 90), 0);
        MidiSupport.add(track, NoteMessages.off(2, 69, 64), 48);
        MidiSupport.end(sequence, 96);
        List<String> captures = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        Receiver receiver = new Receiver() {
            public synchronized void send(MidiMessage message, long timestamp) {
                // Providers can send cleanup controller messages and restore program state on stop.
                if (message instanceof ShortMessage s && s.getChannel() == 2
                        && (s.getCommand() == ShortMessage.PROGRAM_CHANGE || s.getCommand() == ShortMessage.NOTE_ON
                        || s.getCommand() == ShortMessage.NOTE_OFF)) {
                    captures.add(MidiSupport.hex(message)); times.add(System.nanoTime());
                }
            }
            public void close() { }
        };
        try (receiver) {
            MidiPlayback.sequence(sequence, receiver);
            synchronized (receiver) {
                List<String> expected = List.of("C2 28", "92 45 5A", "82 45 40");
                if (captures.size() < 3 || !captures.subList(0, 3).equals(expected)
                        || captures.subList(3, captures.size()).stream().anyMatch(m -> !m.equals("C2 28")))
                    throw new AssertionError("Actual sequencer transport: " + captures);
                double heldSeconds = (times.get(2) - times.get(1)) / 1e9;
                // Loose timing guard: verifies real scheduling without treating desktop jitter as MIDI arithmetic.
                if (heldSeconds < 0.01 || heldSeconds > 2)
                    throw new AssertionError("Unexpected real-time note interval: " + heldSeconds);
                System.out.println("SequencerChecks: actual sequencer transport passed; no synthesizer or audio device.");
            }
        }
    }
}
