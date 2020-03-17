package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.event.MidiEvent;
import com.leff.midi.event.meta.Lyrics;
import com.leff.midi.event.meta.Tempo;
import com.leff.midi.event.meta.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.App;
import ru.liga.Vocal;
import ru.liga.songtask.domain.Note;

import java.util.*;
import java.util.stream.Collectors;

public class AnalyzeMidi {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void analizeMidiFile(MidiFile midiFile, Tempo last) {
        List<Note> uniqueNotes = processOfGettingUniqueNotesList(midiFile);
        searchRangeOfNotes(uniqueNotes);
        countOfNotesSameDuration(last, midiFile, uniqueNotes);
        countOfSameNotes(last, midiFile, uniqueNotes);
    }

    private static List<Note> processOfGettingUniqueNotesList(MidiFile midiFile) {
        List<Vocal> finalVocalEventsList = getVocalsList(midiFile);
        logger.debug("List vocal events was created");
        List<List<Note>> etnChangedLists = getETNChangedLists(midiFile);
        logger.debug("List with all of tracks was created");
        int sizeTrackFinal = getSizeTrackFinal(finalVocalEventsList, etnChangedLists);
        logger.debug("Some size of required track was defined");
        List<Note> finalListNote = new ArrayList<>();
        for (List<Note> list: etnChangedLists) {
            if (list.size() == sizeTrackFinal)
                finalListNote = list;
        }

        return finalListNote;
    }

    private static int getSizeTrackFinal(List<Vocal> finalVocalEventsList, List<List<Note>> etnChangedLists) {
        List<Integer> trackSizesList = getTrackSizesList(etnChangedLists);
        logger.debug("Array with size tracks was created");

        int sizeTrack = finalVocalEventsList.size(),
            sizeTrackMin = 0,
            sizeTrackMax = 0;
        for (int i = 0; i < trackSizesList.size(); i++) {
            if(trackSizesList.get(i) <= sizeTrack)
                sizeTrackMin = trackSizesList.get(i);
        }
        logger.debug("Min size of reqiured track - ", sizeTrackMin);
        for (int i = trackSizesList.size() - 1; i >= 0; i--) {
            if(trackSizesList.get(i) >= sizeTrack)
                sizeTrackMax = trackSizesList.get(i);
        }
        logger.debug("Min size of reqiured track - ", sizeTrackMin);

        if (Math.abs(sizeTrackMax - trackSizesList.size()) >= Math.abs(sizeTrackMin - trackSizesList.size()))
            return sizeTrackMin;
        else
            return sizeTrackMax;
    }

    private static List<Integer> getTrackSizesList(List<List<Note>> etnChangedLists) {
        List<Integer> trackSizesList = etnChangedLists.stream()
                .map(x -> x.size())
                .collect(Collectors.toList());

        Collections.sort(trackSizesList);
        return trackSizesList;
    }

    private static List<List<Note>> getETNChangedLists(MidiFile midiFile) {
        List<List<Note>> eventsToNoteChangedList = new ArrayList<>();
        midiFile.getTracks().stream()
                .forEachOrdered(midiTrack ->  eventsToNoteChangedList.add(SongUtils.eventsToNotes(midiTrack.getEvents())));
        return eventsToNoteChangedList;
    }

    private static List<Vocal> getVocalsList(MidiFile midiFile) {

        List<Vocal> finalVocalEventsList = new ArrayList<>();

        for (int numOfTracks = 1; numOfTracks < midiFile.getTracks().size(); numOfTracks++) {
            List<Vocal> tempVocalEventsList = findVocalEventsFromTrack(midiFile.getTracks().get(numOfTracks).getEvents());
            if (!tempVocalEventsList.isEmpty()){
                finalVocalEventsList.addAll(tempVocalEventsList);
            }
        }
        return finalVocalEventsList;
    }

    private static List<Vocal> findVocalEventsFromTrack(TreeSet<MidiEvent> midiEvents) {
        List<Vocal> resultList = new ArrayList<>();

        for (MidiEvent event: midiEvents) {
            if ((event instanceof Lyrics || event instanceof Text) && event.getTick() > 0) {
                logger.debug("Track with instance Lyrics or Text was found. Event body: \n{}\n", event.toString());
                long endTick = 0L;
                if (event.getTick() == event.getDelta()) {
                    endTick = event.getTick() + 120L;
                } else {
                    endTick = event.getTick() + event.getDelta();
                }
                Vocal vocal = new Vocal(event.toString(), event.getTick(), endTick);
                resultList.add(vocal);
            }
        }

        return resultList;
    }

    private static void searchRangeOfNotes(List<Note> uniqueNotes) {
        logger.debug("Start function of searching range of notes");
        logger.info("[Range]");
        int minMidiNote = uniqueNotes.get(0).sign().getMidi(),
            maxMidiNote = uniqueNotes.get(0).sign().getMidi();
        Note minNote, maxNote;
        minNote = maxNote = null;
        for (Note note: uniqueNotes) {
            if (note.sign().getMidi() >= maxMidiNote) {
                maxMidiNote = note.sign().getMidi();
                maxNote = note;
            }
            if (note.sign().getMidi() < minMidiNote){
                minMidiNote = note.sign().getMidi();
                minNote = note;
            }
        }
        logger.info("Upper: " + maxNote.sign().fullName());
        logger.info("Bottom: " + minNote.sign().fullName());
        logger.info("Range: " + (maxMidiNote - minMidiNote));
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
}
