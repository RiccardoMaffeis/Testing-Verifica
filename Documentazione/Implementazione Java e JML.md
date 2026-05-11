# Documentazione dell’implementazione Java + JML del sistema ascensore

## 1. Introduzione

Questa documentazione descrive l’implementazione Java del nucleo logico del sistema ascensore modellato precedentemente mediante ASM.

L’obiettivo dell’implementazione Java non è quello di sostituire il modello ASM, ma di realizzare una versione eseguibile e verificabile del comportamento principale dell’ascensore. La classe `Ascensore` rappresenta il nucleo operativo del sistema ed è annotata mediante JML per specificare formalmente proprietà, vincoli e contratti dei metodi principali.

Alla classe `Ascensore` si affiancano la classe `InputAscensore`, usata per rappresentare gli input esterni, e la classe `ControlloreAscensore`, che coordina l’evoluzione del sistema a ogni passo logico.

La verifica dell’implementazione è stata svolta tramite specifiche JML, ESC/OpenJML, test JUnit e test Selenium sull’interfaccia web dimostrativa.

Oltre al nucleo logico, il progetto include una semplice interfaccia web basata su `HttpServer`, utilizzata per osservare dinamicamente il comportamento dell’ascensore e per interagire con il sistema in modalità automatica o manuale. L’interfaccia utilizza direttamente le classi del nucleo Java e non introduce una nuova logica di controllo.

---

## 2. Differenze rispetto al modello ASM

Il modello ASM descrive il comportamento dell’ascensore come una macchina astratta a stati. In ASM il sistema è definito tramite domini, funzioni `monitored`, funzioni `controlled` e regole di transizione.

L’implementazione Java organizza invece la logica del sistema in classi e metodi. Ogni metodo rappresenta una parte del comportamento definito nel modello ASM.

Nel modello ASM gli input esterni erano rappresentati da funzioni monitorate:

```asm
monitored chiamataPianoSu: Piano -> Boolean
monitored chiamataPianoGiu: Piano -> Boolean
monitored pulsanteInterno: Piano -> Boolean
monitored eventoGuasto: Boolean
monitored personeEntrate: Integer
monitored personeUscite: Integer
```

Nel codice Java questi input sono raccolti nella classe `InputAscensore` e poi interpretati dal `ControlloreAscensore`.

Ad esempio, una richiesta o un evento vengono impostati sull’oggetto di input:

```java
input.setRichiestaInterna(piano);
input.setChiamataSalita(piano);
input.setChiamataDiscesa(piano);
input.setEventoGuasto(true);
input.setPersoneEntrate(valore);
input.setPersoneUscite(valore);
```

Il controllore usa poi tali informazioni per invocare i metodi della classe `Ascensore`.

Inoltre, mentre nel modello ASM il comportamento complessivo è coordinato da una regola principale, nella versione Java tale ruolo è svolto dal metodo:

```java
eseguiPasso(InputAscensore input)
```

della classe `ControlloreAscensore`.

---

## 3. Struttura del codice

Il codice è organizzato principalmente nel package `progetto`, che contiene il nucleo logico del sistema. A questo si aggiungono il package `web`, dedicato al server HTTP dell’interfaccia dimostrativa, la cartella `webapp`, che contiene la parte statica dell’interfaccia, e il package `test`, dedicato ai test automatici.

La struttura della parte Java è la seguente:

```text
Codice/Java/Progetto/
├── webapp/
│   ├── index.html
│   ├── style.css
│   └── script.js
│
└── src/
    ├── progetto/
    │   ├── Ascensore.java
    │   ├── ControlloreAscensore.java
    │   ├── InputAscensore.java
    │   ├── Direzione.java
    │   ├── StatoCabina.java
    │   ├── StatoPorte.java
    │   └── StatoErrore.java
    │
    ├── web/
    │   └── AscensoreHttpServer.java
    │
    └── test/
        ├── ControlloreAscensoreTest.java
        ├── AscensoreParametricTest.java
        └── AscensoreWebSeleniumTest.java
```

Le classi principali sono:

- `Ascensore`, che contiene lo stato interno e i metodi fondamentali dell’ascensore;
- `InputAscensore`, che rappresenta gli input esterni ricevuti in un passo logico;
- `ControlloreAscensore`, che coordina l’evoluzione del sistema;
- `AscensoreHttpServer`, che realizza il server HTTP dell’interfaccia web dimostrativa;
- `ControlloreAscensoreTest`, che contiene i test JUnit funzionali sul nucleo logico;
- `AscensoreParametricTest`, che contiene test parametrici JUnit 5 su classi di input e casi limite;
- `AscensoreWebSeleniumTest`, che contiene i test Selenium sull’interfaccia web.

La parte statica dell’interfaccia è separata in tre file:

- `index.html`, che definisce la struttura della pagina;
- `style.css`, che definisce lo stile grafico;
- `script.js`, che contiene la logica JavaScript di aggiornamento e interazione.

Le enumerazioni utilizzate sono:

- `Direzione`;
- `StatoCabina`;
- `StatoPorte`;
- `StatoErrore`.

