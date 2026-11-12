# Lab 06: MIDI and musical events

Build MIDI note messages, schedule a melody, select instruments, arrange two
parts, control dynamics and sustain, transpose notes, apply pitch bend, and save
a composition with a tempo change as a Standard MIDI File.

This is a Java starter for **eight required tasks**. `TODO (Task N)` comments mark
**algorithms and MIDI connections you must implement**. Unfinished sections throw
`UnsupportedOperationException("TODO (Task N): ...")`, identifying the missing
task and operation. Message validation, assignment generation, playback lifecycle,
scheduling, MIDI file I/O, event analysis and PNG plots are prepared infrastructure.
Validation and exception handling are not student tasks.

## Setup and commands

Use **JDK 21 or newer** and **Maven 3.9.x or newer**. Open the directory containing
`pom.xml` as a Maven project. The lab uses the standard **Java Sound MIDI API**
(`javax.sound.midi`) and Java2D PNG rendering. There are no third-party runtime
libraries, JSyn dependency, external recordings, or required MIDI keyboard.
The first Maven build needs access to the declared build plugins.

```bash
mvn clean compile
mvn -q exec:java "-Dexec.args=--parameters 11111111"
mvn -q exec:java "-Dexec.args=--check1 11111111"
mvn -q exec:java "-Dexec.args=--task1 11111111"
mvn test
```

Replace `11111111` with your **eight-digit student ID**, including leading zeroes.
Run commands from the project root. Each command is independent of previous runs;
shared student algorithms are reused by later tasks, so complete the tasks in
order. `--checkN` does not run the full test suite.

| Command           | Behavior                                                                                             |
| ----------------- | ---------------------------------------------------------------------------------------------------- |
| `--parameters ID` | Print reproducible assignment settings.                                                              |
| `--check1..8 ID`  | Construct the experiment and export MIDI, CSV and PNG without opening a synthesizer or audio device. |
| `--task1..8 ID`   | Export the same data and play every comparison through a real MIDI synthesizer.                      |
| `mvn test`        | Run the device-free algorithm/file/plot checks and a real sequencer transport check.                 |

In IntelliJ run `Main.main` with, for example, `--check4 11111111` as Program
arguments and the project root as Working directory. A Maven run configuration
uses `compile exec:java "-Dexec.args=--check4 11111111"`, without the leading `mvn`.

Playback requires an available Java Sound synthesizer, a suitable General MIDI
sound bank, and an audio output device. The program prints the synthesizer and
bank names. Different sound banks may produce different audible results from
the same events. Start with low device volume. If playback is unavailable, use
the offline commands to complete event construction and analysis; perform the
listening observations later on a configured workstation. No audio-device
failure is treated as a failed note/timing algorithm.

All files go to **`output/<student-id>/`**. Repeating a command replaces its own
files and preserves unrelated files. Task 3 plays six comparisons, including
both message-based and direct-channel routes. Playback can be interrupted with
Ctrl+C. The prepared normal/error cleanup releases the pedal and sends channel
cleanup messages before closing the synthesizer; abrupt process termination
can bypass Java cleanup.

## Assignment settings and conventions

The generator version is **`lab06-v1`**. Preserve its random-call order and version.
The seed is the numeric value of the ID; the ID itself stays a string.

| Assigned parameter | Values         | Meaning                                                |
| ------------------ | -------------- | ------------------------------------------------------ |
| `rootNote`         | 60, 62, 65, 67 | First note of the assigned melody.                     |
| `bpm`              | 80, 100, 120   | Quarter-note tempo for melody experiments.             |
| `velocity`         | 72, 84, 96     | Attack velocity, shared across controlled comparisons. |
| `fluteVolume`      | 56, 64, 72     | CC7 balance of the octave flute in Task 4.             |
| `transpose`        | -5, 5, 7       | Semitone shift in Task 7.                              |

`parameters.txt` records these values with the ID and generator version. Fixed
teaching fixtures override only the settings explicitly listed in each task.

