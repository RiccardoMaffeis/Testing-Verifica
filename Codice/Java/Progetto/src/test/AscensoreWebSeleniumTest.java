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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import web.AscensoreHttpServer;

public class AscensoreWebSeleniumTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final String AZIONE_URL = BASE_URL + "/azione";

    private static final String CHROME_DRIVER_ENV = "CHROMEDRIVER_PATH";
    private static final String CHROME_DRIVER_PROPERTY = "webdriver.chrome.driver";
    private static final String DEFAULT_CHROME_DRIVER_PATH = "drivers/chromedriver.exe";

    private static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    private static final long ATTESA_MASSIMA_SECONDI = 5;

    private static AscensoreHttpServer server;
    private static WebDriver driver;
    private static WebDriverWait wait;

    @BeforeAll
    public static void setup() throws Exception {
        server = new AscensoreHttpServer();
        server.avvia();

        configuraChromeDriver();
        avviaBrowserHeadless();

        wait = new WebDriverWait(driver, ATTESA_MASSIMA_SECONDI);
    }

    @BeforeEach
    public void resetPrimaDiOgniTest() throws Exception {
        inviaReset();
        driver.get(BASE_URL);

        attendiElementoVisibile(By.tagName("h1"));
        attendiTesto(By.id("statoErrore"), "NESSUNO");
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

    private static WebElement attendiElementoVisibile(By selettore) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(selettore));
    }

    private static WebElement attendiElementoCliccabile(By selettore) {
        return wait.until(ExpectedConditions.elementToBeClickable(selettore));
    }

    private static void attendiTesto(By selettore, String testoAtteso) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(selettore, testoAtteso));
    }

    private static void attendiTestoContenuto(By selettore, String testoAtteso) {
        wait.until(driverCorrente ->
                driverCorrente.findElement(selettore).getText().contains(testoAtteso)
        );
    }

    private static void attendiClasseContenente(By selettore, String classeAttesa) {
        wait.until(driverCorrente -> {
            String classi = driverCorrente.findElement(selettore).getAttribute("class");
            return classi != null && classi.contains(classeAttesa);
        });
    }

    private static void attendiValoreInput(By selettore, String valoreAtteso) {
        wait.until(driverCorrente ->
                valoreAtteso.equals(driverCorrente.findElement(selettore).getAttribute("value"))
        );
    }

    private static void attendiBottoneDisabilitato(By selettore) {
        wait.until(driverCorrente ->
                !driverCorrente.findElement(selettore).isEnabled()
        );
    }

    private static void attendiBottoneAbilitato(By selettore) {
        wait.until(driverCorrente ->
                driverCorrente.findElement(selettore).isEnabled()
        );
    }

    private static void passaAModalitaManuale() {
        attendiElementoCliccabile(By.id("btnModalitaManuale")).click();

        attendiClasseContenente(By.id("pannelloManuale"), "active");
        attendiClasseContenente(By.id("btnModalitaManuale"), "active");
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
    public void passaggioAControlloManualeMostraPannelloManuale() {
        passaAModalitaManuale();

        WebElement pannelloManuale = driver.findElement(By.id("pannelloManuale"));

        assertTrue(pannelloManuale.getAttribute("class").contains("active"));
        assertTrue(driver.findElement(By.id("btnModalitaManuale")).getAttribute("class").contains("active"));
    }

    @Test
    public void richiestaInternaManualeAggiornaRichiesteAttive() {
        passaAModalitaManuale();

        WebElement inputPiano = attendiElementoVisibile(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("2");

        attendiElementoCliccabile(By.id("btnRichiestaInterna")).click();

        attendiTestoContenuto(By.id("richiesteAttive"), "Piano 2");
        attendiTestoContenuto(By.id("richiesteAttive"), "Interna");

        String richiesteAttive = driver.findElement(By.id("richiesteAttive")).getText();

        assertTrue(richiesteAttive.contains("Piano 2"));
        assertTrue(richiesteAttive.contains("Interna"));
    }

    @Test
    public void inputManualiVengonoResettatiDopoInvio() {
        passaAModalitaManuale();

        WebElement inputPiano = attendiElementoVisibile(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("3");

        attendiElementoCliccabile(By.id("btnRichiestaInterna")).click();

        attendiValoreInput(By.id("pianoInterno"), "0");

        assertEquals("0", driver.findElement(By.id("pianoInterno")).getAttribute("value"));
    }

    @Test
    public void simulazioneAutomaticaDisabilitaControlloManuale() {
        attendiElementoCliccabile(By.id("btnAvviaSimulazione")).click();

        attendiTesto(By.id("simulazioneAutomatica"), "ATTIVA");

        WebElement btnManuale = driver.findElement(By.id("btnModalitaManuale"));
        attendiBottoneDisabilitato(By.id("btnModalitaManuale"));

        assertFalse(btnManuale.isEnabled());

        attendiElementoCliccabile(By.id("btnFermaSimulazione")).click();

        attendiTesto(By.id("simulazioneAutomatica"), "FERMA");
        attendiBottoneAbilitato(By.id("btnModalitaManuale"));

        assertTrue(driver.findElement(By.id("btnModalitaManuale")).isEnabled());
    }

    @Test
    public void guastoManualeAggiornaStatoErrore() {
        passaAModalitaManuale();

        attendiElementoCliccabile(By.id("btnGuasto")).click();

        attendiTesto(By.id("statoErrore"), "GUASTO");
        attendiTestoContenuto(By.id("ultimiEventi"), "Guasto attivato manualmente");

        assertEquals("GUASTO", driver.findElement(By.id("statoErrore")).getText());

        String log = driver.findElement(By.id("ultimiEventi")).getText();
        assertTrue(log.contains("Guasto attivato manualmente"));
    }

    @Test
    public void overloadManualeAggiornaStatoErrore() {
        passaAModalitaManuale();

        WebElement inputPiano = attendiElementoVisibile(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("0");

        attendiElementoCliccabile(By.id("btnRichiestaInterna")).click();

        attendiTesto(By.id("statoPorte"), "APERTE");

        assertEquals("APERTE", driver.findElement(By.id("statoPorte")).getText());

        WebElement personeEntrate = attendiElementoVisibile(By.id("personeEntrate"));
        personeEntrate.clear();
        personeEntrate.sendKeys("9");

        attendiElementoCliccabile(By.id("btnAggiornaPersone")).click();

        attendiTesto(By.id("statoErrore"), "OVERLOAD");
        attendiTesto(By.id("numeroPersone"), "9");
        attendiTestoContenuto(By.id("ultimiEventi"), "Entrate 9 persone");

        assertEquals("OVERLOAD", driver.findElement(By.id("statoErrore")).getText());
        assertEquals("9", driver.findElement(By.id("numeroPersone")).getText());

        String log = driver.findElement(By.id("ultimiEventi")).getText();
        assertTrue(log.contains("Entrate 9 persone"));
    }
}