---

## 4. Classe Ascensore

La classe `Ascensore` contiene lo stato principale del sistema e i metodi che implementano la logica dell’ascensore.

Le costanti principali sono:

```java
public static final int PIANO_MINIMO = -1;
public static final int PIANO_MASSIMO = 4;
public static final int NUMERO_PIANI = 6;
public static final int CAPACITA_MASSIMA = 8;
public static final int TIMER_MASSIMO = 10;
```

Queste costanti definiscono:

- il piano minimo raggiungibile;
- il piano massimo raggiungibile;
- il numero totale di piani;
- la capacità massima della cabina;
- il valore massimo del timer di ripristino dopo un guasto.

Essendo dichiarate come `public static final`, tali costanti sono fissate direttamente dal linguaggio Java e non necessitano di ulteriori invarianti JML.

Lo stato interno dell’ascensore è rappresentato dai seguenti attributi:

```java
private /*@ spec_public @*/ int pianoCorrente;
private /*@ spec_public @*/ int timer;
private /*@ spec_public @*/ int numeroPersone;

private /*@ spec_public @*/ StatoCabina statoCabina;
private /*@ spec_public @*/ StatoPorte statoPorte;
private /*@ spec_public @*/ Direzione direzione;
private /*@ spec_public @*/ StatoErrore statoErrore;

private /*@ spec_public @*/ final boolean[] richiesteAttive;
```

Gli attributi sono dichiarati `private` per proteggere lo stato interno della classe e impedire modifiche dirette non controllate dall’esterno.

L’annotazione JML:

```java
/*@ spec_public @*/
```

consente comunque di usare questi campi nelle specifiche JML pubbliche.

---

## 5. Enumerazioni

Le enumerazioni definiscono gli stati discreti principali del sistema.

```java
public enum Direzione {
    SU,
    GIU,
    NESSUNA
}
```

`Direzione` indica la direzione corrente dell’ascensore.

```java
public enum StatoCabina {
    FERMA,
    IN_MOVIMENTO,
    BLOCCATA
}
```

`StatoCabina` indica se la cabina è ferma, in movimento oppure bloccata.

```java
public enum StatoPorte {
    APERTE,
    CHIUSE
}
```

`StatoPorte` indica lo stato delle porte.

```java
public enum StatoErrore {
    NESSUNO,
    OVERLOAD,
    GUASTO
}
```

`StatoErrore` indica se il sistema è operativo, in sovraccarico oppure in guasto tecnico.

---

## 6. Rappresentazione delle richieste attive

Nel modello ASM le richieste erano rappresentate come funzione:

```asm
richiesteAttive: Piano -> Boolean
```

Nel codice Java sono rappresentate tramite un array:

```java
private final boolean[] richiesteAttive;
```

Poiché in Java gli array partono da indice `0`, mentre i piani dell’ascensore vanno da `-1` a `4`, viene utilizzato il metodo:

```java
indiceDelPiano(int piano)
```

Il metodo converte il numero del piano nell’indice dell’array.

Il contratto JML del metodo è:

```java
//@ requires pianoValido(piano);
//@ ensures 0 <= \result && \result < NUMERO_PIANI;
//@ ensures \result == piano - PIANO_MINIMO;
```

Questo garantisce che ogni piano valido venga convertito in un indice valido dell’array.

---

## 7. Invarianti JML della classe Ascensore

La classe `Ascensore` definisce diversi invarianti JML. Gli invarianti rappresentano proprietà che devono essere sempre vere per ogni oggetto valido della classe.

### 7.1 Vincoli sullo stato numerico

```java
//@ public invariant PIANO_MINIMO <= pianoCorrente && pianoCorrente <= PIANO_MASSIMO;
//@ public invariant 0 <= timer && timer <= TIMER_MASSIMO;
//@ public invariant numeroPersone >= 0;
```

Questi invarianti garantiscono che:

- il piano corrente sia sempre compreso tra il piano minimo e il piano massimo;
- il timer sia sempre compreso tra `0` e `TIMER_MASSIMO`;
- il numero di persone non sia mai negativo.

### 7.2 Vincoli di non nullità

```java
//@ public invariant statoCabina != null;
//@ public invariant statoPorte != null;
//@ public invariant direzione != null;
//@ public invariant statoErrore != null;
//@ public invariant richiesteAttive != null;
//@ public invariant richiesteAttive.length == NUMERO_PIANI;
```

Questi invarianti garantiscono che gli stati dell’ascensore siano sempre definiti e che l’array delle richieste sia correttamente inizializzato.

### 7.3 Invarianti di sicurezza sul movimento

```java
//@ public invariant statoCabina == StatoCabina.IN_MOVIMENTO ==> statoPorte == StatoPorte.CHIUSE;
//@ public invariant statoCabina == StatoCabina.IN_MOVIMENTO ==> statoErrore == StatoErrore.NESSUNO;
```

Questi invarianti formalizzano due proprietà fondamentali:

- la cabina può muoversi solo con le porte chiuse;
- la cabina può muoversi solo se non sono presenti errori.

