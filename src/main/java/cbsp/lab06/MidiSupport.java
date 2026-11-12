package cbsp.lab06;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import javax.sound.midi.*;
import cbsp.lab06.Music.Tempo;

/** Prepared message encoding, file I/O and chronological inspection. */
public final class MidiSupport {
    private MidiSupport() { }

    public record Event(long tick, int track, int index, MidiMessage message) { }

    public static ShortMessage message(int command, int channel, int data1, int data2) {
        Music.range(channel, 0, 15, "channel index");
        Music.range(data1, 0, 127, "data1");
        Music.range(data2, 0, 127, "data2");
        try { return new ShortMessage(command, channel, data1, data2); }
        catch (InvalidMidiDataException ex) { throw new IllegalArgumentException(ex); }
    }

    public static ShortMessage cc(int channel, int controller, int value) {
        return message(ShortMessage.CONTROL_CHANGE, channel, controller, value);
    }

    public static MetaMessage meta(int type, byte[] data) {
        try { return new MetaMessage(type, data, data.length); }
        catch (InvalidMidiDataException ex) { throw new IllegalArgumentException(ex); }
    }

    public static MetaMessage text(int type, String value) {
        return meta(type, value.getBytes(StandardCharsets.UTF_8));
    }

    public static MetaMessage tempo(int micros) {
        Music.range(micros, 1, 0xffffff, "tempo");
        return meta(0x51, new byte[]{(byte) (micros >> 16), (byte) (micros >> 8), (byte) micros});
    }

    public static void add(Track track, MidiMessage message, long tick) {
        if (tick < 0) throw new IllegalArgumentException("Negative tick.");
        track.add(new MidiEvent((MidiMessage) message.clone(), tick));
    }

    public static Sequence empty(int ppq) {
        try { return new Sequence(Sequence.PPQ, ppq); }
        catch (InvalidMidiDataException ex) { throw new IllegalArgumentException(ex); }
    }

    public static Track namedTrack(Sequence sequence, String name) {
        Track track = sequence.createTrack();
        add(track, text(0x03, name), 0);
        return track;
    }

    /** Fixed-tempo infrastructure for Tasks 1-7; Task 8 supplies its own conductor. */
    public static Sequence fixed(int ppq, double bpm) {
        if (ppq <= 0 || !Double.isFinite(bpm) || bpm <= 0)
            throw new IllegalArgumentException("Invalid PPQ/tempo.");
        Sequence sequence = empty(ppq);
        Track conductor = namedTrack(sequence, "Conductor");
        add(conductor, tempo((int) Math.round(60_000_000.0 / bpm)), 0);
        add(conductor, meta(0x58, new byte[]{4, 2, 24, 8}), 0);
        return sequence;
    }

    /** Explicit common End of Track position, including any intended rest. */
    public static void end(Sequence sequence, long tick) {
        if (tick < sequence.getTickLength()) throw new IllegalArgumentException("End precedes an event.");
        for (Track track : sequence.getTracks()) add(track, meta(0x2f, new byte[0]), tick);
    }

    public static List<Event> events(Sequence sequence) {
        List<Event> events = new ArrayList<>();
        Track[] tracks = sequence.getTracks();
        for (int t = 0; t < tracks.length; t++)
            for (int i = 0; i < tracks[t].size(); i++) {
                MidiEvent event = tracks[t].get(i);
                events.add(new Event(event.getTick(), t, i, event.getMessage()));
            }
        events.sort(Comparator.comparingLong(Event::tick).thenComparingInt(Event::track)
                .thenComparingInt(Event::index));
        return List.copyOf(events);
    }

