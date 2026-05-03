# Testing-Verifica
````markdown
# Progetto ASM - Sistema di Ascensore

Questo repository contiene il modello ASM di un sistema di ascensore a più piani con un'unica cabina.

Il modello descrive il comportamento logico dell'ascensore, includendo la gestione delle richieste, il movimento tra piani,
l'apertura e chiusura delle porte, il sovraccarico e la gestione dei guasti.

---

## Obiettivo

L'obiettivo del progetto è modellare formalmente un ascensore mediante Abstract State Machines.

Il sistema rappresenta una cabina che serve più piani dell'edificio e che, a ogni passo logico,
decide se:

- acquisire nuove richieste;
- servire il piano corrente;
- scegliere una direzione;
- muoversi verso un altro piano;
- entrare in stato di errore;
- tornare operativo dopo un guasto.

---

## Funzionalità principali

Il modello gestisce:

- richieste interne dalla cabina;
- chiamate esterne dai piani;
- memorizzazione delle richieste attive;
- movimento sicuro solo con porte chiuse;
- apertura delle porte al piano richiesto;
- stato di inattività in assenza di richieste;
- sovraccarico della cabina;
- guasto tecnico con timer di ripristino;
- conservazione delle richieste durante condizioni anomale.

---

## File principali

La repository contiene:

```text
.
├── ascensore.asm
├── scenario_idle.avalla
├── scenario_richiesta_piano_superiore.avalla
├── scenario_richiesta_piano_inferiore.avalla
├── scenario_overload.avalla
├── scenario_risoluzione_overload.avalla
├── scenario_richieste_durante_overload.avalla
├── scenario_guasto.avalla
├── scenario_blocco_richieste_durante_guasto.avalla
├── scenario_conservazione_richieste_guasto.avalla
├── scenario_ripristino_guasto.avalla
└── README.md
````

---

## Modello ASM

Il file principale è:

```text
ascensore.asm
```

Al suo interno sono definiti:

* i domini del sistema;
* le variabili monitored;
* le variabili controlled;
* le regole di movimento;
* la gestione delle richieste;
* la gestione del sovraccarico;
* la gestione del guasto;
* la regola principale `r_main`.

---

## Scenari AVALLA

Gli scenari `.avalla` servono per validare il comportamento del modello.

Ogni scenario testa una situazione specifica, ad esempio:

* assenza di richieste;
* richiesta verso un piano superiore;
* richiesta verso un piano inferiore;
* sovraccarico;
* risoluzione del sovraccarico;
* guasto tecnico;
* blocco delle nuove richieste durante il guasto;
* conservazione delle richieste già acquisite.

Gli scenari sono stati mantenuti separati, così ogni test parte dallo stato iniziale del modello.

---

## Validazione

Per validare il progetto con ASMETA, bisogna eseguire il validator sui file `.avalla`.

Esempio corretto:

```text
scenario_guasto.avalla
```

Non bisogna validare direttamente il file `.asm` con AVALLA.

Esempio non corretto:

```text
ascensore.asm
```

Ogni scenario carica il modello tramite:

```avalla
load ascensore.asm
```

---

## Stato iniziale

Lo stato iniziale del sistema prevede:

* cabina al piano `0`;
* porte chiuse;
* direzione `NESSUNA`;
* nessun errore attivo;
* nessuna richiesta attiva;
* timer a `0`;
* numero di persone pari a `0`.

---

## Tecnologie utilizzate

* ASMETA
* Abstract State Machines
* AVALLA
