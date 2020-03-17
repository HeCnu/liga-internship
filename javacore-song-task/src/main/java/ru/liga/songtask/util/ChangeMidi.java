package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.App;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TreeSet;

public class ChangeMidi {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void changeMidiFile(MidiFile midiFile, Tempo last, String[] args) throws IOException {

        int transVar = 0, tempoVar = 0;
        if (args.length > 2 && args[2].equals("-trans")) {
            try {
                transVar = Integer.parseInt(args[3]);
            } catch (Exception ex) {
                logger.debug("Oops... You forgot to input number. (-trans 2)");
                throw new RuntimeException(ex);
            }
        }
        if (args.length > 4 && args[4].equals("-tempo")) {
            try {
                tempoVar = Integer.parseInt(args[5]);
            } catch (Exception ex) {
                logger.debug("Oops... You forgot to input number. (-tempo 2)", ex);
                throw new RuntimeException(ex);
            }
        }
        if (transVar != 0 && tempoVar != 0) {
            float tempoPercent = 1.0F + tempoVar / 100.0F;
            File file = new File(args[0]);
            logger.debug("Creating new Midi file...");
            MidiFile newMidi1 = changeTempoMidiFile(midiFile, tempoPercent);
            MidiFile newMidi2 = transpositionMidiFile(newMidi1, transVar);
            String pathNew = getSavePath(transVar, tempoVar, file);
            newMidi2.writeToFile(new File(pathNew));
            logger.info("Change pathname: {}", pathNew);
        }
    }

    private static MidiFile changeTempoMidiFile(MidiFile midiFile, float tempo) {
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

    private static String getSavePath(int trans, float tempo, File file) {
        String newName = file.getName().replace(".mid", "") + "-trans" + trans + "-tempo" + tempo + ".mid";
        logger.info("File saved succesful");
        return file.getParentFile().getAbsolutePath() + File.separator + newName;
    }

    private static MidiFile transpositionMidiFile(MidiFile midiFile, int transVar) {
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
                NoteOn on = getChangedNoteOn(transVar, (NoteOn)midiEvent);
                newMidiTrack.getEvents().add(on);
            } else if (midiEvent.getClass().equals(NoteOff.class)) {
                NoteOff off = getChangedNoteOff(transVar, (NoteOff)midiEvent);
                newMidiTrack.getEvents().add(off);
            } else {
                newMidiTrack.getEvents().add(midiEvent);
            }
        }
        return newMidiTrack;
    }

    private static NoteOff getChangedNoteOff(int trans, NoteOff midiEvent) {
        NoteOff off = new NoteOff(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        off.setNoteValue(off.getNoteValue() + trans);
        return off;
    }

    private static NoteOn getChangedNoteOn(int trans, NoteOn midiEvent) {
        NoteOn on = new NoteOn(midiEvent.getTick(), midiEvent.getDelta(), midiEvent.getChannel(), midiEvent.getNoteValue(), midiEvent.getVelocity());
        on.setNoteValue(on.getNoteValue() + trans);
        return on;
    }
}
