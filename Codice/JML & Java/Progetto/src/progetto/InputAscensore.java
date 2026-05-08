package progetto;

public class InputAscensore {

    private int richiestaInterna = Integer.MIN_VALUE;
    private int chiamataSalita = Integer.MIN_VALUE;
    private int chiamataDiscesa = Integer.MIN_VALUE;

    private boolean eventoGuasto;
    private int personeEntrate;
    private int personeUscite;

    public int getRichiestaInterna() {
        return richiestaInterna;
    }

    public void setRichiestaInterna(int richiestaInterna) {
        this.richiestaInterna = richiestaInterna;
    }

    public int getChiamataSalita() {
        return chiamataSalita;
    }

    public void setChiamataSalita(int chiamataSalita) {
        this.chiamataSalita = chiamataSalita;
    }

    public int getChiamataDiscesa() {
        return chiamataDiscesa;
    }

    public void setChiamataDiscesa(int chiamataDiscesa) {
        this.chiamataDiscesa = chiamataDiscesa;
    }

    public boolean isEventoGuasto() {
        return eventoGuasto;
    }

    public void setEventoGuasto(boolean eventoGuasto) {
        this.eventoGuasto = eventoGuasto;
    }

    public int getPersoneEntrate() {
        return personeEntrate;
    }

    public void setPersoneEntrate(int personeEntrate) {
        this.personeEntrate = personeEntrate;
    }

    public int getPersoneUscite() {
        return personeUscite;
    }

    public void setPersoneUscite(int personeUscite) {
        this.personeUscite = personeUscite;
    }

    public boolean haRichiestaInterna() {
        return richiestaInterna != Integer.MIN_VALUE;
    }

    public boolean haChiamataSalita() {
        return chiamataSalita != Integer.MIN_VALUE;
    }

    public boolean haChiamataDiscesa() {
        return chiamataDiscesa != Integer.MIN_VALUE;
    }
}