### 7.4 Invarianti sul guasto

```java
//@ public invariant statoErrore == StatoErrore.GUASTO ==> statoCabina == StatoCabina.BLOCCATA;
//@ public invariant statoErrore == StatoErrore.GUASTO ==> statoPorte == StatoPorte.CHIUSE;
//@ public invariant statoErrore == StatoErrore.GUASTO ==> direzione == Direzione.NESSUNA;
```

Quando il sistema è in stato `GUASTO`, la cabina deve essere bloccata, le porte devono essere chiuse e la direzione deve essere annullata.

### 7.5 Invarianti sul sovraccarico

```java
//@ public invariant statoErrore == StatoErrore.OVERLOAD ==> statoCabina == StatoCabina.BLOCCATA;
//@ public invariant statoErrore == StatoErrore.OVERLOAD ==> statoPorte == StatoPorte.APERTE;
//@ public invariant statoErrore == StatoErrore.OVERLOAD ==> direzione == Direzione.NESSUNA;
```

Quando il sistema è in stato `OVERLOAD`, la cabina deve essere bloccata, le porte devono essere aperte e la direzione deve essere annullata.

### 7.6 Invariante sul timer

```java
//@ public invariant timer > 0 ==> statoErrore == StatoErrore.GUASTO;
```

Questo invariante stabilisce che il timer può essere positivo solo durante uno stato di guasto.

---

## 8. Costruttore

Il costruttore inizializza l’ascensore nello stato iniziale:

```java
public Ascensore()
```

Lo stato iniziale è:

```text
pianoCorrente = 0
timer = 0
numeroPersone = 0
statoCabina = FERMA
statoPorte = CHIUSE
direzione = NESSUNA
statoErrore = NESSUNO
richiesteAttive = array di 6 elementi
```

Le postcondizioni JML garantiscono che l’oggetto venga creato in uno stato coerente:

```java
//@ ensures pianoCorrente == 0;
//@ ensures timer == 0;
//@ ensures numeroPersone == 0;
//@ ensures statoCabina == StatoCabina.FERMA;
//@ ensures statoPorte == StatoPorte.CHIUSE;
//@ ensures direzione == Direzione.NESSUNA;
//@ ensures statoErrore == StatoErrore.NESSUNO;
//@ ensures richiesteAttive != null;
//@ ensures richiesteAttive.length == NUMERO_PIANI;
```

---

## 9. Metodi di osservazione

La classe contiene diversi metodi `pure`, utilizzati per leggere lo stato senza modificarlo.

Esempi:

```java
getPianoCorrente()
getTimer()
getNumeroPersone()
getStatoCabina()
getStatoPorte()
getDirezione()
getStatoErrore()
```

In JML, un metodo dichiarato `pure` non modifica lo stato dell’oggetto e può quindi essere utilizzato all’interno delle specifiche JML.

---

## 10. Gestione delle richieste

La classe permette di aggiungere tre tipi di richieste:

```java
aggiungiRichiestaInterna(int piano)
aggiungiChiamataSalita(int piano)
aggiungiChiamataDiscesa(int piano)
```

Questi metodi rappresentano rispettivamente:

- la pressione di un pulsante interno alla cabina;
- una chiamata esterna verso l’alto;
- una chiamata esterna verso il basso.

Tutti questi metodi richiedono che il piano sia valido e che il sistema non sia in stato di guasto.

Ad esempio:

```java
//@ requires pianoValido(piano);
//@ requires statoErrore != StatoErrore.GUASTO;
//@ ensures richiesteAttive[indiceDelPiano(piano)] == true;
public void aggiungiRichiestaInterna(int piano)
```

In stato `GUASTO`, quindi, non vengono acquisite nuove richieste.

---

## 11. Aggiornamento del numero di persone

Il metodo:

```java
aggiornaPersone(int personeEntrate, int personeUscite)
```

aggiorna il numero di persone presenti in cabina.

Le precondizioni richiedono che:

```java
//@ requires personeEntrate >= 0;
//@ requires personeUscite >= 0;
//@ requires statoPorte == StatoPorte.APERTE;
//@ requires statoCabina == StatoCabina.FERMA || statoCabina == StatoCabina.BLOCCATA;
```

Quindi il numero di persone può essere modificato solo se le porte sono aperte e la cabina è ferma o bloccata.

La postcondizione garantisce che il numero di persone non diventi mai negativo:

```java
//@ ensures numeroPersone >= 0;
```

Il metodo è annotato con:

```java
@CodeBigintMath
```

Questa annotazione viene usata per evitare falsi allarmi legati all’overflow degli interi durante la verifica statica con ESC.

---

## 12. Gestione del sovraccarico

Il metodo:

```java
gestisciSovraccarico()
```

gestisce il passaggio allo stato `OVERLOAD`.

Se il numero di persone supera la capacità massima, il sistema entra in sovraccarico:

```text
statoErrore = OVERLOAD
statoCabina = BLOCCATA
statoPorte = APERTE
direzione = NESSUNA
timer = 0
```

