# Testing-Verifica

## Progetto ASM - Sistema di Ascensore

Questo repository contiene il modello ASM di un sistema di ascensore a più piani con un'unica cabina, una successiva implementazione Java annotata con specifiche JML e una semplice interfaccia web dimostrativa per l'esecuzione e l'osservazione del nucleo Java.

Il progetto comprende quindi sia la modellazione formale astratta del sistema, sia una versione implementativa del nucleo logico dell'ascensore, utilizzata per la specifica e la verifica tramite contratti JML e test JUnit. A supporto della parte implementativa è stata inoltre realizzata una piccola interfaccia web, utilizzata per simulare e visualizzare dinamicamente il comportamento del sistema.

---

## Obiettivo

L'obiettivo del progetto è modellare formalmente un ascensore mediante Abstract State Machines, verificarne il comportamento tramite gli strumenti dell'ambiente ASMETA e realizzare una versione Java del nucleo logico annotata con specifiche JML.

La parte ASM descrive il comportamento astratto del sistema e viene validata tramite scenari AVALLA, scenari generati automaticamente con ATGT e proprietà temporali verificate con AsmetaSMV.

La parte Java + JML fornisce invece una versione eseguibile del nucleo logico, specificando formalmente invarianti, precondizioni e postcondizioni dei principali metodi. A questa si aggiungono test JUnit per verificare il comportamento operativo del controllore.

Il sistema rappresenta una cabina che serve più piani dell'edificio, gestendo richieste, movimento, porte, sovraccarico e guasti.

La validazione, la verifica e la specifica del sistema sono state svolte tramite:

- scenari AVALLA manuali;
- scenari generati automaticamente con ATGT;
- model checking con AsmetaSMV;
- implementazione Java annotata con JML;
- verifica statica tramite ESC/OpenJML;
- test JUnit sul nucleo Java;
- interfaccia web dimostrativa basata sul nucleo Java;
- Continuous Integration tramite GitHub Actions.

---

## Funzionalità principali

Il sistema gestisce:

- richieste interne dalla cabina;
- chiamate esterne dai piani;
- memorizzazione delle richieste attive;
- scelta della direzione di movimento;
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
├── README.md
│
├── .github/
│   └── workflows/
│       └── java-ci.yml
│
├── Codice/
│   │
│   ├── AsmetaL/
│   │   ├── CTLLibrary.asm
│   │   ├── StandardLibrary.asm
│   │   ├── ascensore.asm
│   │   ├── ascensore_smv_small.asm
│   │   └── ascensore_smv_small.smv
│   │
│   ├── Avalla/
│   │   │
│   │   ├── ATGT/
│   │   │   ├── testtest0.avalla
│   │   │   ├── testtest2.avalla
│   │   │   ├── testtest4.avalla
│   │   │   ├── testtest6.avalla
│   │   │   └── testtest8.avalla
│   │   │
│   │   ├── scenario_idle.avalla
│   │   ├── scenario_richiesta_piano_superiore.avalla
│   │   ├── scenario_richiesta_piano_inferiore.avalla
│   │   ├── scenario_overload.avalla
│   │   ├── scenario_risoluzione_overload.avalla
│   │   ├── scenario_richieste_durante_overload.avalla
│   │   ├── scenario_guasto.avalla
│   │   ├── scenario_blocco_richieste_durante_guasto.avalla
│   │   ├── scenario_conservazione_richieste_guasto.avalla
│   │   └── scenario_ripristino_guasto.avalla
│   │
│   └── Java/
│       └── Progetto/
│           │
│           └── src/
│               │
│               ├── progetto/
│               │   ├── Ascensore.java
│               │   ├── ControlloreAscensore.java
│               │   ├── InputAscensore.java
│               │   ├── Direzione.java
│               │   ├── StatoCabina.java
│               │   ├── StatoPorte.java
│               │   └── StatoErrore.java
│               │
│               ├── web/
│               │   └── AscensoreHttpServer.java
│               │
│               └── test/
│                   └── ControlloreAscensoreTest.java
│
└── Documentazione/
    ├── Requisiti e Proprieta.pdf
    ├── AVALLA, ATGT and AsmetaSMV Validation Report.md
    ├── AVALLA, ATGT and AsmetaSMV Validation Report.pdf
    ├── Implementazione Java JML.md
    └── Implementazione Java JML.pdf
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

## Scenari AVALLA e ATGT

Gli scenari `.avalla` servono per validare il comportamento del modello ASM.

Nel progetto sono presenti due gruppi di scenari:

- scenari AVALLA manuali;
- scenari AVALLA generati automaticamente tramite ATGT.

Gli scenari manuali verificano situazioni specifiche e significative del sistema, come il movimento della cabina, il servizio di un piano richiesto, il sovraccarico e il guasto.

Gli scenari generati con ATGT sono utilizzati come supporto alla validazione manuale, per esplorare automaticamente ulteriori configurazioni raggiungibili del modello.

---

## Comportamento in caso di sovraccarico

Quando il numero di persone presenti in cabina supera la capacità massima, il sistema entra nello stato `OVERLOAD`.

In questo stato:

- la cabina viene bloccata;
- le porte restano aperte;
- la direzione viene impostata a `NESSUNA`;
- il sistema continua ad acquisire nuove richieste;
- le richieste già acquisite vengono conservate.

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

## Model checking con AsmetaSMV

Oltre alla validazione tramite AVALLA e ATGT, il progetto include una verifica tramite model checking con AsmetaSMV.

Per questa attività è stata utilizzata una versione ridotta del modello:

```text
Codice/AsmetaL/ascensore_smv_small.asm
```

La versione ridotta mantiene la logica essenziale del sistema:

- acquisizione delle richieste;
- movimento della cabina;
- apertura e chiusura delle porte;
- gestione dello stato di guasto.

