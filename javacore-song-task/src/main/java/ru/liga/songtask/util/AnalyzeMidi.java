package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
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
import java.util.stream.Stream;

public class AnalyzeMidi {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void analizeMidiFile(MidiFile midiFile, Tempo last) {
//        List<Note> notesAll = new ArrayList<>();
//        for (int numOfTracks = 1; numOfTracks < midiFile.getTracks().size(); numOfTracks++) {
//            List<Note> notes =  SongUtils.eventsToNotes(midiFile.getTracks().get(numOfTracks).getEvents());
//            List<MidiEvent> midiEvents = midiFile.getTracks().get(numOfTracks).getEvents().stream()
//                    .filter(midiEvent -> midiEvent.toString().contains("Text:")).collect(Collectors.toList());
//            if (!notes.isEmpty()){
//                notesAll.addAll(notes);
//            }
//        }
//        List<Note> uniqueNotes = searchingUniquePlacesForVocal(last, midiFile, notesAll);

        List<Note> uniqueNotes = processOfGettingUniqueNotesList(midiFile);

        searchRangeOfNotes(uniqueNotes);
        countOfNotesSameDuration(last, midiFile, uniqueNotes);
        countOfSameNotes(last, midiFile, uniqueNotes);
    }

    private static List<Note> processOfGettingUniqueNotesList(MidiFile midiFile) {
        List<Note> uniqueNotes = new ArrayList<>();
        List<Note> allNotes = new ArrayList<>();
        List<Vocal> finalVocalEventsList = new ArrayList<>();


        for (int numOfTracks = 1; numOfTracks < midiFile.getTracks().size(); numOfTracks++) {
            List<Vocal> tempVocalEventsList = findVocalEventsFromTrack(midiFile.getTracks().get(numOfTracks).getEvents());
            if (!tempVocalEventsList.isEmpty()){
                finalVocalEventsList.addAll(tempVocalEventsList);
            }
        }

        List<List<Note>> eventsToNoteChangedList = new ArrayList<>();
        midiFile.getTracks().stream()
                .forEachOrdered(midiTrack ->  eventsToNoteChangedList.add(SongUtils.eventsToNotes(midiTrack.getEvents())));

        List<Integer> trackSizesList = eventsToNoteChangedList.stream()
                .map(x -> x.size())
                .collect(Collectors.toList());

        Collections.sort(trackSizesList);

        int sizeTrack = finalVocalEventsList.size();
        int sizeTrackMin = 0;
        int sizeTrackMax = 0;
        int sizeTrackFinal = 0;
        for (int i = 0; i < trackSizesList.size(); i++) {
            if(trackSizesList.get(i) <= sizeTrack)
                sizeTrackMin = trackSizesList.get(i);
        }
        for (int i = trackSizesList.size() - 1; i >= 0; i--) {
            if(trackSizesList.get(i) >= sizeTrack)
                sizeTrackMax = trackSizesList.get(i);
        }

        if (Math.abs(sizeTrackMax - trackSizesList.size()) >= Math.abs(sizeTrackMin - trackSizesList.size()))
            sizeTrackFinal = sizeTrackMin;
        else
            sizeTrackFinal = sizeTrackMax;

        List<Note> finalListNote = new ArrayList<>();
        for (List<Note> list: eventsToNoteChangedList) {
            if (list.size() == sizeTrackFinal)
                finalListNote = list;
        }

//        for (int numOfTracks = 1; numOfTracks < midiFile.getTracks().size(); numOfTracks++) {
//            List<Vocal> tempVocalEventsList = findVocalEventsFromTrack(midiFile.getTracks().get(numOfTracks).getEvents());
//            List<Note> instrumentEventsList = SongUtils.eventsToNotes(midiFile.getTracks().get(numOfTracks).getEvents());
//            if (!tempVocalEventsList.isEmpty()){
//                finalVocalEventsList.addAll(tempVocalEventsList);
//            }
//            if (!instrumentEventsList.isEmpty()){
//                allNotes.addAll(instrumentEventsList);
//            }
//        }

//        finalVocalEventsList.stream()
//                .forEachOrdered( vocalEvent ->
//                        uniqueNotes.addAll(
//                            (List) allNotes.stream()
//                            .filter(note -> (note.startTick() >= vocalEvent.getStartTick() && note.endTickInclusive() <= vocalEvent.getEndTick()))
//                            .collect(Collectors.toList())
//                        )
//                );

        for (Vocal vocalEvent: finalVocalEventsList) {
            for (Note note: allNotes) {
                if (note.startTick() >= vocalEvent.getStartTick() && note.startTick() <= vocalEvent.getEndTick()) {
                    uniqueNotes.add(note);
                }
            }
        }

        return finalListNote;
    }

    private static List<Vocal> findVocalEventsFromTrack(TreeSet<MidiEvent> midiEvents) {
        List<Vocal> resultList = new ArrayList<>();

        for (MidiEvent event: midiEvents) {
            if ((event instanceof Lyrics || event instanceof Text) && event.getTick() > 0) {
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

    private static List<Note> findNotesLyricsEventsFromTrack1(MidiTrack midiTrack) {
        List<Note> resultList = new ArrayList<>();
        List<Note> notes =  SongUtils.eventsToNotes(midiTrack.getEvents());

        for (MidiEvent event: midiTrack.getEvents()) {
            if ((event instanceof Lyrics || event instanceof Text) && event.getTick() > 0) {
                List<Note> findedLyricsNoteList = notes.stream()
                        .filter(note -> (note.startTick() >= event.getTick() && note.endTickInclusive() <= (event.getTick() + event.getDelta())))
                        .collect(Collectors.toList());
                resultList.addAll(findedLyricsNoteList);
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

//    private static List<Note> searchingUniquePlacesForVocal(Tempo last, MidiFile midiFile, List<Note> notesAll) {
//
//        logger.debug("Start function of searching places for vocal");
//        Long startPlayRequiredNote, finishPlayRequiredNote, startPlayNote;
//        List<Note> foundNotes = new ArrayList<>();
//
//        for (Note note: notesAll) {
//            long lastCheckedVariable = 0;
//            if (note.startTick() > lastCheckedVariable) {
//
//            }
//        }
//
//        for (int i = 0; i < notesAll.size(); i++) {
//            startPlayRequiredNote = notesAll.get(i).startTick();
//            finishPlayRequiredNote = notesAll.get(i).endTickInclusive();
//            int counterRepeatedNotes = 0;
//            for (int j = 0; j < notesAll.size(); j++) {
//                startPlayNote = notesAll.get(j).startTick();
//                if (startPlayRequiredNote <= startPlayNote && startPlayNote < finishPlayRequiredNote) {
//                    counterRepeatedNotes ++;
//                    if (counterRepeatedNotes > 1) {
//                        break;
//                    }
//                }
//            }
//            if (counterRepeatedNotes == 1) {
//                logger.debug("[" + i + "] Нота " + notesAll.get(i).sign().fullName() + " с длительностью " + SongUtils.tickToMs(last.getBpm(), midiFile.getResolution(), notesAll.get(i).durationTicks()) + "мс");
//                foundNotes.add(notesAll.get(i));
//            }
//        }
//
//        logger.debug("Finish function of searching places for vocal. Function returned {} elements in list", foundNotes.size());
//        return foundNotes;
//    }

}
