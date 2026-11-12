package cbsp.lab06;

import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.midi.*;
import cbsp.lab06.Music.Note;
import cbsp.lab06.Music.Tempo;

/** Device-free regression checks against hand-calculated fixtures and musical invariants. */
public final class MidiChecks {
    private static int checks;
    private static final TaskParameters P = new TaskParameters("fixture", "00123456", 65, 100, 84, 64, 7);
    private MidiChecks() { }

    public static void main(String[] args) throws Exception {
        parameters(); messages(); melody(); programs(); arrangement(); expression(); sustain(); pitch(); tempo(); files();
        System.out.println("MidiChecks: " + checks + " checks passed (no audio device).");
    }

    private static void parameters() {
        TaskParameters a = GenerateParameters.generate("00123456");
        eq(a, GenerateParameters.generate("00123456"), "repeatable parameters");
        eq(a.studentId(), "00123456", "leading zeroes");
        eq(a.version(), "lab06-v1", "generator version");
        eq(a, new TaskParameters("lab06-v1", "00123456", 62, 120, 84, 64, 5), "versioned parameter fixture");
        rejects(() -> GenerateParameters.generate("123456"), "short ID");
        rejects(() -> GenerateParameters.generate("1234567a"), "non-numeric ID");
        for (String id : List.of("00000000", "00000001", "11111111", "99999999")) {
            TaskParameters p = GenerateParameters.generate(id);
            check(Set.of(60, 62, 65, 67).contains(p.rootNote()), "root domain");
            check(Set.of(80, 100, 120).contains(p.bpm()), "tempo domain");
            check(Set.of(-5, 5, 7).contains(p.transpose()), "transpose domain");
        }
    }

    private static void messages() throws Exception {
        eq(MidiSupport.hex(NoteMessages.on(0, 60, 100)), "90 3C 64", "Note On bytes");
        eq(MidiSupport.hex(NoteMessages.off(0, 60, 64)), "80 3C 40", "Note Off bytes");
        eq(MidiSupport.hex(NoteMessages.zeroVelocityOff(0, 60)), "90 3C 00", "zero velocity Off bytes");
        eq(MidiSupport.hex(NoteMessages.on(15, 127, 127)), "9F 7F 7F", "channel/note upper limit");
        check(MidiSupport.isOff(NoteMessages.zeroVelocityOff(15, 0)), "zero velocity recognized as Off");
        rejects(() -> NoteMessages.on(16, 60, 90), "channel range");
        rejects(() -> NoteMessages.on(0, 128, 90), "note range");
        rejects(() -> NoteMessages.on(0, 60, 0), "zero cannot start a note");
        Receiver receiver = new Receiver() {
            public void send(MidiMessage message, long stamp) { }
            public void close() { }
        };
        int[] calls = {0};
        Synthesizer synth = (Synthesizer) Proxy.newProxyInstance(Synthesizer.class.getClassLoader(),
                new Class<?>[]{Synthesizer.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getReceiver")) { calls[0]++; return receiver; }
                    throw new AssertionError("Unexpected synthesizer operation: " + method.getName());
                });
        check(NoteMessages.connect(synth) == receiver, "receiver belongs to supplied synth");
        eq(calls[0], 1, "one receiver request");
        for (Experiments.Experiment e : Experiments.build(1, P)) {
            Diagnostics.Span n = Diagnostics.analyze(e.sequence()).spans().get(0);
            eq(n.note(), 60, "fixed middle C"); eq(n.off() - n.on(), 1920L, "two-second fixture ticks");
            near(MidiSupport.time(n.off(), 480, MidiSupport.tempos(e.sequence())), 2, 1e-12, "two-second fixture");
        }
    }

