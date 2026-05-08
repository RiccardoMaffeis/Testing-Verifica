package progetto;

import org.jmlspecs.annotation.CodeBigintMath;

public class Ascensore {

    public static final int PIANO_MINIMO = -1;
    public static final int PIANO_MASSIMO = 4;
    public static final int NUMERO_PIANI = 6;
    public static final int CAPACITA_MASSIMA = 8;
    public static final int TIMER_MASSIMO = 10;

    private /*@ spec_public @*/ int pianoCorrente;
    private /*@ spec_public @*/ int timer;
    private /*@ spec_public @*/ int numeroPersone;

    private /*@ spec_public @*/ StatoCabina statoCabina;
    private /*@ spec_public @*/ StatoPorte statoPorte;
    private /*@ spec_public @*/ Direzione direzione;
    private /*@ spec_public @*/ StatoErrore statoErrore;

    private /*@ spec_public @*/ final boolean[] richiesteAttive;

    /*@
      @ public invariant PIANO_MINIMO <= pianoCorrente && pianoCorrente <= PIANO_MASSIMO;
      @ public invariant 0 <= timer && timer <= TIMER_MASSIMO;
      @ public invariant numeroPersone >= 0;
      @
      @ public invariant statoCabina != null;
      @ public invariant statoPorte != null;
      @ public invariant direzione != null;
      @ public invariant statoErrore != null;
      @
      @ public invariant richiesteAttive != null;
      @ public invariant richiesteAttive.length == NUMERO_PIANI;
      @
      @ public invariant statoCabina == StatoCabina.IN_MOVIMENTO ==> statoPorte == StatoPorte.CHIUSE;
      @ public invariant statoCabina == StatoCabina.IN_MOVIMENTO ==> statoErrore == StatoErrore.NESSUNO;
      @
      @ public invariant statoErrore == StatoErrore.GUASTO ==> statoCabina == StatoCabina.BLOCCATA;
      @ public invariant statoErrore == StatoErrore.GUASTO ==> statoPorte == StatoPorte.CHIUSE;
      @ public invariant statoErrore == StatoErrore.GUASTO ==> direzione == Direzione.NESSUNA;
      @
      @ public invariant statoErrore == StatoErrore.OVERLOAD ==> statoCabina == StatoCabina.BLOCCATA;
      @ public invariant statoErrore == StatoErrore.OVERLOAD ==> statoPorte == StatoPorte.APERTE;
      @ public invariant statoErrore == StatoErrore.OVERLOAD ==> direzione == Direzione.NESSUNA;
      @
      @ public invariant timer > 0 ==> statoErrore == StatoErrore.GUASTO;
      @*/

    //@ ensures pianoCorrente == 0;
    //@ ensures timer == 0;
    //@ ensures numeroPersone == 0;
    //@ ensures statoCabina == StatoCabina.FERMA;
    //@ ensures statoPorte == StatoPorte.CHIUSE;
    //@ ensures direzione == Direzione.NESSUNA;
    //@ ensures statoErrore == StatoErrore.NESSUNO;
    //@ ensures richiesteAttive != null;
    //@ ensures richiesteAttive.length == NUMERO_PIANI;
    public Ascensore() {
        this.pianoCorrente = 0;
        this.timer = 0;
        this.numeroPersone = 0;
        this.statoCabina = StatoCabina.FERMA;
        this.statoPorte = StatoPorte.CHIUSE;
        this.direzione = Direzione.NESSUNA;
        this.statoErrore = StatoErrore.NESSUNO;
        this.richiesteAttive = new boolean[NUMERO_PIANI];
    }

    //@ ensures \result <==> (PIANO_MINIMO <= piano && piano <= PIANO_MASSIMO);
    public /*@ pure @*/ boolean pianoValido(int piano) {
        return PIANO_MINIMO <= piano && piano <= PIANO_MASSIMO;
    }

    //@ requires pianoValido(piano);
    //@ ensures 0 <= \result && \result < NUMERO_PIANI;
    //@ ensures \result == piano - PIANO_MINIMO;
    private /*@ spec_public @*/ /*@ pure @*/ int indiceDelPiano(int piano) {
        return piano - PIANO_MINIMO;
    }

    //@ requires pianoValido(piano);
    //@ ensures \result == richiesteAttive[indiceDelPiano(piano)];
    public /*@ pure @*/ boolean richiestaAttiva(int piano) {
        return richiesteAttive[indiceDelPiano(piano)];
    }

