package cbsp.lab06;

/** Prepared settings. Keep the ID as text, including leading zeroes. */
public record TaskParameters(String version, String studentId, int rootNote,
        int bpm, int velocity, int fluteVolume, int transpose) { }