- **60 = C4**, 69 = A4 (440 Hz at standard tuning). English B corresponds to
  Slovenian/German H; English B-flat corresponds to Slovenian/German B.
- User-facing channels are **1–16**; Java and transmitted channel indices are
  **0–15**. CSV exports contain both `channel` and `index`.
- Instrument programs are transmitted as **0–127**. Piano = 0, violin = 40,
  flute = 73, assuming a compatible General MIDI bank.
- The common resolution is **480 PPQ**, meaning ticks per quarter note. Task 2
  also uses 960 PPQ. This is not the 24-pulse MIDI Timing Clock.
- Track and event positions in Java are **absolute ticks**. The MIDI file writer
  handles the serialized delta times. Do not store a duration in a Note On.
- Note durations mean the interval between Note On and Note Off: **key-held
  duration**, not the complete audible duration of an instrument's sound.
- Default comparison state is CC7 = 100, CC11 = 127, CC64 = 0 and pitch bend = 8192. A fresh synthesizer is used for each live comparison, with explicit reset.
  A MIDI file's final sound still depends on its player's bank and controller support.

## Tasks and expected evidence

All **eight tasks are required and equally weighted**. Small coding sections
can require substantial listening and interpretation. Complete only the marked
student regions; validation, lifecycle, scheduling, plotting and file I/O are
prepared infrastructure. Do not hard-code your generated parameters.

| Task | Main content                                                      | Main output prefixes                                       |
| ---- | ----------------------------------------------------------------- | ---------------------------------------------------------- |
| 1    | Receiver, Note On, two representations of Note Off, message bytes | `task1-note-off`, `task1-zero-velocity-off`                |
| 2    | Melody, absolute positions, constant tempo and PPQ                | `task2-assigned`, `task2-double-tempo`, `task2-double-ppq` |
| 3    | GM program numbers, ShortMessage versus MidiChannel               | `task3-piano/violin/flute-messages/direct`                 |
| 4    | Two instrument channels, a chord, shared channel state            | `task4-independent`, `task4-shared-channel`                |
| 5    | Attack velocity, CC7 and CC11                                     | `task5-velocity`, `task5-volume40/100`, `task5-expression` |
| 6    | CC64 threshold, key release and pedal-deferred release            | `task6-pedal0/127/63/64`                                   |
| 7    | Matching transposed On/Off pairs and channel-wide pitch bend      | `task7-transpose0/<assigned>`, `task7-bend`                |
| 8    | Piecewise tempo conversion, metadata and MIDI roundtrip           | `task8-tempo-map`, `task8-tempo-map-readback`              |

### Task 1 — One note and MIDI message bytes

**Goal:** start and release one note by constructing MIDI messages.

**Your work — `NoteMessages.java`:** replace the TODO sections in all four
methods. Use the method arguments; the methods must also work for notes and
channels other than the runner's example.

1. `connect(synthesizer)`: return the `Receiver` belonging to the supplied,
   already opened synthesizer. Opening and closing the device are prepared.
2. `on(channel, note, velocity)`: return a `ShortMessage` with the Note On
   command and the supplied channel, note number and attack velocity.
3. `off(channel, note, releaseVelocity)`: return a Note Off message with the
   supplied channel, note number and release velocity.
4. `zeroVelocityOff(channel, note)`: return a Note On message for the supplied
   channel and note, with velocity zero.

Use `MidiSupport.message(command, channel, data1, data2)` to construct messages.
The constructor methods return messages; they do not send or schedule them.
Leave the existing range checks in place.

**Prepared experiment:** you do not need to implement playback or waiting.

The prepared runner selects piano, starts **note 60 with velocity 100**, and
releases the key after **two seconds**. It plays through the receiver, not a
sequencer. The offline version records the same scheduled events without opening
the synthesizer. At 120 BPM and PPQ 480, the release position is tick 1920.

Check these byte examples:

```text
90 3C 64   Note On, channel index 0, note 60, velocity 100
80 3C 40   Note Off, same channel and note, release velocity 64
90 3C 00   Alternative Note Off
```

