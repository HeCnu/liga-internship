package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;

import java.util.List;
import java.util.TreeSet;

public class TranspositionChanger {
    public static MidiFile transpositionMidiFile(MidiFile midiFile, int transVar) {
        MidiFile newMidiFile = new MidiFile();
        List<MidiTrack> newListTracks = midiFile.getTracks();
        for (MidiTrack midiTrack: newListTracks){
            MidiTrack newMidiTrack = transpositionEventsInTracks(transVar, midiTrack);
            newMidiFile.addTrack(newMidiTrack);
        }
        return newMidiFile;
    }

    private static MidiTrack transpositionEventsInTracks(int transVar, MidiTrack midiTrack) {
        MidiTrack newMidiTrack = new MidiTrack();
        TreeSet<MidiEvent> newMidiEventList = midiTrack.getEvents();
        for (MidiEvent midiEvent: newMidiEventList) {
            if (midiEvent.getClass().equals(NoteOn.class)) {
                NoteOn on = NoteChanger.getChangedNoteOn(transVar, (NoteOn)midiEvent);
                newMidiTrack.getEvents().add(on);
            } else if (midiEvent.getClass().equals(NoteOff.class)) {
                NoteOff off = NoteChanger.getChangedNoteOff(transVar, (NoteOff)midiEvent);
                newMidiTrack.getEvents().add(off);
            } else {
                newMidiTrack.getEvents().add(midiEvent);
            }
        }
        return newMidiTrack;
    }
}
