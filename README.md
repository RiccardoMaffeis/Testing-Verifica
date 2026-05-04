# Testing-Verifica

## Progetto ASM - Sistema di Ascensore

Questo repository contiene il modello ASM di un sistema di ascensore a più piani con un'unica cabina.

Il modello descrive il comportamento logico dell'ascensore, includendo la gestione delle richieste, il movimento tra piani, l'apertura e chiusura delle porte, il sovraccarico e la gestione dei guasti.

---

## Obiettivo

L'obiettivo del progetto è modellare formalmente un ascensore mediante Abstract State Machines e verificarne il comportamento tramite strumenti dell'ambiente ASMETA.

Il sistema rappresenta una cabina che serve più piani dell'edificio e che, a ogni passo logico, decide se:

- acquisire nuove richieste;
- servire il piano corrente;
- scegliere una direzione;
- muoversi verso un altro piano;
- entrare in stato di errore;
- tornare operativo dopo un guasto.

La validazione e la verifica del modello sono state svolte tramite:

- scenari AVALLA manuali;
- scenari generati automaticamente con ATGT;
- model checking con AsmetaSMV.

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
│   │   ├── CTLLibrary.asm
│   │   ├── StandardLibrary.asm
│   │   ├── ascensore.asm
│   │   ├── ascensore_smv_small.asm
│   │   └── ascensore_smv_small.smv
│   │
│   └── Avalla/
│       ├── ATGT/
│       │   ├── testtest0.avalla
│       │   ├── testtest2.avalla
│       │   ├── testtest4.avalla
│       │   ├── testtest6.avalla
│       │   └── testtest8.avalla
│       ├── scenario_blocco_richieste_durante_guasto.avalla
│       ├── scenario_conservazione_richieste_guasto.avalla
│       ├── scenario_guasto.avalla
│       ├── scenario_idle.avalla
│       ├── scenario_overload.avalla
│       ├── scenario_richiesta_piano_inferiore.avalla
│       ├── scenario_richiesta_piano_superiore.avalla
│       ├── scenario_richieste_durante_overload.avalla
│       ├── scenario_ripristino_guasto.avalla
│       └── scenario_risoluzione_overload.avalla
│
├── Documentazione/
│   ├── AVALLA, ATGT and AsmetaSMV Validation Report.md
│   ├── AVALLA, ATGT and AsmetaSMV Validation Report.pdf
│   └── Requisiti e Proprieta.pdf
│
└── README.md
```

---

## Modello ASM

Il file principale del modello è:

```text
Codice/AsmetaL/ascensore.asm
```

Al suo interno sono definiti:

- i domini del sistema;
- le variabili monitored;
- le variabili controlled;
- le costanti del modello;
- le regole di acquisizione delle richieste;
- le regole di movimento;
- la gestione del sovraccarico;
- la gestione del guasto;
- la regola principale `r_main`.

Questo modello completo è utilizzato per la validazione tramite scenari AVALLA manuali e scenari generati automaticamente con ATGT.

---

## Modello ridotto per AsmetaSMV

Per il model checking con AsmetaSMV è stata utilizzata una versione ridotta del modello:

```text
Codice/AsmetaL/ascensore_smv_small.asm
```

La versione ridotta mantiene la logica essenziale del sistema, cioè:

- acquisizione delle richieste;
- movimento della cabina;
- apertura e chiusura delle porte;
- gestione dello stato di guasto.

Rispetto al modello completo, sono stati limitati il numero di piani e sono state rimosse alcune estensioni, come la gestione del sovraccarico e del numero di persone.

Questa riduzione è stata necessaria per contenere lo spazio degli stati generato dal model checker.

---

## Scenari AVALLA e ATGT

Gli scenari `.avalla` servono per validare il comportamento del modello ASM.

Nel progetto sono presenti due gruppi di scenari:

- scenari AVALLA manuali;
- scenari AVALLA generati automaticamente tramite ATGT.

Gli scenari manuali sono stati scritti per verificare situazioni specifiche e significative del sistema, come il movimento della cabina, il servizio di un piano richiesto, il sovraccarico e il guasto.

Gli scenari generati con ATGT sono stati invece utilizzati come supporto alla validazione manuale, per esplorare automaticamente ulteriori configurazioni raggiungibili del modello.

Gli scenari manuali testano:

- funzionamento in assenza di richieste;
- richiesta verso un piano superiore;
- richiesta verso un piano inferiore;
- ingresso nello stato di sovraccarico;
- risoluzione del sovraccarico;
- acquisizione di richieste durante il sovraccarico;
- ingresso nello stato di guasto;
- blocco delle nuove richieste durante il guasto;
- conservazione delle richieste già acquisite durante il guasto;
- ripristino dopo guasto.

Gli scenari generati con ATGT sono contenuti nella cartella:

```text
Codice/Avalla/ATGT/
```

Gli scenari sono stati mantenuti separati, così ogni validazione parte dallo stato iniziale definito nel modello ASM e risulta chiara la distinzione tra test manuali e test generati automaticamente.

---

## Validazione con AVALLA

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

La validazione AVALLA è stata svolta in due modi:

- mediante scenari AVALLA manuali, progettati per verificare i principali requisiti funzionali;
- mediante scenari generati automaticamente con ATGT, usati per esplorare ulteriori configurazioni del modello.

---

## Generazione automatica con ATGT

Oltre agli scenari AVALLA manuali, è stato utilizzato ATGT per generare automaticamente ulteriori scenari di test.

Gli scenari generati sono contenuti nella cartella:

```text
Codice/Avalla/ATGT/
```

Questi scenari sono stati usati come supporto alla validazione manuale, per esplorare configurazioni aggiuntive del modello e osservare il comportamento del sistema in condizioni generate automaticamente.

Gli scenari ATGT hanno permesso di osservare principalmente:

- acquisizione automatica di più richieste contemporanee;
- ingresso nello stato `GUASTO`;
- decremento progressivo del timer di ripristino;
- ingresso nello stato `OVERLOAD`;
- blocco della cabina durante condizioni anomale;
- conservazione delle richieste già acquisite.

Alcuni valori generati automaticamente per le variabili `personeEntrate` e `personeUscite` risultano molto elevati o negativi, perché tali variabili sono modellate come `Integer`.

Questi casi non rappresentano situazioni realistiche dal punto di vista applicativo, ma sono utili per osservare il comportamento logico del modello rispetto a configurazioni estreme.

---

## Model checking con AsmetaSMV

Oltre alla validazione tramite AVALLA e ATGT, il progetto include una verifica tramite model checking con AsmetaSMV.

Per questa attività è stata utilizzata la versione ridotta:

```text
Codice/AsmetaL/ascensore_smv_small.asm
```

Il model checking è stato usato per verificare proprietà CTL relative a:

- sicurezza del movimento;
- corretta gestione delle porte;
- comportamento del sistema in caso di guasto;
- raggiungibilità degli stati principali.

Le proprietà verificate controllano, in particolare, che:

- la cabina non si muova mai con le porte aperte;
- la cabina si muova solo in assenza di guasto;
- in caso di guasto la cabina sia bloccata;
- in caso di guasto le porte siano chiuse;
- in caso di guasto la direzione sia annullata;
- il sistema possa tornare a uno stato senza guasto;
- siano raggiungibili movimenti verso l'alto e verso il basso;
- lo stato di guasto sia effettivamente raggiungibile.

Durante la traduzione del modello ASM in NuSMV, AsmetaSMV genera automaticamente un file `.smv`, ad esempio:

```text
ascensore_smv_small.smv
```

Il file generato contiene le proprietà CTL tradotte e valori ausiliari interni, come `UNDEF`, utilizzati dal traduttore.

---

## Documentazione

Il risultato completo della validazione e della verifica è documentato nei file:

```text
Documentazione/Avalla ATGT and AsmetaSMV Validation Report.md
Documentazione/Avalla ATGT and AsmetaSMV Validation Report.pdf
Documentazione/Requisiti e Proprieta.pdf
```

Il report di validazione contiene:

- descrizione degli scenari AVALLA validati;
- stati osservati durante la simulazione;
- check eseguiti;
- risultato dei controlli;
- coverage delle regole;
- regole non coperte da ogni singolo scenario;
- scenari generati automaticamente con ATGT;
- osservazioni sui valori generati automaticamente;
- proprietà CTL verificate con AsmetaSMV;
- osservazioni sul file SMV generato;
- conclusione complessiva della validazione e verifica.

---

## Stato iniziale del modello

Lo stato iniziale del sistema prevede:

- cabina al piano `0`;
- porte chiuse;
- direzione `NESSUNA`;
- nessun errore attivo;
- nessuna richiesta attiva;
- timer a `0`;
- numero di persone pari a `0`.

---

## Comportamento in caso di sovraccarico

Quando il numero di persone presenti in cabina supera la capacità massima, il sistema entra nello stato `OVERLOAD`.

In questo stato:

- la cabina viene bloccata;
- le porte restano aperte;
- la direzione viene impostata a `NESSUNA`;
- il sistema continua ad acquisire nuove richieste.

Quando il numero di persone torna entro la capacità massima, il sistema esce dallo stato `OVERLOAD` e torna operativo.

---

## Comportamento in caso di guasto

Quando viene generato un evento di guasto, il sistema entra nello stato `GUASTO`.

In questo stato:

- la cabina viene bloccata;
- le porte vengono chiuse;
- la direzione viene impostata a `NESSUNA`;
- le nuove richieste non vengono acquisite;
- le richieste già acquisite vengono conservate;
- viene avviato un timer di ripristino.

Quando il timer raggiunge `0`, il sistema torna allo stato operativo.

---

## Esito complessivo

La validazione tramite scenari AVALLA manuali, scenari generati automaticamente con ATGT e model checking con AsmetaSMV conferma la coerenza del modello rispetto ai requisiti funzionali e alle principali proprietà di sicurezza considerate.

Gli scenari AVALLA manuali permettono di verificare casi specifici e facilmente interpretabili.

Gli scenari generati con ATGT permettono di esplorare automaticamente ulteriori configurazioni del modello.

Il model checking con AsmetaSMV permette di verificare formalmente proprietà CTL relative alla sicurezza del movimento, alla gestione delle porte, allo stato di guasto e alla raggiungibilità degli stati principali.
