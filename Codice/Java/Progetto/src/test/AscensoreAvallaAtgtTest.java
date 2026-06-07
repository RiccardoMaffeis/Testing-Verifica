package test;

import org.junit.jupiter.api.Test;

import progetto.Ascensore;
import progetto.ControlloreAscensore;
import progetto.Direzione;
import progetto.InputAscensore;
import progetto.StatoCabina;
import progetto.StatoErrore;
import progetto.StatoPorte;

import static org.junit.jupiter.api.Assertions.*;

public class AscensoreAvallaAtgtTest {

    @Test
    public void testtest0_avalla() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input0 = new InputAscensore();
        input0.setRichiestaInterna(4);
        input0.setChiamataSalita(3);
        input0.setChiamataDiscesa(0);
        input0.setEventoGuasto(true);

        controllore.eseguiPasso(input0);

        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(10, ascensore.getTimer());
        assertTrue(ascensore.richiestaAttiva(0));
        assertTrue(ascensore.richiestaAttiva(3));
        assertTrue(ascensore.richiestaAttiva(4));
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());

        for (int timerAtteso = 9; timerAtteso >= 1; timerAtteso--) {
            controllore.eseguiPasso(new InputAscensore());

            assertEquals(timerAtteso, ascensore.getTimer());
            assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
            assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
            assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
            assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        }
    }
    
    @Test
    public void testtest2_avalla() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        ascensore.aggiungiRichiestaInterna(0);
        ascensore.aggiungiRichiestaInterna(2);

        InputAscensore input0 = new InputAscensore();

        input0.setRichiestaInterna(4);
        input0.setChiamataSalita(3);
        input0.setChiamataDiscesa(1);
        input0.setEventoGuasto(true);

        controllore.eseguiPasso(input0);

        assertEquals(10, ascensore.getTimer());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());

        assertTrue(ascensore.richiestaAttiva(0));
        assertTrue(ascensore.richiestaAttiva(1));
        assertTrue(ascensore.richiestaAttiva(2));
        assertTrue(ascensore.richiestaAttiva(3));
        assertTrue(ascensore.richiestaAttiva(4));

        for (int timerAtteso = 9; timerAtteso >= 1; timerAtteso--) {
            controllore.eseguiPasso(new InputAscensore());

            assertEquals(timerAtteso, ascensore.getTimer());
            assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
            assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
            assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
            assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        }
    }
    
    @Test
    public void testtest4_avalla() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.aggiungiRichiestaInterna(1);
        ascensore.aggiungiRichiestaInterna(2);
        ascensore.aggiungiRichiestaInterna(3);
        ascensore.aggiungiRichiestaInterna(4);

        InputAscensore input0 = new InputAscensore();
        input0.setRichiestaInterna(0);

        controllore.eseguiPasso(input0);

        assertTrue(ascensore.richiestaAttiva(-1));
        assertFalse(ascensore.richiestaAttiva(0));
        assertTrue(ascensore.richiestaAttiva(1));
        assertTrue(ascensore.richiestaAttiva(2));
        assertTrue(ascensore.richiestaAttiva(3));
        assertTrue(ascensore.richiestaAttiva(4));

        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());

        InputAscensore input1 = new InputAscensore();
        input1.setPersoneEntrate(Ascensore.CAPACITA_MASSIMA + 1);
        input1.setChiamataSalita(0);

        controllore.eseguiPasso(input1);

        assertEquals(Ascensore.CAPACITA_MASSIMA + 1, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertTrue(ascensore.richiestaAttiva(0));

        InputAscensore input2 = new InputAscensore();
        input2.setPersoneEntrate(1);

        controllore.eseguiPasso(input2);

        assertEquals(Ascensore.CAPACITA_MASSIMA + 2, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());

        InputAscensore input3 = new InputAscensore();
        input3.setPersoneUscite(2);

        controllore.eseguiPasso(input3);

        assertEquals(Ascensore.CAPACITA_MASSIMA, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertFalse(ascensore.richiestaAttiva(0));

        InputAscensore input4 = new InputAscensore();
        input4.setPersoneEntrate(1);

        controllore.eseguiPasso(input4);

        assertEquals(Ascensore.CAPACITA_MASSIMA + 1, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }
    
    @Test
    public void testtest6_avalla() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.aggiungiRichiestaInterna(3);
        ascensore.aggiungiRichiestaInterna(4);

        InputAscensore input0 = new InputAscensore();
        input0.setRichiestaInterna(0);

        controllore.eseguiPasso(input0);

        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertFalse(ascensore.richiestaAttiva(0));
        assertTrue(ascensore.richiestaAttiva(3));
        assertTrue(ascensore.richiestaAttiva(4));
        assertTrue(ascensore.richiestaAttiva(-1));
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());

        ascensore.aggiungiRichiestaInterna(1);
        ascensore.aggiungiRichiestaInterna(2);

        InputAscensore input1 = new InputAscensore();
        input1.setRichiestaInterna(0);
        input1.setPersoneEntrate(Ascensore.CAPACITA_MASSIMA + 1);

        controllore.eseguiPasso(input1);

        assertTrue(ascensore.richiestaAttiva(0));
        assertTrue(ascensore.richiestaAttiva(1));
        assertTrue(ascensore.richiestaAttiva(2));
        assertEquals(Ascensore.CAPACITA_MASSIMA + 1, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(Ascensore.CAPACITA_MASSIMA + 1, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(Ascensore.CAPACITA_MASSIMA + 1, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());

        InputAscensore input4 = new InputAscensore();
        input4.setPersoneUscite(Ascensore.CAPACITA_MASSIMA + 1);

        controllore.eseguiPasso(input4);

        assertEquals(0, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertFalse(ascensore.richiestaAttiva(0));

        InputAscensore input5 = new InputAscensore();
        input5.setEventoGuasto(true);

        controllore.eseguiPasso(input5);

        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(Ascensore.TIMER_MASSIMO, ascensore.getTimer());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(0, ascensore.getNumeroPersone());

        for (int timerAtteso = 9; timerAtteso >= 6; timerAtteso--) {
            controllore.eseguiPasso(new InputAscensore());

            assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
            assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
            assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
            assertEquals(timerAtteso, ascensore.getTimer());
            assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
            assertEquals(0, ascensore.getNumeroPersone());
        }
    }
    
    @Test
    public void testtest8_avalla() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input0 = new InputAscensore();

        input0.setChiamataSalita(0);
        input0.setChiamataDiscesa(4);
        input0.setRichiestaInterna(2);
        input0.setEventoGuasto(true);

        controllore.eseguiPasso(input0);

        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Ascensore.TIMER_MASSIMO, ascensore.getTimer());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertTrue(ascensore.richiestaAttiva(2));
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertTrue(ascensore.richiestaAttiva(0));
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertTrue(ascensore.richiestaAttiva(4));

        for (int timerAtteso = 9; timerAtteso >= 1; timerAtteso--) {
            controllore.eseguiPasso(new InputAscensore());

            assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
            assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
            assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
            assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
            assertEquals(timerAtteso, ascensore.getTimer());
        }
    }
}