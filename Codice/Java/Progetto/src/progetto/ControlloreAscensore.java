package progetto;

public class ControlloreAscensore {

    private final Ascensore ascensore;

    public ControlloreAscensore(Ascensore ascensore) {
        if (ascensore == null) {
            throw new IllegalArgumentException("Ascensore non puo essere null");
        }

        this.ascensore = ascensore;
    }

    public Ascensore getAscensore() {
        return ascensore;
    }

    public void eseguiPasso(InputAscensore input) {
        if (input == null) {
            throw new IllegalArgumentException("InputAscensore non puo essere null");
        }

        acquisisciRichieste(input);
        gestisciPersone(input);
        gestisciErrore(input);
        gestisciComportamentoNormale();
    }

    private void acquisisciRichieste(InputAscensore input) {
        if (ascensore.getStatoErrore() == StatoErrore.GUASTO) {
            return;
        }

        if (input.haRichiestaInterna() && ascensore.pianoValido(input.getRichiestaInterna())) {
            ascensore.aggiungiRichiestaInterna(input.getRichiestaInterna());
        }

        if (input.haChiamataSalita()
                && ascensore.pianoValido(input.getChiamataSalita())
                && input.getChiamataSalita() < Ascensore.PIANO_MASSIMO) {
            ascensore.aggiungiChiamataSalita(input.getChiamataSalita());
        }

        if (input.haChiamataDiscesa()
                && ascensore.pianoValido(input.getChiamataDiscesa())
                && input.getChiamataDiscesa() > Ascensore.PIANO_MINIMO) {
            ascensore.aggiungiChiamataDiscesa(input.getChiamataDiscesa());
        }
    }

    private void gestisciPersone(InputAscensore input) {
        if (ascensore.getStatoPorte() != StatoPorte.APERTE) {
            return;
        }

        if (input.getPersoneEntrate() < 0) {
            return;
        }

        if (input.getPersoneUscite() < 0) {
            return;
        }

        ascensore.aggiornaPersone(input.getPersoneEntrate(), input.getPersoneUscite());
    }

    private void gestisciErrore(InputAscensore input) {
        ascensore.gestisciSovraccarico();

        if (ascensore.getStatoErrore() == StatoErrore.OVERLOAD) {
            return;
        }

        if (ascensore.getStatoErrore() == StatoErrore.GUASTO) {
            ascensore.gestisciTimerGuasto();
            return;
        }

        if (input.isEventoGuasto()) {
            ascensore.attivaGuasto();
        }
    }

    private void gestisciComportamentoNormale() {
        if (ascensore.getStatoErrore() != StatoErrore.NESSUNO) {
            return;
        }

        if (ascensore.getStatoPorte() == StatoPorte.APERTE) {
            ascensore.chiudiPorte();
            return;
        }

        if (ascensore.richiestaAttiva(ascensore.getPianoCorrente())) {
            ascensore.serviPianoCorrente();
            return;
        }

        if (!ascensore.esisteRichiesta()) {
            ascensore.mettiInAttesa();
            return;
        }

        ascensore.scegliDirezione();
        ascensore.muoviDiUnPiano();

    }
}