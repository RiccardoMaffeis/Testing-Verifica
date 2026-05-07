# Testing-Verifica

## Progetto ASM - Sistema di Ascensore

Questo repository contiene il modello ASM di un sistema di ascensore a più piani con un'unica cabina e una successiva implementazione Java annotata con specifiche JML.

Il progetto comprende quindi sia la modellazione formale astratta del sistema, sia una versione implementativa del nucleo logico dell’ascensore, utilizzata per la specifica e la verifica tramite contratti JML.

---

## Obiettivo

L'obiettivo del progetto è modellare formalmente un ascensore mediante Abstract State Machines, verificarne il comportamento tramite strumenti dell'ambiente ASMETA e realizzare una versione Java del nucleo logico annotata con specifiche JML.

La parte ASM viene utilizzata per descrivere il comportamento astratto del sistema e per validarlo tramite scenari e proprietà temporali. La parte Java + JML consente invece di ottenere una versione eseguibile del nucleo logico, specificando formalmente invarianti, precondizioni e postcondizioni dei principali metodi.

Il sistema rappresenta una cabina che serve più piani dell'edificio, gestendo richieste, movimento, porte, sovraccarico e guasti.

La validazione, la verifica e la specifica del sistema sono state svolte tramite:

- scenari AVALLA manuali;
- scenari generati automaticamente con ATGT;
- model checking con AsmetaSMV;
- implementazione Java annotata con JML;
- verifica statica tramite ESC/OpenJML.

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
├── Java/
│   └── Progetto/
│       └── src/
│           └── progetto/
│               ├── Ascensore.java
│               ├── Direzione.java
│               ├── StatoCabina.java
│               ├── StatoPorte.java
│               └── StatoErrore.java
│
├── Documentazione/
│   ├── AVALLA, ATGT and AsmetaSMV Validation Report.md
│   ├── AVALLA, ATGT and AsmetaSMV Validation Report.pdf
│   ├── Implementazione Java JML.md
│   ├── Implementazione Java JML.pdf
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

## Scenari AVALLA e ATGT

Gli scenari `.avalla` servono per validare il comportamento del modello ASM.

Nel progetto sono presenti due gruppi di scenari:

- scenari AVALLA manuali;
- scenari AVALLA generati automaticamente tramite ATGT.

Gli scenari manuali sono stati scritti per verificare situazioni specifiche e significative del sistema, come il movimento della cabina, il servizio di un piano richiesto, il sovraccarico e il guasto.

Gli scenari generati con ATGT sono stati invece utilizzati come supporto alla validazione manuale, per esplorare automaticamente ulteriori configurazioni raggiungibili del modello.

---

## Model checking con AsmetaSMV

Oltre alla validazione tramite AVALLA e ATGT, il progetto include una verifica tramite model checking con AsmetaSMV.

Per questa attività è stata utilizzata la versione ridotta:

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

---

## Implementazione Java + JML

Oltre al modello ASM, il progetto contiene una versione Java del nucleo logico del sistema ascensore.

Il file principale è:

```text
Java/Progetto/src/progetto/Ascensore.java
```

La classe `Ascensore` implementa le principali funzionalità del sistema modellato in ASM, tra cui:

- gestione del piano corrente;
- gestione dello stato della cabina;
- gestione dello stato delle porte;
- gestione della direzione;
- gestione delle richieste attive;
- gestione del numero di persone in cabina;
- gestione del sovraccarico;
- gestione dello stato di guasto;
- gestione del timer di ripristino;
- movimento della cabina di un piano alla volta.

La classe è annotata tramite JML per specificare formalmente:

- invarianti di classe;
- precondizioni dei metodi;
- postcondizioni dei metodi;
- proprietà sui cicli;
- metodi `pure` di osservazione dello stato.

Gli attributi principali della classe sono dichiarati `private` per proteggere lo stato interno dell’oggetto. L’annotazione JML `spec_public` viene utilizzata per permettere comunque l’uso di tali campi all’interno delle specifiche pubbliche.

La parte Java + JML non sostituisce il modello ASM, ma rappresenta una realizzazione eseguibile e verificabile del nucleo logico del sistema.

---

## Verifica statica con JML e OpenJML

La verifica della parte Java è stata svolta tramite contratti JML e controllo statico con ESC/OpenJML.

Le proprietà specificate riguardano principalmente:

- validità del piano corrente;
- validità del timer;
- non negatività del numero di persone;
- assenza di valori nulli negli stati principali;
- movimento consentito solo con porte chiuse;
- movimento consentito solo in assenza di errore;
- comportamento corretto in stato di guasto;
- comportamento corretto in stato di sovraccarico;
- gestione coerente delle richieste attive.

La verifica statica ha permesso di controllare automaticamente una parte significativa del codice rispetto alle proprietà dichiarate.

Alcuni metodi con logica decisionale più articolata possono richiedere ulteriori controlli tramite test eseguibili.

---

## Documentazione

Il risultato completo della modellazione, validazione, verifica e implementazione è documentato nei file:

```text
Documentazione/AVALLA, ATGT and AsmetaSMV Validation Report.md
Documentazione/AVALLA, ATGT and AsmetaSMV Validation Report.pdf
Documentazione/Implementazione Java JML.md
Documentazione/Implementazione Java JML.pdf
Documentazione/Requisiti e Proprieta.pdf
```

---

## Esito complessivo

La validazione tramite scenari AVALLA manuali, scenari generati automaticamente con ATGT e model checking con AsmetaSMV conferma la coerenza del modello ASM rispetto ai requisiti funzionali e alle principali proprietà di sicurezza considerate.

Gli scenari AVALLA manuali permettono di verificare casi specifici e facilmente interpretabili.

Gli scenari generati con ATGT permettono di esplorare automaticamente ulteriori configurazioni del modello.

Il model checking con AsmetaSMV permette di verificare formalmente proprietà CTL relative alla sicurezza del movimento, alla gestione delle porte, allo stato di guasto e alla raggiungibilità degli stati principali.

La successiva implementazione Java + JML realizza il nucleo logico principale del sistema in una forma eseguibile. Le specifiche JML permettono di formalizzare e controllare proprietà di sicurezza e coerenza dello stato, come la validità del piano corrente, la gestione degli errori, il movimento solo con porte chiuse e la corretta gestione delle richieste.

Nel complesso, il progetto integra modellazione ASM, validazione tramite AVALLA, generazione automatica di scenari con ATGT, model checking con AsmetaSMV e specifica formale del codice Java tramite JML.
```
```