    //@ ensures \result == pianoCorrente;
    public /*@ pure @*/ int getPianoCorrente() {
        return pianoCorrente;
    }

    //@ ensures \result == timer;
    public /*@ pure @*/ int getTimer() {
        return timer;
    }

    //@ ensures \result == numeroPersone;
    public /*@ pure @*/ int getNumeroPersone() {
        return numeroPersone;
    }

    //@ ensures \result == statoCabina;
    public /*@ pure @*/ StatoCabina getStatoCabina() {
        return statoCabina;
    }

    //@ ensures \result == statoPorte;
    public /*@ pure @*/ StatoPorte getStatoPorte() {
        return statoPorte;
    }

    //@ ensures \result == direzione;
    public /*@ pure @*/ Direzione getDirezione() {
        return direzione;
    }

    //@ ensures \result == statoErrore;
    public /*@ pure @*/ StatoErrore getStatoErrore() {
        return statoErrore;
    }

    //@ requires pianoValido(piano);
    //@ requires statoErrore != StatoErrore.GUASTO;
    //@ ensures richiesteAttive[indiceDelPiano(piano)] == true;
    public void aggiungiRichiestaInterna(int piano) {
        richiesteAttive[indiceDelPiano(piano)] = true;
    }

    //@ requires pianoValido(piano);
    //@ requires piano < PIANO_MASSIMO;
    //@ requires statoErrore != StatoErrore.GUASTO;
    //@ ensures richiesteAttive[indiceDelPiano(piano)] == true;
    public void aggiungiChiamataSalita(int piano) {
        richiesteAttive[indiceDelPiano(piano)] = true;
    }

    //@ requires pianoValido(piano);
    //@ requires piano > PIANO_MINIMO;
    //@ requires statoErrore != StatoErrore.GUASTO;
    //@ ensures richiesteAttive[indiceDelPiano(piano)] == true;
    public void aggiungiChiamataDiscesa(int piano) {
        richiesteAttive[indiceDelPiano(piano)] = true;
    }

    /*@
      @ requires personeEntrate >= 0;
      @ requires personeUscite >= 0;
      @ requires statoPorte == StatoPorte.APERTE;
      @ requires statoCabina == StatoCabina.FERMA || statoCabina == StatoCabina.BLOCCATA;
      @ ensures numeroPersone >= 0;
      @ ensures \old(numeroPersone) + personeEntrate - personeUscite >= 0 ==>
      @     numeroPersone == \old(numeroPersone) + personeEntrate - personeUscite;
      @ ensures \old(numeroPersone) + personeEntrate - personeUscite < 0 ==>
      @     numeroPersone == 0;
      @*/
    @CodeBigintMath
    public void aggiornaPersone(int personeEntrate, int personeUscite) {
        int nuovoNumeroPersone = numeroPersone + personeEntrate - personeUscite;

        if (nuovoNumeroPersone >= 0) {
            numeroPersone = nuovoNumeroPersone;
        } else {
            numeroPersone = 0;
        }
    }

    /*@
      @ ensures \old(numeroPersone) > CAPACITA_MASSIMA ==>
      @     statoErrore == StatoErrore.OVERLOAD &&
      @     statoCabina == StatoCabina.BLOCCATA &&
      @     statoPorte == StatoPorte.APERTE &&
      @     direzione == Direzione.NESSUNA &&
      @     timer == 0;
      @
      @ ensures \old(numeroPersone) <= CAPACITA_MASSIMA &&
      @         \old(statoErrore) == StatoErrore.OVERLOAD ==>
      @     statoErrore == StatoErrore.NESSUNO &&
      @     statoCabina == StatoCabina.FERMA &&
      @     statoPorte == StatoPorte.CHIUSE &&
      @     direzione == Direzione.NESSUNA &&
      @     timer == 0;
      @
      @ ensures \old(numeroPersone) <= CAPACITA_MASSIMA &&
      @         \old(statoErrore) != StatoErrore.OVERLOAD ==>
      @     statoErrore == \old(statoErrore) &&
      @     statoCabina == \old(statoCabina) &&
      @     statoPorte == \old(statoPorte) &&
      @     direzione == \old(direzione) &&
      @     timer == \old(timer);
      @*/
    public void gestisciSovraccarico() {
        if (numeroPersone > CAPACITA_MASSIMA) {
            statoErrore = StatoErrore.OVERLOAD;
            statoCabina = StatoCabina.BLOCCATA;
            statoPorte = StatoPorte.APERTE;
            direzione = Direzione.NESSUNA;
            timer = 0;
        } else if (statoErrore == StatoErrore.OVERLOAD) {
            statoErrore = StatoErrore.NESSUNO;
            statoCabina = StatoCabina.FERMA;
            statoPorte = StatoPorte.CHIUSE;
            direzione = Direzione.NESSUNA;
            timer = 0;
        }
    }
    //@ requires statoErrore == StatoErrore.NESSUNO;
    //@ ensures statoErrore == StatoErrore.GUASTO;
    //@ ensures statoCabina == StatoCabina.BLOCCATA;
    //@ ensures statoPorte == StatoPorte.CHIUSE;
    //@ ensures direzione == Direzione.NESSUNA;
    //@ ensures timer == TIMER_MASSIMO;
    public void attivaGuasto() {
        statoErrore = StatoErrore.GUASTO;
        statoCabina = StatoCabina.BLOCCATA;
        statoPorte = StatoPorte.CHIUSE;
        direzione = Direzione.NESSUNA;
        timer = TIMER_MASSIMO;
    }

