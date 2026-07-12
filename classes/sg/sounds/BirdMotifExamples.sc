// BirdMotifExamples — Sammlung Messiaen-inspirierter Beispiel-Motive für MessiaenBirdSound
// (Intent 59). Bewusst getrennt von BirdMotif selbst: BirdMotif ist die generische
// Datenstruktur, hier steckt die konkrete musikalische Komposition -- keine Zitate aus dem
// urheberrechtlich geschützten Catalogue d'oiseaux, sondern eigene, an dessen charakteristischen
// Merkmalen orientierte Motive (perlende Läufe, Triller-Kaskaden, große Intervallsprünge).
// Halbtonschritte/Dauern sind ein erster Entwurf, im Hörtest (demos/sounds/messiaenbird.scd)
// musikalisch nachjustierbar -- wie bei CricketSound/BirdSound kein TDD-Anspruch an die
// Klangqualität selbst, nur an die Struktur (Notenanzahl/Dauer-Bereiche, siehe
// TestBirdMotifExamples).
BirdMotifExamples {

	// perlender, schneller Lauf (Amsel-artig): Auf-und-ab in kleinen Schritten, gleichmäßig kurze
	// Noten.
	*blackbirdRun {
		var steps = [0, 2, 4, 5, 7, 5, 4, 2];
		var durs = steps.collect { |step| rrand(0.08, 0.15) };
		^BirdMotif.fromIntervals(3200, steps, durs)
	}

	// sehr schnelle Triller-Kaskade (Zaunkönig-artig): viele, sehr kurze Noten, eng benachbarte
	// Tonhöhen, die zwischen zwei Nachbartönen alternieren.
	*wrenTrillCascade {
		var steps = (0..15).collect { |i| if(i.even) { 0 } { 1 } };
		var durs = steps.collect { |step| rrand(0.02, 0.05) };
		^BirdMotif.fromIntervals(5500, steps, durs)
	}

	// großer Intervallsprung-Rufvogel: wenige, längere Noten mit einem Sprung von einer Oktave
	// oder mehr, gefolgt von einer kurzen Rückkehr.
	*callBirdLeaps {
		var steps = [0, 14, 2];
		var durs = steps.collect { |step| rrand(0.15, 0.4) };
		^BirdMotif.fromIntervals(2200, steps, durs)
	}
}
