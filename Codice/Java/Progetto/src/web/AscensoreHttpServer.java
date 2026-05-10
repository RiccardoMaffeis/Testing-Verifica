package web;

import progetto.Ascensore;
import progetto.ControlloreAscensore;
import progetto.InputAscensore;
import progetto.StatoErrore;
import progetto.StatoPorte;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

public class AscensoreHttpServer {

    private static final int PORTA = 8080;
    private static final int INTERVALLO_SIMULAZIONE_MS = 1000;

    private static final int PROBABILITA_NUOVA_RICHIESTA = 35;
    private static final int PROBABILITA_GUASTO = 3;
    private static final int PROBABILITA_INGRESSO_EXTRA = 30;

    private static final int NUMERO_EVENTI_VISUALIZZATI = 8;

    private Ascensore ascensore;
    private ControlloreAscensore controllore;

    private final Random random = new Random();

    private final boolean[] richiesteVisuali = new boolean[Ascensore.NUMERO_PIANI];
    private final String[] tipiRichiesteVisuali = new String[Ascensore.NUMERO_PIANI];
    private final LinkedList<String> ultimiEventi = new LinkedList<>();

    private volatile boolean simulazioneAutomaticaAttiva;
    private Thread threadSimulazione;

    public AscensoreHttpServer() {
        resettaAscensore();
    }

    public static void main(String[] args) throws IOException {
        AscensoreHttpServer applicazione = new AscensoreHttpServer();
        applicazione.avvia();
    }