    /*@
      @ requires statoErrore == StatoErrore.GUASTO;
      @ ensures \old(timer) > 0 ==> timer == \old(timer) - 1;
      @ ensures \old(timer) > 0 ==> statoErrore == StatoErrore.GUASTO;
      @ ensures \old(timer) == 0 ==>
      @     statoErrore == StatoErrore.NESSUNO &&
      @     statoCabina == StatoCabina.FERMA &&
      @     statoPorte == StatoPorte.CHIUSE &&
      @     direzione == Direzione.NESSUNA &&
      @     timer == 0;
      @*/
    public void gestisciTimerGuasto() {
        if (timer > 0) {
            timer = timer - 1;
        } else {
            statoErrore = StatoErrore.NESSUNO;
            statoCabina = StatoCabina.FERMA;
            statoPorte = StatoPorte.CHIUSE;
            direzione = Direzione.NESSUNA;
            timer = 0;
        }
    }

    //@ requires statoErrore == StatoErrore.NESSUNO;
    //@ requires statoCabina == StatoCabina.FERMA;
    //@ requires statoPorte == StatoPorte.APERTE;
    //@ ensures statoPorte == StatoPorte.CHIUSE;
    public void chiudiPorte() {
        statoPorte = StatoPorte.CHIUSE;
    }

    //@ requires statoErrore == StatoErrore.NESSUNO;
    //@ requires richiestaAttiva(pianoCorrente);
    //@ ensures statoCabina == StatoCabina.FERMA;
    //@ ensures statoPorte == StatoPorte.APERTE;
    //@ ensures direzione == Direzione.NESSUNA;
    //@ ensures richiesteAttive[indiceDelPiano(pianoCorrente)] == false;
    public void serviPianoCorrente() {
        statoCabina = StatoCabina.FERMA;
        statoPorte = StatoPorte.APERTE;
        richiesteAttive[indiceDelPiano(pianoCorrente)] = false;
        direzione = Direzione.NESSUNA;
    }

	//@ requires statoErrore == StatoErrore.NESSUNO;
	//@ ensures statoCabina == StatoCabina.FERMA;
	//@ ensures direzione == Direzione.NESSUNA;
	public void mettiInAttesa() {
	    statoCabina = StatoCabina.FERMA;
	    direzione = Direzione.NESSUNA;
	}

    //@ ensures \result <==> (\exists int i; 0 <= i && i < NUMERO_PIANI; richiesteAttive[i]);
    public /*@ pure @*/ boolean esisteRichiesta() {
        //@ loop_invariant 0 <= i && i <= NUMERO_PIANI;
        //@ loop_invariant (\forall int j; 0 <= j && j < i; richiesteAttive[j] == false);
        for (int i = 0; i < NUMERO_PIANI; i++) {
            if (richiesteAttive[i]) {
                return true;
            }
        }

        return false;
    }

    /*@
      @ ensures \result <==>
      @     (\exists int p; pianoCorrente < p && p <= PIANO_MASSIMO;
      @         richiesteAttive[indiceDelPiano(p)]);
      @*/
    public /*@ pure @*/ boolean esisteRichiestaSopra() {
        int piano = pianoCorrente + 1;

        /*@
          @ loop_invariant pianoCorrente + 1 <= piano && piano <= PIANO_MASSIMO + 1;
          @ loop_invariant (\forall int p; pianoCorrente < p && p < piano;
          @     richiesteAttive[indiceDelPiano(p)] == false);
          @*/
        while (piano <= PIANO_MASSIMO) {
            if (richiesteAttive[indiceDelPiano(piano)]) {
                return true;
            }

            piano++;
        }

        return false;
    }

