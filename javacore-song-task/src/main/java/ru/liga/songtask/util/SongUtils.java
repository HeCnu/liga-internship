package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import com.leff.midi.MidiTrack;
import com.leff.midi.event.meta.Tempo;

public class SongUtils {

    /**
     * Перевод тиков в миллисекунды
     * @param bpm - количество ударов в минуту (темп)
     * @param resolution - midiFile.getResolution()
     * @param amountOfTick - то что переводим в миллисекунды
     * @return
     */
    public static int tickToMs(float bpm, int resolution, long amountOfTick) {
        return (int) (((60 * 1000) / (bpm * resolution)) * amountOfTick);
    }

    public static Tempo getTempo(MidiFile midiFile) {
        Tempo tempo = (Tempo)((MidiTrack)midiFile.getTracks().get(0)).getEvents().stream().filter((value) -> {
            return value instanceof Tempo;
        }).findFirst().get();
        return tempo;
    }

}
