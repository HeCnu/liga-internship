package ru.liga;

import com.leff.midi.MidiFile;
import com.leff.midi.event.meta.Tempo;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.songtask.util.AnalyzeMidi;
import ru.liga.songtask.util.ChangeMidi;

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
        ChangeMidi changeMidi = new ChangeMidi();
        AnalyzeMidi analyzeMidi = new AnalyzeMidi();

        switch (args[1]) {
            case "analyze":
                logger.debug("Start analyze");
                analyzeMidi.analizeMidiFile(midiFile, last);
                logger.debug("Finish analyze");
                break;
            case "change":
                logger.debug("Start changing");
                changeMidi.changeMidiFile(midiFile, last, args);
                logger.debug("Finish changing");
                break;
            default:
                logger.debug("No input action!");
                break;
        }
    }
}
