package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import web.AscensoreHttpServer;

public class AscensoreWebSeleniumTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AZIONE_URL = BASE_URL + "/azione";

    private static final String CHROME_DRIVER_ENV = "CHROMEDRIVER_PATH";
    private static final String CHROME_DRIVER_PROPERTY = "webdriver.chrome.driver";
    private static final String DEFAULT_CHROME_DRIVER_PATH = "drivers/chromedriver.exe";

    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    private static final int ATTESA_BREVE_MS = 300;
    private static final int ATTESA_MEDIA_MS = 500;
    private static final int ATTESA_SIMULAZIONE_MS = 1200;
    private static final int ATTESA_STOP_SIMULAZIONE_MS = 700;

    private static AscensoreHttpServer server;
    private static WebDriver driver;

    @BeforeAll
    public static void setup() throws Exception {
        server = new AscensoreHttpServer();
        server.avvia();

        configuraChromeDriver();
        avviaBrowserHeadless();
    }

    @BeforeEach
    public void resetPrimaDiOgniTest() throws Exception {
        inviaReset();
        driver.get(BASE_URL);
        Thread.sleep(ATTESA_BREVE_MS);
    }

    @AfterAll
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }

        if (server != null) {
            server.ferma();
            server = null;
        }
    }

    private static void configuraChromeDriver() {
        String chromeDriverPath = System.getenv(CHROME_DRIVER_ENV);

        if (chromeDriverPath == null || chromeDriverPath.trim().isEmpty()) {
            chromeDriverPath = DEFAULT_CHROME_DRIVER_PATH;
        }

        System.setProperty(CHROME_DRIVER_PROPERTY, chromeDriverPath);
    }

    private static void avviaBrowserHeadless() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1280,900");

        driver = new ChromeDriver(options);
    }

    private static void inviaReset() throws Exception {
        HttpURLConnection connessione =
                (HttpURLConnection) URI.create(AZIONE_URL).toURL().openConnection();

        connessione.setRequestMethod("POST");
        connessione.setDoOutput(true);
        connessione.setRequestProperty("Content-Type", CONTENT_TYPE_FORM);

        String body = "azione=reset";

        try (OutputStream output = connessione.getOutputStream()) {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        }

        connessione.getResponseCode();
        connessione.disconnect();
    }

    @Test
    public void paginaInizialeMostraStatoAscensore() {
        assertEquals("Simulatore Ascensore", driver.findElement(By.tagName("h1")).getText());

        assertEquals("0", driver.findElement(By.id("pianoCorrente")).getText());
        assertEquals("0", driver.findElement(By.id("numeroPersone")).getText());
        assertEquals("CHIUSE", driver.findElement(By.id("statoPorte")).getText());
        assertEquals("NESSUNA", driver.findElement(By.id("direzione")).getText());
        assertEquals("NESSUNO", driver.findElement(By.id("statoErrore")).getText());
    }

    @Test
    public void passaggioAControlloManualeMostraPannelloManuale() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        WebElement pannelloManuale = driver.findElement(By.id("pannelloManuale"));

        assertTrue(pannelloManuale.getAttribute("class").contains("active"));
        assertTrue(driver.findElement(By.id("btnModalitaManuale")).getAttribute("class").contains("active"));
    }

    @Test
    public void richiestaInternaManualeAggiornaRichiesteAttive() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        WebElement inputPiano = driver.findElement(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("2");

        driver.findElement(By.id("btnRichiestaInterna")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        String richiesteAttive = driver.findElement(By.id("richiesteAttive")).getText();

        assertTrue(richiesteAttive.contains("Piano 2"));
        assertTrue(richiesteAttive.contains("Interna"));
    }

    @Test
    public void inputManualiVengonoResettatiDopoInvio() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        WebElement inputPiano = driver.findElement(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("3");

        driver.findElement(By.id("btnRichiestaInterna")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        assertEquals("0", driver.findElement(By.id("pianoInterno")).getAttribute("value"));
    }

    @Test
    public void simulazioneAutomaticaDisabilitaControlloManuale() throws InterruptedException {
        driver.findElement(By.id("btnAvviaSimulazione")).click();

        Thread.sleep(ATTESA_SIMULAZIONE_MS);

        assertEquals("ATTIVA", driver.findElement(By.id("simulazioneAutomatica")).getText());

        WebElement btnManuale = driver.findElement(By.id("btnModalitaManuale"));
        assertFalse(btnManuale.isEnabled());

        driver.findElement(By.id("btnFermaSimulazione")).click();

        Thread.sleep(ATTESA_STOP_SIMULAZIONE_MS);

        assertEquals("FERMA", driver.findElement(By.id("simulazioneAutomatica")).getText());

        assertTrue(driver.findElement(By.id("btnModalitaManuale")).isEnabled());
    }

    @Test
    public void guastoManualeAggiornaStatoErrore() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        driver.findElement(By.id("btnGuasto")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        assertEquals("GUASTO", driver.findElement(By.id("statoErrore")).getText());

        String log = driver.findElement(By.id("ultimiEventi")).getText();
        assertTrue(log.contains("Guasto attivato manualmente"));
    }

    @Test
    public void overloadManualeAggiornaStatoErrore() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        WebElement inputPiano = driver.findElement(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("0");

        driver.findElement(By.id("btnRichiestaInterna")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        assertEquals("APERTE", driver.findElement(By.id("statoPorte")).getText());

        WebElement personeEntrate = driver.findElement(By.id("personeEntrate"));
        personeEntrate.clear();
        personeEntrate.sendKeys("9");

        driver.findElement(By.id("btnAggiornaPersone")).click();

        Thread.sleep(ATTESA_MEDIA_MS);

        assertEquals("OVERLOAD", driver.findElement(By.id("statoErrore")).getText());
        assertEquals("9", driver.findElement(By.id("numeroPersone")).getText());

        String log = driver.findElement(By.id("ultimiEventi")).getText();
        assertTrue(log.contains("Entrate 9 persone"));
    }
}