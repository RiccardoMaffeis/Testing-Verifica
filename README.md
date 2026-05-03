# Testing-Verifica

## Progetto ASM - Sistema di Ascensore

Questo repository contiene il modello ASM di un sistema di ascensore a più piani con un'unica cabina.

Il modello descrive il comportamento logico dell'ascensore, includendo la gestione delle richieste, il movimento tra piani, l'apertura e chiusura delle porte, il sovraccarico e la gestione dei guasti.

---

## Obiettivo

L'obiettivo del progetto è modellare formalmente un ascensore mediante Abstract State Machines.

Il sistema rappresenta una cabina che serve più piani dell'edificio e che, a ogni passo logico, decide se:

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
- risoluzione del sovraccarico;
- guasto tecnico con timer di ripristino;
- blocco delle nuove richieste durante il guasto;
- conservazione delle richieste già acquisite durante condizioni anomale;
- ripristino del sistema dopo un guasto.

---

## Struttura della repository

La repository è organizzata nel seguente modo:

```text
.
├── Codice/
│   ├── AsmetaL/
│   │   └── ascensore.asm
│   ├── Avalla/
│   │   ├── scenario_blocco_richieste_durante_guasto.avalla
│   │   ├── scenario_conservazione_richieste_guasto.avalla
│   │   ├── scenario_guasto.avalla
│   │   ├── scenario_idle.avalla
│   │   ├── scenario_overload.avalla
│   │   ├── scenario_richiesta_piano_inferiore.avalla
│   │   ├── scenario_richiesta_piano_superiore.avalla
│   │   ├── scenario_richieste_durante_overload.avalla
│   │   ├── scenario_ripristino_guasto.avalla
│   │   └── scenario_risoluzione_overload.avalla
│
├── Documentazione/
│   ├── Avalla Validation Report.md
│   └── Requisiti e Proprieta.pdf
│
└── README.md
````

---

## Modello ASM

Il file principale del modello è:

```text
Codice/AsmetaL/ascensore.asm
```

Al suo interno sono definiti:

* i domini del sistema;
* le variabili monitored;
* le variabili controlled;
* le costanti del modello;
* le regole di acquisizione delle richieste;
* le regole di movimento;
* la gestione del sovraccarico;
* la gestione del guasto;
* la regola principale `r_main`.

---

## Scenari AVALLA

Gli scenari `.avalla` servono per validare il comportamento del modello ASM.

Ogni scenario testa una situazione specifica:

* funzionamento in assenza di richieste;
* richiesta verso un piano superiore;
* richiesta verso un piano inferiore;
* ingresso nello stato di sovraccarico;
* risoluzione del sovraccarico;
* acquisizione di richieste durante il sovraccarico;
* ingresso nello stato di guasto;
* blocco delle nuove richieste durante il guasto;
* conservazione delle richieste già acquisite durante il guasto;
* ripristino dopo guasto.

Gli scenari sono stati mantenuti separati, così ogni validazione parte dallo stato iniziale definito nel modello ASM.

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

Il risultato completo della validazione è documentato nel file:

```text
Documentazione/validation_report.md
```

Il report contiene:

* descrizione degli scenari validati;
* stati osservati durante la simulazione;
* check eseguiti;
* risultato dei controlli;
* coverage delle regole;
* regole non coperte da ogni singolo scenario;
* conclusione complessiva della validazione.

---

## Stato iniziale del modello

Lo stato iniziale del sistema prevede:

* cabina al piano `0`;
* porte chiuse;
* direzione `NESSUNA`;
* nessun errore attivo;
* nessuna richiesta attiva;
* timer a `0`;
* numero di persone pari a `0`.

---

## Comportamento in caso di sovraccarico

Quando il numero di persone presenti in cabina supera la capacità massima, il sistema entra nello stato `OVERLOAD`.

In questo stato:

* la cabina viene bloccata;
* le porte restano aperte;
* la direzione viene impostata a `NESSUNA`;
* il sistema continua ad acquisire nuove richieste.

Quando il numero di persone torna entro la capacità massima, il sistema esce dallo stato `OVERLOAD` e torna operativo.

---

## Comportamento in caso di guasto

Quando viene generato un evento di guasto, il sistema entra nello stato `GUASTO`.

In questo stato:

* la cabina viene bloccata;
* le porte vengono chiuse;
* la direzione viene impostata a `NESSUNA`;
* le nuove richieste non vengono acquisite;
* le richieste già acquisite vengono conservate;
* viene avviato un timer di ripristino.

Quando il timer raggiunge `0`, il sistema torna allo stato operativo.
