asm ascensore_smv_small

import StandardLibrary
import CTLLibrary

signature:

	domain Piano subsetof Integer
	domain Timer subsetof Integer
	
	enum domain Direzione = {SU | GIU | NESSUNA}
	enum domain StatoCabina = {FERMA | IN_MOVIMENTO | BLOCCATA}
	enum domain StatoPorte = {APERTE | CHIUSE}
	enum domain StatoErrore = {NESSUNO | GUASTO}
	
	monitored richiestaPresente: Boolean
	monitored pianoRichiesto: Piano
	monitored eventoGuasto: Boolean
	
	controlled pianoCorrente: Piano
	controlled statoCabina: StatoCabina
	controlled statoPorte: StatoPorte
	controlled direzione: Direzione
	controlled statoErrore: StatoErrore
	controlled timer: Timer
	
	controlled richiesta0: Boolean
	controlled richiesta1: Boolean
	controlled richiesta2: Boolean
	
	derived esisteRichiesta: Boolean
	derived richiestaPianoCorrente: Boolean
	derived richiestaSopra: Boolean
	derived richiestaSotto: Boolean
	
	static tMax: Timer

definitions:

	domain Piano = {0:2}
	domain Timer = {0:2}
	
	function tMax = 2
	
	function esisteRichiesta =
		richiesta0 or richiesta1 or richiesta2
	
	function richiestaPianoCorrente =
		if pianoCorrente = 0 then
			richiesta0
		else if pianoCorrente = 1 then
			richiesta1
		else
			richiesta2
		endif endif
	
	function richiestaSopra =
		if pianoCorrente = 0 then
			richiesta1 or richiesta2
		else if pianoCorrente = 1 then
			richiesta2
		else
			false
		endif endif
	
	function richiestaSotto =
		if pianoCorrente = 2 then
			richiesta0 or richiesta1
		else if pianoCorrente = 1 then
			richiesta0
		else
			false
		endif endif

	rule r_acquisisciRichiesta =
		if richiestaPresente then
			par
				if pianoRichiesto = 0 then
					richiesta0 := true
				endif
				
				if pianoRichiesto = 1 then
					richiesta1 := true
				endif
				
				if pianoRichiesto = 2 then
					richiesta2 := true
				endif
			endpar
		endif

	rule r_gestisciGuasto =
		if statoErrore = GUASTO then
			if timer > 0 then
				timer := timer - 1
			else
				par
					statoErrore := NESSUNO
					statoCabina := FERMA
					statoPorte := CHIUSE
					direzione := NESSUNA
					timer := 0
				endpar
			endif
		else if eventoGuasto = true and statoErrore = NESSUNO then
			par
				statoErrore := GUASTO
				statoCabina := BLOCCATA
				statoPorte := CHIUSE
				direzione := NESSUNA
				timer := tMax
			endpar
		endif endif

	rule r_chiudiPorte =
		if statoCabina = FERMA and statoPorte = APERTE and statoErrore = NESSUNO then
			statoPorte := CHIUSE
		endif

	rule r_serviPianoCorrente =
		if statoErrore = NESSUNO and richiestaPianoCorrente then
			par
				statoCabina := FERMA
				statoPorte := APERTE
				direzione := NESSUNA
				
				if pianoCorrente = 0 then
					richiesta0 := false
				endif
				
				if pianoCorrente = 1 then
					richiesta1 := false
				endif
				
				if pianoCorrente = 2 then
					richiesta2 := false
				endif
			endpar
		endif

	rule r_idle =
		par
			statoCabina := FERMA
			direzione := NESSUNA
		endpar

	rule r_scegliEMuovi =
		if statoErrore = NESSUNO and statoPorte = CHIUSE then
			if richiestaSopra then
				par
					direzione := SU
					statoCabina := IN_MOVIMENTO
					
					if pianoCorrente = 0 then
						pianoCorrente := 1
					endif
					
					if pianoCorrente = 1 then
						pianoCorrente := 2
					endif
				endpar
			else if richiestaSotto then
				par
					direzione := GIU
					statoCabina := IN_MOVIMENTO
					
					if pianoCorrente = 2 then
						pianoCorrente := 1
					endif
					
					if pianoCorrente = 1 then
						pianoCorrente := 0
					endif
				endpar
			else
				par
					statoCabina := FERMA
					direzione := NESSUNA
				endpar
			endif endif
		endif

	rule r_gestisciAscensore =
		if statoErrore = NESSUNO then
			if statoPorte = APERTE then
				r_chiudiPorte[]
			else if richiestaPianoCorrente then
				r_serviPianoCorrente[]
			else if esisteRichiesta then
				r_scegliEMuovi[]
			else
				r_idle[]
			endif endif endif
		endif

	// ============================================================
	// Proprietà CTL per model checking con AsmetaSMV
	// ============================================================

	// P1 - La cabina non deve mai muoversi con le porte aperte.
	// Se la cabina è in movimento, allora le porte devono essere chiuse.
	CTLSPEC ag(statoCabina = IN_MOVIMENTO implies statoPorte = CHIUSE)

	// P2 - La cabina può muoversi solo se il sistema non è in stato di guasto.
	// Questo impedisce movimenti durante condizioni anomale.
	CTLSPEC ag(statoCabina = IN_MOVIMENTO implies statoErrore = NESSUNO)

	// P3 - Durante il guasto la cabina deve essere bloccata.
	// Il sistema non deve permettere movimento quando statoErrore = GUASTO.
	CTLSPEC ag(statoErrore = GUASTO implies statoCabina = BLOCCATA)

	// P4 - Durante il guasto le porte devono restare chiuse.
	// Questa proprietà rappresenta una condizione di sicurezza.
	CTLSPEC ag(statoErrore = GUASTO implies statoPorte = CHIUSE)

	// P5 - Durante il guasto non deve esserci una direzione attiva.
	// La direzione viene annullata impostandola a NESSUNA.
	CTLSPEC ag(statoErrore = GUASTO implies direzione = NESSUNA)

	// P6 - Se le porte sono aperte e il sistema non è in guasto,
	// allora la cabina deve essere ferma.
	CTLSPEC ag(statoPorte = APERTE and statoErrore = NESSUNO implies statoCabina = FERMA)

	// P7 - Se la cabina è bloccata, allora il sistema deve essere in guasto.
	// Nel modello ridotto l'unico stato anomalo modellato è GUASTO.
	CTLSPEC ag(statoCabina = BLOCCATA implies statoErrore = GUASTO)

	// P8 - Se il timer è maggiore di zero, allora il sistema deve essere in guasto.
	// Il timer viene usato solo per gestire il ripristino dal guasto.
	CTLSPEC ag(timer > 0 implies statoErrore = GUASTO)

	// P9 - Se il sistema è in guasto con timer uguale a zero,
	// al passo successivo deve tornare operativo.
	CTLSPEC ag(statoErrore = GUASTO and timer = 0 implies ax(statoErrore = NESSUNO))

	// P10 - Da ogni stato è possibile raggiungere uno stato senza guasto.
	// Verifica che il guasto non sia permanente.
	CTLSPEC ag(ef(statoErrore = NESSUNO))

	// P11 - Da ogni stato è possibile raggiungere uno stato con cabina ferma.
	// Verifica che il sistema possa sempre tornare a uno stato stabile.
	CTLSPEC ag(ef(statoCabina = FERMA))

	// P12 - Esiste almeno un'esecuzione in cui la cabina può muoversi verso l'alto.
	// Verifica che il movimento verso piani superiori sia raggiungibile.
	CTLSPEC ef(direzione = SU and statoCabina = IN_MOVIMENTO)

	// P13 - Esiste almeno un'esecuzione in cui la cabina può muoversi verso il basso.
	// Verifica che il movimento verso piani inferiori sia raggiungibile.
	CTLSPEC ef(direzione = GIU and statoCabina = IN_MOVIMENTO)

	// P14 - Esiste almeno un'esecuzione in cui il sistema entra in guasto.
	// Verifica che lo stato GUASTO sia effettivamente raggiungibile.
	CTLSPEC ef(statoErrore = GUASTO)

	main rule r_main =
		seq
			if statoErrore != GUASTO then
				r_acquisisciRichiesta[]
			endif

			r_gestisciGuasto[]

			if statoErrore = NESSUNO then
				r_gestisciAscensore[]
			endif
		endseq

default init s0:

	function pianoCorrente = 0
	function statoCabina = FERMA
	function statoPorte = CHIUSE
	function direzione = NESSUNA
	function statoErrore = NESSUNO
	function timer = 0
	
	function richiesta0 = false
	function richiesta1 = false
	function richiesta2 = false