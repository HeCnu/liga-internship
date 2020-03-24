package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.meta.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.App;

public class TempoChanger {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static MidiFile changeTempoMidiFile(MidiFile midiFile, float tempo) {
        logger.debug("Old Bpm = {}", SongUtils.getTempo(midiFile).getBpm());
        MidiFile newMidiFile = new MidiFile();
        midiFile.getTracks().stream()
                .forEachOrdered(track -> newMidiFile.addTrack(changeTempoOfMidiTrack(tempo, track)));
        logger.debug("New Bpm = {}", SongUtils.getTempo(newMidiFile).getBpm());
        return newMidiFile;
    }

    private static MidiTrack changeTempoOfMidiTrack(float tempoVar, MidiTrack midiTrack) {
        MidiTrack newMidiTrack = new MidiTrack();
        midiTrack.getEvents().stream()
                .map(midiEvent -> midiEvent.getClass().equals(Tempo.class) ? getChangedTempo(tempoVar, (Tempo) midiEvent) : midiEvent)
                .forEachOrdered(track -> newMidiTrack.getEvents().add(track));
        return newMidiTrack;
    }

    private static Tempo getChangedTempo(float percentTempo, Tempo midiEvent) {
        Tempo tempo = new Tempo(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getMpqn());
        tempo.setBpm(tempo.getBpm() * percentTempo);
        return tempo;
    }
}