Inspect the exported decimal bytes, hexadecimal bytes, event lengths and paired
notes. Explain which bits identify the command and channel, why attack velocity
0 cannot start a note, and why Note Off can be followed by an audible release
tail. Do not infer a fixed release time from release velocity.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check1 11111111"
mvn -q compile exec:java "-Dexec.args=--task1 11111111"
```

### Task 2 — Melody, tempo and tick resolution

**Goal:** turn a list of pitches and note values into a timed MIDI melody,
then compare the effects of changing tempo and PPQ.

**Your work — `Melody.java`:** complete these four methods in order:

1. `ticks(quarters, ppq)`: return the number of ticks for the supplied duration
   in quarter-note units, rounded to the nearest whole tick.
2. `seconds(ticks, ppq, bpm)`: return the duration in seconds at the supplied
   constant quarter-note tempo. Use floating-point division.
3. `notes(root, channel, velocity, ppq)`: return the 12 melody notes as a
   `List<Music.Note>`. Use the prepared `OFFSETS` and `QUARTERS` tables.
   Each note number is relative to `root`. The first note starts at tick 0;
   each following note starts when the preceding note ends. Convert each
   duration to ticks and preserve the supplied channel and velocity.
4. `add(track, notes)`: add two messages to `track` for every supplied note:
   Note On at its start and Note Off at its end. Both must use the note's
   channel and number. Use its attack velocity and release velocity 64.
   For consecutive repetitions of the same pitch, store the earlier Off
   before the later On at their shared tick.

`Music.Note` stores `(channel, number, start, duration, velocity)`; its `end()`
method returns `start + duration`. Do not edit the prepared melody tables or
write a separate melody in the runner.

**Helper for adding events:**

To schedule an individual note, use the prepared **`MidiSupport.add`** helper
once for its Note On and once for its Note Off. Its signature is:

```java
MidiSupport.add(Track track, MidiMessage message, long tick)
```

- `track`: the existing destination `Track` in your `Sequence`.
- `message`: the MIDI message to store, for example one returned by
  `NoteMessages.on` or `NoteMessages.off` from Task 1.
- `tick`: the **absolute position from the start of the sequence**, in ticks.
  It is neither a duration nor a delay relative to the previous event.

The helper checks that the tick is nonnegative, copies the message, wraps that
copy in a `MidiEvent` at the requested position, and adds it to the track.
The track keeps events ordered by position. The helper returns no value and
schedules data only; playback happens later when the sequencer runs.

For example, assuming `track` already exists and PPQ is 480, schedule C4 on
channel index 0 from tick 480 to tick 960:

```java
MidiSupport.add(track, NoteMessages.on(0, 60, 90), 480);
MidiSupport.add(track, NoteMessages.off(0, 60, 64), 960);
```

Both messages identify the same channel and note. Their positions differ by
480 ticks, so the key-held duration is one quarter note. At 120 BPM the note
starts at 0.5 seconds, releases at 1.0 seconds, and is held for 0.5 seconds.
The `90` is attack velocity; `64` in the Off message is release velocity.
In `Melody.add`, apply this same pair of helper calls to each note using its
own start and end positions. `Melody.add` handles a list of notes, whereas
`MidiSupport.add` inserts just **one timed MIDI message** per call.

| Note at original root F4 | MIDI number | Duration in quarter notes |
| ------------------------ | ----------- | ------------------------- |
| F4                       | 65          | 1                         |
| F4                       | 65          | 1                         |
| F4                       | 65          | 1                         |
| C4                       | 60          | 1                         |
| D4                       | 62          | 1                         |
| D4                       | 62          | 1                         |
| C4                       | 60          | 2                         |
| A4                       | 69          | 1                         |
| A4                       | 69          | 1                         |
| G4                       | 67          | 1                         |
| G4                       | 67          | 1                         |
| F4                       | 65          | 2                         |

`OFFSETS` and `QUARTERS` are prepared immutable input tables. The assigned root
replaces 65; all intervals stay the same. The excerpt is **14 quarter notes**,
not four complete 4/4 bars. The runner appends one quarter rest, giving an End of
Track position of 15 quarters. Report melody duration separately from file duration.

For constant tempo:

```text
ticks = round(quarter_note_units * PPQ)
event_seconds = tick / PPQ * 60 / BPM
key_seconds = (off_tick - on_tick) / PPQ * 60 / BPM
```

Use floating-point division. Compare assigned tempo, double tempo, and double
PPQ at the original tempo. Doubling tempo preserves the tick-domain melody and
halves its time in seconds. Doubling PPQ doubles the positions but preserves
musical placement and time in seconds. At a repeated note boundary, insert the
old Note Off before the new Note On.

Submit a piano roll and a note table with at least one quarter note and one half
note worked out by hand. Explain why 4/4 does not itself specify a duration in seconds.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check2 11111111"
mvn -q compile exec:java "-Dexec.args=--task2 11111111"
```

