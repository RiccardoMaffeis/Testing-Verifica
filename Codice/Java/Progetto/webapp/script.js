async function eseguiAzione(azione, dati) {
    dati = dati || {};

    const formData = new URLSearchParams();
    formData.append("azione", azione);

    for (const chiave in dati) {
        formData.append(chiave, dati[chiave]);
    }

    try {
        const risposta = await fetch("/azione", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: formData.toString()
        });

        const datiRisposta = await risposta.json();

        if (!risposta.ok) {
            mostraMessaggio(datiRisposta.errore || "Errore durante esecuzione azione", true);
            return false;
        }

        aggiornaStato(datiRisposta);
        return true;
    } catch (errore) {
        mostraMessaggio("Errore di comunicazione con il server", true);
        return false;
    }
}

async function caricaStato() {
    try {
        const risposta = await fetch("/stato");
        const stato = await risposta.json();

        aggiornaStato(stato);
    } catch (errore) {
        mostraMessaggio("Impossibile aggiornare lo stato", true);
    }
}

function aggiornaStato(stato) {
    impostaTesto("pianoCorrente", stato.pianoCorrente);
    impostaTesto("timer", stato.timer);
    impostaTesto("numeroPersone", stato.numeroPersone);
    impostaTesto("statoCabina", stato.statoCabina);
    impostaTesto("statoPorte", stato.statoPorte);
    impostaTesto("direzione", stato.direzione);
    impostaTesto("statoErrore", stato.statoErrore);
    impostaTesto("simulazioneAutomatica", stato.simulazioneAutomaticaAttiva ? "ATTIVA" : "FERMA");

    aggiornaEvidenzaErrore(stato.statoErrore);
    aggiornaEvidenzaCabina(stato.statoCabina);
    aggiornaEvidenzaPorte(stato.statoPorte);
    aggiornaEvidenzaSimulazione(stato.simulazioneAutomaticaAttiva);
    aggiornaVistaAscensore(stato);
    aggiornaRichiesteVisuali(stato);
    aggiornaUltimiEventi(stato);
    aggiornaDisponibilitaModalita(stato);
}

function impostaTesto(id, valore) {
    document.getElementById(id).textContent = valore;
}

function aggiornaVistaAscensore(stato) {
    const cabina = document.getElementById("cabinaAscensore");
    const cabinaInfo = document.getElementById("cabinaInfo");
    const contenitorePersone = document.getElementById("cabinaPersone");

    const altezzaPiano = 70;
    const offsetBase = 7;
    const pianoMinimo = -1;
    const indicePiano = stato.pianoCorrente - pianoMinimo;
    const bottom = offsetBase + indicePiano * altezzaPiano;

    cabina.style.bottom = bottom + "px";
    cabinaInfo.textContent = "P" + stato.pianoCorrente;

    cabina.className = "cabina";

    if (stato.statoCabina === "IN_MOVIMENTO") {
        cabina.classList.add("in-movimento");
    }

    if (
        stato.statoCabina === "BLOCCATA" ||
        stato.statoErrore === "GUASTO" ||
        stato.statoErrore === "OVERLOAD"
    ) {
        cabina.classList.add("bloccata");
    }

    if (stato.statoPorte === "APERTE") {
        cabina.classList.add("porte-aperte");
    }

    contenitorePersone.innerHTML = "";

    for (let i = 0; i < stato.numeroPersone; i++) {
        const persona = document.createElement("span");
        persona.className = "mini-persona";

        if (stato.statoErrore === "OVERLOAD") {
            persona.classList.add("overload");
        }

        contenitorePersone.appendChild(persona);
    }

    evidenziaPianoCorrente(stato.pianoCorrente);
}

function aggiornaRichiesteVisuali(stato) {
    const contenitore = document.getElementById("richiesteAttive");
    contenitore.innerHTML = "";

    for (let i = 0; i < stato.richiesteVisuali.length; i++) {
        const richiesta = stato.richiesteVisuali[i];

        const riga = document.createElement("div");
        riga.className = richiesta.attiva ? "richiesta-riga attiva" : "richiesta-riga";

        const piano = document.createElement("span");
        piano.className = "richiesta-piano";
        piano.textContent = "Piano " + richiesta.piano;

        const badge = document.createElement("span");
        badge.className = richiesta.attiva ? "badge-richiesta attiva" : "badge-richiesta";
        badge.textContent = richiesta.attiva ? richiesta.tipo : "Nessuna";

        riga.appendChild(piano);
        riga.appendChild(badge);
        contenitore.appendChild(riga);
    }
}

function aggiornaUltimiEventi(stato) {
    const lista = document.getElementById("ultimiEventi");
    lista.innerHTML = "";

    if (!stato.ultimiEventi || stato.ultimiEventi.length === 0) {
        const elemento = document.createElement("li");
        elemento.className = "vuoto";
        elemento.textContent = "Nessun evento";
        lista.appendChild(elemento);
        return;
    }

    for (let i = 0; i < stato.ultimiEventi.length; i++) {
        const testoEvento = stato.ultimiEventi[i];

        const elemento = document.createElement("li");
        elemento.className = classificaEvento(testoEvento);

        const matchOrario = testoEvento.match(/^\[(.*?)\]\s*(.*)$/);

        if (matchOrario) {
            const orario = document.createElement("span");
            orario.className = "evento-orario";
            orario.textContent = "[" + matchOrario[1] + "]";

            const messaggio = document.createElement("span");
            messaggio.textContent = " " + matchOrario[2];

            elemento.appendChild(orario);
            elemento.appendChild(messaggio);
        } else {
            elemento.textContent = testoEvento;
        }

        lista.appendChild(elemento);
    }
}

