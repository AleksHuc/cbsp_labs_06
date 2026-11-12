package cbsp.lab06;

import java.util.Random;

/** Prepared deterministic assignment generator; preserve version and call order. */
public final class GenerateParameters {
    public static final String VERSION = "lab06-v1";
    private GenerateParameters() { }

    public static TaskParameters generate(String id) {
        if (id == null || !id.matches("[0-9]{8}"))
            throw new IllegalArgumentException("Student ID must contain exactly eight digits.");
        Random random = new Random(Long.parseLong(id));
        return new TaskParameters(VERSION, id,
                new int[]{60, 62, 65, 67}[random.nextInt(4)],
                new int[]{80, 100, 120}[random.nextInt(3)],
                new int[]{72, 84, 96}[random.nextInt(3)],
                new int[]{56, 64, 72}[random.nextInt(3)],
                new int[]{-5, 5, 7}[random.nextInt(3)]);
    }
}
