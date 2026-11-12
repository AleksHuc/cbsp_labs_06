package cbsp.lab06;

import java.util.Locale;

public final class Main {
    private Main() { }
    public static void main(String[] args) {
        Locale.setDefault(Locale.ROOT);
        if (args.length == 0 || args.length == 1 && args[0].equals("--help")) { help(); return; }
        if (args.length != 2 || !(args[0].equals("--parameters") || args[0].matches("--(task|check)[1-8]"))) {
            help(); System.exit(2); return;
        }
        try {
            TaskParameters p = GenerateParameters.generate(args[1]);
            if (args[0].equals("--parameters")) System.out.print(Diagnostics.parameterText(p));
            else Experiments.run(Integer.parseInt(args[0].substring(args[0].length() - 1)), p,
                    args[0].startsWith("--task"));
        } catch (Exception ex) {
            System.err.println(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            System.exit(2);
        }
    }

    private static void help() {
        System.out.println("Usage: Main <command> <eight-digit-student-id>");
        System.out.println("--parameters   Show reproducible assignment settings");
        System.out.println("--check1..8    Export MIDI/CSV/PNG without a synthesizer or audio device");
        System.out.println("--task1..8     Same exports plus actual MIDI instrument playback");
        System.out.println("All eight tasks are required. Output: output/<student-id>/");
    }
}
