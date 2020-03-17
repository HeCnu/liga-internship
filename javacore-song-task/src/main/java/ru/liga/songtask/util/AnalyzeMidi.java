package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.event.meta.Tempo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.App;
import ru.liga.songtask.domain.Note;
import java.util.*;

public class AnalyzeMidi {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void analizeMidiFile(MidiFile midiFile, Tempo last) {
        List<Note> notesAll = new ArrayList<>();
        for (int numOfTracks = 1; numOfTracks < midiFile.getTracks().size(); numOfTracks++) {
            List<Note> notes =  SongUtils.eventsToNotes(midiFile.getTracks().get(numOfTracks).getEvents());
            if (!notes.isEmpty()){
                notesAll.addAll(notes);
            }
        }
        List<Note> uniqueNotes = searchingUniquePlacesForVocal(last, midiFile, notesAll);

        searchRangeOfNotes(uniqueNotes);
        countOfNotesSameDuration(last, midiFile, uniqueNotes);
        countOfSameNotes(last, midiFile, uniqueNotes);
    }

    private static void searchRangeOfNotes(List<Note> uniqueNotes) {
        logger.debug("Start function of searching range of notes");
        logger.info("[Range]");
        int minMidiNote = uniqueNotes.get(0).sign().getMidi(),
            maxMidiNote = uniqueNotes.get(0).sign().getMidi();
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
        Set<Integer> uniqueDurationList = new HashSet(durationList);

        logger.info("[Number of notes by duration]");
        for (Integer uniqueDuration: uniqueDurationList){
            long counterDuration = getCounterDuration(last, midiFile, uniqueNotesList, uniqueDuration);
            logger.info(uniqueDuration + "ms = " + counterDuration);
        }
        logger.debug("Finish function of counting of notes same duration");
    }

    private static long getCounterDuration(Tempo last, MidiFile midiFile, List<Note> uniqueNotesList, Integer uniqueDuration) {
        return uniqueNotesList.stream()
                .filter(uniqueNote -> uniqueDuration == SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), uniqueNote.durationTicks())).count();
    }

    private static void countOfSameNotes(Tempo last, MidiFile midiFile, List<Note> uniqueNotesList) {
        logger.debug("Start function of counting of same notes");
        List<String> enteriesNoteList = new ArrayList<>();
        uniqueNotesList.stream()
                .forEachOrdered(tempNote -> enteriesNoteList.add(tempNote.sign().fullName()));
        Set<String> uniqueEnteriesNoteList =  new HashSet(enteriesNoteList);
        logger.info("[List of notes with the number of occurrences]");

        uniqueEnteriesNoteList.stream()
                .forEachOrdered(enteriesNote -> {
                    long counterEnteries = getCounterEnteries(uniqueNotesList, enteriesNote);
                    logger.info(enteriesNote + " : " + counterEnteries );
                });
        logger.debug("Finish function of counting of same notes");
    }

    private static long getCounterEnteries(List<Note> uniqueNotesList, String enteriesNote) {
        return uniqueNotesList.stream()
                .filter(uniqueNote -> enteriesNote.equals(uniqueNote.sign().fullName())).count();
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
                logger.debug("[" + i + "] Нота " + notesAll.get(i).sign().fullName() + " с длительностью " + SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), notesAll.get(i).durationTicks()) + "мс");
                foundNotes.add(notesAll.get(i));
            }
        }

        logger.debug("Finish function of searching places for vocal. Function returned {} elements in list", foundNotes.size());
        return foundNotes;
    }

}