### Task 3 — Program Change and direct channel methods

**Goal:** play the same performance with three instruments and compare
message-based control with direct calls to a `MidiChannel`.

**Your work — `Programs.java`:**

1. In `change(channel, transmittedProgram)`, return a Program Change
   `ShortMessage` for the supplied channel and transmitted program number.
   Use `MidiSupport.message`; its unused `data2` argument can be zero.
   Do not add one to the program number. Program Change carries one data byte,
   so the complete message is two bytes long.
2. In `direct(channel, message)`, inspect the supplied message's command and
   perform the corresponding operation on the supplied `MidiChannel`:
   `programChange`, `noteOn`, or `noteOff`. Forward the message's program or
   note and velocity values, including release velocity. This method performs
   the channel operation and returns no value.

The runner supplies the correct channel object. You do not need to create
threads, connect devices, or implement delays.

**Prepared experiment:** the runner uses the following GM programs.

| GM sound             | Common list number | Transmitted program |
| -------------------- | ------------------ | ------------------- |
| Acoustic Grand Piano | 1                  | 0                   |
| Violin               | 41                 | 40                  |
| Flute                | 74                 | 73                  |

Play the same melody with all three instruments. For each, compare sending
ShortMessages to a Receiver with calling `programChange`, `noteOn`, and `noteOff`
on the corresponding `MidiChannel`. Dispatch the requested message values,
including release velocity; do not substitute a fixed note or instrument.

The direct comparisons export `-direct-comparison.csv` using a device-free
channel spy. Live direct playback actually calls the synthesizer's `MidiChannel`
methods. The prepared scheduler uses absolute deadlines; timing and sleeping
are not student tasks.

Explain what changes in the data when you change instrument, why the two API
routes represent the same performance, and why a different GM bank can sound different.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check3 11111111"
mvn -q compile exec:java "-Dexec.args=--task3 11111111"
```

### Task 4 — Tracks, channels and simultaneous notes

**Goal:** build two arrangements that demonstrate independent channel state
and shared channel state.

**Your work — `Arrangement.java`:** the supplied `Sequence` already contains a
conductor track. Use its `getResolution()` as PPQ. Create the musical tracks and
populate them in the two marked methods; both methods return no value.

1. In `build(sequence, p)`, create one piano track and one flute track. Select
   piano program 0 on channel index 0 and flute program 73 on channel index 1
   at tick 0. Set CC7 to 100 for piano and `p.fluteVolume()` for flute.
   Add the assigned melody to both tracks, with the flute **12 semitones
   higher**. Both parts use `p.velocity()` and start together at tick 0.
   Also add three piano notes at tick 0, at offsets **-12, -8, -5** from
   `p.rootNote()`, lasting two quarters and using `p.velocity()`.
2. In `sharedChannel(sequence, p)`, create two musical tracks that both use
   channel index 0. Select piano in the first at tick 0, then flute in the
   second at tick PPQ. Add one note to each track at tick `2*PPQ`: root in
   the first and root+7 in the second. Both last one quarter and use the
   assigned velocity.

Reuse `Melody.notes` and `Melody.add` for the melodies and note pairs.
`MidiSupport.namedTrack` creates a named track; `Programs.change` creates a
program message; `MidiSupport.cc(channel, controller, value)` creates a
controller message. Insert each message with `MidiSupport.add`.
The runner supplies the tempo and final End of Track positions.

**Check the two arrangements:**

The later shared-channel program is intentionally selected **before** both
attacks. This avoids relying on provider-dependent ordering of conflicting
program changes at the same tick in different tracks.

The independent arrangement has three tracks including the conductor, two
channels, and a maximum of **five key-held notes**. The shared example has three
tracks, one channel and two simultaneous notes, both starting under the flute
program. Explain why adding tracks does not create independent instrument state
or increase the synthesizer's available voice resources.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check4 11111111"
mvn -q compile exec:java "-Dexec.args=--task4 11111111"
```

