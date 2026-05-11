package test;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AscensoreParametricTest {

    @ParameterizedTest
    @CsvSource({
            "-1, true",
            "0, true",
            "1, true",
            "2, true",
            "3, true",
            "4, true",
            "-2, false",
            "5, false"
    })
    public void pianoValidoRiconoscePianiValidiENonValidi(
            int piano,
            boolean risultatoAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        assertEquals(risultatoAtteso, ascensore.pianoValido(piano));
    }

    @ParameterizedTest
    @CsvSource({
            "-1",
            "0",
            "1",
            "2",
            "3",
            "4"
    })
    public void richiestaInternaValidaVieneSempreAcquisita(int piano) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(piano);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(piano));
    }

    @ParameterizedTest
    @ValueSource(ints = {-3, -2, 5, 6, 10})
    public void richiestaInternaNonValidaVieneIgnorata(int pianoNonValido) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(pianoNonValido);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    @ParameterizedTest
    @CsvSource({
            "-1, true",
            "0, true",
            "1, true",
            "2, true",
            "3, true",
            "4, false",
            "-2, false",
            "5, false"
    })
    public void chiamataSalitaVieneAccettataSoloSeValida(
            int piano,
            boolean deveEssereAcquisita
    ) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(piano);

        controllore.eseguiPasso(input);

        if (deveEssereAcquisita) {
            assertTrue(ascensore.richiestaAttiva(piano));
        } else {
            assertFalse(ascensore.esisteRichiesta());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "-1, false",
            "0, true",
            "1, true",
            "2, true",
            "3, true",
            "4, true",
            "-2, false",
            "5, false"
    })
    public void chiamataDiscesaVieneAccettataSoloSeValida(
            int piano,
            boolean deveEssereAcquisita
    ) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(piano);

        controllore.eseguiPasso(input);

        if (deveEssereAcquisita) {
            assertTrue(ascensore.richiestaAttiva(piano));
        } else {
            assertFalse(ascensore.esisteRichiesta());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "0, 0, 0",
            "1, 0, 1",
            "3, 1, 2",
            "2, 5, 0",
            "8, 0, 8",
            "9, 0, 9"
    })
    public void aggiornaPersoneCalcolaNumeroPersoneCorrettamente(
            int personeEntrate,
            int personeUscite,
            int numeroAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        apriPorteAlPianoCorrente(ascensore);

        ascensore.aggiornaPersone(personeEntrate, personeUscite);

        assertEquals(numeroAtteso, ascensore.getNumeroPersone());
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
    public void gestisciSovraccaricoRiconosceIlSuperamentoDellaCapacita(
            int numeroPersone,
            StatoErrore statoErroreAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        apriPorteAlPianoCorrente(ascensore);
        ascensore.aggiornaPersone(numeroPersone, 0);

        ascensore.gestisciSovraccarico();

        assertEquals(statoErroreAtteso, ascensore.getStatoErrore());

        if (statoErroreAtteso == StatoErrore.OVERLOAD) {
            assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
            assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
            assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "1, NESSUNO",
            "2, NESSUNO",
            "3, NESSUNO",
            "4, NESSUNO",
            "5, NESSUNO",
            "6, NESSUNO",
            "7, NESSUNO",
            "8, NESSUNO",
            "9, OVERLOAD"
    })
    public void ingressoPersoneDaControlloreGestisceSogliaOverload(
            int personeEntrate,
            StatoErrore statoErroreAtteso
    ) {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        apriPorteAlPianoCorrente(ascensore);

        InputAscensore input = new InputAscensore();
        input.setPersoneEntrate(personeEntrate);

        controllore.eseguiPasso(input);

        assertEquals(personeEntrate, ascensore.getNumeroPersone());
        assertEquals(statoErroreAtteso, ascensore.getStatoErrore());
    }

    @ParameterizedTest
    @CsvSource({
            "1, 9",
            "2, 8",
            "3, 7",
            "5, 5",
            "10, 0"
    })
    public void timerGuastoDecrementaCorrettamente(
            int passiDaEseguire,
            int timerAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        ascensore.attivaGuasto();

        for (int i = 0; i < passiDaEseguire; i++) {
            ascensore.gestisciTimerGuasto();
        }

        assertEquals(timerAtteso, ascensore.getTimer());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1, 2, 3, 4})
    public void scegliDirezioneDaPianoZeroSceglieDirezioneCorretta(int pianoRichiesto) {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(pianoRichiesto);
        ascensore.scegliDirezione();

        if (pianoRichiesto > 0) {
            assertEquals(Direzione.SU, ascensore.getDirezione());
        } else {
            assertEquals(Direzione.GIU, ascensore.getDirezione());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "SU, 1, IN_MOVIMENTO",
            "GIU, -1, IN_MOVIMENTO"
    })
    public void muoviDiUnPianoRispettaLaDirezione(
            Direzione direzioneAttesa,
            int pianoAtteso,
            StatoCabina statoCabinaAtteso
    ) {
        Ascensore ascensore = new Ascensore();

        if (direzioneAttesa == Direzione.SU) {
            ascensore.aggiungiRichiestaInterna(4);
        } else {
            ascensore.aggiungiRichiestaInterna(-1);
        }

        ascensore.scegliDirezione();
        ascensore.muoviDiUnPiano();

        assertEquals(direzioneAttesa, ascensore.getDirezione());
        assertEquals(pianoAtteso, ascensore.getPianoCorrente());
        assertEquals(statoCabinaAtteso, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    private void apriPorteAlPianoCorrente(Ascensore ascensore) {
        ascensore.aggiungiRichiestaInterna(ascensore.getPianoCorrente());
        ascensore.serviPianoCorrente();

        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
    }
}