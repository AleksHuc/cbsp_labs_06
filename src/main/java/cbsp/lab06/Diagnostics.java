package cbsp.lab06;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.sound.midi.*;

/** Prepared semantic event inspection, independent of the student transformations. */
public final class Diagnostics {
    private Diagnostics() { }

    public record Span(int track, int channel, int note, int velocity, int program,
            long on, long off, long heldUntil) { }
    public record Row(MidiSupport.Event event, int program, int volume, int expression,
            int pedal, int bend, int keyHeld, int deferred) { }
    public record Analysis(List<Span> spans, List<Row> rows, int maxKeyHeld, int maxHeldOrDeferred,
            Set<Integer> channels) { }

    private static final class Held {
        final int track, channel, note, velocity, program;
        final long on;
        long off = -1;
        Held(MidiSupport.Event event, ShortMessage message, int program) {
            track = event.track(); channel = message.getChannel(); note = message.getData1();
            velocity = message.getData2(); on = event.tick(); this.program = program;
        }
        Span finish(long tick) { return new Span(track, channel, note, velocity, program, on, off, tick); }
    }

    public static Analysis analyze(Sequence sequence) {
        Held[][] down = new Held[16][128];
        List<List<Held>> deferred = new ArrayList<>();
        for (int ch = 0; ch < 16; ch++) deferred.add(new ArrayList<>());
        int[] program = new int[16], volume = new int[16], expression = new int[16];
        int[] pedal = new int[16], bend = new int[16];
        Arrays.fill(volume, 100); Arrays.fill(expression, 127); Arrays.fill(bend, 8192);
        List<Span> spans = new ArrayList<>();
        List<Row> rows = new ArrayList<>();
        Set<Integer> channels = new TreeSet<>();
        int keyHeld = 0, pedalHeld = 0, maxKey = 0, maxTotal = 0;
        for (MidiSupport.Event event : MidiSupport.events(sequence)) {
            int ch = -1;
            if (event.message() instanceof ShortMessage message && message.getStatus() < 0xf0) {
                ch = message.getChannel();
                channels.add(ch);
                int number = message.getData1();
                if (MidiSupport.isOn(message)) {
                    if (down[ch][number] != null)
                        throw new IllegalArgumentException("Overlapping key-down of same channel/note at tick " + event.tick());
                    down[ch][number] = new Held(event, message, program[ch]);
                    keyHeld++;
                } else if (MidiSupport.isOff(message)) {
                    Held held = down[ch][number];
                    if (held == null) throw new IllegalArgumentException("Unmatched Note Off at tick " + event.tick());
                    held.off = event.tick();
                    down[ch][number] = null;
                    keyHeld--;
                    if (pedal[ch] >= 64) { deferred.get(ch).add(held); pedalHeld++; }
                    else spans.add(held.finish(event.tick()));
                } else if (message.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                    program[ch] = number;
                } else if (message.getCommand() == ShortMessage.PITCH_BEND) {
                    bend[ch] = number | message.getData2() << 7;
                } else if (message.getCommand() == ShortMessage.CONTROL_CHANGE) {
                    switch (number) {
                        case 7 -> volume[ch] = message.getData2();
                        case 11 -> expression[ch] = message.getData2();
                        case 64 -> {
                            pedal[ch] = message.getData2();
                            if (pedal[ch] < 64) {
                                for (Held held : deferred.get(ch)) spans.add(held.finish(event.tick()));
                                pedalHeld -= deferred.get(ch).size();
                                deferred.get(ch).clear();
                            }
                        }
                        default -> { } // RPN and other CCs remain visible as raw events.
                    }
                }
            }
            maxKey = Math.max(maxKey, keyHeld);
            maxTotal = Math.max(maxTotal, keyHeld + pedalHeld);
            rows.add(new Row(event, ch < 0 ? -1 : program[ch], ch < 0 ? -1 : volume[ch],
                    ch < 0 ? -1 : expression[ch], ch < 0 ? -1 : pedal[ch],
                    ch < 0 ? -1 : bend[ch], keyHeld, pedalHeld));
        }
        if (keyHeld != 0 || pedalHeld != 0)
            throw new IllegalArgumentException("Sequence ends with unreleased keys or pedal-deferred notes.");
        spans.sort(Comparator.comparingLong(Span::on).thenComparingInt(Span::channel).thenComparingInt(Span::note));
        return new Analysis(List.copyOf(spans), List.copyOf(rows), maxKey, maxTotal, Set.copyOf(channels));
    }

