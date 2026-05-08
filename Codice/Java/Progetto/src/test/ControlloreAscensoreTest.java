package test;

import progetto.Ascensore;
import progetto.ControlloreAscensore;
import progetto.InputAscensore;
import progetto.Direzione;
import progetto.StatoCabina;
import progetto.StatoErrore;
import progetto.StatoPorte;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControlloreAscensoreTest {

    // Metodi di supporto

    private void eseguiPassiVuoti(ControlloreAscensore controllore, int numeroPassi) {
        for (int i = 0; i < numeroPassi; i++) {
            controllore.eseguiPasso(new InputAscensore());
        }
    }

    private void apriPorteAlPianoCorrente(Ascensore ascensore, ControlloreAscensore controllore) {
        ascensore.aggiungiRichiestaInterna(ascensore.getPianoCorrente());
        controllore.eseguiPasso(new InputAscensore());

        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
    }

    private void portaAscensoreAlPianoTreConPorteChiuse(
            Ascensore ascensore,
            ControlloreAscensore controllore
    ) {
        InputAscensore richiesta = new InputAscensore();
        richiesta.setRichiestaInterna(3);

        controllore.eseguiPasso(richiesta);
        eseguiPassiVuoti(controllore, 4);

        assertEquals(3, ascensore.getPianoCorrente());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
    }

    private void portaAscensoreInOverload(
            Ascensore ascensore,
            ControlloreAscensore controllore
    ) {
        apriPorteAlPianoCorrente(ascensore, controllore);

        InputAscensore ingresso = new InputAscensore();
        ingresso.setPersoneEntrate(9);

        controllore.eseguiPasso(ingresso);

        assertEquals(9, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    private void attivaGuasto(ControlloreAscensore controllore) {
        InputAscensore guasto = new InputAscensore();
        guasto.setEventoGuasto(true);

        controllore.eseguiPasso(guasto);
    }

    // Stato iniziale, costruttore e input

    @Test
    public void statoInizialeAscensore() {
        Ascensore ascensore = new Ascensore();

        assertEquals(0, ascensore.getPianoCorrente());
        assertEquals(0, ascensore.getTimer());
        assertEquals(0, ascensore.getNumeroPersone());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    @Test
    public void costruttoreControlloreRifiutaAscensoreNull() {
        IllegalArgumentException eccezione = assertThrows(
                IllegalArgumentException.class,
                () -> new ControlloreAscensore(null)
        );

        assertEquals("Ascensore non puo essere null", eccezione.getMessage());
    }

    @Test
    public void eseguiPassoRifiutaInputNull() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        IllegalArgumentException eccezione = assertThrows(
                IllegalArgumentException.class,
                () -> controllore.eseguiPasso(null)
        );

        assertEquals("InputAscensore non puo essere null", eccezione.getMessage());
    }

    @Test
    public void getAscensoreRestituisceAscensoreAssociato() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        assertSame(ascensore, controllore.getAscensore());
    }

    @Test
    public void inputAscensoreSenzaValoriNonHaRichieste() {
        InputAscensore input = new InputAscensore();

        assertFalse(input.haRichiestaInterna());
        assertFalse(input.haChiamataSalita());
        assertFalse(input.haChiamataDiscesa());
        assertFalse(input.isEventoGuasto());
        assertEquals(0, input.getPersoneEntrate());
        assertEquals(0, input.getPersoneUscite());
    }

    @Test
    public void inputAscensoreConValoriHaRichieste() {
        InputAscensore input = new InputAscensore();

        input.setRichiestaInterna(2);
        input.setChiamataSalita(1);
        input.setChiamataDiscesa(3);
        input.setEventoGuasto(true);
        input.setPersoneEntrate(4);
        input.setPersoneUscite(1);

        assertTrue(input.haRichiestaInterna());
        assertTrue(input.haChiamataSalita());
        assertTrue(input.haChiamataDiscesa());
        assertTrue(input.isEventoGuasto());
        assertEquals(2, input.getRichiestaInterna());
        assertEquals(1, input.getChiamataSalita());
        assertEquals(3, input.getChiamataDiscesa());
        assertEquals(4, input.getPersoneEntrate());
        assertEquals(1, input.getPersoneUscite());
    }

    @Test
    public void pianoValidoRiconosceLimiti() {
        Ascensore ascensore = new Ascensore();

        assertTrue(ascensore.pianoValido(Ascensore.PIANO_MINIMO));
        assertTrue(ascensore.pianoValido(0));
        assertTrue(ascensore.pianoValido(Ascensore.PIANO_MASSIMO));

        assertFalse(ascensore.pianoValido(Ascensore.PIANO_MINIMO - 1));
        assertFalse(ascensore.pianoValido(Ascensore.PIANO_MASSIMO + 1));
    }

    // Comportamento idle

    @Test
    public void idleSenzaRichieste() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(0, ascensore.getPianoCorrente());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    // Acquisizione richieste

    @Test
    public void richiestaInternaVersoPianoSuperiore() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(3);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(3));
        assertEquals(1, ascensore.getPianoCorrente());
        assertEquals(Direzione.SU, ascensore.getDirezione());
        assertEquals(StatoCabina.IN_MOVIMENTO, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
    }

    @Test
    public void piuRichiesteNelloStessoPassoVengonoAcquisite() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(2);
        input.setChiamataSalita(1);
        input.setChiamataDiscesa(3);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(1));
        assertTrue(ascensore.richiestaAttiva(2));
        assertTrue(ascensore.richiestaAttiva(3));
    }

    @Test
    public void richiestaInternaNonValidaVieneIgnorataDalControllore() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setRichiestaInterna(Ascensore.PIANO_MASSIMO + 1);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void chiamataSalitaValidaVieneAcquisita() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(1);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(1));
        assertEquals(Direzione.SU, ascensore.getDirezione());
        assertEquals(1, ascensore.getPianoCorrente());
    }

    @Test
    public void chiamataSalitaAlPianoMinimoVieneAcquisita() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(Ascensore.PIANO_MINIMO);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(Ascensore.PIANO_MINIMO));
        assertEquals(Direzione.GIU, ascensore.getDirezione());
        assertEquals(-1, ascensore.getPianoCorrente());
    }

    @Test
    public void chiamataSalitaAlPianoMassimoVieneIgnorata() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(Ascensore.PIANO_MASSIMO);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.richiestaAttiva(Ascensore.PIANO_MASSIMO));
        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
    }

    @Test
    public void chiamataSalitaNonValidaVieneIgnorata() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataSalita(Ascensore.PIANO_MASSIMO + 1);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void chiamataDiscesaValidaVieneAcquisita() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(2);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(2));
        assertEquals(Direzione.SU, ascensore.getDirezione());
        assertEquals(1, ascensore.getPianoCorrente());
    }

    @Test
    public void chiamataDiscesaAlPianoMassimoVieneAcquisita() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(Ascensore.PIANO_MASSIMO);

        controllore.eseguiPasso(input);

        assertTrue(ascensore.richiestaAttiva(Ascensore.PIANO_MASSIMO));
        assertEquals(Direzione.SU, ascensore.getDirezione());
        assertEquals(1, ascensore.getPianoCorrente());
    }

    @Test
    public void chiamataDiscesaAlPianoMinimoVieneIgnorata() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(Ascensore.PIANO_MINIMO);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.richiestaAttiva(Ascensore.PIANO_MINIMO));
        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
    }

    @Test
    public void chiamataDiscesaNonValidaVieneIgnorata() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore input = new InputAscensore();
        input.setChiamataDiscesa(Ascensore.PIANO_MINIMO - 1);

        controllore.eseguiPasso(input);

        assertFalse(ascensore.esisteRichiesta());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    // Movimento, servizio piano e porte

    @Test
    public void ascensoreServePianoSuperioreRichiesto() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore richiesta = new InputAscensore();
        richiesta.setRichiestaInterna(3);

        controllore.eseguiPasso(richiesta);
        eseguiPassiVuoti(controllore, 3);

        assertEquals(3, ascensore.getPianoCorrente());
        assertFalse(ascensore.richiestaAttiva(3));
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void chiudePorteDopoAverServitoUnPiano() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore richiesta = new InputAscensore();
        richiesta.setRichiestaInterna(1);

        controllore.eseguiPasso(richiesta);
        controllore.eseguiPasso(new InputAscensore());

        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
    }

    @Test
    public void richiestaVersoPianoInferiore() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        portaAscensoreAlPianoTreConPorteChiuse(ascensore, controllore);

        InputAscensore richiestaGiu = new InputAscensore();
        richiestaGiu.setChiamataDiscesa(0);

        controllore.eseguiPasso(richiestaGiu);

        assertTrue(ascensore.richiestaAttiva(0));
        assertEquals(2, ascensore.getPianoCorrente());
        assertEquals(Direzione.GIU, ascensore.getDirezione());
        assertEquals(StatoCabina.IN_MOVIMENTO, ascensore.getStatoCabina());
    }

    @Test
    public void richiestaSoloAlPianoCorrenteNonProduceMovimento() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        ascensore.aggiungiRichiestaInterna(0);

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(0, ascensore.getPianoCorrente());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertFalse(ascensore.richiestaAttiva(0));
    }

    @Test
    public void movimentoAlPianoMassimoPoiSiFerma() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(4);
        ascensore.scegliDirezione();

        eseguiMovimentiDiretti(ascensore, 4);

        assertEquals(4, ascensore.getPianoCorrente());
        assertEquals(Direzione.SU, ascensore.getDirezione());

        ascensore.muoviDiUnPiano();

        assertEquals(4, ascensore.getPianoCorrente());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void movimentoAlPianoMinimoPoiSiFerma() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.scegliDirezione();

        ascensore.muoviDiUnPiano();

        assertEquals(-1, ascensore.getPianoCorrente());
        assertEquals(Direzione.GIU, ascensore.getDirezione());

        ascensore.muoviDiUnPiano();

        assertEquals(-1, ascensore.getPianoCorrente());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    private void eseguiMovimentiDiretti(Ascensore ascensore, int numeroMovimenti) {
        for (int i = 0; i < numeroMovimenti; i++) {
            ascensore.muoviDiUnPiano();
        }
    }

    // Gestione persone e sovraccarico

    @Test
    public void gestisciPersoneConPorteChiuseNonAggiornaNumeroPersone() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());

        InputAscensore input = new InputAscensore();
        input.setPersoneEntrate(3);

        controllore.eseguiPasso(input);

        assertEquals(0, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    @Test
    public void gestisciPersoneConValoriNegativiIgnoraAggiornamento() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        apriPorteAlPianoCorrente(ascensore, controllore);

        InputAscensore personeEntrateNegative = new InputAscensore();
        personeEntrateNegative.setPersoneEntrate(-3);
        personeEntrateNegative.setPersoneUscite(0);

        controllore.eseguiPasso(personeEntrateNegative);

        assertEquals(0, ascensore.getNumeroPersone());

        InputAscensore personeUsciteNegative = new InputAscensore();
        personeUsciteNegative.setPersoneEntrate(1);
        personeUsciteNegative.setPersoneUscite(-1);

        controllore.eseguiPasso(personeUsciteNegative);

        assertEquals(0, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    @Test
    public void gestisciPersoneConPorteAperteAggiornaNumeroPersone() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        apriPorteAlPianoCorrente(ascensore, controllore);

        InputAscensore input = new InputAscensore();
        input.setPersoneEntrate(2);

        controllore.eseguiPasso(input);

        assertEquals(2, ascensore.getNumeroPersone());
    }

    @Test
    public void aggiornaPersoneNonPermetteNumeroNegativo() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(0);
        ascensore.serviPianoCorrente();

        ascensore.aggiornaPersone(2, 5);

        assertEquals(0, ascensore.getNumeroPersone());
    }

    @Test
    public void entraInOverload() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        portaAscensoreInOverload(ascensore, controllore);
    }

    @Test
    public void risolveOverloadQuandoUnaPersonaEsce() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        portaAscensoreInOverload(ascensore, controllore);

        InputAscensore uscita = new InputAscensore();
        uscita.setPersoneUscite(1);

        controllore.eseguiPasso(uscita);

        assertEquals(8, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void richiesteDuranteOverloadVengonoAcquisite() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        portaAscensoreInOverload(ascensore, controllore);

        InputAscensore richiesta = new InputAscensore();
        richiesta.setRichiestaInterna(2);

        controllore.eseguiPasso(richiesta);

        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertTrue(ascensore.richiestaAttiva(2));
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
    }

    @Test
    public void eventoGuastoDuranteOverloadNonAttivaGuasto() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        portaAscensoreInOverload(ascensore, controllore);

        InputAscensore guastoDuranteOverload = new InputAscensore();
        guastoDuranteOverload.setEventoGuasto(true);

        controllore.eseguiPasso(guastoDuranteOverload);

        assertEquals(StatoErrore.OVERLOAD, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());
        assertEquals(0, ascensore.getTimer());
    }

    @Test
    public void gestisciSovraccaricoSenzaOverloadNonCambiaStato() {
        Ascensore ascensore = new Ascensore();

        ascensore.gestisciSovraccarico();

        assertEquals(0, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(0, ascensore.getTimer());
    }
    
    @Test
    public void gestisciPersoneIgnoraPersoneUsciteNegativeConPorteAperte() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        apriPorteAlPianoCorrente(ascensore, controllore);

        assertEquals(StatoPorte.APERTE, ascensore.getStatoPorte());

        InputAscensore input = new InputAscensore();
        input.setPersoneEntrate(1);
        input.setPersoneUscite(-1);

        controllore.eseguiPasso(input);

        assertEquals(0, ascensore.getNumeroPersone());
        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
    }

    // Guasto e ripristino

    @Test
    public void attivaGuastoTecnico() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        attivaGuasto(controllore);

        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.BLOCCATA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(Ascensore.TIMER_MASSIMO, ascensore.getTimer());
    }

    @Test
    public void nonAcquisisceNuoveRichiesteDuranteGuasto() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        attivaGuasto(controllore);

        InputAscensore richiestaDuranteGuasto = new InputAscensore();
        richiestaDuranteGuasto.setRichiestaInterna(4);

        controllore.eseguiPasso(richiestaDuranteGuasto);

        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertFalse(ascensore.richiestaAttiva(4));
    }

    @Test
    public void conservaRichiesteGiaAcquisiteDuranteGuasto() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        InputAscensore richiesta = new InputAscensore();
        richiesta.setRichiestaInterna(3);

        controllore.eseguiPasso(richiesta);

        assertTrue(ascensore.richiestaAttiva(3));

        attivaGuasto(controllore);

        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertTrue(ascensore.richiestaAttiva(3));
    }

    @Test
    public void decrementaTimerDuranteGuasto() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        attivaGuasto(controllore);

        assertEquals(10, ascensore.getTimer());

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());
        assertEquals(9, ascensore.getTimer());
    }

    @Test
    public void ripristinaDopoGuasto() {
        Ascensore ascensore = new Ascensore();
        ControlloreAscensore controllore = new ControlloreAscensore(ascensore);

        attivaGuasto(controllore);

        eseguiPassiVuoti(controllore, Ascensore.TIMER_MASSIMO);

        assertEquals(0, ascensore.getTimer());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());

        controllore.eseguiPasso(new InputAscensore());

        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
        assertEquals(0, ascensore.getTimer());
    }

    @Test
    public void gestisciTimerGuastoRipristinaQuandoTimerZero() {
        Ascensore ascensore = new Ascensore();

        ascensore.attivaGuasto();

        for (int i = 0; i < Ascensore.TIMER_MASSIMO; i++) {
            ascensore.gestisciTimerGuasto();
        }

        assertEquals(0, ascensore.getTimer());
        assertEquals(StatoErrore.GUASTO, ascensore.getStatoErrore());

        ascensore.gestisciTimerGuasto();

        assertEquals(StatoErrore.NESSUNO, ascensore.getStatoErrore());
        assertEquals(StatoCabina.FERMA, ascensore.getStatoCabina());
        assertEquals(StatoPorte.CHIUSE, ascensore.getStatoPorte());
        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    // Ricerca richieste e scelta direzione

    @Test
    public void esisteRichiestaSopraESottoFunzionano() {
        Ascensore ascensore = new Ascensore();

        assertFalse(ascensore.esisteRichiesta());
        assertFalse(ascensore.esisteRichiestaSopra());
        assertFalse(ascensore.esisteRichiestaSotto());

        ascensore.aggiungiRichiestaInterna(3);

        assertTrue(ascensore.esisteRichiesta());
        assertTrue(ascensore.esisteRichiestaSopra());
        assertFalse(ascensore.esisteRichiestaSotto());

        ascensore.aggiungiRichiestaInterna(-1);

        assertTrue(ascensore.esisteRichiestaSotto());
    }

    @Test
    public void scegliDirezioneSenzaRichiesteImpostaNessuna() {
        Ascensore ascensore = new Ascensore();

        ascensore.scegliDirezione();

        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneConRichiestaAlPianoCorrenteRimaneNessuna() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(0);

        assertTrue(ascensore.richiestaAttiva(0));

        ascensore.scegliDirezione();

        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneDaNessunaSceglieSuSeRichiestaSopra() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(2);
        ascensore.scegliDirezione();

        assertEquals(Direzione.SU, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneDaNessunaSceglieGiuSeRichiestaSotto() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.scegliDirezione();

        assertEquals(Direzione.GIU, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneMantieneSuSeCiSonoRichiesteSopra() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(3);
        ascensore.scegliDirezione();

        assertEquals(Direzione.SU, ascensore.getDirezione());

        ascensore.aggiungiRichiestaInterna(4);
        ascensore.scegliDirezione();

        assertEquals(Direzione.SU, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneMantieneGiuSeCiSonoRichiesteSotto() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.scegliDirezione();

        assertEquals(Direzione.GIU, ascensore.getDirezione());

        ascensore.scegliDirezione();

        assertEquals(Direzione.GIU, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneInverteDaSuAGiuSeNonCiSonoRichiesteSopra() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(3);
        ascensore.scegliDirezione();

        eseguiMovimentiDiretti(ascensore, 3);

        assertEquals(3, ascensore.getPianoCorrente());
        assertEquals(Direzione.SU, ascensore.getDirezione());

        ascensore.aggiungiRichiestaInterna(0);
        ascensore.scegliDirezione();

        assertEquals(Direzione.GIU, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneInverteDaGiuASuSeNonCiSonoRichiesteSotto() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.scegliDirezione();

        ascensore.muoviDiUnPiano();

        assertEquals(-1, ascensore.getPianoCorrente());
        assertEquals(Direzione.GIU, ascensore.getDirezione());

        ascensore.aggiungiRichiestaInterna(3);
        ascensore.scegliDirezione();

        assertEquals(Direzione.SU, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneDaSuDiventaNessunaSeRestaSoloRichiestaAlPianoCorrente() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(3);
        ascensore.scegliDirezione();

        eseguiMovimentiDiretti(ascensore, 3);

        assertEquals(3, ascensore.getPianoCorrente());
        assertEquals(Direzione.SU, ascensore.getDirezione());
        assertTrue(ascensore.richiestaAttiva(3));

        ascensore.scegliDirezione();

        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }

    @Test
    public void scegliDirezioneDaGiuDiventaNessunaSeRestaSoloRichiestaAlPianoCorrente() {
        Ascensore ascensore = new Ascensore();

        ascensore.aggiungiRichiestaInterna(-1);
        ascensore.scegliDirezione();

        ascensore.muoviDiUnPiano();

        assertEquals(-1, ascensore.getPianoCorrente());
        assertEquals(Direzione.GIU, ascensore.getDirezione());
        assertTrue(ascensore.richiestaAttiva(-1));

        ascensore.scegliDirezione();

        assertEquals(Direzione.NESSUNA, ascensore.getDirezione());
    }
}