function classificaEvento(evento) {
    const testo = evento.toLowerCase();

    if (testo.includes("guasto") || testo.includes("overload") || testo.includes("ignorata")) {
        return "evento-errore";
    }

    if (testo.includes("entrate")) {
        return "evento-warning";
    }

    if (testo.includes("richiesta") || testo.includes("chiamata")) {
        return "evento-info";
    }

    if (
        testo.includes("servito") ||
        testo.includes("uscite") ||
        testo.includes("inizializzato") ||
        testo.includes("simulazione")
    ) {
        return "evento-successo";
    }

    return "";
}

function evidenziaPianoCorrente(pianoCorrente) {
    const ids = ["M1", "0", "1", "2", "3", "4"];

    for (let i = 0; i < ids.length; i++) {
        const riga = document.getElementById("pianoRiga" + ids[i]);

        if (riga) {
            riga.classList.remove("highlight-piano");
        }
    }

    const id = pianoCorrente < 0 ? "M" + Math.abs(pianoCorrente) : String(pianoCorrente);
    const corrente = document.getElementById("pianoRiga" + id);

    if (corrente) {
        corrente.classList.add("highlight-piano");
    }
}

function aggiornaEvidenzaErrore(statoErrore) {
    const card = document.getElementById("statoErroreCard");
    card.className = "card";

    if (statoErrore === "OVERLOAD" || statoErrore === "GUASTO") {
        card.classList.add("danger");
    } else {
        card.classList.add("success");
    }
}

function aggiornaEvidenzaCabina(statoCabina) {
    const card = document.getElementById("statoCabina").parentElement;
    card.className = "card";

    if (statoCabina === "IN_MOVIMENTO") {
        card.classList.add("warning");
    } else if (statoCabina === "BLOCCATA") {
        card.classList.add("danger");
    }
}

function aggiornaEvidenzaPorte(statoPorte) {
    const card = document.getElementById("statoPorte").parentElement;
    card.className = "card";

    if (statoPorte === "APERTE") {
        card.classList.add("warning");
    }
}

function aggiornaEvidenzaSimulazione(attiva) {
    const card = document.getElementById("simulazioneCard");
    card.className = "card";

    if (attiva) {
        card.classList.add("success");
    }
}

function aggiornaDisponibilitaModalita(stato) {
    const btnManuale = document.getElementById("btnModalitaManuale");
    btnManuale.disabled = stato.simulazioneAutomaticaAttiva;

    if (stato.simulazioneAutomaticaAttiva) {
        btnManuale.title = "Ferma la simulazione automatica prima di passare al controllo manuale";
    } else {
        btnManuale.title = "";
    }
}

async function mostraModalita(modalita) {
    const manuale = document.getElementById("pannelloManuale");
    const btnAutomatico = document.getElementById("btnModalitaAutomatica");
    const btnManuale = document.getElementById("btnModalitaManuale");
    const comandiAutomatici = document.getElementById("comandiAutomatici");
    const comandiManuali = document.getElementById("comandiManuali");

    if (modalita === "automatica") {
        if (btnManuale.classList.contains("active")) {
            const resetOk = await eseguiAzione("reset");

            if (!resetOk) {
                return;
            }
        }

        manuale.classList.remove("active");
        btnAutomatico.classList.add("active");
        btnManuale.classList.remove("active");
        comandiAutomatici.style.display = "block";
        comandiManuali.style.display = "none";
    } else {
        if (btnManuale.disabled) {
            mostraMessaggio("Ferma la simulazione automatica prima di passare al controllo manuale", true);
            return;
        }

        if (btnAutomatico.classList.contains("active")) {
            const resetOk = await eseguiAzione("reset");

            if (!resetOk) {
                return;
            }
        }

        manuale.classList.add("active");
        btnManuale.classList.add("active");
        btnAutomatico.classList.remove("active");
        comandiAutomatici.style.display = "none";
        comandiManuali.style.display = "block";
    }
}

function richiestaInterna() {
    const inputPiano = document.getElementById("pianoInterno");
    const piano = inputPiano.value;

    eseguiAzione("richiestaInterna", { piano: piano });
    inputPiano.value = 0;
}

function chiamataSalita() {
    const inputPiano = document.getElementById("pianoSalita");
    const piano = inputPiano.value;

    eseguiAzione("chiamataSalita", { piano: piano });
    inputPiano.value = 0;
}

function chiamataDiscesa() {
    const inputPiano = document.getElementById("pianoDiscesa");
    const piano = inputPiano.value;

    eseguiAzione("chiamataDiscesa", { piano: piano });
    inputPiano.value = 0;
}

function aggiornaPersone() {
    const inputEntrate = document.getElementById("personeEntrate");
    const inputUscite = document.getElementById("personeUscite");

    const entrate = inputEntrate.value;
    const uscite = inputUscite.value;

    eseguiAzione("persone", {
        personeEntrate: entrate,
        personeUscite: uscite
    });

    inputEntrate.value = 0;
    inputUscite.value = 0;
}

function mostraMessaggio(testo, errore) {
    const messaggio = document.getElementById("messaggio");
    messaggio.textContent = testo;
    messaggio.style.color = errore ? "#b00020" : "#2e7d32";
}

caricaStato();
setInterval(caricaStato, 1000);