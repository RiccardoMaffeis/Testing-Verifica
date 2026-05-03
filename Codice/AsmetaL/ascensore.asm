asm ascensore

import StandardLibrary

signature:

	domain Piano subsetof Integer
	domain Timer subsetof Integer
	
	enum domain Direzione = {SU | GIU | NESSUNA} 
    enum domain StatoCabina = {FERMA | IN_MOVIMENTO | BLOCCATA} 
    enum domain StatoPorte = {APERTE | CHIUSE} 
    enum domain StatoErrore = {NESSUNO | OVERLOAD | GUASTO} 
	
	monitored chiamataPianoSu: Piano -> Boolean
	monitored chiamataPianoGiu: Piano -> Boolean
	monitored pulsanteInterno: Piano -> Boolean
	monitored eventoGuasto: Boolean
	monitored personeEntrate: Integer
	monitored personeUscite: Integer
	
	controlled pianoCorrente: Piano
	controlled statoCabina: StatoCabina
	controlled statoPorte: StatoPorte
	controlled direzione: Direzione
	controlled richiesteAttive: Piano -> Boolean
	controlled statoErrore: StatoErrore
	controlled timer: Timer
	controlled numeroPersone: Integer
	
	static tMax: Integer
	static capacitaMassima: Integer
	static pianoMin: Piano
	static pianoMax: Piano

definitions:

	domain Piano = {-1:4}
	domain Timer = {0:10}
	
	function tMax = 10
	function capacitaMassima = 8
	function pianoMin = -1
	function pianoMax = 4
		
	rule r_acquisisciRichieste =
		forall $p in Piano with
			pulsanteInterno($p) or
			($p < pianoMax and chiamataPianoSu($p)) or
			($p > pianoMin and chiamataPianoGiu($p))
		do
			richiesteAttive($p) := true
			
	rule r_aggiornaPersone =
	    if statoPorte = APERTE and (statoCabina = FERMA or statoCabina = BLOCCATA) then
	        if numeroPersone + personeEntrate - personeUscite >= 0 then
	            numeroPersone := numeroPersone + personeEntrate - personeUscite
	        else
	            numeroPersone := 0
	        endif
	    endif
		
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
		
	rule r_gestisciErrori =
	    if numeroPersone > capacitaMassima or statoErrore = OVERLOAD then
	        r_gestisciOverload[]
	    else
	        r_gestisciGuasto[]
	    endif	
	
	/*rule r_gestisciAscensore =
		if statoErrore = NESSUNO then
			statoPorte := CHIUSE
		endif*/
		
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
