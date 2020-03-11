package ru.liga;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AppTest
{

    public static App app;
    @Before
    public void setup()
    {
        app = new App();
    }

    @Test
    public void consoleArgs_WhenArgsListIsEmpty() throws IOException {
        System.out.println("main");
        String[] args = null;
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> app.main(args));
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmpty_Belle() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Belle.mid", "analyze"};
        app.main(args);
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmpty_UnderneathYourClothes() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Underneath Your Clothes.mid", "analyze"};
        app.main(args);
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmpty_WreckingBall() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Wrecking Ball.mid", "analyze"};
        app.main(args);
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmptyWithTrans() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Wrecking Ball.mid", "change", "-trans", "2"};
        app.main(args);
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmptyWithTransWithoutNumber() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Wrecking Ball.mid", "change", "-trans"};
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> app.main(args));
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmptyWithTransAndTempo() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Wrecking Ball.mid", "change", "-trans", "2", "-tempo", "20"};
        app.main(args);
    }

    @Test
    public void consoleArgs_WhenArgsListNotEmptyWithTransAndTempoWithoutNumber() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Wrecking Ball.mid", "change", "-trans", "2", "-tempo"};
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> app.main(args));
    }


    @Test
    public void consoleArgs_WhenArgsListNotEmptyWithMinusTransAndTempo() throws IOException {
        System.out.println("main");
        String[] args = {"E:\\Wrecking Ball.mid", "change", "-trans", "-2", "-tempo", "-20"};
        app.main(args);
    }

}