    private static void melody() {
        eq(Melody.ticks(0.5, 480), 240L, "eighth note");
        eq(Melody.ticks(2, 480), 960L, "half note");
        near(Melody.seconds(480, 480, 120), 0.5, 1e-12, "quarter at 120 BPM");
        near(Melody.seconds(480, 480, 60), 1, 1e-12, "quarter at 60 BPM");
        List<Note> notes = Melody.notes(65, 3, 90, 480);
        eq(notes.stream().map(Note::number).toList(), List.of(65, 65, 65, 60, 62, 62, 60, 69, 69, 67, 67, 65), "old slide transcription");
        eq(notes.stream().map(Note::start).toList(), List.of(0L, 480L, 960L, 1440L, 1920L, 2400L, 2880L, 3840L, 4320L, 4800L, 5280L, 5760L), "melody onsets");
        eq(notes.get(11).end(), 6720L, "14-quarter melody");
        Sequence sequence = MidiSupport.fixed(480, 120);
        Track track = sequence.createTrack(); Melody.add(track, notes);
        Diagnostics.Analysis a = Diagnostics.analyze(sequence);
        eq(a.spans().size(), 12, "paired events"); eq(a.maxKeyHeld(), 1, "repeated notes do not overlap");
        List<MidiSupport.Event> at480 = MidiSupport.events(sequence).stream()
                .filter(e -> e.tick() == 480 && e.message() instanceof ShortMessage).toList();
        check(MidiSupport.isOff((ShortMessage) at480.get(0).message()), "release before reattack at shared tick");
        List<Experiments.Experiment> cases = Experiments.build(2, P);
        var base = Diagnostics.analyze(cases.get(0).sequence()).spans();
        var fast = Diagnostics.analyze(cases.get(1).sequence()).spans();
        var highPpq = Diagnostics.analyze(cases.get(2).sequence()).spans();
        eq(base, fast, "tempo preserves all tick-domain notes");
        for (int i = 0; i < base.size(); i++) {
            eq(highPpq.get(i).on(), 2 * base.get(i).on(), "PPQ doubles positions");
            eq(highPpq.get(i).off(), 2 * base.get(i).off(), "PPQ doubles ends");
        }
        near(MidiSupport.time(6720, 480, MidiSupport.tempos(cases.get(1).sequence())), 4.2, 1e-12, "double tempo halves melody time");
        near(MidiSupport.time(13440, 960, MidiSupport.tempos(cases.get(2).sequence())), 8.4, 1e-12, "double PPQ preserves seconds");
    }

    private static void programs() {
        eq(MidiSupport.hex(Programs.change(0, 0)), "C0 00", "piano program and two-byte length");
        eq(MidiSupport.hex(Programs.change(1, 40)), "C1 28", "violin program");
        eq(MidiSupport.hex(Programs.change(15, 73)), "CF 49", "flute program");
        ChannelCapture capture = new ChannelCapture();
        MidiChannel channel = capture.channel(2);
        Programs.direct(channel, Programs.change(2, 40));
        Programs.direct(channel, NoteMessages.on(2, 69, 90));
        Programs.direct(channel, NoteMessages.off(2, 69, 45));
        Programs.direct(channel, NoteMessages.zeroVelocityOff(2, 60));
        eq(capture.messages().stream().map(MidiSupport::hex).toList(),
                List.of("C2 28", "92 45 5A", "82 45 2D", "92 3C 00"), "direct method dispatch");
        List<Experiments.Experiment> cases = Experiments.build(3, P);
        for (int i = 0; i < cases.size(); i += 2) {
            eq(MidiSupport.signature(cases.get(i).sequence()), MidiSupport.signature(cases.get(i + 1).sequence()), "message/direct musical equivalence");
        }
    }

    private static void arrangement() {
        List<Experiments.Experiment> cases = Experiments.build(4, P);
        Diagnostics.Analysis separate = Diagnostics.analyze(cases.get(0).sequence());
        eq(cases.get(0).sequence().getTracks().length, 3, "conductor and two musical tracks");
        eq(separate.channels(), Set.of(0, 1), "two channels");
        eq(separate.maxKeyHeld(), 5, "melody plus three-note chord plus flute");
        List<Diagnostics.Span> flute = separate.spans().stream().filter(n -> n.channel() == 1).toList();
        eq(flute.size(), 12, "flute melody count");
        eq(flute.get(0).note(), 77, "flute is one octave above root F4");
        check(flute.stream().allMatch(n -> n.program() == 73 && n.velocity() == 84), "flute program and unchanged velocity");
        check(separate.rows().stream().anyMatch(r -> r.volume() == 64 && r.event().message() instanceof ShortMessage s && s.getChannel() == 1), "flute balance uses CC7");
        Diagnostics.Analysis shared = Diagnostics.analyze(cases.get(1).sequence());
        eq(shared.channels(), Set.of(0), "two tracks can share one channel");
        eq(shared.maxKeyHeld(), 2, "shared-channel notes coexist");
        check(shared.spans().stream().allMatch(n -> n.program() == 73), "latest shared program controls both attacks");
    }