### Task 5 — Velocity, channel balance and expression

**Goal:** distinguish attack velocity, channel balance and dynamics changed
during a held note.

**Your work — `Expression.java`:** add events to the supplied track in three
separate methods. All three use channel index 0 and return no value.

1. `velocity(track, note, ppq)`: add three On/Off pairs for the supplied note.
   Use attack velocities **32, 80, 120**, starting at beats **0, 2, 4**.
   Each note lasts one quarter; use release velocity 64. Convert beat
   positions to ticks using the supplied PPQ. Add no CC7 or CC11 changes.
2. `volume(track, value)`: add one CC7 message at tick 0 with the supplied
   value. The runner calls this method in separate cases with **40** and
   **100**, and adds the same assigned melody to both cases. This method
   must not add notes or modify their velocity.
3. `phrase(track, ppq)`: add CC11 values **32, 56, 88, 112, 72, 32** at beats
   **0, 1, 2, 3, 4, 5**. The runner supplies a single flute note held for
   six quarters. Do not add or retrigger the note in this method.

Use `MidiSupport.cc` to construct controller messages and `MidiSupport.add`
to place them in the track. The runner selects the instrument and tempo;
velocity and expression examples use 120 BPM. CC7 stays at 100 in the
velocity and expression examples; CC11 stays at 127 in the other examples.

**Check and compare:**

Use the controller plots to distinguish a value sent at attack from state
changed during a sounding note. Listen for level and possible timbre differences.
Velocity, CC7 and CC11 values are not decibels or universal linear gain factors.
Describe the observed response of your bank instead of assuming that doubling
a value doubles amplitude. Explain why CC11 can change a phrase without retriggering a note.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check5 11111111"
mvn -q compile exec:java "-Dexec.args=--task5 11111111"
```

### Task 6 — Sustain pedal and key release

**Goal:** show how sustain changes the release of sounding notes while
leaving their Note Off positions unchanged.

**Your work — `Sustain.java`:**

1. `held(cc64)`: return `false` for values **0–63** and `true` for
   **64–127**. This method only interprets a controller value; it adds no events.
2. `pedal(track, value, upTick)`: add a CC64 message on channel index 0
   with the supplied `value` at tick 0. Add a second CC64 message with
   value 0 at the supplied absolute `upTick`. This method returns no value.

Reuse `MidiSupport.cc` and `MidiSupport.add`. Keep the existing validation.
Do not change the prepared notes or their Note Off messages. The runner
supplies the pedal-up position as beat **4**, converted to ticks.

**Prepared experiment:**

The prepared piano phrase contains three ascending notes at beats **0, 1, 2**,
each key held for **half a quarter note**, at 120 BPM. Compare initial pedal
values **0, 127, 63, 64** without changing any Note On/Off event.

The piano roll shows key-held rectangles and pale pedal-deferred extensions.
These extensions indicate when the pedal permits release, **not measured audio
envelopes or guaranteed audible sustain**. A piano sample can decay while the
pedal is held and can continue releasing after pedal-up.

Report that the maximum key-held count stays one, while the count of key-held
or pedal-deferred attacks reaches three for pedal-on cases. Explain the boundary
between 63 and 64, why Note Off is still sent while the pedal is held, and why
the pedal differs from an ADSR envelope's sustain level.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check6 11111111"
mvn -q compile exec:java "-Dexec.args=--task6 11111111"
```

