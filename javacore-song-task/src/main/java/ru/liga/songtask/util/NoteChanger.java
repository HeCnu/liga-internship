package ru.liga.songtask.util;

import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;

public class NoteChanger {

    public static NoteOff getChangedNoteOff(int trans, NoteOff midiEvent) {
        NoteOff off = new NoteOff(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        off.setNoteValue(off.getNoteValue() + trans);
        return off;
    }

    public static NoteOn getChangedNoteOn(int trans, NoteOn midiEvent) {
        NoteOn on = new NoteOn(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        on.setNoteValue(on.getNoteValue() + trans);
        return on;
    }

}
