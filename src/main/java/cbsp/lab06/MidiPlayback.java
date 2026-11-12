package cbsp.lab06;

import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.sound.midi.*;

/** Prepared audio lifecycle. Offline exports never call this class. */
public final class MidiPlayback {
    public enum Route { SEQUENCER, RECEIVER, DIRECT_CHANNEL }
    private MidiPlayback() { }

    public static void play(Sequence sequence, Route route) throws Exception {
        try (Synthesizer synth = MidiSystem.getSynthesizer()) {
            synth.open();
            Soundbank bank = synth.getDefaultSoundbank();
            System.out.println("Synthesizer: " + synth.getDeviceInfo());
            System.out.println("Sound bank: " + (bank == null ? "provider-defined / no default bank" : bank.getName()));
            if (synth.getAvailableInstruments().length == 0)
                throw new MidiUnavailableException("No instruments available. Install/configure a GM sound bank.");
            try (Receiver receiver = NoteMessages.connect(synth)) {
                reset(receiver);
                try {
                    switch (route) {
                        case SEQUENCER -> sequence(sequence, receiver);
                        case RECEIVER -> scheduled(sequence, (message) -> receiver.send(message, -1));
                        case DIRECT_CHANNEL -> {
                            MidiChannel[] channels = synth.getChannels();
                            scheduled(sequence, message -> Programs.direct(channels[message.getChannel()], message));
                        }
                    }
                    // Give the actual instrument's release envelope time; not an assertion of silence.
                    TimeUnit.MILLISECONDS.sleep(1200);
                } finally { reset(receiver); }
            }
        }
    }

    public static void sequence(Sequence sequence, Receiver receiver) throws Exception {
        // false prevents the provider from implicitly opening a synthesizer.
        try (Sequencer sequencer = MidiSystem.getSequencer(false)) {
            if (sequencer == null) throw new MidiUnavailableException("No sequencer provider.");
            sequencer.open();
            try (Transmitter transmitter = sequencer.getTransmitter()) {
                transmitter.setReceiver(receiver);
                sequencer.setSequence(sequence);
                sequencer.setTempoFactor(1.0f);
                sequencer.setTickPosition(0);
                long timeout = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(sequence.getMicrosecondLength())
                        + TimeUnit.SECONDS.toNanos(5);
                sequencer.start();
                try {
                    while (sequencer.isRunning() || sequencer.getTickPosition() < sequence.getTickLength()) {
                        if (System.nanoTime() > timeout) throw new IllegalStateException("Sequencer did not finish.");
                        TimeUnit.MILLISECONDS.sleep(5);
                    }
                } finally { sequencer.stop(); }
            }
        }
    }

    @FunctionalInterface
    private interface Sender { void send(ShortMessage message); }

    /** Only Tasks 1 and 3: absolute monotonic deadlines prevent accumulated sleep drift. */
    private static void scheduled(Sequence sequence, Sender sender) throws InterruptedException {
        List<Music.Tempo> tempos = MidiSupport.tempos(sequence);
        long start = System.nanoTime();
        for (MidiSupport.Event event : MidiSupport.events(sequence)) {
            if (!(event.message() instanceof ShortMessage message)) continue;
            waitUntil(start + Math.round(MidiSupport.time(event.tick(), sequence.getResolution(), tempos) * 1e9));
            sender.send(message);
        }
        waitUntil(start + Math.round(MidiSupport.time(sequence.getTickLength(), sequence.getResolution(), tempos) * 1e9));
    }

    private static void waitUntil(long deadline) throws InterruptedException {
        long remaining;
        while ((remaining = deadline - System.nanoTime()) > 0)
            TimeUnit.NANOSECONDS.sleep(remaining);
    }

    private static void reset(Receiver receiver) {
        for (int ch = 0; ch < 16; ch++) {
            receiver.send(MidiSupport.cc(ch, 64, 0), -1);
            receiver.send(MidiSupport.cc(ch, 123, 0), -1);
            receiver.send(MidiSupport.cc(ch, 120, 0), -1);
            receiver.send(MidiSupport.cc(ch, 121, 0), -1);
            receiver.send(MidiSupport.cc(ch, 7, 100), -1);
            receiver.send(MidiSupport.cc(ch, 10, 64), -1);
            receiver.send(MidiSupport.cc(ch, 11, 127), -1);
            receiver.send(MidiSupport.message(ShortMessage.PITCH_BEND, ch, 0, 64), -1);
            receiver.send(MidiSupport.message(ShortMessage.PROGRAM_CHANGE, ch, 0, 0), -1);
        }
    }
}