    /*@
      @ ensures \result <==>
      @     (\exists int p; PIANO_MINIMO <= p && p < pianoCorrente;
      @         richiesteAttive[indiceDelPiano(p)]);
      @*/
    public /*@ pure @*/ boolean esisteRichiestaSotto() {
        int piano = pianoCorrente - 1;

        /*@
          @ loop_invariant PIANO_MINIMO - 1 <= piano && piano <= pianoCorrente - 1;
          @ loop_invariant (\forall int p; piano < p && p < pianoCorrente;
          @     richiesteAttive[indiceDelPiano(p)] == false);
          @*/
        while (piano >= PIANO_MINIMO) {
            if (richiesteAttive[indiceDelPiano(piano)]) {
                return true;
            }

            piano--;
        }

        return false;
    }

	/*@
	  @ requires statoErrore == StatoErrore.NESSUNO;
	  @ ensures !esisteRichiesta() ==> direzione == Direzione.NESSUNA;
	  @ ensures \old(direzione) == Direzione.SU && \old(esisteRichiestaSopra()) ==> direzione == Direzione.SU;
	  @ ensures \old(direzione) == Direzione.GIU && \old(esisteRichiestaSotto()) ==> direzione == Direzione.GIU;
	  @*/
	public void scegliDirezione() {
	    if (!esisteRichiesta()) {
	        direzione = Direzione.NESSUNA;
	        return;
	    }
	
	    if (direzione == Direzione.SU) {
	        if (esisteRichiestaSopra()) {
	            direzione = Direzione.SU;
	        } else if (esisteRichiestaSotto()) {
	            direzione = Direzione.GIU;
	        } else {
	            direzione = Direzione.NESSUNA;
	        }
	    } else if (direzione == Direzione.GIU) {
	        if (esisteRichiestaSotto()) {
	            direzione = Direzione.GIU;
	        } else if (esisteRichiestaSopra()) {
	            direzione = Direzione.SU;
	        } else {
	            direzione = Direzione.NESSUNA;
	        }
	    } else {
	        if (esisteRichiestaSopra()) {
	            direzione = Direzione.SU;
	        } else if (esisteRichiestaSotto()) {
	            direzione = Direzione.GIU;
	        } else {
	            direzione = Direzione.NESSUNA;
	        }
	    }
	}

    /*@
      @ requires statoErrore == StatoErrore.NESSUNO;
      @ requires statoPorte == StatoPorte.CHIUSE;
      @
      @ ensures PIANO_MINIMO <= pianoCorrente && pianoCorrente <= PIANO_MASSIMO;
      @
      @ ensures \old(direzione) == Direzione.SU && \old(pianoCorrente) < PIANO_MASSIMO ==>
      @     pianoCorrente == \old(pianoCorrente) + 1 &&
      @     statoCabina == StatoCabina.IN_MOVIMENTO;
      @
      @ ensures \old(direzione) == Direzione.GIU && \old(pianoCorrente) > PIANO_MINIMO ==>
      @     pianoCorrente == \old(pianoCorrente) - 1 &&
      @     statoCabina == StatoCabina.IN_MOVIMENTO;
      @
      @ ensures \old(direzione) == Direzione.SU && \old(pianoCorrente) == PIANO_MASSIMO ==>
      @     statoCabina == StatoCabina.FERMA &&
      @     direzione == Direzione.NESSUNA;
      @
      @ ensures \old(direzione) == Direzione.GIU && \old(pianoCorrente) == PIANO_MINIMO ==>
      @     statoCabina == StatoCabina.FERMA &&
      @     direzione == Direzione.NESSUNA;
      @*/
    public void muoviDiUnPiano() {
        if (direzione == Direzione.SU && pianoCorrente < PIANO_MASSIMO) {
            pianoCorrente = pianoCorrente + 1;
            statoCabina = StatoCabina.IN_MOVIMENTO;
        } else if (direzione == Direzione.GIU && pianoCorrente > PIANO_MINIMO) {
            pianoCorrente = pianoCorrente - 1;
            statoCabina = StatoCabina.IN_MOVIMENTO;
        } else {
            statoCabina = StatoCabina.FERMA;
            direzione = Direzione.NESSUNA;
        }
    }
}