Rispetto al modello completo, sono stati limitati il numero di piani e il dominio del timer. Inoltre, sono state rimosse alcune estensioni, come la gestione del sovraccarico e del numero di persone, per contenere lo spazio degli stati generato dal model checker.

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

Il file NuSMV generato è:

```text
Codice/AsmetaL/ascensore_smv_small.smv
```

---

## Implementazione Java + JML

Oltre al modello ASM, il progetto contiene una versione Java del nucleo logico del sistema ascensore.

Il file principale è:

```text
Codice/Java/Progetto/src/progetto/Ascensore.java
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

Gli attributi principali della classe sono dichiarati `private` per proteggere lo stato interno dell'oggetto. L'annotazione JML `spec_public` permette comunque di usare tali campi all'interno delle specifiche pubbliche.

La parte Java + JML non sostituisce il modello ASM, ma rappresenta una realizzazione eseguibile e verificabile del nucleo logico del sistema.

---

## Controllore Java

La classe:

```text
Codice/Java/Progetto/src/progetto/ControlloreAscensore.java
```

coordina l'esecuzione di un passo logico del sistema.

A ogni passo il controllore:

- acquisisce eventuali richieste dall'input;
- aggiorna il numero di persone quando le porte sono aperte;
- gestisce sovraccarico e guasto;
- chiude le porte quando necessario;
- serve il piano corrente se è presente una richiesta;
- mette l'ascensore in attesa se non ci sono richieste;
- sceglie la direzione e muove la cabina se esistono richieste pendenti.

Gli input esterni sono rappresentati dalla classe:

```text
Codice/Java/Progetto/src/progetto/InputAscensore.java
```

---

## Interfaccia web dimostrativa

Oltre alle classi del nucleo logico, il progetto include una semplice interfaccia web dimostrativa basata su `HttpServer` di Java.

Il file principale è:

```text
Codice/Java/Progetto/src/web/AscensoreHttpServer.java
```

L'interfaccia web non introduce una nuova logica di gestione dell'ascensore, ma utilizza direttamente le classi già presenti nel progetto:

```java
Ascensore
ControlloreAscensore
InputAscensore
```

In questo modo la simulazione web rimane coerente con l'implementazione Java verificata tramite JML e test JUnit.

Questa interfaccia ha quindi lo scopo di rendere più facilmente osservabile il comportamento del nucleo Java, senza sostituire le attività di verifica formale e di testing già presenti nel progetto.

---

## Test JUnit

Il progetto contiene una suite di test JUnit relativa all'implementazione Java:

```text
Codice/Java/Progetto/src/test/ControlloreAscensoreTest.java
```

I test verificano:

- lo stato iniziale dell'ascensore;
- il comportamento in assenza di richieste;
- l'acquisizione di richieste interne ed esterne;
- il movimento verso piani superiori e inferiori;
- il servizio del piano richiesto;
- la chiusura delle porte dopo il servizio;
- la gestione del sovraccarico;
- la risoluzione del sovraccarico;
- il comportamento durante il guasto;
- il blocco delle nuove richieste durante il guasto;
- la conservazione delle richieste già acquisite;
- il decremento del timer di guasto;
- il ripristino dopo il guasto;
- casi limite e input non validi.

I test sono stati utilizzati anche per analizzare la copertura del codice tramite EclEmma.

---

## Continuous Integration

Il repository include un workflow di Continuous Integration basato su GitHub Actions:

```text
.github/workflows/java-ci.yml
```

Il workflow viene eseguito automaticamente a ogni push sui branch principali e a ogni pull_request.

La pipeline configura un ambiente Java, compila i sorgenti del progetto ed esegue automaticamente la suite di test JUnit.

Poiché il progetto non utilizza Maven o Gradle, la compilazione viene eseguita direttamente tramite javac.

La CI non sostituisce la verifica statica svolta con OpenJML, ma controlla automaticamente che il codice Java compili correttamente e che i test JUnit continuino a passare dopo ogni modifica.

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
Documentazione/Requisiti e Proprieta.pdf
Documentazione/AVALLA, ATGT and AsmetaSMV Validation Report.md
Documentazione/AVALLA, ATGT and AsmetaSMV Validation Report.pdf
Documentazione/Implementazione Java JML.md
Documentazione/Implementazione Java JML.pdf
```

---

## Esito complessivo

La validazione tramite scenari AVALLA manuali, scenari generati automaticamente con ATGT e model checking con AsmetaSMV conferma la coerenza del modello ASM rispetto ai requisiti funzionali e alle principali proprietà di sicurezza considerate.

Gli scenari AVALLA manuali permettono di verificare casi specifici e facilmente interpretabili.

Gli scenari generati con ATGT permettono di esplorare automaticamente ulteriori configurazioni del modello.

Il model checking con AsmetaSMV permette di verificare formalmente proprietà CTL relative alla sicurezza del movimento, alla gestione delle porte, allo stato di guasto e alla raggiungibilità degli stati principali.

La successiva implementazione Java + JML realizza il nucleo logico principale del sistema in una forma eseguibile. Le specifiche JML permettono di formalizzare e controllare proprietà di sicurezza e coerenza dello stato, come la validità del piano corrente, la gestione degli errori, il movimento solo con porte chiuse e la corretta gestione delle richieste.

I test JUnit completano la verifica della parte implementativa, controllando il comportamento del controllore Java nei casi ordinari, nei casi anomali e nei principali casi limite.

Nel complesso, il progetto integra modellazione ASM, validazione tramite AVALLA, generazione automatica di scenari con ATGT, model checking con AsmetaSMV, specifica formale del codice Java tramite JML, test automatici JUnit, interfaccia web dimostrativa e Continuous Integration tramite GitHub Actions.