Se invece il sistema era in `OVERLOAD` e il numero di persone torna entro la capacità massima, il sistema ritorna operativo:

```text
statoErrore = NESSUNO
statoCabina = FERMA
statoPorte = CHIUSE
direzione = NESSUNA
timer = 0
```

Questo comportamento è coerente con il modello ASM, nel quale il sovraccarico blocca la cabina e mantiene le porte aperte.

---

## 13. Gestione del guasto

Il metodo:

```java
attivaGuasto()
```

attiva lo stato di guasto tecnico.

Le precondizioni richiedono che il sistema non sia già in errore:

```java
//@ requires statoErrore == StatoErrore.NESSUNO;
```

Quando viene attivato il guasto, il sistema passa nello stato:

```text
statoErrore = GUASTO
statoCabina = BLOCCATA
statoPorte = CHIUSE
direzione = NESSUNA
timer = TIMER_MASSIMO
```

Questo è coerente con il modello ASM, nel quale il guasto blocca la cabina, chiude le porte e inizializza un timer di ripristino.

---

## 14. Gestione del timer di guasto

Il metodo:

```java
gestisciTimerGuasto()
```

viene utilizzato quando il sistema si trova nello stato `GUASTO`.

Se il timer è maggiore di zero, viene decrementato:

```java
timer = timer - 1;
```

Se invece il timer è uguale a zero, il sistema viene ripristinato:

```text
statoErrore = NESSUNO
statoCabina = FERMA
statoPorte = CHIUSE
direzione = NESSUNA
timer = 0
```

Il contratto JML descrive formalmente entrambi i casi.

---

## 15. Gestione delle porte

Il metodo:

```java
chiudiPorte()
```

chiude le porte dell’ascensore.

Può essere eseguito solo se:

```java
//@ requires statoErrore == StatoErrore.NESSUNO;
//@ requires statoCabina == StatoCabina.FERMA;
//@ requires statoPorte == StatoPorte.APERTE;
```

Quindi le porte vengono chiuse solo se il sistema è operativo, la cabina è ferma e le porte sono aperte.

---

## 16. Servizio del piano corrente

Il metodo:

```java
serviPianoCorrente()
```

serve la richiesta associata al piano corrente.

Le postcondizioni garantiscono che:

```text
statoCabina = FERMA
statoPorte = APERTE
direzione = NESSUNA
richiesta del piano corrente = false
```

Il metodo rappresenta il momento in cui l’ascensore arriva al piano richiesto, apre le porte e rimuove la richiesta attiva.

---

## 17. Stato di attesa

Il metodo:

```java
mettiInAttesa()
```

porta l’ascensore nello stato di attesa.

Il metodo richiede che non siano presenti errori:

```java
//@ requires statoErrore == StatoErrore.NESSUNO;
```

Le postcondizioni garantiscono:

```text
statoCabina = FERMA
direzione = NESSUNA
```

Questo metodo rappresenta il comportamento dell’ascensore quando non ci sono richieste da servire.

---

## 18. Ricerca delle richieste

La classe contiene tre metodi per analizzare le richieste attive:

```java
esisteRichiesta()
esisteRichiestaSopra()
esisteRichiestaSotto()
```

Questi metodi sono dichiarati `pure`, perché leggono lo stato ma non lo modificano.

### 18.1 esisteRichiesta

Il metodo:

```java
esisteRichiesta()
```

verifica se esiste almeno una richiesta attiva in qualsiasi piano.

La postcondizione JML è:

```java
//@ ensures \result <==> (\exists int i; 0 <= i && i < NUMERO_PIANI; richiesteAttive[i]);
```

Il ciclo è annotato con invarianti di ciclo:

```java
//@ loop_invariant 0 <= i && i <= NUMERO_PIANI;
//@ loop_invariant (\forall int j; 0 <= j && j < i; richiesteAttive[j] == false);
```

### 18.2 esisteRichiestaSopra

Il metodo:

```java
esisteRichiestaSopra()
```

verifica se esiste una richiesta attiva a un piano superiore rispetto al piano corrente.

La postcondizione JML è:

```java
//@ ensures \result <==>
//@     (\exists int p; pianoCorrente < p && p <= PIANO_MASSIMO;
//@         richiesteAttive[indiceDelPiano(p)]);
```

### 18.3 esisteRichiestaSotto

Il metodo:

```java
esisteRichiestaSotto()
```

verifica se esiste una richiesta attiva a un piano inferiore rispetto al piano corrente.

La postcondizione JML è:

```java
//@ ensures \result <==>
//@     (\exists int p; PIANO_MINIMO <= p && p < pianoCorrente;
//@         richiesteAttive[indiceDelPiano(p)]);
```

---

## 19. Scelta della direzione

Il metodo:

```java
scegliDirezione()
```

decide la direzione dell’ascensore in base alle richieste attive.

Il metodo può essere eseguito solo se il sistema non presenta errori:

```java
//@ requires statoErrore == StatoErrore.NESSUNO;
```

La logica implementata è la seguente:

