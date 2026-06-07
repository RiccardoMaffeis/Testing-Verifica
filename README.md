# Testing-Verifica

## Progetto ASM - Sistema di Ascensore

Questo repository contiene il modello ASM di un sistema di ascensore a più piani, la relativa implementazione Java annotata con specifiche JML e una semplice interfaccia web dimostrativa.

Il progetto integra modellazione formale, validazione tramite scenari AVALLA e ATGT, model checking con AsmetaSMV, verifica statica con OpenJML, test JUnit 5, test Selenium e Continuous Integration tramite GitHub Actions.

---

## Obiettivo

L'obiettivo del progetto è modellare e verificare il comportamento di un ascensore mediante Abstract State Machines e realizzare una versione Java eseguibile del nucleo logico.

Il sistema gestisce richieste interne ed esterne, movimento della cabina, apertura e chiusura delle porte, condizioni di sovraccarico e guasto tecnico.

La parte ASM descrive il comportamento astratto del sistema ed è validata tramite scenari AVALLA manuali, scenari generati automaticamente con ATGT e proprietà CTL verificate con AsmetaSMV.

La parte Java + JML rappresenta invece una realizzazione eseguibile del nucleo logico, specificata tramite invarianti, precondizioni e postcondizioni. Il comportamento implementativo è verificato tramite test JUnit 5, test parametrici, test Selenium sull'interfaccia web e Continuous Integration.

---

## Funzionalità principali

Il sistema gestisce:

- richieste interne dalla cabina e chiamate esterne dai piani;
- memorizzazione delle richieste attive;
- scelta della direzione di movimento;
- movimento sicuro solo con porte chiuse;
- apertura delle porte al piano richiesto;
- stato di inattività in assenza di richieste;
- sovraccarico della cabina e successiva risoluzione;
- guasto tecnico con timer di ripristino;
- conservazione delle richieste già acquisite durante condizioni anomale.

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
│           ├── drivers/
│           │   └── chromedriver.exe
│           │
│           ├── lib/
│           │   └── selenium/
│           │       ├── client-combined-3.141.59.jar
│           │       ├── byte-buddy-1.8.15.jar
│           │       ├── commons-exec-1.3.jar
│           │       ├── guava-25.0-jre.jar
│           │       ├── okhttp-3.11.0.jar
│           │       └── okio-1.14.0.jar
│           │
│           ├── webapp/
│           │   ├── index.html
│           │   ├── style.css
│           │   └── script.js
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
│                   ├── ControlloreAscensoreTest.java
│                   ├── AscensoreParametricTest.java
│                   ├── AscensoreAvallaAtgtTest.java
│                   └── AscensoreWebSeleniumTest.java
│
└── Documentazione/
    ├── Requisiti e Proprieta.pdf
    ├── Guida utilizzo ascensore.pdf
    ├── AVALLA, ATGT and AsmetaSMV Validation Report.pdf
    ├── Implementazione Java e JML.pdf
    └── Analisi statica/
        ├── Analisi statica_1.pdf
        └── Analisi statica_2.pdf
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

Il file principale del server è:

```text
Codice/Java/Progetto/src/web/AscensoreHttpServer.java
```

La parte statica dell'interfaccia è stata separata in tre file distinti:

```text
Codice/Java/Progetto/webapp/index.html
Codice/Java/Progetto/webapp/style.css
Codice/Java/Progetto/webapp/script.js
```

Questa suddivisione separa la struttura della pagina, lo stile grafico e la logica JavaScript, rendendo il codice più leggibile, manutenibile e coerente con l'analisi statica svolta sul progetto.

L'interfaccia web non introduce una nuova logica di gestione dell'ascensore, ma utilizza direttamente le classi già presenti nel progetto.

In questo modo la simulazione web rimane coerente con l'implementazione Java verificata tramite JML e test JUnit.

L'interfaccia permette di eseguire il sistema in modalità automatica o manuale, mostrando lo stato corrente dell'ascensore, le richieste attive e un log degli eventi principali.

Questa interfaccia ha quindi lo scopo di rendere più facilmente osservabile il comportamento del nucleo Java, senza sostituire le attività di verifica formale e di testing già presenti nel progetto.

---

## Test JUnit

Il progetto contiene test JUnit 5 relativi all'implementazione Java del nucleo logico:

```text
Codice/Java/Progetto/src/test/ControlloreAscensoreTest.java
Codice/Java/Progetto/src/test/AscensoreParametricTest.java
Codice/Java/Progetto/src/test/AscensoreAvallaAtgtTest.java
```

La classe `ControlloreAscensoreTest` verifica scenari funzionali completi, come stato iniziale, acquisizione delle richieste, movimento della cabina, apertura e chiusura delle porte, gestione del sovraccarico, gestione del guasto e ripristino del sistema.

