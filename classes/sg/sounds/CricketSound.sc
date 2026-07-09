// CricketSound — InsectSound-Ableitung mit auf Gryllus-bimaculatus-Bioakustik basierenden
// Default-Werten (Intent 42). Quellen: PMC4718538/PubMed 26785351 (PLOS ONE, "No Effect of
// Body Size on the Frequency of Calling and Courtship Song in the Two-Spotted Cricket"), eine
// bioRxiv/ResearchGate-Studie "Calling song maturity in two-spotted cricket", songsofinsects.com
// -- dominante Trägerfrequenz ~4.6-4.8kHz, Silben-/Pulsrate 25-30Hz, 3-5 Silben pro Zirplaut à
// 15-23ms, Zirplaut-Dauer 120-200ms, Zirp-Wiederholrate 2-3Hz. wingRate wird hier als Silben-/
// Pulsrate gelesen (dasselbe LFPulse-treibt-Ringz-Resonanzen-Modell wie bei InsectSound, nur
// biologisch als Stridulation statt Flügelschlag interpretiert). ringFreq1/ringFreq2 bewusst
// nah beieinander -- Grillen haben EINE dominante Trägerfrequenz, kein zweites Formant wie im
// generischen Insekt-Modell; das ist eine Anpassungsentscheidung, keine weitere sourced
// Tatsache. Timing/Pattern-Details siehe Intent 31 (Puls/Chirp-Zeitmodell).
CricketSound : InsectSound {

	*new { |wingRate = 27, wingDuty = 0.5, ringFreq1 = 4600, ringFreq2 = 4700,
			ringDecay1 = 0.02, ringDecay2 = 0.015, amp = 0.35, pattern|
		^super.new(wingRate, wingDuty, ringFreq1, ringFreq2, ringDecay1, ringDecay2, amp,
			pattern ?? { CricketSound.makePattern });
	}

	// erzeugt ein zur Grillen-Zirplaut-Struktur passendes CallingPattern über
	// RhythmPatternCreator (Intent 33), statt Intent 31s einem festen Beispiel-Pattern hart zu
	// verdrahten -- jeder Aufruf liefert eine neue, unabhängig zufällig gewichtete Instanz,
	// damit mehrere Grillen in einer Komposition nicht synchron zirpen (Intent 42).
	// chirpDur/chirpPeriod aus der Recherche: Zirplaut-Dauer ~160ms, Zirp-Wiederholrate ~3Hz
	// (-> Periode ~330ms). numSegments=8 mit RhythmPatternCreators strikt alternierendem An/
	// Aus (startWithSound: true) ergibt 4 Puls- und 4 Pausen-Segmente, analog Intent 31s
	// Beispiel (4 Puls/Pause-Paare, letztes Aus-Segment als Interchirp-Pause).
	*makePattern { |chirpDur = 0.16, chirpPeriod = 0.33, numSegments = 8|
		^RhythmPatternCreator.new(
			totalDur: chirpPeriod,
			numSegments: numSegments,
			soundRatio: chirpDur / chirpPeriod,
			startWithSound: true
		).createNewPattern;
	}
}