- se non esistono richieste, la direzione diventa `NESSUNA`;
- se la direzione corrente è `SU` e ci sono richieste sopra, la direzione resta `SU`;
- se la direzione corrente è `SU` ma non ci sono richieste sopra, il sistema cerca richieste sotto;
- se la direzione corrente è `GIU` e ci sono richieste sotto, la direzione resta `GIU`;
- se la direzione corrente è `GIU` ma non ci sono richieste sotto, il sistema cerca richieste sopra;
- se la direzione corrente è `NESSUNA`, il sistema sceglie una direzione in base alle richieste disponibili.

Questo metodo è il più articolato dal punto di vista logico, perché contiene più condizioni ramificate e dipende dai metodi ausiliari di ricerca delle richieste.

Durante la verifica ESC, questo metodo può risultare più difficile da dimostrare completamente, soprattutto a causa dell’uso di metodi `pure` all’interno di postcondizioni con `\old`.

Questo non indica necessariamente un errore nel comportamento, ma rappresenta un limite pratico della verifica automatica.

---

## 20. Movimento della cabina

Il metodo:

```java
muoviDiUnPiano()
```

sposta l’ascensore di un piano nella direzione corrente.

Può essere eseguito solo se:

```java
//@ requires statoErrore == StatoErrore.NESSUNO;
//@ requires statoPorte == StatoPorte.CHIUSE;
```

Quindi l’ascensore può muoversi solo senza errori e con le porte chiuse.

Se la direzione è `SU` e il piano corrente è minore del piano massimo, il piano viene incrementato:

```java
pianoCorrente = pianoCorrente + 1;
statoCabina = StatoCabina.IN_MOVIMENTO;
```

Se la direzione è `GIU` e il piano corrente è maggiore del piano minimo, il piano viene decrementato:

```java
pianoCorrente = pianoCorrente - 1;
statoCabina = StatoCabina.IN_MOVIMENTO;
```

Se non è possibile muoversi, la cabina viene fermata e la direzione viene annullata.

---

## 21. Classe InputAscensore

La classe `InputAscensore` rappresenta gli input esterni ricevuti dal sistema durante un singolo passo logico.

La classe contiene i seguenti attributi principali:

```java
private int richiestaInterna = Integer.MIN_VALUE;
private int chiamataSalita = Integer.MIN_VALUE;
private int chiamataDiscesa = Integer.MIN_VALUE;

private boolean eventoGuasto;
private int personeEntrate;
private int personeUscite;
```

I valori relativi alle richieste sono inizializzati a `Integer.MIN_VALUE`. Tale valore viene usato come valore sentinella per indicare che, in quel passo logico, non è stata impostata alcuna richiesta di quel tipo.

La classe fornisce metodi di lettura e scrittura per ciascun input:

```java
setRichiestaInterna(int richiestaInterna)
setChiamataSalita(int chiamataSalita)
setChiamataDiscesa(int chiamataDiscesa)
setEventoGuasto(boolean eventoGuasto)
setPersoneEntrate(int personeEntrate)
setPersoneUscite(int personeUscite)
```

Sono inoltre presenti metodi di supporto che permettono di verificare se una richiesta è effettivamente presente:

```java
haRichiestaInterna()
haChiamataSalita()
haChiamataDiscesa()
```

Questi metodi restituiscono `true` quando il valore corrispondente è diverso da `Integer.MIN_VALUE`.

La classe `InputAscensore` non modifica direttamente lo stato dell’ascensore. Il suo compito è raccogliere gli input esterni, che vengono poi interpretati dalla classe `ControlloreAscensore`.

---

## 22. Classe ControlloreAscensore

La classe `ControlloreAscensore` ha il compito di coordinare l’evoluzione del sistema a ogni passo logico. Mentre la classe `Ascensore` contiene lo stato interno e i metodi che modificano tale stato, il controllore stabilisce l’ordine con cui questi metodi devono essere invocati.

Questa separazione permette di mantenere distinta la rappresentazione dello stato dalla logica di coordinamento, rendendo il codice più leggibile e più vicino alla struttura del modello ASM, nel quale una regola principale coordina l’esecuzione delle singole regole.

Il metodo principale della classe è:

```java
public void eseguiPasso(InputAscensore input)
```

Questo metodo riceve un oggetto `InputAscensore`, che rappresenta gli input esterni del sistema nel passo corrente. Se l’input è `null`, viene sollevata un’eccezione per evitare l’esecuzione del controllore con dati non validi.

A ogni passo logico, il controllore esegue quattro operazioni principali:

```java
acquisisciRichieste(input);
gestisciPersone(input);
gestisciErrore(input);
gestisciComportamentoNormale();
```

La prima fase acquisisce eventuali richieste interne o chiamate esterne, purché il sistema non sia in stato di guasto e i piani indicati siano validi.

La seconda fase aggiorna il numero di persone presenti in cabina, ma solo quando le porte sono aperte e i valori ricevuti sono non negativi.

La terza fase gestisce le condizioni anomale, dando priorità al sovraccarico rispetto al guasto. Se il sistema è in guasto, viene invece aggiornato il timer di ripristino.