### Task 7 — Transposition and pitch bend

**Goal:** compare changing stored note numbers with changing the sounding
pitch of all notes on one channel.

**Your work — `Pitch.java`:**

1. `transpose(notes, semitones)`: return a new immutable list of notes with
   the supplied semitone offset added to every note number. Preserve channel,
   start, duration and velocity. Do not modify the input list or its notes.
   Leave the prepared range validation in place; do not clamp or wrap pitches.
   The runner generates matching On/Off messages from your returned notes.
2. `bend(channel, value)`: return a Pitch Bend `ShortMessage` for the supplied
   channel and 14-bit value. Split it into **low seven bits first**, then
   high seven bits, using `MidiSupport.message`. Minimum = 0, center =
   **8192**, maximum = 16383. This method does not schedule the message.
3. `curve(track, channel, ppq)`: construct and add **33** bend messages on
   the supplied channel over beats 0–3. Start centered, reach maximum at
   beat 1.5, and return to center at beat 3. Reuse `bend` and
   `MidiSupport.add`. This method adds controller events only, not notes.

Use these mathematical definitions for the curve, with floating-point division.
Round both tick positions and bend offsets to the nearest integer:

```text
i = 0..32
position_quarters = 3*i/32
triangle = i/16               for i <= 16
           (32-i)/16          for i > 16
value = 8192 + round(8191*triangle)
```

The prepared RPN setup requests a bend sensitivity of **+/-2 semitones** on
channel index 0. Two violin notes on that channel are held over the curve:
root at beat 0 and root+7 at beat 1, both released at beat 4. A root+12 violin
reference on channel index 1 is held from beat 1 to beat 4 and stays unbent.

