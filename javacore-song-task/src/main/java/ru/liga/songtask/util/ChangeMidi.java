package ru.liga.songtask.util;

import com.leff.midi.MidiFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.liga.App;
import java.io.File;
import java.io.IOException;

public class ChangeMidi {

    private static Logger logger = LoggerFactory.getLogger(App.class);

    public static void changeMidiFile(MidiFile midiFile, String[] args) throws IOException {

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
            MidiFile newMidi1 = TempoChanger.changeTempoMidiFile(midiFile, tempoPercent);
            MidiFile newMidi2 = TranspositionChanger.transpositionMidiFile(newMidi1, transVar);
            String pathNew = getSavePath(transVar, tempoVar, file);
            newMidi2.writeToFile(new File(pathNew));
            logger.info("Change pathname: {}", pathNew);
        }
    }


    private static String getSavePath(int trans, float tempo, File file) {
        String newName = file.getName().replace(".mid", "") + "-trans" + trans + "-tempo" + tempo + ".mid";
        logger.info("File saved succesful");
        return file.getParentFile().getAbsolutePath() + File.separator + newName;
    }
}