La quarta fase descrive il comportamento ordinario dell’ascensore: chiusura delle porte, servizio del piano corrente, stato di attesa oppure scelta della direzione e movimento di un piano.

In questo modo `ControlloreAscensore` svolge un ruolo simile alla regola principale del modello ASM, perché coordina le varie operazioni elementari e determina l’evoluzione complessiva del sistema.

---

## 23. Interfaccia web dimostrativa

Oltre al nucleo logico del sistema, il progetto include una semplice interfaccia web dimostrativa realizzata tramite `HttpServer` di Java.

La classe principale è:

```text
AscensoreHttpServer
```

L’interfaccia web ha lo scopo di rendere osservabile il comportamento del nucleo Java dell’ascensore. Non introduce una nuova logica di gestione del sistema, ma utilizza direttamente le classi già presenti nell’implementazione:

```text
Ascensore
ControlloreAscensore
InputAscensore
```

La classe `AscensoreHttpServer` espone tre endpoint principali:

```text
/
```

utilizzato per restituire la pagina HTML dell’interfaccia;

```text
/stato
```

utilizzato per ottenere lo stato corrente dell’ascensore in formato JSON;

```text
/azione
```

utilizzato per inviare comandi al sistema, come richieste interne, chiamate esterne, aggiornamento del numero di persone, guasto, reset e gestione della simulazione automatica.

L’interfaccia supporta due modalità operative:

- simulazione automatica;
- controllo manuale.

Nella modalità automatica il sistema genera periodicamente richieste, ingressi e uscite di persone e possibili guasti. 
Nella modalità manuale l’utente può invece inserire direttamente richieste interne, chiamate esterne, variazioni del numero di persone e guasti.

La pagina web mostra dinamicamente:

- piano corrente;
- numero di persone;
- stato della cabina;
- stato delle porte;
- direzione;
- stato di errore;
- stato della simulazione automatica;
- richieste attive;
- log degli eventi principali;
- rappresentazione grafica semplificata della cabina.

---

## 24. Test JUnit 5

Oltre alla verifica statica tramite JML/OpenJML, il progetto include test JUnit 5 per verificare il comportamento eseguibile della parte Java.

Le classi di test principali sono:

```text
ControlloreAscensoreTest
AscensoreParametricTest
```

La classe `ControlloreAscensoreTest` contiene test funzionali sul comportamento del sistema. La suite verifica, tra gli altri, i seguenti aspetti:

- stato iniziale dell’ascensore;
- comportamento in assenza di richieste;
- acquisizione di richieste interne;
- acquisizione di chiamate esterne verso l’alto e verso il basso;
- rifiuto di richieste non valide;
- movimento verso piani superiori e inferiori;
- servizio del piano corrente;
- apertura e chiusura delle porte;
- aggiornamento del numero di persone;
- rifiuto di valori negativi per persone entrate o uscite;
- ingresso e risoluzione dello stato di sovraccarico;
- acquisizione di richieste durante lo stato OVERLOAD;
- ingresso nello stato di guasto;
- blocco di nuove richieste durante lo stato GUASTO;
- conservazione delle richieste già acquisite durante il guasto;
- decremento del timer di guasto;
- ripristino dopo il guasto;
- casi limite della scelta della direzione;
- casi limite del movimento ai piani estremi;
- gestione di input null.

La classe `AscensoreParametricTest` contiene invece test parametrici JUnit 5. Questi test permettono di eseguire la stessa logica di verifica su più valori di input, riducendo duplicazioni e rendendo più sistematico il controllo dei casi limite.

I test parametrici verificano in particolare:

- piani validi e non validi;
- richieste interne su più piani;
- chiamate di salita valide e non valide;
- chiamate di discesa valide e non valide;
- aggiornamento del numero di persone;
- soglie di sovraccarico;
- decremento del timer di guasto;
- scelta della direzione;
- movimento coerente con la direzione selezionata.

I test parametrici non sostituiscono i test funzionali, ma li completano. I test funzionali controllano scenari completi del sistema, mentre i test parametrici permettono di verificare più classi di input con una struttura compatta.

I test sono stati eseguiti sia in ambiente Eclipse tramite JUnit, sia automaticamente nella pipeline GitHub Actions. La copertura del codice è stata analizzata tramite EclEmma.

---

### Collegamento tra scenari AVALLA e test JUnit

Alcuni scenari AVALLA utilizzati per validare il modello ASM sono stati ripresi anche nella suite JUnit dell’implementazione Java.  
Questo permette di mantenere un collegamento tra la validazione del modello astratto e la verifica eseguibile del nucleo Java.

