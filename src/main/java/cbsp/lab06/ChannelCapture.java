package cbsp.lab06;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;

/** Prepared device-free MidiChannel spy, intentionally limited to Task 3 operations. */
public final class ChannelCapture {
    private final List<ShortMessage> messages = new ArrayList<>();

    public List<ShortMessage> messages() { return List.copyOf(messages); }

    public MidiChannel channel(int channelIndex) {
        Music.range(channelIndex, 0, 15, "channel");
        return (MidiChannel) Proxy.newProxyInstance(MidiChannel.class.getClassLoader(),
                new Class<?>[]{MidiChannel.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "programChange" -> {
                            if (args.length != 1) throw new UnsupportedOperationException("Bank overload is not used.");
                            messages.add(MidiSupport.message(ShortMessage.PROGRAM_CHANGE,
                                    channelIndex, (int) args[0], 0));
                        }
                        case "noteOn" -> messages.add(MidiSupport.message(ShortMessage.NOTE_ON,
                                channelIndex, (int) args[0], (int) args[1]));
                        case "noteOff" -> messages.add(MidiSupport.message(ShortMessage.NOTE_OFF,
                                channelIndex, (int) args[0], args.length == 2 ? (int) args[1] : 64));
                        case "toString" -> { return "Task3ChannelCapture[" + channelIndex + "]"; }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "equals" -> { return proxy == args[0]; }
                        default -> throw new UnsupportedOperationException("Not a Task 3 operation: " + method.getName());
                    }
                    return null;
                });
    }
}
