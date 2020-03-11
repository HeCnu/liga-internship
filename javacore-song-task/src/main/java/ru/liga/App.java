package ru.liga;

import ch.qos.logback.classic.util.ContextInitializer;
import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.NoteOff;
import com.leff.midi.event.NoteOn;
import com.leff.midi.event.meta.Tempo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.domain.Note;
import ru.liga.songtask.domain.NoteSign;
import ru.liga.songtask.util.SongUtils;

@Slf4j
public class App {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {

        if(args.length < 1) {
            logger.debug("Not enough data for the program to work");
            logger.info("Input java -jar (dir to midifile.midi) (analyze/change -trans 2 -tempo 20)");
            throw new IOException();
        }

        final String midiPath = args[0];
        MidiFile midiFile = new MidiFile(new FileInputStream(midiPath));
        Tempo last = (Tempo) midiFile.getTracks().get(0).getEvents().last();

        switch (args[1]) {
            case "analyze":
                analizeMidiFile(midiFile, last);
                break;
            case "change":
                changeMidiFile(midiFile, last, args);
                break;
            default:
                logger.debug("No input action!");
                break;
        }

    }

    private static void changeMidiFile(MidiFile midiFile, Tempo last, String[] args) throws IOException {

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
            MidiFile midiFile1 = new MidiFile(file);
            MidiFile newMidi = transpositionMidiFile(midiFile1, transVar);
            newMidi = changeTempoMidiFile(midiFile1, tempoPercent);
            String pathNew = getSavePath(transVar, tempoVar, file);
            newMidi.writeToFile(new File(pathNew));
            logger.info("Change pathname: {}", pathNew);
        }
    }

    private static MidiFile changeTempoMidiFile(MidiFile midiFile, float tempo) {
        logger.debug("Old Bpm = {}", SongUtils.getTempo(midiFile).getBpm());
        MidiFile newMidiFile = new MidiFile();
        List<MidiTrack> newListTracks = midiFile.getTracks();
        for (MidiTrack midiTrack: newListTracks){
            MidiTrack newMidiTrack = changeTempoOfMidiTrack(tempo, midiTrack);
            newMidiFile.addTrack(newMidiTrack);
        }

        logger.debug("New Bpm = {}", SongUtils.getTempo(newMidiFile).getBpm());
        return newMidiFile;
    }

    private static MidiTrack changeTempoOfMidiTrack(float tempoVar, MidiTrack midiTrack) {
        MidiTrack newMidiTrack = new MidiTrack();
        TreeSet<MidiEvent> newMidiEventList = midiTrack.getEvents();
        for (MidiEvent midiEvent: newMidiEventList) {
            if (midiEvent.getClass().equals(Tempo.class)) {
                Tempo tempo = getChangedTempo(tempoVar, (Tempo)midiEvent);
                tempo.setBpm(tempo.getBpm() * tempoVar);
                newMidiTrack.getEvents().add(tempo);
            } else {
                newMidiTrack.getEvents().add(midiEvent);
            }
        }
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

    private static void analizeMidiFile(MidiFile midiFile, Tempo last) {
        List<Note> notesAll = new ArrayList<>();
        for (int numOfTracks = 1; numOfTracks < midiFile.getTracks().size(); numOfTracks++) {
            List<Note> notes =  eventsToNotes(midiFile.getTracks().get(numOfTracks).getEvents());
            if (!notes.isEmpty()){
                notesAll.addAll(notes);
            }
        }
        notesAll = sortNotes(notesAll);
        List<Note> uniqueNotes = searchingUniquePlacesForVocal(last, midiFile, notesAll);

        searchRangeOfNotes(uniqueNotes);
        countOfNotesSameDuration(last, midiFile, uniqueNotes);
        countOfSameNotes(last, midiFile, uniqueNotes);
    }

    private static void searchRangeOfNotes(List<Note> uniqueNotes) {
        logger.debug("Start function of searching range of notes");
        logger.info("[Range]");
        int minMidiNote = uniqueNotes.get(0).sign().getMidi(), maxMidiNote = uniqueNotes.get(0).sign().getMidi();
        Note minNote, maxNote;
        minNote = maxNote = null;
        for (Note note: uniqueNotes) {
            if(note.sign().getMidi() >= maxMidiNote) {
                maxMidiNote = note.sign().getMidi();
                maxNote = note;
            }
            else {
                minMidiNote = note.sign().getMidi();
                minNote = note;
            }
        }
        logger.info("Upper: " + maxNote.sign().fullName()); logger.info("Bottom: " + minNote.sign().fullName()); logger.info("Range: " + (maxMidiNote - minMidiNote));
        logger.debug("Finish function of searching range of notes");
    }

    private static void countOfNotesSameDuration(Tempo last, MidiFile midiFile, List<Note> uniqueNotesList) {
        logger.debug("Start function of counting of notes same duration");
        List<Integer> durationList = new ArrayList<>();
        for (Note tempNote: uniqueNotesList) {
            durationList.add(SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), tempNote.durationTicks()));
        }
        Set<Integer> uniqueDurationList = new HashSet<Integer>(durationList);
        logger.info("[Number of notes by duration]");
        for (Integer uniqueDuration: uniqueDurationList){
            int counterDuration = 0;
            for (Note uniqueNote: uniqueNotesList){
                if (uniqueDuration == SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), uniqueNote.durationTicks()))
                    counterDuration++;
            }
            logger.info(uniqueDuration + "ms = " + counterDuration );
        }
        logger.debug("Finish function of counting of notes same duration");
    }

    private static void countOfSameNotes(Tempo last, MidiFile midiFile, List<Note> uniqueNotesList) {
        logger.debug("Start function of counting of same notes");
        List<String> enteriesNoteList = new ArrayList<>();
        for (Note tempNote: uniqueNotesList) {
            enteriesNoteList.add(tempNote.sign().fullName());
        }
        Set<String> uniqueEnteriesNoteList =  new HashSet<String>(enteriesNoteList);
        logger.info("[List of notes with the number of occurrences]");
        for (String enteriesNote: uniqueEnteriesNoteList){
            int counterEnteries = 0;
            for (Note uniqueNote: uniqueNotesList){
                if (enteriesNote.equals(uniqueNote.sign().fullName()))
                    counterEnteries++;
            }
            logger.info(enteriesNote + " : " + counterEnteries );
        }
        logger.debug("Finish function of counting of same notes");
    }

    private static List<Note> searchingUniquePlacesForVocal(Tempo last, MidiFile midiFile, List<Note> notesAll) {

        logger.debug("Start function of searching places for vocal");
        Long startPlayRequiredNote, finishPlayRequiredNote, startPlayNote;
        List<Note> foundNotes = new ArrayList<>();

        for (int i = 0; i < notesAll.size(); i++) {
            startPlayRequiredNote = notesAll.get(i).startTick();
            finishPlayRequiredNote = notesAll.get(i).endTickInclusive();
            int counterRepeatedNotes = 0;
            for (int j = 0; j < notesAll.size(); j++) {
                startPlayNote = notesAll.get(j).startTick();
                if (startPlayRequiredNote <= startPlayNote && startPlayNote < finishPlayRequiredNote) {
                    counterRepeatedNotes ++;
                    if (counterRepeatedNotes > 1) {
                        break;
                    }
                }
            }
            if (counterRepeatedNotes == 1) {
                logger.debug("[{}] Нота {} с длительностью {}мс", i, notesAll.get(i).sign().fullName(),SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), notesAll.get(i).durationTicks()));
                foundNotes.add(notesAll.get(i));
            }
        }

        logger.debug("Finish function of searching places for vocal. Function returned {} elements in list", foundNotes.size());
        return foundNotes;
    }

    /**
     * Этот метод, чтобы вы не афигели переводить эвенты в ноты
     *
     * @param events эвенты одного трека
     * @return список нот
     */
    public static List<Note> eventsToNotes(TreeSet<MidiEvent> events) {
        logger.debug("Start function of eventToNotes");
        List<Note> vbNotes = new ArrayList<>();

        Queue<NoteOn> noteOnQueue = new LinkedBlockingQueue<>();
        for (MidiEvent event : events) {
            if (event instanceof NoteOn || event instanceof NoteOff) {
                if (isEndMarkerNote(event)) {
                    NoteSign noteSign = NoteSign.fromMidiNumber(extractNoteValue(event));
                    if (noteSign != NoteSign.NULL_VALUE) {
                        NoteOn noteOn = noteOnQueue.poll();
                        if (noteOn != null) {
                            long start = noteOn.getTick();
                            long end = event.getTick();
                            vbNotes.add(
                                    new Note(noteSign, start, end - start));
                        }
                    }
                } else {
                    noteOnQueue.offer((NoteOn) event);
                }
            }
        }
        logger.debug("Finish function of eventToNotes");
        return vbNotes;
    }

    private static List<Note> sortNotes(List<Note> notes) {

        logger.debug("Sorting notes ...");
        List<Note> notes1 = notes;
        for (int i = 0; i < notes.size(); i++) {
            for (int j = i+1; j < notes1.size(); j++) {
                if (notes1.get(j).startTick() <= notes.get(i).startTick()) {
                    Note tempNote = notes.get(i);
                    notes.set(i, notes1.get(j));
                    notes.set(j, tempNote);
                }
            }
        }
        logger.debug("Notes were sort succesful");
        return notes;
    }

    private static Integer extractNoteValue(MidiEvent event) {
        if (event instanceof NoteOff) {
            return ((NoteOff) event).getNoteValue();
        } else if (event instanceof NoteOn) {
            return ((NoteOn) event).getNoteValue();
        } else {
            return null;
        }
    }

    private static boolean isEndMarkerNote(MidiEvent event) {
        if (event instanceof NoteOff) {
            return true;
        } else if (event instanceof NoteOn) {
            return ((NoteOn) event).getVelocity() == 0;
        } else {
            return false;
        }

    }
}
