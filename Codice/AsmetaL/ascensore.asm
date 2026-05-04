asm ascensore

import StandardLibrary

signature:

	// Domini numerici
	domain Piano subsetof Integer
	domain Timer subsetof Integer
	
	// Stati principali dell'ascensore
	enum domain Direzione = {SU | GIU | NESSUNA} 
	enum domain StatoCabina = {FERMA | IN_MOVIMENTO | BLOCCATA} 
	enum domain StatoPorte = {APERTE | CHIUSE} 
	enum domain StatoErrore = {NESSUNO | OVERLOAD | GUASTO} 
	
	// Input esterni
	monitored chiamataPianoSu: Piano -> Boolean
	monitored chiamataPianoGiu: Piano -> Boolean
	monitored pulsanteInterno: Piano -> Boolean
	monitored eventoGuasto: Boolean
	monitored personeEntrate: Integer
	monitored personeUscite: Integer
	
	// Variabili controllate dal sistema
	controlled pianoCorrente: Piano
	controlled statoCabina: StatoCabina
	controlled statoPorte: StatoPorte
	controlled direzione: Direzione
	controlled richiesteAttive: Piano -> Boolean
	controlled statoErrore: StatoErrore
	controlled timer: Timer
	controlled numeroPersone: Integer
	
	// Costanti
	static tMax: Integer
	static capacitaMassima: Integer
	static pianoMin: Piano
	static pianoMax: Piano

definitions:

	// Valori concreti dei domini e delle costanti
	domain Piano = {-1:4}
	domain Timer = {0:10}
	
	function tMax = 10
	function capacitaMassima = 8
	function pianoMin = -1
	function pianoMax = 4
		
	// Registra le richieste valide tra quelle attive
	rule r_acquisisciRichieste =
		forall $p in Piano with
			pulsanteInterno($p) or
			($p < pianoMax and chiamataPianoSu($p)) or
			($p > pianoMin and chiamataPianoGiu($p))
		do
			richiesteAttive($p) := true
			
	// Aggiorna il numero di persone quando le porte sono aperte
	rule r_aggiornaPersone =
	    if statoPorte = APERTE and (statoCabina = FERMA or statoCabina = BLOCCATA) then
	        if numeroPersone + personeEntrate - personeUscite >= 0 then
	            numeroPersone := numeroPersone + personeEntrate - personeUscite
	        else
	            numeroPersone := 0
	        endif
	    endif
		
	// Gestisce l'ingresso e l'uscita dallo stato OVERLOAD
	rule r_gestisciOverload =
		if numeroPersone > capacitaMassima then
			par
				statoErrore := OVERLOAD
				statoCabina := BLOCCATA
				statoPorte := APERTE
				direzione := NESSUNA
			endpar
		else if statoErrore = OVERLOAD and numeroPersone <= capacitaMassima then
				par
					statoErrore := NESSUNO
					statoCabina := FERMA
					statoPorte := CHIUSE
					direzione := NESSUNA
				endpar
			endif
		endif
		
	// Gestisce l'ingresso in GUASTO e il timer di ripristino
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
			endif
		endif
		
	// Seleziona quale errore gestire, dando priorità all'OVERLOAD
	rule r_gestisciErrori =
	    if numeroPersone > capacitaMassima or statoErrore = OVERLOAD then
	        r_gestisciOverload[]
	    else
	        r_gestisciGuasto[]
	    endif	
	
	// Chiude le porte quando l'ascensore è fermo e operativo
	rule r_chiudiPorte =
	    if statoCabina = FERMA and statoPorte = APERTE and statoErrore = NESSUNO then
	        statoPorte := CHIUSE
	    endif

	// Serve la richiesta relativa al piano corrente
	rule r_serviPianoCorrente =
	    if statoErrore = NESSUNO and richiesteAttive(pianoCorrente) then
	        par
	            statoCabina := FERMA
	            statoPorte := APERTE
	            richiesteAttive(pianoCorrente) := false
	            direzione := NESSUNA
	        endpar
	    endif

	// Porta l'ascensore in stato di attesa
	rule r_idle =
	    par
	        statoCabina := FERMA
	        direzione := NESSUNA
	    endpar

	// Determina la direzione in base alle richieste attive
	rule r_scegliDirezione =
	    if (exist $p0 in Piano with richiesteAttive($p0)) then
	
	        if direzione = SU then
	            if (exist $p1 in Piano with $p1 > pianoCorrente and richiesteAttive($p1)) then
	                direzione := SU
	            else if (exist $p2 in Piano with $p2 < pianoCorrente and richiesteAttive($p2)) then
	                direzione := GIU
	            else
	                direzione := NESSUNA
	            endif endif
	
	        else if direzione = GIU then
	            if (exist $p3 in Piano with $p3 < pianoCorrente and richiesteAttive($p3)) then
	                direzione := GIU
	            else if (exist $p4 in Piano with $p4 > pianoCorrente and richiesteAttive($p4)) then
	                direzione := SU
	            else
	                direzione := NESSUNA
	            endif endif
	
	        else
	            if (exist $p5 in Piano with $p5 > pianoCorrente and richiesteAttive($p5)) then
	                direzione := SU
	            else if (exist $p6 in Piano with $p6 < pianoCorrente and richiesteAttive($p6)) then
	                direzione := GIU
	            else
	                direzione := NESSUNA
	            endif endif
	        endif endif
	
	    else
	        direzione := NESSUNA
	    endif

	// Muove la cabina di un piano nella direzione scelta
	rule r_muoviAscensore =
	    if statoErrore = NESSUNO and statoPorte = CHIUSE then
	
	        if direzione = SU and pianoCorrente < pianoMax then
	            par
	                pianoCorrente := pianoCorrente + 1
	                statoCabina := IN_MOVIMENTO
	            endpar
	
	        else if direzione = GIU and pianoCorrente > pianoMin then
	            par
	                pianoCorrente := pianoCorrente - 1
	                statoCabina := IN_MOVIMENTO
	            endpar
	
	        else
	            par
	                statoCabina := FERMA
	                direzione := NESSUNA
	            endpar
	        endif endif
	
	    endif

	// Coordina il comportamento normale dell'ascensore
	rule r_gestisciAscensore =
	    if statoErrore = NESSUNO then
	
	        if statoPorte = APERTE then
	            r_chiudiPorte[]
	
	        else if richiesteAttive(pianoCorrente) then
	            r_serviPianoCorrente[]
	
	        else if (exist $p in Piano with richiesteAttive($p)) then
	            seq
	                r_scegliDirezione[]
	                r_muoviAscensore[]
	            endseq
	
	        else
	            r_idle[]
	
	        endif endif endif
	
	    endif
		
	main rule r_main =
	    seq
	        if statoErrore != GUASTO then
	            r_acquisisciRichieste[]
	        endif
	
	        r_aggiornaPersone[]
	        r_gestisciErrori[]
	
	        if statoErrore = NESSUNO then
	            r_gestisciAscensore[]
	        endif
	    endseq
		
default init s0:

	function pianoCorrente = 0
	function timer = 0
	function numeroPersone = 0
	function statoCabina = FERMA
	function statoPorte = CHIUSE
	function direzione = NESSUNA
	function statoErrore = NESSUNO
	
	function richiesteAttive($p in Piano) = false