    public static Analysis export(Sequence sequence, Path prefix, TaskParameters p, String description) throws IOException {
        Files.createDirectories(prefix.toAbsolutePath().getParent());
        Analysis analysis = analyze(sequence);
        List<Music.Tempo> map = MidiSupport.tempos(sequence);
        int ppq = sequence.getResolution();
        StringBuilder events = new StringBuilder("tick,quarter,seconds,track,type,channel,index,data1,data2,length,status_hex,bytes_hex,bytes_decimal,program,cc7,cc11,cc64,pitch_bend,key_held,pedal_deferred\n");
        for (Row row : analysis.rows()) {
            MidiSupport.Event event = row.event();
            MidiMessage message = event.message();
            ShortMessage shortMessage = message instanceof ShortMessage s && s.getStatus() < 0xf0 ? s : null;
            events.append(csv(event.tick(), event.tick() / (double) ppq, MidiSupport.time(event.tick(), ppq, map),
                    event.track(), type(message), shortMessage == null ? "" : shortMessage.getChannel() + 1,
                    shortMessage == null ? "" : shortMessage.getChannel(),
                    shortMessage == null ? "" : shortMessage.getData1(),
                    shortMessage == null || shortMessage.getLength() < 3 ? "" : shortMessage.getData2(),
                    message.getLength(), String.format(Locale.ROOT, "%02X", message.getStatus()),
                    MidiSupport.hex(message), MidiSupport.decimal(message),
                    row.program(), row.volume(), row.expression(), row.pedal(), row.bend(), row.keyHeld(), row.deferred()));
        }
        write(prefix, "-events.csv", events.toString());
        StringBuilder notes = new StringBuilder("track,channel,index,note,name,velocity,program,on_tick,off_tick,key_ticks,on_seconds,off_seconds,key_seconds,pedal_release_tick,pedal_release_seconds\n");
        for (Span span : analysis.spans()) {
            double on = MidiSupport.time(span.on(), ppq, map), off = MidiSupport.time(span.off(), ppq, map);
            notes.append(csv(span.track(), span.channel() + 1, span.channel(), span.note(), name(span.note()),
                    span.velocity(), span.program(), span.on(), span.off(), span.off() - span.on(), on, off, off - on,
                    span.heldUntil(), MidiSupport.time(span.heldUntil(), ppq, map)));
        }
        write(prefix, "-notes.csv", notes.toString());
        StringBuilder timeline = new StringBuilder("tick,quarter,seconds\n");
        TreeSet<Long> positions = new TreeSet<>();
        for (long tick = 0; tick <= sequence.getTickLength(); tick += Math.max(1, ppq / 4)) positions.add(tick);
        positions.add(sequence.getTickLength());
        for (Music.Tempo tempo : map) positions.add(tempo.tick());
        for (long tick : positions) timeline.append(csv(tick, tick / (double) ppq, MidiSupport.time(tick, ppq, map)));
        write(prefix, "-timeline.csv", timeline.toString());
        write(prefix, "-settings.txt", "description=" + description + "\n" + parameterText(p)
                + "division=PPQ\nppq=" + ppq + "\ntracks=" + sequence.getTracks().length
                + "\nchannels=" + new TreeSet<>(analysis.channels()).stream().map(ch -> ch + 1).toList()
                + "\nendTick=" + sequence.getTickLength()
                + "\nsequenceSeconds=" + MidiSupport.time(sequence.getTickLength(), ppq, map)
                + "\nmaxKeyHeld=" + analysis.maxKeyHeld() + "\nmaxKeyHeldOrPedalDeferred=" + analysis.maxHeldOrDeferred()
                + "\ntempoMap=" + map + "\n"
                + "Counts describe MIDI key/pedal state, not measured synthesizer voice allocation or audible release.\n"
                + "program in notes.csv is captured at Note On; event rows show current channel state.\n"
                + "Equal-tick ordering is track index then stored event order; examples avoid conflicting cross-track programs at one tick.\n");
        MidiSupport.write(sequence, Path.of(prefix + ".mid"));
        Plot.write(sequence, analysis, prefix);
        return analysis;
    }

    public static String parameterText(TaskParameters p) {
        return "version=" + p.version() + "\nstudentId=" + p.studentId() + "\nrootNote=" + p.rootNote()
                + "\nbpm=" + p.bpm() + "\nvelocity=" + p.velocity() + "\nfluteVolume=" + p.fluteVolume()
                + "\ntranspose=" + p.transpose() + "\n";
    }

    public static String name(int note) {
        return new String[]{"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"}[note % 12]
                + (note / 12 - 1);
    }

    public static String type(MidiMessage message) {
        if (message instanceof MetaMessage meta) return switch (meta.getType()) {
            case 0x03 -> "Track Name";
            case 0x06 -> "Marker";
            case 0x2f -> "End of Track";
            case 0x51 -> "Set Tempo";
            case 0x58 -> "Time Signature";
            default -> "Meta " + meta.getType();
        };
        if (message instanceof ShortMessage m) {
            if (MidiSupport.isOff(m)) return m.getCommand() == ShortMessage.NOTE_ON ? "Note Off (On velocity 0)" : "Note Off";
            return switch (m.getCommand()) {
                case ShortMessage.NOTE_ON -> "Note On";
                case ShortMessage.PROGRAM_CHANGE -> "Program Change";
                case ShortMessage.CONTROL_CHANGE -> "Control Change";
                case ShortMessage.PITCH_BEND -> "Pitch Bend";
                default -> "Short " + m.getStatus();
            };
        }
        return "Other";
    }

    public static String csv(Object... values) {
        StringJoiner line = new StringJoiner(",");
        for (Object value : values) {
            String text = String.valueOf(value);
            line.add(text.contains(",") || text.contains("\"") || text.contains("\n")
                    ? "\"" + text.replace("\"", "\"\"") + "\"" : text);
        }
        return line + "\n";
    }

    public static void write(Path prefix, String suffix, String content) throws IOException {
        Files.writeString(Path.of(prefix + suffix), content, StandardCharsets.UTF_8);
    }
}