    private static void expression() {
        List<Experiments.Experiment> cases = Experiments.build(5, P);
        Diagnostics.Analysis attacks = Diagnostics.analyze(cases.get(0).sequence());
        eq(attacks.spans().stream().map(Diagnostics.Span::velocity).toList(), List.of(32, 80, 120), "attack velocity cases");
        var quiet = Diagnostics.analyze(cases.get(1).sequence());
        var loud = Diagnostics.analyze(cases.get(2).sequence());
        eq(quiet.spans(), loud.spans(), "CC7 changes leave note data untouched");
        eq(lastCc(cases.get(1).sequence(), 0, 7), 40, "quiet channel volume");
        eq(lastCc(cases.get(2).sequence(), 0, 7), 100, "loud channel volume");
        Sequence phrase = cases.get(3).sequence();
        eq(Diagnostics.analyze(phrase).spans().size(), 1, "expression needs only one attack");
        eq(shorts(phrase).stream().filter(m -> m.getCommand() == ShortMessage.CONTROL_CHANGE && m.getData1() == 11)
                .map(ShortMessage::getData2).toList(), List.of(32, 56, 88, 112, 72, 32), "expression shape");
    }

    private static void sustain() {
        for (int value = 0; value <= 127; value++) eq(Sustain.held(value), value >= 64, "CC64 threshold " + value);
        List<Experiments.Experiment> cases = Experiments.build(6, P);
        for (int i = 0; i < cases.size(); i++) {
            Diagnostics.Analysis a = Diagnostics.analyze(cases.get(i).sequence());
            boolean pedal = i == 1 || i == 3;
            eq(a.maxKeyHeld(), 1, "key-held polyphony does not change with pedal");
            eq(a.maxHeldOrDeferred(), pedal ? 3 : 1, "pedal-deferred count");
            eq(a.spans().stream().map(Diagnostics.Span::off).toList(), List.of(240L, 720L, 1200L), "Note Off schedule stays fixed");
            for (Diagnostics.Span n : a.spans()) eq(n.heldUntil(), pedal ? 1920L : n.off(), "pedal release placement");
            eq(lastCc(cases.get(i).sequence(), 0, 64), 0, "pedal explicitly released");
        }
        // Repeated pitch after Note Off may coexist with a pedal-deferred earlier attack.
        Sequence sequence = MidiSupport.fixed(480, 120); Track track = sequence.createTrack();
        MidiSupport.add(track, MidiSupport.cc(0, 64, 127), 0);
        Melody.add(track, List.of(new Note(0, 60, 0, 240, 90), new Note(0, 60, 480, 240, 90)));
        MidiSupport.add(track, MidiSupport.cc(0, 64, 0), 960);
        eq(Diagnostics.analyze(sequence).maxHeldOrDeferred(), 2, "repeated pedal-held pitch is counted separately");
    }

    private static void pitch() {
        List<Note> original = List.of(new Note(3, 60, 480, 960, 80));
        eq(Pitch.transpose(original, 12), List.of(new Note(3, 72, 480, 960, 80)), "transposition preserves other fields");
        eq(original.get(0).number(), 60, "transposition preserves input");
        eq(Pitch.transpose(List.of(new Note(0, 0, 0, 1, 1)), 127).get(0).number(), 127, "upper legal transposition");
        rejects(() -> Pitch.transpose(List.of(new Note(0, 0, 0, 1, 1)), -1), "reject low overflow");
        rejects(() -> Pitch.transpose(List.of(new Note(0, 127, 0, 1, 1)), 1), "reject high overflow");
        rejects(() -> Pitch.transpose(original, Integer.MAX_VALUE), "reject integer overflow");
        eq(MidiSupport.hex(Pitch.bend(0, 0)), "E0 00 00", "minimum bend");
        eq(MidiSupport.hex(Pitch.bend(0, 8192)), "E0 00 40", "center bend");
        eq(MidiSupport.hex(Pitch.bend(1, 16383)), "E1 7F 7F", "maximum bend and channel");
        Sequence bend = Experiments.build(7, P).get(2).sequence();
        List<ShortMessage> bends = shorts(bend).stream().filter(m -> m.getCommand() == ShortMessage.PITCH_BEND).toList();
        eq(bends.size(), 33, "bend curve resolution");
        eq(MidiSupport.hex(bends.get(32)), "E0 00 40", "bend returns to center");
        check(bends.stream().allMatch(m -> m.getChannel() == 0), "other channel is unbent");
        eq(Diagnostics.analyze(bend).spans().stream().map(Diagnostics.Span::note).toList(), List.of(65, 72, 77), "bend preserves note numbers");
        check(shorts(bend).stream().anyMatch(m -> m.getCommand() == ShortMessage.CONTROL_CHANGE && m.getData1() == 6 && m.getData2() == 2), "RPN sets two-semitone range");
    }

