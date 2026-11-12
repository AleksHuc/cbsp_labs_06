package cbsp.lab06;

import java.nio.file.*;
import java.util.*;
import javax.sound.midi.*;
import cbsp.lab06.Music.Note;
import cbsp.lab06.MidiPlayback.Route;

/** Prepared independent experiment entry points; shared algorithms are reused between tasks. */
public final class Experiments {
    public record Experiment(String name, String description, Sequence sequence, Route route) { }
    private Experiments() { }

    public static List<Experiment> build(int task, TaskParameters p) {
        return switch (task) {
            case 1 -> one();
            case 2 -> two(p);
            case 3 -> three(p);
            case 4 -> four(p);
            case 5 -> five(p);
            case 6 -> six(p);
            case 7 -> seven(p);
            case 8 -> eight(p);
            default -> throw new IllegalArgumentException("Task must be 1..8.");
        };
    }

    public static void run(int task, TaskParameters p, boolean play) throws Exception {
        Path directory = Path.of("output", p.studentId());
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("parameters.txt"), Diagnostics.parameterText(p));
        for (Experiment experiment : build(task, p)) {
            Path prefix = directory.resolve(experiment.name());
            Diagnostics.Analysis analysis = Diagnostics.export(experiment.sequence(), prefix, p, experiment.description());
            if (task == 3 && experiment.route() == Route.DIRECT_CHANNEL) compareDirect(experiment.sequence(), prefix);
            if (task == 8) roundtrip(experiment.sequence(), prefix);
            System.out.println(experiment.name() + ": " + analysis.spans().size() + " notes, "
                    + experiment.sequence().getTracks().length + " tracks, max key-held=" + analysis.maxKeyHeld());
            if (play) MidiPlayback.play(experiment.sequence(), experiment.route());
        }
        System.out.println("Exported: " + directory.toAbsolutePath());
    }

    private static List<Experiment> one() {
        List<Experiment> experiments = new ArrayList<>();
        for (boolean zeroOff : new boolean[]{false, true}) {
            Sequence sequence = MidiSupport.fixed(Music.PPQ, 120);
            Track track = preparedInstrument(sequence, "Middle C for two seconds", Music.PIANO);
            MidiSupport.add(track, NoteMessages.on(0, 60, 100), 0);
            MidiSupport.add(track, zeroOff ? NoteMessages.zeroVelocityOff(0, 60) : NoteMessages.off(0, 60, 64), 1920);
            MidiSupport.end(sequence, 2400);
            experiments.add(new Experiment("task1-" + (zeroOff ? "zero-velocity-off" : "note-off"),
                    "C4=60, velocity 100; receiver route, key held for 2 s", sequence, Route.RECEIVER));
        }
        return List.copyOf(experiments);
    }

    private static List<Experiment> two(TaskParameters p) {
        return List.of(melody("task2-assigned", p, p.bpm(), Music.PPQ, Music.PIANO, Route.SEQUENCER),
                melody("task2-double-tempo", p, p.bpm() * 2, Music.PPQ, Music.PIANO, Route.SEQUENCER),
                melody("task2-double-ppq", p, p.bpm(), Music.PPQ * 2, Music.PIANO, Route.SEQUENCER));
    }

    private static Experiment melody(String name, TaskParameters p, int bpm, int ppq, int program, Route route) {
        Sequence sequence = MidiSupport.fixed(ppq, bpm);
        Track track = MidiSupport.namedTrack(sequence, "Melody");
        MidiSupport.add(track, name.startsWith("task3") ? Programs.change(0, program)
                : MidiSupport.message(ShortMessage.PROGRAM_CHANGE, 0, program, 0), 0);
        Melody.add(track, Melody.notes(p.rootNote(), 0, p.velocity(), ppq));
        MidiSupport.end(sequence, 15L * ppq); // 14-quarter melody followed by one quarter rest
        return new Experiment(name, "Assigned melody, BPM=" + bpm + ", PPQ=" + ppq
                + ", program=" + program + ", route=" + route, sequence, route);
    }

    private static List<Experiment> three(TaskParameters p) {
        List<Experiment> experiments = new ArrayList<>();
        int[] programs = {Music.PIANO, Music.VIOLIN, Music.FLUTE};
        String[] names = {"piano", "violin", "flute"};
        for (int i = 0; i < programs.length; i++)
            for (Route route : new Route[]{Route.RECEIVER, Route.DIRECT_CHANNEL})
                experiments.add(melody("task3-" + names[i] + (route == Route.RECEIVER ? "-messages" : "-direct"),
                        p, p.bpm(), Music.PPQ, programs[i], route));
        return List.copyOf(experiments);
    }

    private static List<Experiment> four(TaskParameters p) {
        Sequence separate = MidiSupport.fixed(Music.PPQ, p.bpm());
        Arrangement.build(separate, p);
        MidiSupport.end(separate, 15L * Music.PPQ);
        Sequence shared = MidiSupport.fixed(Music.PPQ, p.bpm());
        Arrangement.sharedChannel(shared, p);
        MidiSupport.end(shared, 4L * Music.PPQ);
        return List.of(new Experiment("task4-independent", "Piano/chord channel 1, flute +12 channel 2; equal attack velocity", separate, Route.SEQUENCER),
                new Experiment("task4-shared-channel", "Two musical tracks share channel 1; flute program selected before both attacks", shared, Route.SEQUENCER));
    }

    private static List<Experiment> five(TaskParameters p) {
        List<Experiment> experiments = new ArrayList<>();
        Sequence attacks = MidiSupport.fixed(Music.PPQ, 120);
        Track attackTrack = preparedInstrument(attacks, "Attack velocity 32 80 120", Music.PIANO);
        Expression.velocity(attackTrack, p.rootNote(), Music.PPQ);
        MidiSupport.end(attacks, 6L * Music.PPQ);
        experiments.add(new Experiment("task5-velocity", "Only attack velocity changes; CC7=100, CC11=127", attacks, Route.SEQUENCER));
        for (int volume : new int[]{40, 100}) {
            Sequence sequence = MidiSupport.fixed(Music.PPQ, p.bpm());
            Track track = preparedInstrument(sequence, "Channel volume " + volume, Music.PIANO);
            Expression.volume(track, volume);
            Melody.add(track, Melody.notes(p.rootNote(), 0, p.velocity(), Music.PPQ));
            MidiSupport.end(sequence, 15L * Music.PPQ);
            experiments.add(new Experiment("task5-volume" + volume, "Same notes/velocity; only CC7 differs; CC11=127", sequence, Route.SEQUENCER));
        }
        Sequence phrase = MidiSupport.fixed(Music.PPQ, 120);
        Track phraseTrack = preparedInstrument(phrase, "One flute attack, changing expression", Music.FLUTE);
        Expression.phrase(phraseTrack, Music.PPQ);
        Melody.add(phraseTrack, List.of(new Note(0, p.rootNote(), 0, 6L * Music.PPQ, p.velocity())));
        MidiSupport.end(phrase, 7L * Music.PPQ);
        experiments.add(new Experiment("task5-expression", "One flute note; only CC11 changes during key-held time; CC7=100", phrase, Route.SEQUENCER));
        return List.copyOf(experiments);
    }

    private static List<Experiment> six(TaskParameters p) {
        List<Experiment> experiments = new ArrayList<>();
        for (int pedal : new int[]{0, 127, 63, 64}) {
            Sequence sequence = MidiSupport.fixed(Music.PPQ, 120);
            Track track = preparedInstrument(sequence, "Piano, CC64=" + pedal, Music.PIANO);
            Sustain.pedal(track, pedal, 4L * Music.PPQ);
            for (int i = 0; i < 3; i++)
                Melody.add(track, List.of(new Note(0, p.rootNote() + i * 2, (long) i * Music.PPQ,
                        Music.PPQ / 2, p.velocity())));
            MidiSupport.end(sequence, 6L * Music.PPQ);
            experiments.add(new Experiment("task6-pedal" + pedal, "Identical key events; pedal state="
                    + Sustain.held(pedal) + "; pedal up at beat 4", sequence, Route.SEQUENCER));
        }
        return List.copyOf(experiments);
    }

    private static List<Experiment> seven(TaskParameters p) {
        List<Experiment> experiments = new ArrayList<>();
        for (int shift : new int[]{0, p.transpose()}) {
            Sequence sequence = MidiSupport.fixed(Music.PPQ, p.bpm());
            Track track = preparedInstrument(sequence, "Transposition " + shift, Music.VIOLIN);
            List<Note> source = Melody.notes(p.rootNote(), 0, p.velocity(), Music.PPQ);
            Melody.add(track, Pitch.transpose(source, shift));
            MidiSupport.end(sequence, 15L * Music.PPQ);
            experiments.add(new Experiment("task7-transpose" + shift, "All Note On/Off pairs transposed by " + shift, sequence, Route.SEQUENCER));
        }
        Sequence bend = MidiSupport.fixed(Music.PPQ, 120);
        Track bent = preparedInstrument(bend, "Two notes share pitch bend on channel 1", Music.VIOLIN);
        MidiSupport.bendRange(bent, 0);
        Pitch.curve(bent, 0, Music.PPQ);
        Melody.add(bent, List.of(new Note(0, p.rootNote(), 0, 4L * Music.PPQ, p.velocity()),
                new Note(0, p.rootNote() + 7, Music.PPQ, 3L * Music.PPQ, p.velocity())));
        Track reference = MidiSupport.namedTrack(bend, "Unbent reference on channel 2");
        MidiSupport.add(reference, MidiSupport.message(ShortMessage.PROGRAM_CHANGE, 1, Music.VIOLIN, 0), 0);
        Melody.add(reference, List.of(new Note(1, p.rootNote() + 12, Music.PPQ, 3L * Music.PPQ, p.velocity())));
        MidiSupport.end(bend, 5L * Music.PPQ);
        experiments.add(new Experiment("task7-bend", "RPN sets +/-2 semitones; bend persists on channel 1, channel 2 remains centered", bend, Route.SEQUENCER));
        return List.copyOf(experiments);
    }

    private static List<Experiment> eight(TaskParameters p) {
        Sequence sequence = MidiSupport.empty(Music.PPQ);
        Track conductor = sequence.createTrack();
        TempoFiles.metadata(conductor, Music.PPQ);
        Track pulse = preparedInstrument(sequence, "Eight quarter-note pulses", Music.PIANO);
        for (int i = 0; i < 8; i++)
            Melody.add(pulse, List.of(new Note(0, p.rootNote(), (long) i * Music.PPQ, Music.PPQ, p.velocity())));
        Track crossing = MidiSupport.namedTrack(sequence, "Flute crosses the tempo change");
        MidiSupport.add(crossing, MidiSupport.message(ShortMessage.PROGRAM_CHANGE, 1, Music.FLUTE, 0), 0);
        Melody.add(crossing, List.of(new Note(1, p.rootNote() + 7, 3L * Music.PPQ, 2L * Music.PPQ, p.velocity())));
        MidiSupport.end(sequence, 8L * Music.PPQ);
        return List.of(new Experiment("task8-tempo-map", "120 BPM until tick 1920, then 60 BPM; crossing note 1440..2400", sequence, Route.SEQUENCER));
    }

    /** Default instrument selection is prepared outside Task 3. Controller defaults are playback/analysis defaults. */
    private static Track preparedInstrument(Sequence sequence, String name, int program) {
        Track track = MidiSupport.namedTrack(sequence, name);
        MidiSupport.add(track, MidiSupport.message(ShortMessage.PROGRAM_CHANGE, 0, program, 0), 0);
        return track;
    }

    private static void compareDirect(Sequence sequence, Path prefix) throws Exception {
        ChannelCapture capture = new ChannelCapture();
        List<String> expected = new ArrayList<>();
        StringBuilder result = new StringBuilder("tick,channel,message_hex,direct_hex\n");
        for (MidiSupport.Event event : MidiSupport.events(sequence)) {
            if (!(event.message() instanceof ShortMessage message)) continue;
            Programs.direct(capture.channel(message.getChannel()), message);
            ShortMessage actual = capture.messages().get(capture.messages().size() - 1);
            expected.add(MidiSupport.hex(message));
            result.append(Diagnostics.csv(event.tick(), message.getChannel() + 1, MidiSupport.hex(message), MidiSupport.hex(actual)));
        }
        if (!expected.equals(capture.messages().stream().map(MidiSupport::hex).toList()))
            throw new IllegalStateException("Direct MidiChannel calls differ from ShortMessage sequence.");
        Diagnostics.write(prefix, "-direct-comparison.csv", result.toString());
    }

    private static void roundtrip(Sequence sequence, Path prefix) throws Exception {
        Sequence restored = MidiSupport.read(Path.of(prefix + ".mid"));
        boolean equal = MidiSupport.signature(sequence).equals(MidiSupport.signature(restored));
        if (!equal) throw new IllegalStateException("MIDI roundtrip changed musical events or timing.");
        List<Music.Tempo> map = MidiSupport.tempos(restored);
        double duration = TempoFiles.secondsAt(3840, Music.PPQ, map);
        double crossing = TempoFiles.duration(1440, 2400, Music.PPQ, map);
        if (Math.abs(duration - 6) > 1e-10 || Math.abs(crossing - 1.5) > 1e-10)
            throw new IllegalStateException("Tempo conversion should give 6 s total and 1.5 s crossing note.");
        Diagnostics.export(restored, Path.of(prefix + "-readback"),
                GenerateParameters.generate(prefix.getParent().getFileName().toString()), "Re-read type 1 MIDI file");
        StringBuilder table = new StringBuilder("tick,student_seconds,reference_seconds\n");
        for (int tick : new int[]{0, 480, 1440, 1920, 2400, 3840})
            table.append(Diagnostics.csv(tick, TempoFiles.secondsAt(tick, Music.PPQ, map),
                    MidiSupport.time(tick, Music.PPQ, map)));
        Diagnostics.write(prefix, "-conversion.csv", table.toString());
        Diagnostics.write(prefix, "-roundtrip.txt", "format=SMF type 1\neventsAndPositionsPreserved=" + equal
                + "\ntotalSeconds=" + duration + "\ncrossingNoteSeconds=" + crossing
                + "\nFile bytes need not be identical: serialization/running-status choices are writer-owned.\n");
    }
}