| Scenario AVALLA | Comportamento verificato nel modello ASM | Test JUnit corrispondente | Comportamento verificato nel codice Java |
|---|---|---|---|
| `scenario_idle.avalla` | Verifica che, in assenza di richieste, l’ascensore rimanga fermo nello stato iniziale. | `idleSenzaRichieste()` | Verifica che, eseguendo un passo senza input, l’ascensore resti al piano 0, con cabina ferma, porte chiuse, direzione `NESSUNA` e nessun errore. |
| `scenario_richiesta_piano_superiore.avalla` | Verifica l’acquisizione di una richiesta verso un piano superiore, il movimento verso l’alto e il servizio del piano richiesto. | `richiestaInternaVersoPianoSuperiore()` e `ascensoreServePianoSuperioreRichiesto()` | Verifica che una richiesta interna verso il piano 3 venga acquisita, che l’ascensore scelga la direzione `SU`, si muova verso l’alto e, una volta raggiunto il piano, apra le porte e rimuova la richiesta. |
| `scenario_richiesta_piano_inferiore.avalla` | Verifica l’acquisizione di una richiesta verso un piano inferiore, il movimento verso il basso e il servizio del piano richiesto. | `richiestaVersoPianoInferiore()` | Verifica che, dopo aver portato l’ascensore a un piano superiore, una richiesta verso un piano inferiore venga acquisita e produca movimento in direzione `GIU`. |
| `scenario_overload.avalla` | Verifica l’ingresso nello stato `OVERLOAD` quando il numero di persone supera la capacità massima. | `entraInOverload()` | Verifica che, con 9 persone in cabina, il sistema entri nello stato `OVERLOAD`, blocchi la cabina, mantenga le porte aperte e imposti la direzione a `NESSUNA`. |
| `scenario_risoluzione_overload.avalla` | Verifica l’uscita dallo stato `OVERLOAD` quando il numero di persone torna entro la capacità massima. | `risolveOverloadQuandoUnaPersonaEsce()` | Verifica che, dopo l’uscita di una persona, il numero di persone torni a 8 e il sistema rientri nello stato operativo `NESSUNO`, con cabina ferma, porte chiuse e direzione `NESSUNA`. |
| `scenario_richieste_durante_overload.avalla` | Verifica che durante lo stato `OVERLOAD` le richieste possano essere ancora acquisite. | `richiesteDuranteOverloadVengonoAcquisite()` | Verifica che, mentre il sistema è in sovraccarico, una richiesta interna valida venga comunque registrata tra le richieste attive. |
| `scenario_guasto.avalla` | Verifica l’ingresso nello stato `GUASTO` quando viene generato un evento di guasto. | `attivaGuastoTecnico()` | Verifica che l’evento di guasto imposti lo stato `GUASTO`, blocchi la cabina, chiuda le porte, annulli la direzione e inizializzi il timer al valore massimo. |
| `scenario_blocco_richieste_durante_guasto.avalla` | Verifica che durante lo stato `GUASTO` non vengano acquisite nuove richieste. | `nonAcquisisceNuoveRichiesteDuranteGuasto()` | Verifica che una richiesta inserita mentre il sistema è in guasto venga ignorata e non risulti attiva. |
| `scenario_conservazione_richieste_guasto.avalla` | Verifica che una richiesta acquisita prima del guasto venga conservata durante il guasto. | `conservaRichiesteGiaAcquisiteDuranteGuasto()` | Verifica che una richiesta già attiva prima dell’attivazione del guasto rimanga memorizzata anche durante lo stato `GUASTO`. |
| `scenario_ripristino_guasto.avalla` | Verifica il decremento del timer di guasto e il ritorno allo stato operativo. | `decrementaTimerDuranteGuasto()` e `ripristinaDopoGuasto()` | Verifica che il timer venga decrementato durante il guasto e che, una volta terminato, il sistema torni allo stato `NESSUNO`, con cabina ferma, porte chiuse e direzione `NESSUNA`. |

Questa corrispondenza non sostituisce la validazione AVALLA, ma mostra che i principali comportamenti osservati nel modello ASM sono stati ripresi anche nei test eseguibili dell’implementazione Java.

---

## 25. Test Selenium

Oltre ai test JUnit sul nucleo logico, il progetto include test Selenium per verificare il corretto funzionamento dell’interfaccia web dimostrativa.

La classe di test è:

```text
AscensoreWebSeleniumTest
```

I test Selenium sono test di integrazione, perché verificano il collegamento tra:

```text
pagina web
server HTTP
ControlloreAscensore
Ascensore
```

Prima di ogni test viene eseguito un reset dello stato del sistema, così da rendere i test indipendenti tra loro. 
Al termine dell’esecuzione, il browser viene chiuso e il server viene arrestato.

I test Selenium verificano in particolare:

- apertura corretta della pagina web;
- visualizzazione dello stato iniziale dell’ascensore;
- passaggio alla modalità di controllo manuale;
- inserimento di una richiesta interna tramite interfaccia;
- aggiornamento della sezione delle richieste attive;
- reset dei campi di input manuali dopo l’invio;
- avvio e arresto della simulazione automatica;
- disabilitazione del controllo manuale durante la simulazione automatica;
- visualizzazione dello stato di guasto;
- visualizzazione dello stato di sovraccarico.

