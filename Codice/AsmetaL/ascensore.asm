asm ascensore

import StandardLibrary

signature:

	domain Piano subsetof Integer
	domain Timer subsetof Integer
	
	enum domain Direzione = {SU | GIU | NESSUNA} 
    enum domain StatoCabina = {FERMA | IN_MOVIMENTO | BLOCCATA} 
    enum domain StatoPorte = {APERTE | CHIUSE} 
    enum domain StatoErrore = {NESSUNO | OVERLOAD | GUASTO} 
	
	monitored pulsanteInterno: Piano -> Boolean
	monitored chiamataPianoSu: Piano -> Boolean
	monitored chiamataPianoGiu: Piano -> Boolean
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
	
	//derived guastoAttivo: Boolean
	//derived overloadAttivo: Boolean

definitions:

	domain Piano = {-1:4}
	domain Timer = {0:10}
	
	function tMax = 10
	function capacitaMassima = 8
	function pianoMin = -1
	function pianoMax = 4
	
	/*function guastoAttivo = 
		statoErrore = GUASTO*/
	
	/*function overloadAttivo =
		statoErrore = OVERLOAD*/
		
	rule r_acquisisciRichieste =
		forall $p in Piano with
			pulsanteInterno($p) or
			($p < pianoMax and chiamataPianoSu($p)) or
			($p > pianoMin and chiamataPianoGiu($p))
		do
			richiesteAttive($p) := true
			
	rule r_aggiornaPersone =
		if statoCabina = FERMA and statoPorte = APERTE then
			if numeroPersone + personeEntrate - personeUscite >= 0 then
				numeroPersone := numeroPersone + personeEntrate - personeUscite
			else
				numeroPersone := 0
			endif
		endif
		
	rule r_gestisciErrori =
	    if numeroPersone > capacitaMassima or statoErrore = OVERLOAD then
	        r_gestisciOverload[]
	    else
	        r_gestisciGuasto[]
	    endif
			
	//rule r_gestisciOverload =
	//rule r_gestisciGuasto =
	//rule r_gestisciAscensore =
		
	main rule r_main =
		seq
			r_acquisisciRichieste[]
			r_aggiornaPersone[]
			r_gestisciErrori[]
			r_gestisciAscensore[]
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