La classe `AscensoreParametricTest` contiene test parametrici usati per verificare più valori di input con la stessa logica di test, in particolare piani validi e non validi, richieste interne ed esterne, soglie di sovraccarico, timer di guasto, scelta della direzione e movimento della cabina.

La classe `AscensoreAvallaAtgtTest` contiene test JUnit derivati da alcuni scenari AVALLA generati automaticamente tramite ATGT. Questi test mantengono separati i casi funzionali scritti manualmente dai casi ottenuti a partire dalla generazione automatica.

---

## Test Selenium

Oltre ai test JUnit sul nucleo logico, il progetto include una classe di test Selenium per verificare il corretto funzionamento dell'interfaccia web dimostrativa.

Il file principale è:

```text
Codice/Java/Progetto/src/test/AscensoreWebSeleniumTest.java
```

I test Selenium verificano il collegamento tra pagina web, server HTTP e nucleo logico Java.

In particolare, controllano:

- apertura corretta della pagina web;
- visualizzazione dello stato iniziale dell'ascensore;
- passaggio alla modalità di controllo manuale;
- inserimento di richieste tramite interfaccia;
- aggiornamento della sezione delle richieste attive;
- reset dei campi di input manuali dopo l'invio;
- avvio e arresto della simulazione automatica;
- disabilitazione del controllo manuale durante la simulazione automatica;
- visualizzazione dello stato di guasto;
- visualizzazione dello stato di sovraccarico.

I test Selenium avviano automaticamente il server web, eseguono le interazioni sulla pagina tramite browser e arrestano il server al termine dell'esecuzione.

I test sono stati aggiornati per utilizzare attese esplicite tramite `WebDriverWait`, evitando l'uso di attese fisse basate su `Thread.sleep`. Questo rende i test più robusti sia in locale sia nell'ambiente di Continuous Integration, dove i tempi di caricamento della pagina possono variare.

---

## Continuous Integration

Il repository include un workflow di Continuous Integration basato su GitHub Actions:

```text
.github/workflows/java-ci.yml
```

Il workflow viene eseguito automaticamente a ogni push sui branch principali e a ogni pull_request.

La pipeline configura un ambiente Java, compila i sorgenti del progetto ed esegue automaticamente i test JUnit 5, i test parametrici e i test Selenium dell'interfaccia web.
Poiché il progetto non utilizza Maven o Gradle, la compilazione viene eseguita direttamente tramite javac.

La CI non sostituisce la verifica statica svolta con OpenJML, ma controlla automaticamente che il codice Java compili correttamente, che i test JUnit 5, inclusi quelli parametrici, continuino a passare e che l'interfaccia web rimanga eseguibile e testabile tramite Selenium.

Poiché i test Selenium richiedono un browser, la pipeline configura Chrome e ChromeDriver ed esegue tali test in modalità headless, cioè senza apertura visibile del browser. In questo modo anche l'interfaccia web dimostrativa viene verificata automaticamente nell'ambiente di Continuous Integration.

Nel workflow viene inoltre impostato il percorso di ChromeDriver tramite la variabile d'ambiente `CHROMEDRIVER_PATH`, in modo da rendere i test Selenium eseguibili anche su ambiente Linux headless.

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

Il risultato completo della modellazione, validazione, verifica, implementazione e analisi statica è documentato nei file:

```text
Documentazione/Requisiti e Proprieta.pdf
Documentazione/Guida utilizzo ascensore.pdf
Documentazione/AVALLA, ATGT and AsmetaSMV Validation Report.pdf
Documentazione/Implementazione Java e JML.pdf
Documentazione/Analisi statica/Analisi statica_1.pdf
Documentazione/Analisi statica/Analisi statica_2.pdf
```

---

## Esito complessivo

Nel complesso, il progetto mostra un percorso completo di modellazione, implementazione e verifica del sistema ascensore.

Il modello ASM è stato validato tramite scenari AVALLA manuali e scenari generati automaticamente con ATGT. Inoltre, una versione ridotta del modello è stata utilizzata per il model checking con AsmetaSMV, verificando proprietà CTL relative alla sicurezza del movimento, alla gestione delle porte, al guasto e alla raggiungibilità degli stati principali.

La successiva implementazione Java + JML realizza il nucleo logico del sistema in forma eseguibile e specificata formalmente. I test JUnit 5, i test parametrici, i test derivati da scenari ATGT, i test Selenium e la Continuous Integration completano la verifica della parte implementativa.

Nel complesso, il progetto integra modellazione ASM, validazione tramite scenari AVALLA e ATGT, model checking con AsmetaSMV, specifica formale tramite JML, verifica statica con OpenJML, test automatici JUnit 5, test parametrici, test Selenium dell'interfaccia web e Continuous Integration tramite GitHub Actions.