    private void avvia() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORTA), 0);

        server.createContext("/", this::gestisciPagina);
        server.createContext("/stato", this::gestisciStato);
        server.createContext("/azione", this::gestisciAzione);

        server.setExecutor(null);
        server.start();

        System.out.println("Server avviato su http://localhost:" + PORTA);
    }

    private void gestisciPagina(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            inviaErroreMetodo(exchange);
            return;
        }

        inviaHtml(exchange, costruisciPagina());
    }

    private void gestisciStato(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            inviaErroreMetodo(exchange);
            return;
        }

        synchronized (this) {
            inviaJson(exchange, statoJson());
        }
    }

    private void gestisciAzione(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            inviaErroreMetodo(exchange);
            return;
        }

        try {
            String body = leggiBody(exchange);
            Map<String, String> parametri = leggiParametri(body);

            String azione = parametri.getOrDefault("azione", "passo");

            synchronized (this) {
                if ("reset".equals(azione)) {
                    fermaSimulazioneAutomatica();
                    resettaAscensore();
                    inviaJson(exchange, statoJson());
                    return;
                }

                if ("avviaSimulazione".equals(azione)) {
                    avviaSimulazioneAutomatica();
                    aggiungiEvento("Simulazione automatica avviata");
                    inviaJson(exchange, statoJson());
                    return;
                }

                if ("fermaSimulazione".equals(azione)) {
                    fermaSimulazioneAutomatica();
                    aggiungiEvento("Simulazione automatica fermata");
                    inviaJson(exchange, statoJson());
                    return;
                }

                InputAscensore input = new InputAscensore();

                String tipoRichiestaDaRegistrare = null;
                int pianoRichiestaDaRegistrare = 0;

                int personeEntrate = 0;
                int personeUscite = 0;

                boolean guastoManuale = false;

                if ("richiestaInterna".equals(azione)) {
                    pianoRichiestaDaRegistrare = leggiIntero(parametri, "piano", 0);
                    tipoRichiestaDaRegistrare = "Interna";
                    input.setRichiestaInterna(pianoRichiestaDaRegistrare);
                } else if ("chiamataSalita".equals(azione)) {
                    pianoRichiestaDaRegistrare = leggiIntero(parametri, "piano", 0);
                    tipoRichiestaDaRegistrare = "Salita";
                    input.setChiamataSalita(pianoRichiestaDaRegistrare);
                } else if ("chiamataDiscesa".equals(azione)) {
                    pianoRichiestaDaRegistrare = leggiIntero(parametri, "piano", 0);
                    tipoRichiestaDaRegistrare = "Discesa";
                    input.setChiamataDiscesa(pianoRichiestaDaRegistrare);
                } else if ("persone".equals(azione)) {
                    personeEntrate = leggiIntero(parametri, "personeEntrate", 0);
                    personeUscite = leggiIntero(parametri, "personeUscite", 0);
                    input.setPersoneEntrate(personeEntrate);
                    input.setPersoneUscite(personeUscite);
                } else if ("guasto".equals(azione)) {
                    guastoManuale = true;
                    input.setEventoGuasto(true);
                }

                if (tipoRichiestaDaRegistrare != null) {
                    if (ascensore.getStatoErrore() == StatoErrore.GUASTO) {
                        aggiungiEvento("Richiesta ignorata: ascensore in guasto");
                    } else {
                        registraRichiestaVisuale(pianoRichiestaDaRegistrare, tipoRichiestaDaRegistrare);
                        aggiungiEvento("Richiesta " + tipoRichiestaDaRegistrare.toLowerCase()
                                + " inserita al piano " + pianoRichiestaDaRegistrare);
                    }
                }

                if ("persone".equals(azione)) {
                    if (personeEntrate > 0) {
                        aggiungiEvento("Entrate " + personeEntrate + " persone");
                    }
                    if (personeUscite > 0) {
                        aggiungiEvento("Uscite " + personeUscite + " persone");
                    }
                    if (personeEntrate == 0 && personeUscite == 0) {
                        aggiungiEvento("Aggiornamento persone senza variazioni");
                    }
                }

                if (guastoManuale) {
                    aggiungiEvento("Guasto attivato manualmente");
                }

                controllore.eseguiPasso(input);
                aggiornaRichiesteServite();

                inviaJson(exchange, statoJson());
            }
        } catch (RuntimeException e) {
            inviaJsonErrore(exchange, e.getMessage());
        }
    }

    private void resettaAscensore() {
        this.ascensore = new Ascensore();
        this.controllore = new ControlloreAscensore(ascensore);

        resettaRichiesteVisuali();
        ultimiEventi.clear();
        aggiungiEvento("Sistema inizializzato");
    }

    private void resettaRichiesteVisuali() {
        for (int i = 0; i < richiesteVisuali.length; i++) {
            richiesteVisuali[i] = false;
            tipiRichiesteVisuali[i] = "";
        }
    }

    private void registraRichiestaVisuale(int piano, String tipo) {
        if (!pianoValido(piano)) {
            return;
        }

        int indice = indiceDelPiano(piano);
        richiesteVisuali[indice] = true;

        if (tipiRichiesteVisuali[indice] == null || tipiRichiesteVisuali[indice].isEmpty()) {
            tipiRichiesteVisuali[indice] = tipo;
        } else if (!tipiRichiesteVisuali[indice].contains(tipo)) {
            tipiRichiesteVisuali[indice] = tipiRichiesteVisuali[indice] + ", " + tipo;
        }
    }

    private void aggiornaRichiesteServite() {
        int pianoCorrente = ascensore.getPianoCorrente();

        if (!pianoValido(pianoCorrente)) {
            return;
        }

        if (ascensore.getStatoPorte() == StatoPorte.APERTE) {
            int indice = indiceDelPiano(pianoCorrente);

            if (richiesteVisuali[indice]) {
                richiesteVisuali[indice] = false;
                tipiRichiesteVisuali[indice] = "";
                aggiungiEvento("Servito il piano " + pianoCorrente);
            }
        }
    }

    private boolean pianoValido(int piano) {
        return Ascensore.PIANO_MINIMO <= piano && piano <= Ascensore.PIANO_MASSIMO;
    }

    private int indiceDelPiano(int piano) {
        return piano - Ascensore.PIANO_MINIMO;
    }

    private void aggiungiEvento(String evento) {
        if (evento == null || evento.trim().isEmpty()) {
            return;
        }

        String orario = new SimpleDateFormat("HH:mm:ss").format(new Date());
        ultimiEventi.addFirst("[" + orario + "] " + evento);

        while (ultimiEventi.size() > NUMERO_EVENTI_VISUALIZZATI) {
            ultimiEventi.removeLast();
        }
    }

    private void avviaSimulazioneAutomatica() {
        if (simulazioneAutomaticaAttiva) {
            return;
        }

        simulazioneAutomaticaAttiva = true;

        threadSimulazione = new Thread(new Runnable() {
            @Override
            public void run() {
                while (simulazioneAutomaticaAttiva) {
                    synchronized (AscensoreHttpServer.this) {
                        InputAscensore input = generaInputCasuale();
                        controllore.eseguiPasso(input);
                        aggiornaRichiesteServite();
                    }

                    try {
                        Thread.sleep(INTERVALLO_SIMULAZIONE_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        simulazioneAutomaticaAttiva = false;
                    }
                }
            }
        });

        threadSimulazione.setDaemon(true);
        threadSimulazione.start();
    }

    private void fermaSimulazioneAutomatica() {
        simulazioneAutomaticaAttiva = false;

        if (threadSimulazione != null) {
            threadSimulazione.interrupt();
            threadSimulazione = null;
        }
    }

    private InputAscensore generaInputCasuale() {
        InputAscensore input = new InputAscensore();

        if (ascensore.getStatoErrore() == StatoErrore.GUASTO) {
            return input;
        }

        generaGuastoCasuale(input);

        if (!input.isEventoGuasto()) {
            generaPersoneCasuali(input);
            generaRichiestaCasuale(input);
        }

        return input;
    }

    private void generaGuastoCasuale(InputAscensore input) {
        if (ascensore.getStatoErrore() != StatoErrore.NESSUNO) {
            return;
        }

        if (random.nextInt(100) < PROBABILITA_GUASTO) {
            input.setEventoGuasto(true);
            aggiungiEvento("Guasto generato automaticamente");
        }
    }

    private void generaPersoneCasuali(InputAscensore input) {
        if (ascensore.getStatoPorte() != StatoPorte.APERTE) {
            return;
        }

        if (ascensore.getStatoErrore() == StatoErrore.OVERLOAD) {
            int personeUscite = 1 + random.nextInt(3);

            input.setPersoneEntrate(0);
            input.setPersoneUscite(personeUscite);

            aggiungiEvento("Overload: uscite " + personeUscite + " persone");
            return;
        }

        int numeroPersone = ascensore.getNumeroPersone();
        int postiLiberi = Ascensore.CAPACITA_MASSIMA - numeroPersone;

        int personeEntrate;

        if (postiLiberi <= 0) {
            personeEntrate = 1 + random.nextInt(3);
        } else {
            personeEntrate = random.nextInt(Math.min(4, postiLiberi + 1));

            if (random.nextInt(100) < PROBABILITA_INGRESSO_EXTRA) {
                personeEntrate = personeEntrate + 1 + random.nextInt(2);
            }
        }

        int personeUscite = random.nextInt(Math.min(4, numeroPersone + 1));

        input.setPersoneEntrate(personeEntrate);
        input.setPersoneUscite(personeUscite);

        if (personeEntrate > 0) {
            aggiungiEvento("Entrate " + personeEntrate + " persone");
        }

        if (personeUscite > 0) {
            aggiungiEvento("Uscite " + personeUscite + " persone");
        }
    }

    private void generaRichiestaCasuale(InputAscensore input) {
        if (ascensore.getStatoErrore() == StatoErrore.GUASTO) {
            return;
        }

        int probabilita = random.nextInt(100);

        if (probabilita >= PROBABILITA_NUOVA_RICHIESTA) {
            return;
        }

        int piano = Ascensore.PIANO_MINIMO + random.nextInt(Ascensore.NUMERO_PIANI);
        int tipoRichiesta = random.nextInt(3);

        if (tipoRichiesta == 0) {
            input.setRichiestaInterna(piano);
            registraRichiestaVisuale(piano, "Interna");
            aggiungiEvento("Richiesta interna generata al piano " + piano);
        } else if (tipoRichiesta == 1) {
            input.setChiamataSalita(piano);
            registraRichiestaVisuale(piano, "Salita");
            aggiungiEvento("Chiamata salita generata al piano " + piano);
        } else {
            input.setChiamataDiscesa(piano);
            registraRichiestaVisuale(piano, "Discesa");
            aggiungiEvento("Chiamata discesa generata al piano " + piano);
        }
    }

    private String leggiBody(HttpExchange exchange) throws IOException {
        StringBuilder body = new StringBuilder();
        byte[] buffer = new byte[1024];

        int bytesLetti;
        while ((bytesLetti = exchange.getRequestBody().read(buffer)) != -1) {
            body.append(new String(buffer, 0, bytesLetti, StandardCharsets.UTF_8));
        }

        return body.toString();
    }

    private Map<String, String> leggiParametri(String body) {
        Map<String, String> parametri = new HashMap<>();

        if (body == null || body.isEmpty()) {
            return parametri;
        }

        String[] coppie = body.split("&");

        for (String coppia : coppie) {
            String[] parti = coppia.split("=", 2);

            String chiave = decodifica(parti[0]);
            String valore = parti.length > 1 ? decodifica(parti[1]) : "";

            parametri.put(chiave, valore);
        }

        return parametri;
    }

    private String decodifica(String valore) {
        try {
            return URLDecoder.decode(valore, "UTF-8");
        } catch (Exception e) {
            return valore;
        }
    }

    private int leggiIntero(Map<String, String> parametri, String nome, int valoreDefault) {
        String valore = parametri.get(nome);

        if (valore == null || valore.trim().isEmpty()) {
            return valoreDefault;
        }

        try {
            return Integer.parseInt(valore.trim());
        } catch (NumberFormatException e) {
            return valoreDefault;
        }
    }

    private String statoJson() {
        return "{"
                + "\"pianoCorrente\":" + ascensore.getPianoCorrente() + ","
                + "\"timer\":" + ascensore.getTimer() + ","
                + "\"numeroPersone\":" + ascensore.getNumeroPersone() + ","
                + "\"statoCabina\":\"" + ascensore.getStatoCabina() + "\","
                + "\"statoPorte\":\"" + ascensore.getStatoPorte() + "\","
                + "\"direzione\":\"" + ascensore.getDirezione() + "\","
                + "\"statoErrore\":\"" + ascensore.getStatoErrore() + "\","
                + "\"simulazioneAutomaticaAttiva\":" + simulazioneAutomaticaAttiva + ","
                + "\"richiesteVisuali\":" + richiesteVisualiJson() + ","
                + "\"ultimiEventi\":" + ultimiEventiJson()
                + "}";
    }

    private String richiesteVisualiJson() {
        StringBuilder json = new StringBuilder();

        json.append("[");

        boolean primo = true;

        for (int piano = Ascensore.PIANO_MASSIMO; piano >= Ascensore.PIANO_MINIMO; piano--) {
            if (!primo) {
                json.append(",");
            }

            primo = false;

            int indice = indiceDelPiano(piano);

            json.append("{");
            json.append("\"piano\":").append(piano).append(",");
            json.append("\"attiva\":").append(richiesteVisuali[indice]).append(",");
            json.append("\"tipo\":\"").append(escapeJson(tipiRichiesteVisuali[indice])).append("\"");
            json.append("}");
        }

        json.append("]");

        return json.toString();
    }

    private String ultimiEventiJson() {
        StringBuilder json = new StringBuilder();

        json.append("[");

        for (int i = 0; i < ultimiEventi.size(); i++) {
            if (i > 0) {
                json.append(",");
            }

            json.append("\"").append(escapeJson(ultimiEventi.get(i))).append("\"");
        }

        json.append("]");

        return json.toString();
    }

    private void inviaHtml(HttpExchange exchange, String html) throws IOException {
        byte[] risposta = html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, risposta.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(risposta);
        }
    }

    private void inviaJson(HttpExchange exchange, String json) throws IOException {
        byte[] risposta = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(200, risposta.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(risposta);
        }
    }

    private void inviaJsonErrore(HttpExchange exchange, String messaggio) throws IOException {
        String json = "{"
                + "\"errore\":\"" + escapeJson(messaggio) + "\""
                + "}";

        byte[] risposta = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(500, risposta.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(risposta);
        }
    }

    private void inviaErroreMetodo(HttpExchange exchange) throws IOException {
        String json = "{\"errore\":\"Metodo HTTP non supportato\"}";
        byte[] risposta = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(405, risposta.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(risposta);
        }
    }

    private String costruisciPagina() {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"it\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<title>Simulatore Ascensore</title>");

        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 32px; }");
        html.append(".container { max-width: 1100px; margin: auto; background: white; padding: 24px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.08); }");
        html.append(".header { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 24px; }");
        html.append(".header h1 { margin: 0; }");
        html.append("h2 { margin-top: 28px; }");

        html.append(".simulatore-panel { display: grid; grid-template-columns: 340px 1fr; gap: 24px; align-items: start; margin-bottom: 28px; }");
        html.append(".ascensore-box { background: #fafafa; border: 1px solid #ddd; border-radius: 12px; padding: 16px; }");
        html.append(".ascensore-box h2 { margin-top: 0; }");
        html.append(".vano { position: relative; width: 180px; height: 420px; margin: 0 auto; border: 3px solid #555; border-radius: 10px; background: linear-gradient(to top, #f0f0f0, #fcfcfc); overflow: hidden; }");
        html.append(".piano-riga { position: absolute; left: 0; width: 100%; height: 70px; border-top: 1px dashed #cfcfcf; box-sizing: border-box; transition: background 0.25s; }");
        html.append(".etichetta-piano { position: absolute; left: 8px; top: 6px; font-size: 12px; color: #666; font-weight: bold; }");
        html.append(".highlight-piano { background: rgba(255, 235, 59, 0.18); }");

        html.append(".cabina { position: absolute; left: 35px; width: 110px; height: 56px; background: #90caf9; border: 2px solid #42a5f5; border-radius: 8px; box-sizing: border-box; transition: bottom 0.7s ease-in-out, transform 0.3s ease, background 0.3s, border-color 0.3s; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.18); }");
        html.append(".cabina.in-movimento { transform: scale(1.03); }");
        html.append(".cabina.bloccata { background: #ef9a9a; border-color: #e57373; }");
        html.append(".porte { display: flex; width: 100%; height: 100%; }");
        html.append(".porta { width: 50%; height: 100%; background: rgba(255,255,255,0.58); border-right: 1px solid rgba(0,0,0,0.14); transition: transform 0.5s ease-in-out; }");
        html.append(".porta-dx { border-right: none; border-left: 1px solid rgba(0,0,0,0.14); }");
        html.append(".cabina.porte-aperte .porta-sx { transform: translateX(-85%); }");
        html.append(".cabina.porte-aperte .porta-dx { transform: translateX(85%); }");
        html.append(".cabina-info { position: absolute; left: 0; right: 0; bottom: 2px; text-align: center; font-size: 11px; font-weight: bold; color: #0d47a1; z-index: 2; }");
        html.append(".cabina-persone { position: absolute; left: 6px; right: 6px; top: 6px; display: flex; flex-wrap: wrap; gap: 4px; z-index: 1; justify-content: center; }");
        html.append(".mini-persona { width: 10px; height: 10px; border-radius: 50%; background: #1565c0; }");
        html.append(".mini-persona.overload { background: #b71c1c; }");

        html.append(".grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-bottom: 16px; }");
        html.append(".card { border: 1px solid #ddd; background: #fafafa; border-radius: 8px; padding: 12px; transition: background 0.2s, border-color 0.2s; }");
        html.append(".label { font-weight: bold; display: block; margin-bottom: 4px; }");
        html.append(".value { font-size: 20px; }");
        html.append(".success { background: #e8f5e9; border-color: #a5d6a7; }");
        html.append(".warning { background: #fff8e1; border-color: #ffcc80; }");
        html.append(".danger { background: #ffebee; border-color: #ef9a9a; }");

        html.append(".quick-controls { margin-top: 12px; padding-top: 12px; border-top: 1px solid #eee; }");
        html.append(".quick-controls h2 { margin: 0 0 8px 0; font-size: 18px; }");
        html.append(".quick-controls button { margin-bottom: 6px; }");

        html.append(".mode-switch { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }");
        html.append(".mode-button { font-weight: bold; background: #f7f7f7; }");
        html.append(".mode-button.active { background: #e3f2fd; border-color: #90caf9; }");
        html.append(".mode-button:disabled { opacity: 0.5; cursor: not-allowed; }");
        html.append(".mode-panel { display: none; margin-top: 16px; }");
        html.append(".mode-panel > .section:first-child { border-top: none; padding-top: 0; }");
        html.append(".mode-panel.active { display: block; }");

        html.append(".monitor-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }");
        html.append(".monitor-card { border: 1px solid #ddd; background: #fafafa; border-radius: 12px; padding: 16px; }");
        html.append(".monitor-card h2 { margin-top: 0; font-size: 20px; }");
        html.append(".richieste-lista { display: grid; gap: 8px; }");
        html.append(".richiesta-riga { display: flex; justify-content: space-between; align-items: center; border: 1px solid #e0e0e0; background: white; border-radius: 8px; padding: 8px 10px; }");
        html.append(".richiesta-riga.attiva { background: #e3f2fd; border-color: #90caf9; }");
        html.append(".richiesta-piano { font-weight: bold; }");
        html.append(".badge-richiesta { font-size: 12px; border-radius: 999px; padding: 4px 8px; background: #eee; color: #555; }");
        html.append(".badge-richiesta.attiva { background: #1565c0; color: white; }");

        html.append(".eventi-lista { margin: 0; padding: 0; list-style: none; font-family: Consolas, monospace; }");
        html.append(".eventi-lista li { margin-bottom: 6px; padding: 6px 8px; background: white; border: 1px solid #e0e0e0; border-left: 5px solid #bdbdbd; border-radius: 6px; color: #333; }");
        html.append(".evento-successo { border-left-color: #2e7d32 !important; }");
        html.append(".evento-info { border-left-color: #1565c0 !important; }");
        html.append(".evento-warning { border-left-color: #ef6c00 !important; }");
        html.append(".evento-errore { border-left-color: #b00020 !important; color: #7f0000; }");
        html.append(".evento-orario { font-weight: bold; color: #555; margin-right: 6px; }");
        html.append(".vuoto { color: #777; font-style: italic; }");

        html.append(".playButton { background: #e8f5e9; border-color: #81c784; font-weight: bold; }");
        html.append(".stopButton { background: #fff3e0; border-color: #ffb74d; font-weight: bold; }");

        html.append(".section { border-top: 1px solid #eee; padding-top: 16px; }");
        html.append("input { padding: 8px; width: 80px; margin: 4px; }");
        html.append("button { padding: 9px 14px; margin: 4px; cursor: pointer; border: 1px solid #ccc; border-radius: 6px; background: #f7f7f7; }");
        html.append("button:hover { background: #eaeaea; }");
        html.append(".primary { background: #e3f2fd; border-color: #90caf9; }");
        html.append(".dangerButton { background: #ffebee; border-color: #ef9a9a; }");
        html.append(".floor-buttons button { min-width: 46px; }");
        html.append("#messaggio { margin-top: 16px; font-weight: bold; }");
        html.append("@media (max-width: 900px) { .header { align-items: flex-start; flex-direction: column; } .mode-switch { justify-content: flex-start; } .simulatore-panel { grid-template-columns: 1fr; } .grid { grid-template-columns: 1fr; } .monitor-grid { grid-template-columns: 1fr; } }");
        html.append("</style>");

        html.append("</head>");
        html.append("<body>");

        html.append("<div class=\"container\">");

        html.append("<div class=\"header\">");
        html.append("<h1>Simulatore Ascensore</h1>");
        html.append("<div class=\"mode-switch\">");
        html.append("<button id=\"btnModalitaAutomatica\" class=\"mode-button active\" type=\"button\" onclick=\"mostraModalita('automatica')\">Simulazione automatica</button>");
        html.append("<button id=\"btnModalitaManuale\" class=\"mode-button\" type=\"button\" onclick=\"mostraModalita('manuale')\">Controllo manuale</button>");
        html.append("</div>");
        html.append("</div>");

        html.append("<div class=\"simulatore-panel\">");

        html.append("<div class=\"ascensore-box\">");
        html.append("<h2>Vista ascensore</h2>");
        html.append("<div class=\"vano\">");

        for (int piano = Ascensore.PIANO_MASSIMO; piano >= Ascensore.PIANO_MINIMO; piano--) {
            int indiceVisivo = Ascensore.PIANO_MASSIMO - piano;
            int top = indiceVisivo * 70;

            html.append("<div class=\"piano-riga\" id=\"pianoRiga")
                    .append(pianoToId(piano))
                    .append("\" style=\"top:")
                    .append(top)
                    .append("px;\">");

            html.append("<div class=\"etichetta-piano\">Piano ")
                    .append(piano)
                    .append("</div>");

            html.append("</div>");
        }

        html.append("<div class=\"cabina\" id=\"cabinaAscensore\" style=\"bottom: 7px;\">");
        html.append("<div class=\"cabina-persone\" id=\"cabinaPersone\"></div>");
        html.append("<div class=\"porte\">");
        html.append("<div class=\"porta porta-sx\"></div>");
        html.append("<div class=\"porta porta-dx\"></div>");
        html.append("</div>");
        html.append("<div class=\"cabina-info\" id=\"cabinaInfo\">P0</div>");
        html.append("</div>");

        html.append("</div>");
        html.append("</div>");

        html.append("<div>");
        html.append("<h2 style=\"margin-top: 0;\">Stato ascensore</h2>");

        html.append("<div class=\"grid\">");
        aggiungiCard(html, "Piano corrente", "pianoCorrente", String.valueOf(ascensore.getPianoCorrente()));
        aggiungiCard(html, "Timer", "timer", String.valueOf(ascensore.getTimer()));
        aggiungiCard(html, "Numero persone", "numeroPersone", String.valueOf(ascensore.getNumeroPersone()));
        aggiungiCard(html, "Stato cabina", "statoCabina", ascensore.getStatoCabina().toString());
        aggiungiCard(html, "Stato porte", "statoPorte", ascensore.getStatoPorte().toString());
        aggiungiCard(html, "Direzione", "direzione", ascensore.getDirezione().toString());
        aggiungiCardConIdCard(html, "Stato errore", "statoErrore", "statoErroreCard", ascensore.getStatoErrore().toString());
        aggiungiCardConIdCard(html, "Simulazione automatica", "simulazioneAutomatica", "simulazioneCard", "FERMA");
        html.append("</div>");

        html.append("<div class=\"quick-controls\">");
        html.append("<h2>Comandi rapidi</h2>");

        html.append("<div id=\"comandiAutomatici\">");
        html.append("<button id=\"btnAvviaSimulazione\" class=\"playButton\" type=\"button\" onclick=\"eseguiAzione('avviaSimulazione')\">Avvia simulazione</button>");
        html.append("<button id=\"btnFermaSimulazione\" class=\"stopButton\" type=\"button\" onclick=\"eseguiAzione('fermaSimulazione')\">Ferma simulazione</button>");
        html.append("<button id=\"btnResetAutomatico\" type=\"button\" onclick=\"eseguiAzione('reset')\">Reset</button>");
        html.append("</div>");

        html.append("<div id=\"comandiManuali\" style=\"display: none;\">");
        html.append("<button id=\"btnPasso\" class=\"primary\" type=\"button\" onclick=\"eseguiAzione('passo')\">Esegui passo</button>");
        html.append("<button id=\"btnGuasto\" class=\"dangerButton\" type=\"button\" onclick=\"eseguiAzione('guasto')\">Attiva guasto</button>");
        html.append("<button id=\"btnResetManuale\" type=\"button\" onclick=\"eseguiAzione('reset')\">Reset</button>");
        html.append("</div>");

        html.append("</div>");

        html.append("</div>");

        html.append("</div>");

        html.append("<div class=\"monitor-grid\">");

        html.append("<div class=\"monitor-card\">");
        html.append("<h2>Richieste attive</h2>");
        html.append("<div id=\"richiesteAttive\" class=\"richieste-lista\"></div>");
        html.append("</div>");

        html.append("<div class=\"monitor-card\">");
        html.append("<h2>Ultimi eventi</h2>");
        html.append("<ul id=\"ultimiEventi\" class=\"eventi-lista\"></ul>");
        html.append("</div>");

        html.append("</div>");

        html.append("<div id=\"pannelloManuale\" class=\"mode-panel\">");

        html.append("<div class=\"section\">");
        html.append("<h2>Richiesta interna</h2>");
        html.append("<label for=\"pianoInterno\">Piano:</label>");
        html.append("<input id=\"pianoInterno\" type=\"number\" min=\"-1\" max=\"4\" value=\"0\">");
        html.append("<button id=\"btnRichiestaInterna\" class=\"primary\" type=\"button\" onclick=\"richiestaInterna()\">Invia richiesta interna</button>");

        html.append("<div class=\"floor-buttons\">");
        html.append("<span>Rapido: </span>");
        for (int piano = Ascensore.PIANO_MINIMO; piano <= Ascensore.PIANO_MASSIMO; piano++) {
            html.append("<button id=\"btnInterno")
                    .append(pianoToId(piano))
                    .append("\" type=\"button\" onclick=\"eseguiAzione('richiestaInterna', {piano: ")
                    .append(piano)
                    .append("})\">")
                    .append(piano)
                    .append("</button>");
        }
        html.append("</div>");
        html.append("</div>");

        html.append("<div class=\"section\">");
        html.append("<h2>Chiamata esterna</h2>");

        html.append("<label for=\"pianoSalita\">Piano salita:</label>");
        html.append("<input id=\"pianoSalita\" type=\"number\" min=\"-1\" max=\"4\" value=\"0\">");
        html.append("<button id=\"btnChiamataSalita\" type=\"button\" onclick=\"chiamataSalita()\">Chiamata salita</button>");

        html.append("<br>");

        html.append("<label for=\"pianoDiscesa\">Piano discesa:</label>");
        html.append("<input id=\"pianoDiscesa\" type=\"number\" min=\"-1\" max=\"4\" value=\"0\">");
        html.append("<button id=\"btnChiamataDiscesa\" type=\"button\" onclick=\"chiamataDiscesa()\">Chiamata discesa</button>");
        html.append("</div>");

        html.append("<div class=\"section\">");
        html.append("<h2>Persone</h2>");
        html.append("<label for=\"personeEntrate\">Entrate:</label>");
        html.append("<input id=\"personeEntrate\" type=\"number\" value=\"0\">");
        html.append("<label for=\"personeUscite\">Uscite:</label>");
        html.append("<input id=\"personeUscite\" type=\"number\" value=\"0\">");
        html.append("<button id=\"btnAggiornaPersone\" type=\"button\" onclick=\"aggiornaPersone()\">Aggiorna persone</button>");
        html.append("</div>");

        html.append("</div>");

        html.append("<div id=\"messaggio\"></div>");

        html.append("</div>");

        html.append("<script>");

        html.append("async function eseguiAzione(azione, dati) {");
        html.append("  dati = dati || {};");
        html.append("  const formData = new URLSearchParams();");
        html.append("  formData.append('azione', azione);");
        html.append("  for (const chiave in dati) { formData.append(chiave, dati[chiave]); }");
        html.append("  try {");
        html.append("    const risposta = await fetch('/azione', {");
        html.append("      method: 'POST',");
        html.append("      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },");
        html.append("      body: formData.toString()");
        html.append("    });");
        html.append("    const datiRisposta = await risposta.json();");
        html.append("    if (!risposta.ok) {");
        html.append("      mostraMessaggio(datiRisposta.errore || 'Errore durante esecuzione azione', true);");
        html.append("      return false;");
        html.append("    }");
        html.append("    aggiornaStato(datiRisposta);");
        html.append("    return true;");
        html.append("  } catch (errore) {");
        html.append("    mostraMessaggio('Errore di comunicazione con il server', true);");
        html.append("    return false;");
        html.append("  }");
        html.append("}");

        html.append("async function caricaStato() {");
        html.append("  try {");
        html.append("    const risposta = await fetch('/stato');");
        html.append("    const stato = await risposta.json();");
        html.append("    aggiornaStato(stato);");
        html.append("  } catch (errore) {");
        html.append("    mostraMessaggio('Impossibile aggiornare lo stato', true);");
        html.append("  }");
        html.append("}");

        html.append("function aggiornaStato(stato) {");
        html.append("  impostaTesto('pianoCorrente', stato.pianoCorrente);");
        html.append("  impostaTesto('timer', stato.timer);");
        html.append("  impostaTesto('numeroPersone', stato.numeroPersone);");
        html.append("  impostaTesto('statoCabina', stato.statoCabina);");
        html.append("  impostaTesto('statoPorte', stato.statoPorte);");
        html.append("  impostaTesto('direzione', stato.direzione);");
        html.append("  impostaTesto('statoErrore', stato.statoErrore);");
        html.append("  impostaTesto('simulazioneAutomatica', stato.simulazioneAutomaticaAttiva ? 'ATTIVA' : 'FERMA');");
        html.append("  aggiornaEvidenzaErrore(stato.statoErrore);");
        html.append("  aggiornaEvidenzaCabina(stato.statoCabina);");
        html.append("  aggiornaEvidenzaPorte(stato.statoPorte);");
        html.append("  aggiornaEvidenzaSimulazione(stato.simulazioneAutomaticaAttiva);");
        html.append("  aggiornaVistaAscensore(stato);");
        html.append("  aggiornaRichiesteVisuali(stato);");
        html.append("  aggiornaUltimiEventi(stato);");
        html.append("  aggiornaDisponibilitaModalita(stato);");
        html.append("}");

        html.append("function impostaTesto(id, valore) {");
        html.append("  document.getElementById(id).textContent = valore;");
        html.append("}");

        html.append("function aggiornaVistaAscensore(stato) {");
        html.append("  const cabina = document.getElementById('cabinaAscensore');");
        html.append("  const cabinaInfo = document.getElementById('cabinaInfo');");
        html.append("  const contenitorePersone = document.getElementById('cabinaPersone');");

        html.append("  const altezzaPiano = 70;");
        html.append("  const offsetBase = 7;");
        html.append("  const indicePiano = stato.pianoCorrente - (-1);");
        html.append("  const bottom = offsetBase + indicePiano * altezzaPiano;");
        html.append("  cabina.style.bottom = bottom + 'px';");

        html.append("  cabinaInfo.textContent = 'P' + stato.pianoCorrente;");

        html.append("  cabina.className = 'cabina';");
        html.append("  if (stato.statoCabina === 'IN_MOVIMENTO') {");
        html.append("    cabina.classList.add('in-movimento');");
        html.append("  }");
        html.append("  if (stato.statoCabina === 'BLOCCATA' || stato.statoErrore === 'GUASTO' || stato.statoErrore === 'OVERLOAD') {");
        html.append("    cabina.classList.add('bloccata');");
        html.append("  }");
        html.append("  if (stato.statoPorte === 'APERTE') {");
        html.append("    cabina.classList.add('porte-aperte');");
        html.append("  }");

        html.append("  contenitorePersone.innerHTML = '';");
        html.append("  for (let i = 0; i < stato.numeroPersone; i++) {");
        html.append("    const persona = document.createElement('span');");
        html.append("    persona.className = 'mini-persona';");
        html.append("    if (stato.statoErrore === 'OVERLOAD') {");
        html.append("      persona.classList.add('overload');");
        html.append("    }");
        html.append("    contenitorePersone.appendChild(persona);");
        html.append("  }");

        html.append("  evidenziaPianoCorrente(stato.pianoCorrente);");
        html.append("}");

        html.append("function aggiornaRichiesteVisuali(stato) {");
        html.append("  const contenitore = document.getElementById('richiesteAttive');");
        html.append("  contenitore.innerHTML = '';");
        html.append("  for (let i = 0; i < stato.richiesteVisuali.length; i++) {");
        html.append("    const richiesta = stato.richiesteVisuali[i];");
        html.append("    const riga = document.createElement('div');");
        html.append("    riga.className = richiesta.attiva ? 'richiesta-riga attiva' : 'richiesta-riga';");
        html.append("    const piano = document.createElement('span');");
        html.append("    piano.className = 'richiesta-piano';");
        html.append("    piano.textContent = 'Piano ' + richiesta.piano;");
        html.append("    const badge = document.createElement('span');");
        html.append("    badge.className = richiesta.attiva ? 'badge-richiesta attiva' : 'badge-richiesta';");
        html.append("    badge.textContent = richiesta.attiva ? richiesta.tipo : 'Nessuna';");
        html.append("    riga.appendChild(piano);");
        html.append("    riga.appendChild(badge);");
        html.append("    contenitore.appendChild(riga);");
        html.append("  }");
        html.append("}");

        html.append("function aggiornaUltimiEventi(stato) {");
        html.append("  const lista = document.getElementById('ultimiEventi');");
        html.append("  lista.innerHTML = '';");
        html.append("  if (!stato.ultimiEventi || stato.ultimiEventi.length === 0) {");
        html.append("    const elemento = document.createElement('li');");
        html.append("    elemento.className = 'vuoto';");
        html.append("    elemento.textContent = 'Nessun evento';");
        html.append("    lista.appendChild(elemento);");
        html.append("    return;");
        html.append("  }");
        html.append("  for (let i = 0; i < stato.ultimiEventi.length; i++) {");
        html.append("    const testoEvento = stato.ultimiEventi[i];");
        html.append("    const elemento = document.createElement('li');");
        html.append("    elemento.className = classificaEvento(testoEvento);");
        html.append("    const matchOrario = testoEvento.match(/^\\[(.*?)\\]\\s*(.*)$/);");
        html.append("    if (matchOrario) {");
        html.append("      const orario = document.createElement('span');");
        html.append("      orario.className = 'evento-orario';");
        html.append("      orario.textContent = '[' + matchOrario[1] + ']';");
        html.append("      const messaggio = document.createElement('span');");
        html.append("      messaggio.textContent = ' ' + matchOrario[2];");
        html.append("      elemento.appendChild(orario);");
        html.append("      elemento.appendChild(messaggio);");
        html.append("    } else {");
        html.append("      elemento.textContent = testoEvento;");
        html.append("    }");
        html.append("    lista.appendChild(elemento);");
        html.append("  }");
        html.append("}");

        html.append("function classificaEvento(evento) {");
        html.append("  const testo = evento.toLowerCase();");
        html.append("  if (testo.includes('guasto') || testo.includes('overload') || testo.includes('ignorata')) {");
        html.append("    return 'evento-errore';");
        html.append("  }");
        html.append("  if (testo.includes('entrate')) {");
        html.append("    return 'evento-warning';");
        html.append("  }");
        html.append("  if (testo.includes('richiesta') || testo.includes('chiamata')) {");
        html.append("    return 'evento-info';");
        html.append("  }");
        html.append("  if (testo.includes('servito') || testo.includes('uscite') || testo.includes('inizializzato') || testo.includes('simulazione')) {");
        html.append("    return 'evento-successo';");
        html.append("  }");
        html.append("  return '';");
        html.append("}");

        html.append("function evidenziaPianoCorrente(pianoCorrente) {");
        html.append("  const ids = ['M1', '0', '1', '2', '3', '4'];");
        html.append("  for (let i = 0; i < ids.length; i++) {");
        html.append("    const riga = document.getElementById('pianoRiga' + ids[i]);");
        html.append("    if (riga) {");
        html.append("      riga.classList.remove('highlight-piano');");
        html.append("    }");
        html.append("  }");
        html.append("  let id = pianoCorrente < 0 ? 'M' + Math.abs(pianoCorrente) : '' + pianoCorrente;");
        html.append("  const corrente = document.getElementById('pianoRiga' + id);");
        html.append("  if (corrente) {");
        html.append("    corrente.classList.add('highlight-piano');");
        html.append("  }");
        html.append("}");

        html.append("function aggiornaEvidenzaErrore(statoErrore) {");
        html.append("  const card = document.getElementById('statoErroreCard');");
        html.append("  card.className = 'card';");
        html.append("  if (statoErrore === 'OVERLOAD') { card.classList.add('danger'); }");
        html.append("  else if (statoErrore === 'GUASTO') { card.classList.add('danger'); }");
        html.append("  else { card.classList.add('success'); }");
        html.append("}");

        html.append("function aggiornaEvidenzaCabina(statoCabina) {");
        html.append("  const card = document.getElementById('statoCabina').parentElement;");
        html.append("  card.className = 'card';");
        html.append("  if (statoCabina === 'IN_MOVIMENTO') { card.classList.add('warning'); }");
        html.append("  else if (statoCabina === 'BLOCCATA') { card.classList.add('danger'); }");
        html.append("}");

        html.append("function aggiornaEvidenzaPorte(statoPorte) {");
        html.append("  const card = document.getElementById('statoPorte').parentElement;");
        html.append("  card.className = 'card';");
        html.append("  if (statoPorte === 'APERTE') { card.classList.add('warning'); }");
        html.append("}");

        html.append("function aggiornaEvidenzaSimulazione(attiva) {");
        html.append("  const card = document.getElementById('simulazioneCard');");
        html.append("  card.className = 'card';");
        html.append("  if (attiva) { card.classList.add('success'); }");
        html.append("}");

        html.append("function aggiornaDisponibilitaModalita(stato) {");
        html.append("  const btnManuale = document.getElementById('btnModalitaManuale');");
        html.append("  btnManuale.disabled = stato.simulazioneAutomaticaAttiva;");
        html.append("  if (stato.simulazioneAutomaticaAttiva) {");
        html.append("    btnManuale.title = 'Ferma la simulazione automatica prima di passare al controllo manuale';");
        html.append("  } else {");
        html.append("    btnManuale.title = '';");
        html.append("  }");
        html.append("}");

        html.append("async function mostraModalita(modalita) {");
        html.append("  const manuale = document.getElementById('pannelloManuale');");
        html.append("  const btnAutomatico = document.getElementById('btnModalitaAutomatica');");
        html.append("  const btnManuale = document.getElementById('btnModalitaManuale');");
        html.append("  const comandiAutomatici = document.getElementById('comandiAutomatici');");
        html.append("  const comandiManuali = document.getElementById('comandiManuali');");

        html.append("  if (modalita === 'automatica') {");
        html.append("    if (btnManuale.classList.contains('active')) {");
        html.append("      const resetOk = await eseguiAzione('reset');");
        html.append("      if (!resetOk) {");
        html.append("        return;");
        html.append("      }");
        html.append("    }");
        html.append("    manuale.classList.remove('active');");
        html.append("    btnAutomatico.classList.add('active');");
        html.append("    btnManuale.classList.remove('active');");
        html.append("    comandiAutomatici.style.display = 'block';");
        html.append("    comandiManuali.style.display = 'none';");
        html.append("  } else {");
        html.append("    if (btnManuale.disabled) {");
        html.append("      mostraMessaggio('Ferma la simulazione automatica prima di passare al controllo manuale', true);");
        html.append("      return;");
        html.append("    }");
        html.append("    if (btnAutomatico.classList.contains('active')) {");
        html.append("      const resetOk = await eseguiAzione('reset');");
        html.append("      if (!resetOk) {");
        html.append("        return;");
        html.append("      }");
        html.append("    }");
        html.append("    manuale.classList.add('active');");
        html.append("    btnManuale.classList.add('active');");
        html.append("    btnAutomatico.classList.remove('active');");
        html.append("    comandiAutomatici.style.display = 'none';");
        html.append("    comandiManuali.style.display = 'block';");
        html.append("  }");
        html.append("}");

        html.append("function richiestaInterna() {");
        html.append("  const inputPiano = document.getElementById('pianoInterno');");
        html.append("  const piano = inputPiano.value;");
        html.append("  eseguiAzione('richiestaInterna', { piano: piano });");
        html.append("  inputPiano.value = 0;");
        html.append("}");

        html.append("function chiamataSalita() {");
        html.append("  const inputPiano = document.getElementById('pianoSalita');");
        html.append("  const piano = inputPiano.value;");
        html.append("  eseguiAzione('chiamataSalita', { piano: piano });");
        html.append("  inputPiano.value = 0;");
        html.append("}");

        html.append("function chiamataDiscesa() {");
        html.append("  const inputPiano = document.getElementById('pianoDiscesa');");
        html.append("  const piano = inputPiano.value;");
        html.append("  eseguiAzione('chiamataDiscesa', { piano: piano });");
        html.append("  inputPiano.value = 0;");
        html.append("}");

        html.append("function aggiornaPersone() {");
        html.append("  const inputEntrate = document.getElementById('personeEntrate');");
        html.append("  const inputUscite = document.getElementById('personeUscite');");
        html.append("  const entrate = inputEntrate.value;");
        html.append("  const uscite = inputUscite.value;");
        html.append("  eseguiAzione('persone', { personeEntrate: entrate, personeUscite: uscite });");
        html.append("  inputEntrate.value = 0;");
        html.append("  inputUscite.value = 0;");
        html.append("}");

        html.append("function mostraMessaggio(testo, errore) {");
        html.append("  const messaggio = document.getElementById('messaggio');");
        html.append("  messaggio.textContent = testo;");
        html.append("  messaggio.style.color = errore ? '#b00020' : '#2e7d32';");
        html.append("}");

        html.append("caricaStato();");
        html.append("setInterval(caricaStato, 1000);");

        html.append("</script>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private void aggiungiCard(StringBuilder html, String label, String id, String valore) {
        aggiungiCardConIdCard(html, label, id, null, valore);
    }

    private void aggiungiCardConIdCard(StringBuilder html, String label, String id, String idCard, String valore) {
        html.append("<div class=\"card\"");

        if (idCard != null) {
            html.append(" id=\"").append(idCard).append("\"");
        }

        html.append(">");

        html.append("<span class=\"label\">").append(escapeHtml(label)).append("</span>");
        html.append("<span class=\"value\" id=\"").append(id).append("\">").append(escapeHtml(valore)).append("</span>");
        html.append("</div>");
    }

    private String pianoToId(int piano) {
        if (piano < 0) {
            return "M" + Math.abs(piano);
        }

        return String.valueOf(piano);
    }

    private String escapeHtml(String testo) {
        if (testo == null) {
            return "";
        }

        return testo
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String escapeJson(String testo) {
        if (testo == null) {
            return "";
        }

        return testo
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}