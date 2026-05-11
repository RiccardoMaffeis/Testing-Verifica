package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import progetto.Ascensore;
import progetto.ControlloreAscensore;
import progetto.Direzione;
import progetto.InputAscensore;
import progetto.StatoCabina;
import progetto.StatoErrore;
import progetto.StatoPorte;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AscensoreParametricTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4})
    public void pianoValidoAccettaSoloPianiNelRange(int piano) {
        Ascensore ascensore = new Ascensore();

        assertTrue(ascensore.pianoValido(piano));
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, -2, 5, 6, 10})
    public void pianoValidoRifiutaPianiFuoriRange(int piano) {
        Ascensore ascensore = new Ascensore();

        assertFalse(ascensore.pianoValido(piano));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2, 3, 4})
    public void richiestaInternaValidaVieneGestita(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(piano);

        controllore.eseguiPasso(input);

        if (piano == 0) {
            assertFalse(ascensore.richiestaAttiva(piano));
            assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
            assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        } else {
            assertTrue(ascensore.richiestaAttiva(piano));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -2, 5, 8})
    public void richiestaInternaNonValidaVieneIgnorata(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(piano);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 2, 3})
    public void chiamataSalitaValidaVieneGestita(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(piano);

        controllore.eseguiPasso(input);

        if (piano == 0) {
            assertFalse(ascensore.richiestaAttiva(piano));
            assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
            assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        } else {
            assertTrue(ascensore.richiestaAttiva(piano));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -2, 4, 5, 8})
    public void chiamataSalitaNonValidaVieneIgnorata(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(piano);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    public void chiamataDiscesaValidaVieneGestita(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(piano);

        controllore.eseguiPasso(input);

        if (piano == 0) {
            assertFalse(ascensore.richiestaAttiva(piano));
            assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
            assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        } else {
            assertTrue(ascensore.richiestaAttiva(piano));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -2, -1, 5, 8})
    public void chiamataDiscesaNonValidaVieneIgnorata(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(piano);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "1, 0, 1",
            "2, 0, 2",
            "2, 1, 1",
            "2, 2, 0",
            "2, 5, 0",
            "5, 2, 3",
            "8, 0, 8",
            "9, 0, 9"
    })
    public void aggiornaPersoneCalcolaNumeroPersoneSenzaAndareSottoZero(
            int personeEntrate,
            int personeUscite,
            int personeAttese
    ) {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(0);
        ascensore.serviPianoCorrente();

        ascensore.aggiornaPersone(personeEntrate, personeUscite);

        assertEquals(personeAttese, ascensore.getNumeroPersone());
    }

    @ParameterizedTest
    @CsvSource({
            "0, NESSUNO",
            "1, NESSUNO",
            "7, NESSUNO",
            "8, NESSUNO",
            "9, OVERLOAD",
            "10, OVERLOAD"
    })
    public void gestisciSovraccaricoRispettaLaCapacitaMassima(
            int personeEntrate,
            StatoErrore statoAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(0);
        ascensore.serviPianoCorrente();
        ascensore.aggiornaPersone(personeEntrate, 0);

        ascensore.gestisciSovraccarico();

        assertEquals(statoAtteso, ascensore.getStatoErrore());

        if (statoAtteso == StatoErrore.OVERLOAD) {
            assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
            assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
            assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    public void timerGuastoVieneDecrementatoDiUnPasso(int decrementi) {
        Ascensore ascensore = new Ascensore();

        ascensore.attivaGuasto();

        for (int i = 0; i < decrementi; i++) {
            ascensore.gestisciTimerGuasto();
        }

        assertEquals(Ascensore.TIMER_MASSIMO - decrementi, ascensore.getTimer());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
    }

    @ParameterizedTest
    @CsvSource({
            "1, SU",
            "2, SU",
            "3, SU",
            "4, SU",
            "-1, GIU"
    })
    public void scegliDirezioneInBaseAllaPosizioneDellaRichiesta(
            int pianoRichiesto,
            Direzione direzioneAttesa
    ) {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(pianoRichiesto);
        ascensore.scegliDirezione();

        assertEquals(direzioneAttesa, ascensore.getDirezione());
    }

    @ParameterizedTest
    @CsvSource({
            "SU, 1",
            "GIU, -1"
    })
    public void movimentoSegueLaDirezioneScelta(
            Direzione direzione,
            int pianoAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        if (direzione == Direzione.SU) {
            ascensore.aggiungiRichiestaInterna(1);
        } else {
            ascensore.aggiungiRichiestaInterna(-1);
        }

        ascensore.scegliDirezione();
        ascensore.muoviDiUnPiano();

        assertEquals(pianoAtteso, ascensore.getPianoCorrente());
        assertEquals(StatoCabina.IN_MOVIMENTO, ascensore.getStatoCabina());
    }
}