    private static void tempo() {
        List<Tempo> map = List.of(new Tempo(0, 500_000), new Tempo(1920, 1_000_000));
        near(TempoFiles.secondsAt(1920, 480, map), 2, 1e-12, "tempo boundary");
        near(TempoFiles.secondsAt(3840, 480, map), 6, 1e-12, "eight-quarter example");
        near(TempoFiles.duration(1440, 2400, 480, map), 1.5, 1e-12, "note crosses tempo change");
        List<Tempo> other = List.of(new Tempo(0, 500_000), new Tempo(96, 1_000_000), new Tempo(288, 250_000));
        near(TempoFiles.secondsAt(384, 96, other), 2.75, 1e-12, "three tempo segments");
        near(TempoFiles.duration(48, 336, 96, other), 2.375, 1e-12, "note crosses two changes");
        rejects(() -> TempoFiles.secondsAt(480, 480, List.of(new Tempo(100, 500_000))), "tempo map starts at zero");
        rejects(() -> TempoFiles.secondsAt(480, 480, List.of(new Tempo(0, 500_000), new Tempo(0, 1_000_000))), "tempo map cannot repeat tick");
        Sequence sequence = Experiments.build(8, P).get(0).sequence();
        eq(MidiSupport.tempos(sequence), map, "encoded tempo map");
        eq(sequence.getTickLength(), 3840L, "eight-quarter file length");
        near(sequence.getMicrosecondLength() / 1e6, 6, 1e-12, "Java sequence tempo interpretation");
        check(MidiSupport.events(sequence).stream().anyMatch(e -> e.message() instanceof MetaMessage m
                && m.getType() == 0x58 && Arrays.equals(m.getData(), new byte[]{4, 2, 24, 8})), "4/4 metadata");
    }

    private static void files() throws Exception {
        Path temp = Files.createTempDirectory("cbsp-midi-checks-");
        try {
            for (int task = 1; task <= 8; task++) {
                for (Experiments.Experiment experiment : Experiments.build(task, P)) {
                    Sequence sequence = experiment.sequence();
                    Diagnostics.analyze(sequence); // no unmatched On/Off or unreleased pedal
                    Path file = temp.resolve(experiment.name() + ".mid");
                    MidiSupport.write(sequence, file);
                    eq(MidiSystem.getMidiFileFormat(file.toFile()).getType(), 1, "SMF type 1");
                    Sequence restored = MidiSupport.read(file);
                    eq(MidiSupport.signature(sequence), MidiSupport.signature(restored), "roundtrip " + experiment.name());
                    eq(Diagnostics.analyze(sequence).spans(), Diagnostics.analyze(restored).spans(), "semantic roundtrip");
                }
            }
            Sequence sequence = Experiments.build(8, P).get(0).sequence();
            Path prefix = temp.resolve("task8");
            Diagnostics.export(sequence, prefix, P, "fixture");
            check(Files.readString(Path.of(prefix + "-notes.csv")).contains("1.5,3.0,1.5"), "crossing note CSV includes 1.5 s duration");
            for (String suffix : List.of("-piano-roll.png", "-controllers.png", "-time.png"))
                eq(ImageIO.read(Path.of(prefix + suffix).toFile()).getWidth(), 1200, "readable headless PNG " + suffix);
            eq(Diagnostics.name(60), "C4", "octave convention");
            eq(Diagnostics.name(69), "A4", "A4 label");
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static List<ShortMessage> shorts(Sequence sequence) {
        return MidiSupport.events(sequence).stream().map(MidiSupport.Event::message)
                .filter(m -> m instanceof ShortMessage).map(m -> (ShortMessage) m).toList();
    }

    private static int lastCc(Sequence sequence, int channel, int controller) {
        return shorts(sequence).stream().filter(m -> m.getCommand() == ShortMessage.CONTROL_CHANGE
                && m.getChannel() == channel && m.getData1() == controller).reduce((a, b) -> b).orElseThrow().getData2();
    }

    private static void check(boolean condition, String label) {
        checks++; if (!condition) throw new AssertionError(label);
    }
    private static void eq(Object actual, Object expected, String label) {
        check(Objects.equals(actual, expected), label + ": expected " + expected + ", got " + actual);
    }
    private static void near(double actual, double expected, double tolerance, String label) {
        check(Double.isFinite(actual) && Math.abs(actual - expected) <= tolerance,
                label + ": expected " + expected + ", got " + actual);
    }
    private static void rejects(Runnable action, String label) {
        try { action.run(); } catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError(label + " should reject input");
    }
}