    public static List<Tempo> tempos(Sequence sequence) {
        if (sequence.getDivisionType() != Sequence.PPQ)
            throw new IllegalArgumentException("This lab uses PPQ, not SMPTE division.");
        TreeMap<Long, Integer> tempos = new TreeMap<>();
        tempos.put(0L, 500_000); // SMF default if Set Tempo is absent
        Set<Long> explicit = new HashSet<>();
        for (Event event : events(sequence)) {
            if (event.message() instanceof MetaMessage meta && meta.getType() == 0x51) {
                byte[] bytes = meta.getData();
                if (bytes.length != 3) throw new IllegalArgumentException("Set Tempo needs three bytes.");
                if (!explicit.add(event.tick()))
                    throw new IllegalArgumentException("Ambiguous simultaneous tempo events.");
                int micros = (bytes[0] & 255) << 16 | (bytes[1] & 255) << 8 | bytes[2] & 255;
                Music.range(micros, 1, 0xffffff, "tempo");
                tempos.put(event.tick(), micros);
            }
        }
        return tempos.entrySet().stream().map(e -> new Tempo(e.getKey(), e.getValue())).toList();
    }

    /** Prepared reference conversion used by diagnostics, independent of student Task 8. */
    public static double time(long tick, int ppq, List<Tempo> map) {
        Music.tempoMap(ppq, map);
        if (tick < 0) throw new IllegalArgumentException("Negative tick.");
        long previous = 0;
        int micros = map.get(0).microsPerQuarter();
        double elapsedMicros = 0;
        for (int i = 1; i < map.size() && map.get(i).tick() <= tick; i++) {
            Tempo change = map.get(i);
            elapsedMicros += (change.tick() - previous) * (double) micros / ppq;
            previous = change.tick();
            micros = change.microsPerQuarter();
        }
        return (elapsedMicros + (tick - previous) * (double) micros / ppq) / 1_000_000.0;
    }

    public static void write(Sequence sequence, Path path) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        if (!MidiSystem.isFileTypeSupported(1, sequence))
            throw new IOException("No Standard MIDI File type 1 writer is available.");
        if (MidiSystem.write(sequence, 1, path.toFile()) <= 0)
            throw new IOException("MIDI writer did not write any bytes.");
    }

    public static Sequence read(Path path) throws IOException, InvalidMidiDataException {
        return MidiSystem.getSequence(path.toFile());
    }

    public static String hex(MidiMessage message) {
        return HexFormat.ofDelimiter(" ").withUpperCase().formatHex(message.getMessage());
    }

    public static String decimal(MidiMessage message) {
        byte[] bytes = message.getMessage();
        StringJoiner result = new StringJoiner(" ");
        for (byte b : bytes) result.add(Integer.toString(b & 255));
        return result.toString();
    }

    /** Ordered per-track musical data; End of Track byte representation is writer-owned. */
    public static List<String> signature(Sequence sequence) {
        List<String> result = new ArrayList<>();
        result.add(sequence.getDivisionType() + "/" + sequence.getResolution()
                + "/" + sequence.getTracks().length + "/" + sequence.getTickLength());
        for (Track track : sequence.getTracks()) {
            result.add("TRACK");
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (event.getMessage() instanceof MetaMessage meta && meta.getType() == 0x2f) continue;
                result.add(event.getTick() + ":" + hex(event.getMessage()));
            }
        }
        return List.copyOf(result);
    }

    public static boolean isOn(ShortMessage message) {
        return message.getCommand() == ShortMessage.NOTE_ON && message.getData2() > 0;
    }

    public static boolean isOff(ShortMessage message) {
        return message.getCommand() == ShortMessage.NOTE_OFF
                || message.getCommand() == ShortMessage.NOTE_ON && message.getData2() == 0;
    }

    /** Prepared RPN 0 setup: pitch bend sensitivity = two semitones, zero cents. */
    public static void bendRange(Track track, int channel) {
        for (int[] pair : new int[][]{{101, 0}, {100, 0}, {6, 2}, {38, 0}, {101, 127}, {100, 127}})
            add(track, cc(channel, pair[0], pair[1]), 0);
    }
}
