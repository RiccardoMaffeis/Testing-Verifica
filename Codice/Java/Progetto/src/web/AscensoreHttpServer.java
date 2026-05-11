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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    private static final String CONTENT_TYPE_CSS = "text/css; charset=UTF-8";
    private static final String CONTENT_TYPE_JS = "application/javascript; charset=UTF-8";
    private static final String ENCODING_UTF_8 = "UTF-8";

    private static final Path WEBAPP_DIR = Paths.get("webapp");
    private static final Path INDEX_HTML = WEBAPP_DIR.resolve("index.html");
    private static final Path STYLE_CSS = WEBAPP_DIR.resolve("style.css");
    private static final Path SCRIPT_JS = WEBAPP_DIR.resolve("script.js");

    private Ascensore ascensore;
    private ControlloreAscensore controllore;
    private HttpServer server;

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

    public void avvia() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORTA), 0);

        server.createContext("/", this::gestisciPagina);
        server.createContext("/index.html", this::gestisciPagina);
        server.createContext("/style.css", exchange ->
                gestisciFileStatico(exchange, STYLE_CSS, CONTENT_TYPE_CSS));
        server.createContext("/script.js", exchange ->
                gestisciFileStatico(exchange, SCRIPT_JS, CONTENT_TYPE_JS));
        server.createContext("/stato", this::gestisciStato);
        server.createContext("/azione", this::gestisciAzione);

        server.setExecutor(null);
        server.start();

        System.out.println("Server avviato su http://localhost:" + PORTA);
    }

    public void ferma() {
        fermaSimulazioneAutomatica();

        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private void gestisciPagina(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (!"/".equals(path) && !"/index.html".equals(path)) {
            inviaRisposta(exchange, 404, CONTENT_TYPE_JSON,
                    "{\"errore\":\"Risorsa non trovata\"}");
            return;
        }

        gestisciFileStatico(exchange, INDEX_HTML, CONTENT_TYPE_HTML);
    }

    private void gestisciFileStatico(
            HttpExchange exchange,
            Path file,
            String contentType
    ) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            inviaErroreMetodo(exchange);
            return;
        }

        if (!Files.exists(file)) {
            String messaggio = "File statico non trovato: " + file.toAbsolutePath().normalize();

            inviaRisposta(exchange, 404, CONTENT_TYPE_HTML,
                    "<h1>404 - File statico non trovato</h1><p>"
                            + messaggio
                            + "</p>");
            return;
        }

        byte[] contenuto = Files.readAllBytes(file);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, contenuto.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(contenuto);
        }
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
                if (gestisciAzioneDiSistema(exchange, azione)) {
                    return;
                }

                InputAscensore input = costruisciInputDaAzione(azione, parametri);
                registraEventoAzioneManuale(azione, parametri, input);

                controllore.eseguiPasso(input);
                aggiornaRichiesteServite();

                inviaJson(exchange, statoJson());
            }
        } catch (RuntimeException e) {
            inviaJsonErrore(exchange, e.getMessage());
        }
    }

    private boolean gestisciAzioneDiSistema(HttpExchange exchange, String azione) throws IOException {
        if ("reset".equals(azione)) {
            fermaSimulazioneAutomatica();
            resettaAscensore();
            inviaJson(exchange, statoJson());
            return true;
        }

        if ("avviaSimulazione".equals(azione)) {
            avviaSimulazioneAutomatica();
            aggiungiEvento("Simulazione automatica avviata");
            inviaJson(exchange, statoJson());
            return true;
        }

        if ("fermaSimulazione".equals(azione)) {
            fermaSimulazioneAutomatica();
            aggiungiEvento("Simulazione automatica fermata");
            inviaJson(exchange, statoJson());
            return true;
        }

        return false;
    }

    private InputAscensore costruisciInputDaAzione(String azione, Map<String, String> parametri) {
        InputAscensore input = new InputAscensore();

        if ("richiestaInterna".equals(azione)) {
            input.setRichiestaInterna(leggiIntero(parametri, "piano", 0));
        } else if ("chiamataSalita".equals(azione)) {
            input.setChiamataSalita(leggiIntero(parametri, "piano", 0));
        } else if ("chiamataDiscesa".equals(azione)) {
            input.setChiamataDiscesa(leggiIntero(parametri, "piano", 0));
        } else if ("persone".equals(azione)) {
            input.setPersoneEntrate(leggiIntero(parametri, "personeEntrate", 0));
            input.setPersoneUscite(leggiIntero(parametri, "personeUscite", 0));
        } else if ("guasto".equals(azione)) {
            input.setEventoGuasto(true);
        }

        return input;
    }

    private void registraEventoAzioneManuale(
            String azione,
            Map<String, String> parametri,
            InputAscensore input
    ) {
        if ("richiestaInterna".equals(azione)) {
            registraEventoRichiestaManuale(
                    leggiIntero(parametri, "piano", 0),
                    "Interna"
            );
        } else if ("chiamataSalita".equals(azione)) {
            registraEventoRichiestaManuale(
                    leggiIntero(parametri, "piano", 0),
                    "Salita"
            );
        } else if ("chiamataDiscesa".equals(azione)) {
            registraEventoRichiestaManuale(
                    leggiIntero(parametri, "piano", 0),
                    "Discesa"
            );
        } else if ("persone".equals(azione)) {
            registraEventoPersone(
                    input.getPersoneEntrate(),
                    input.getPersoneUscite()
            );
        } else if ("guasto".equals(azione)) {
            aggiungiEvento("Guasto attivato manualmente");
        }
    }

    private void registraEventoRichiestaManuale(int piano, String tipoRichiesta) {
        if (ascensore.getStatoErrore() == StatoErrore.GUASTO) {
            aggiungiEvento("Richiesta ignorata: ascensore in guasto");
            return;
        }

        registraRichiestaVisuale(piano, tipoRichiesta);
        aggiungiEvento("Richiesta " + tipoRichiesta.toLowerCase()
                + " inserita al piano " + piano);
    }

    private void registraEventoPersone(int personeEntrate, int personeUscite) {
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

        threadSimulazione = new Thread(() -> {
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
            return URLDecoder.decode(valore, ENCODING_UTF_8);
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

    private void inviaJson(HttpExchange exchange, String json) throws IOException {
        inviaRisposta(exchange, 200, CONTENT_TYPE_JSON, json);
    }

    private void inviaJsonErrore(HttpExchange exchange, String messaggio) throws IOException {
        String json = "{"
                + "\"errore\":\"" + escapeJson(messaggio) + "\""
                + "}";

        inviaRisposta(exchange, 500, CONTENT_TYPE_JSON, json);
    }

    private void inviaErroreMetodo(HttpExchange exchange) throws IOException {
        String json = "{\"errore\":\"Metodo HTTP non supportato\"}";
        inviaRisposta(exchange, 405, CONTENT_TYPE_JSON, json);
    }

    private void inviaRisposta(
            HttpExchange exchange,
            int codiceStato,
            String contentType,
            String contenuto
    ) throws IOException {
        byte[] risposta = contenuto.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(codiceStato, risposta.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(risposta);
        }
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