package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import web.AscensoreHttpServer;

public class AscensoreWebSeleniumTest {

    private static AscensoreHttpServer server;
    private static WebDriver driver;

    @BeforeClass
    public static void setup() throws Exception {
        server = new AscensoreHttpServer();
        server.avvia();

        System.setProperty("webdriver.chrome.driver", "drivers/chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1280,900");

        driver = new ChromeDriver(options);
    }

    @Before
    public void resetPrimaDiOgniTest() throws Exception {
        inviaReset();
        driver.get("http://localhost:8080");
        Thread.sleep(300);
    }

    @AfterClass
    public static void tearDown() {
        if (driver != null) {
            driver.quit();
        }

        if (server != null) {
            server.ferma();
        }
    }

    private static void inviaReset() throws Exception {
        URL url = new URL("http://localhost:8080/azione");
        HttpURLConnection connessione = (HttpURLConnection) url.openConnection();

        connessione.setRequestMethod("POST");
        connessione.setDoOutput(true);
        connessione.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "azione=reset";

        try (OutputStream output = connessione.getOutputStream()) {
            output.write(body.getBytes("UTF-8"));
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

        Thread.sleep(500);

        WebElement pannelloManuale = driver.findElement(By.id("pannelloManuale"));

        assertTrue(pannelloManuale.getAttribute("class").contains("active"));
        assertTrue(driver.findElement(By.id("btnModalitaManuale")).getAttribute("class").contains("active"));
    }

    @Test
    public void richiestaInternaManualeAggiornaRichiesteAttive() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(500);

        WebElement inputPiano = driver.findElement(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("2");

        driver.findElement(By.id("btnRichiestaInterna")).click();

        Thread.sleep(500);

        String richiesteAttive = driver.findElement(By.id("richiesteAttive")).getText();

        assertTrue(richiesteAttive.contains("Piano 2"));
        assertTrue(richiesteAttive.contains("Interna"));
    }

    @Test
    public void inputManualiVengonoResettatiDopoInvio() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(500);

        WebElement inputPiano = driver.findElement(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("3");

        driver.findElement(By.id("btnRichiestaInterna")).click();

        Thread.sleep(500);

        assertEquals("0", driver.findElement(By.id("pianoInterno")).getAttribute("value"));
    }

    @Test
    public void simulazioneAutomaticaDisabilitaControlloManuale() throws InterruptedException {
        driver.findElement(By.id("btnAvviaSimulazione")).click();

        Thread.sleep(1200);

        assertEquals("ATTIVA", driver.findElement(By.id("simulazioneAutomatica")).getText());

        WebElement btnManuale = driver.findElement(By.id("btnModalitaManuale"));
        assertFalse(btnManuale.isEnabled());

        driver.findElement(By.id("btnFermaSimulazione")).click();

        Thread.sleep(700);

        assertEquals("FERMA", driver.findElement(By.id("simulazioneAutomatica")).getText());

        assertTrue(driver.findElement(By.id("btnModalitaManuale")).isEnabled());
    }

    @Test
    public void guastoManualeAggiornaStatoErrore() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(500);

        driver.findElement(By.id("btnGuasto")).click();

        Thread.sleep(500);

        assertEquals("GUASTO", driver.findElement(By.id("statoErrore")).getText());

        String log = driver.findElement(By.id("ultimiEventi")).getText();
        assertTrue(log.contains("Guasto attivato manualmente"));
    }
    
    @Test
    public void overloadManualeAggiornaStatoErrore() throws InterruptedException {
        driver.findElement(By.id("btnModalitaManuale")).click();

        Thread.sleep(500);

        WebElement inputPiano = driver.findElement(By.id("pianoInterno"));
        inputPiano.clear();
        inputPiano.sendKeys("0");

        driver.findElement(By.id("btnRichiestaInterna")).click();

        Thread.sleep(500);

        assertEquals("APERTE", driver.findElement(By.id("statoPorte")).getText());

        WebElement personeEntrate = driver.findElement(By.id("personeEntrate"));
        personeEntrate.clear();
        personeEntrate.sendKeys("9");

        driver.findElement(By.id("btnAggiornaPersone")).click();

        Thread.sleep(500);

        assertEquals("OVERLOAD", driver.findElement(By.id("statoErrore")).getText());
        assertEquals("9", driver.findElement(By.id("numeroPersone")).getText());

        String log = driver.findElement(By.id("ultimiEventi")).getText();
        assertTrue(log.contains("Entrate 9 persone"));
    }
}