Show that bend changes neither stored note number. Explain why it also affects
the later note on the same channel and persists until changed. Check that the
final curve value is centered before key release. The exact sound and support
for the requested range depend on the receiving instrument.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check7 11111111"
mvn -q compile exec:java "-Dexec.args=--task7 11111111"
```

### Task 8 — Tempo map, metadata and MIDI file

**Goal:** calculate elapsed time across a tempo change and create the
metadata needed to save and replay that change in a MIDI file.

**Your work — `TempoFiles.java`:**

1. `secondsAt(tick, ppq, map)`: return elapsed seconds from the sequence start
   to the supplied absolute tick. Each `Music.Tempo` contains the position
   where a tempo begins and its microseconds per quarter note. Split the
   interval at tempo changes, convert each segment at its own tempo, and
   sum the segment durations. Handle any valid supplied map, including more
   than two tempos; do not hard-code the six-second example.
2. `duration(on, off, ppq, map)`: return the difference between elapsed time
   at Off and elapsed time at On, reusing `secondsAt`. A note can cross one
   or more tempo boundaries.
3. `metadata(conductor, ppq)`: add a descriptive Track Name and a 4/4 time
   signature at tick 0, the two Set Tempo events below, and a descriptive
   marker at the tempo change. Use tick **4*ppq** for that change, rather
   than hard-coding 1920. This method returns no value.

For a segment, use `seconds = segmentTicks / PPQ * microsPerQuarter / 1000000`
with floating-point arithmetic. Prepared validation checks the map's ordering,
initial tempo and valid ranges. Keep that validation unchanged.

**Metadata to add:**

| Position  | Tempo   | Set Tempo value                  |
| --------- | ------- | -------------------------------- |
| tick 0    | 120 BPM | 500000 microseconds per quarter  |
| tick 1920 | 60 BPM  | 1000000 microseconds per quarter |

Use the prepared `MidiSupport.text`, `meta` and `tempo` constructors. Track Name
has type `0x03`, Marker `0x06`, Time Signature `0x58`, and Set Tempo `0x51`.
The four data bytes for the 4/4 event are **[4, 2, 24, 8]**: numerator, power-of-two
denominator exponent, MIDI clocks per metronome click, and notated 32nd notes
per MIDI quarter. Thus the denominator byte is 2, not 4.

The prepared arrangement has eight quarter-note piano pulses on channel index
0 and a flute note on channel index 1 from **tick 1440 to 2400**. End of Track
is tick **3840**. Expected results:

```text
first four quarters:  4 * 0.5 s = 2 s
next four quarters:   4 * 1.0 s = 4 s
total sequence:                    6 s
crossing flute note:  0.5 + 1.0 = 1.5 s
```

The prepared writer saves a **type 1 Standard MIDI File**. The runner reopens
it and compares division type, PPQ, track count, end position, and each track's
ordered message content and tick positions. File bytes themselves need not be
identical after serialization. End of Track encoding is managed by the MIDI
writer; the intended musical end position must stay unchanged.

Inspect `-conversion.csv`, `-roundtrip.txt` and the `-readback` exports. Listen
for the tempo change. Explain why changing PPQ alone would not create this
change in playback speed and why one constant-tempo formula cannot be used
across a tempo boundary.

**Run and inspect:** replace `11111111` with your student ID. The first command
exports without audio; the second exports and plays all comparisons.

```bash
mvn -q compile exec:java "-Dexec.args=--check8 11111111"
mvn -q compile exec:java "-Dexec.args=--task8 11111111"
```

## Exports and interpretation

Each experiment exports:

| File suffix        | Content                                                                                                      |
| ------------------ | ------------------------------------------------------------------------------------------------------------ |
| `.mid`             | Type 1 MIDI file: performance data and metadata, not recorded audio.                                         |
| `-events.csv`      | Absolute ticks, quarter positions, seconds, tracks, channel/index, commands, raw bytes and post-event state. |
| `-notes.csv`       | Paired On/Off events, note names, key-held durations and pedal-release eligibility.                          |
| `-timeline.csv`    | Musical positions and elapsed seconds, including tempo boundaries.                                           |
| `-piano-roll.png`  | MIDI note numbers against musical position; color identifies channel.                                        |
| `-controllers.png` | Attack velocities and stepwise CC7, CC11, CC64 and pitch bend state.                                         |
| `-time.png`        | Musical position against elapsed seconds.                                                                    |
| `-settings.txt`    | Assignment settings, tempo map, track/channel counts and maximum logical note counts.                        |

The event CSV's `track` is zero-based. Metadata rows have empty channel/data
fields and -1 for inapplicable channel-state columns. `program` in the note
table is captured at Note On; event rows show the current channel state.
Controller curves assume the documented default state until an explicit event.
Tempo plots and exports use an independent prepared reference conversion, so
Tasks 1–7 do not depend on completing Task 8.

The diagnostic analyzer supports the note/CC events used here. It rejects an
unmatched Off, overlapping key-downs of the same channel/note, or a sequence
ending with unreleased or pedal-deferred notes. A repeated pitch after key
release while the pedal remains held is allowed and counted as a separate attack.
Counts describe MIDI state, not measured synthesizer voice allocation or a
guarantee that every retained attack is still audible. The analyzer is not a
general interpreter of Channel Mode, sostenuto or arbitrary MIDI files.

All plots are rendered without a graphical display. The lab exports no WAV and
makes no audio-level, spectral or sample-accuracy claims. Prepared live playback
waits for sequencer completion, allows 1.2 seconds for release, then cleans up.
That grace interval is not a measurement of when a particular instrument becomes silent.

## Verification and report

```bash
mvn test
```

`MidiChecks` covers hand-calculated message bytes, receiver access, channel and
note ranges, melody transcription, repeated-note ordering, PPQ/tempo invariants,
GM program indices, direct-channel equivalence, track/channel state, chords,
controlled dynamics, all 128 pedal values, pedal-deferred repeated pitches,
transposition boundaries and input preservation, pitch-bend fields, multiple
tempo segments, crossing notes, MIDI roundtrip and readable PNG exports.

`SequencerChecks` opens a real Java sequencer with **no default synthesizer**,
connects a capturing Receiver and checks the transmitted program and note
messages. Its short scheduling guard tolerates desktop timing jitter. Neither
suite opens a synthesizer or needs an audio device. Java's `-ea` flag is not
required. The Maven build fails when an explicit check throws.

The project compiles before the TODOs are filled, but its experiments
and tests intentionally fail at unimplemented sections. A TODO exception is an
expected failure until that section is implemented; it is not a passing or skipped
check. Use the relevant `--checkN` while progressing.

After completing all TODOs, run `mvn test` and export **all eight tasks** with
`--check1` through `--check8`. Use `--task1` through `--task8` for the listening
observations on a workstation with a configured synthesizer and sound bank.
Numerical checks do not replace listening and explanation.

Generated `target/`, `output/` and distribution ZIPs are build or experiment
artifacts; exclude them from a student distribution.

For **each task**, report:

1. Student ID, relevant generated settings, and your prediction.
2. Implemented message/data transformation and a small worked example.
3. Relevant CSV values and annotated plots, with units and channel labels.
4. Explanation of the result and listening observations, including bank name.

Also answer these short questions:

- Why does MIDI not normally capture the waveform of someone singing?
- How do PPQ 480 and MIDI Timing Clock's 24 pulses per quarter differ?
- Which roles do Timing Clock, Start/Continue/Stop and Active Sensing have?
- Why do system messages have no channel index? What is SysEx used for?
- Why does `FF` introduce a meta event inside a MIDI file but mean System Reset
  on a MIDI 1.0 connection?
- In General MIDI, how does channel 10 (index 9) use note numbers differently
  from the melodic channels used in this lab?

System-message transmission, external MIDI hardware, a custom synthesizer,
WAV rendering, and a GUI editor are outside the required implementation.

## Code map and sources

| File                                                           | Responsibility                                                  |
| -------------------------------------------------------------- | --------------------------------------------------------------- |
| `NoteMessages.java`                                            | Task 1 receiver and note message constructors.                  |
| `Melody.java`                                                  | Task 2 time conversion, melody positions and On/Off pairs.      |
| `Programs.java`                                                | Task 3 Program Change and direct MidiChannel dispatch.          |
| `Arrangement.java`                                             | Task 4 independent channels, chord and shared-channel example.  |
| `Expression.java`                                              | Task 5 velocity, CC7 and CC11.                                  |
| `Sustain.java`                                                 | Task 6 CC64 threshold and pedal events.                         |
| `Pitch.java`                                                   | Task 7 immutable transposition, bend fields and curve.          |
| `TempoFiles.java`                                              | Task 8 piecewise conversion and conductor metadata.             |
| `Music.java`, `GenerateParameters.java`, `TaskParameters.java` | Prepared records, validation and versioned assignments.         |
| `MidiSupport.java`                                             | Prepared messages, tracks, tempo helpers and file I/O.          |
| `Experiments.java`, `Main.java`                                | Prepared fixtures, routes, exports and commands.                |
| `MidiPlayback.java`, `ChannelCapture.java`                     | Prepared live lifecycle/scheduling and device-free channel spy. |
| `Diagnostics.java`, `Plot.java`                                | Prepared event analysis, CSV and headless PNG exports.          |

The assignment follows **Lab 6: MIDI and musical events**, slides 2–20.
Tasks 1–4 retain and expand Tasks 1–9 from **6MIDI.pdf**, slides 9–16; the melody
comes from slide 14. The Maven/CLI/parameter/export structure follows Lab 05.
Tasks 5–8 cover the newer material on dynamics, pedal behavior, bend and tempo maps.

API references: [Java Sound MIDI package](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/midi/package-summary.html),
[Sequencer](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/midi/Sequencer.html),
[MidiChannel](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/sound/midi/MidiChannel.html).