In locale i test utilizzano il ChromeDriver presente nella cartella `drivers`, mentre nella pipeline GitHub Actions ChromeDriver viene configurato automaticamente per l’ambiente Linux del runner. In CI i test vengono eseguiti in modalità headless.

Questi test non sostituiscono i test JUnit sul nucleo logico, ma verificano che l’interfaccia web sia correttamente collegata alla logica Java del sistema.

---

## 26. Verifica tramite ESC/OpenJML

La verifica statica è stata eseguita mediante ESC di OpenJML.

L’obiettivo della verifica è controllare che i metodi della classe rispettino le specifiche JML definite tramite:

- invarianti di classe;
- precondizioni;
- postcondizioni;
- annotazioni sui cicli;
- metodi `pure`.

La verifica ha permesso di controllare la coerenza del codice rispetto alle proprietà dichiarate.

Alcuni metodi con logica decisionale più articolata, come `scegliDirezione`, possono risultare più difficili da dimostrare completamente tramite verifica automatica. Questo non indica necessariamente un errore nel comportamento, ma rappresenta un limite pratico della verifica statica.

Per questo motivo, la verifica statica tramite JML è stata affiancata da test JUnit sul comportamento eseguibile e da test Selenium per verificare l’integrazione dell’interfaccia web con il nucleo Java.

---

## 27. Confronto con i requisiti del sistema

La classe `Ascensore` e le classi di supporto soddisfano i principali requisiti funzionali del sistema:

| Requisito | Implementazione |
|---|---|
| Gestione dei piani | `pianoCorrente`, `pianoValido`, `indiceDelPiano` |
| Richieste interne | `aggiungiRichiestaInterna` |
| Chiamate esterne verso l’alto | `aggiungiChiamataSalita` |
| Chiamate esterne verso il basso | `aggiungiChiamataDiscesa` |
| Movimento della cabina | `muoviDiUnPiano` |
| Scelta della direzione | `scegliDirezione` |
| Apertura porte al piano servito | `serviPianoCorrente` |
| Chiusura porte | `chiudiPorte` |
| Stato di attesa | `mettiInAttesa` |
| Gestione persone | `aggiornaPersone` |
| Gestione sovraccarico | `gestisciSovraccarico` |
| Gestione guasto | `attivaGuasto` |
| Ripristino da guasto | `gestisciTimerGuasto` |
| Input esterni | `InputAscensore` |
| Coordinamento del passo logico | `ControlloreAscensore.eseguiPasso` |
| Test funzionali del nucleo Java | `ControlloreAscensoreTest` |
| Test parametrici su classi di input e casi limite | `AscensoreParametricTest` |
| Interfaccia web dimostrativa | `AscensoreHttpServer`, `index.html`, `style.css`, `script.js` |
| Test dell’interfaccia web | `AscensoreWebSeleniumTest` |
| Continuous Integration | `java-ci.yml` |

---

## 28. Conclusione

L’implementazione Java rappresenta il nucleo logico principale del sistema ascensore modellato in ASM.

La classe `Ascensore` contiene lo stato interno del sistema e i metodi fondamentali per modificarlo. La classe `InputAscensore` raccoglie gli input esterni, mentre `ControlloreAscensore` coordina l’evoluzione del sistema a ogni passo logico.

La specifica JML permette di formalizzare le principali proprietà di sicurezza e coerenza dello stato, tra cui:

- validità del piano corrente;
- validità del timer;
- non negatività del numero di persone;
- assenza di valori nulli negli stati;
- movimento consentito solo con porte chiuse;
- movimento consentito solo in assenza di errore;
- comportamento corretto in stato di guasto;
- comportamento corretto in stato di sovraccarico;
- gestione corretta delle richieste attive.

Il codice Java non sostituisce il modello ASM completo, ma ne implementa una parte significativa in forma eseguibile e verificabile.

La verifica tramite ESC/OpenJML controlla il rispetto delle specifiche JML sui metodi principali della classe `Ascensore`. I test JUnit 5 verificano invece il comportamento operativo del sistema nei principali scenari previsti, mentre i test parametrici rafforzano il controllo su classi di input, valori limite e condizioni di validità.

Il collegamento tra scenari AVALLA e test JUnit mostra la continuità tra validazione del modello ASM e verifica dell’implementazione Java: alcuni comportamenti validati nel modello astratto sono stati ripresi anche nei test eseguibili del nucleo Java.

L’interfaccia web dimostrativa permette di osservare dinamicamente il comportamento del nucleo Java e di interagire con il sistema in modalità automatica o manuale. Poiché utilizza direttamente `Ascensore`, `ControlloreAscensore` e `InputAscensore`, essa rimane coerente con la logica implementativa verificata. I test Selenium completano questa parte controllando che pagina web, server HTTP e nucleo logico Java interagiscano correttamente.

Infine, i test automatici JUnit, i test parametrici e i test Selenium sono stati integrati in un workflow GitHub Actions, così da automatizzare la compilazione del progetto e l’esecuzione dei test a ogni modifica del repository.

Nel complesso, la parte Java/JML può essere considerata coerente con il modello formale e adeguata come nucleo implementativo del progetto.
