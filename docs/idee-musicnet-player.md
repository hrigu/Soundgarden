# Idee (noch nicht umgesetzt): musicnet-player

Eigenständiges Projekt, getrennt von Soundgarden — hier nur als Notiz festgehalten, damit die
Idee nicht verloren geht.

## Grundidee

`musicnet` (die Rails-Webapp: Suchen/Finden/Sortieren von Tracks/Playlists, siehe
`hrigu/musicnet`) übernimmt weiterhin die Bibliotheks- und Queue-Verwaltung. Die eigentliche
DJ-Funktionalität — Wiedergabe, Lautstärke, Filter, Vorhören/Cue — soll stattdessen in
SuperCollider laufen, ferngesteuert von der Webapp aus.

Bewährtes Architekturmuster dafür (kein Neuland): Steuerung getrennt vom Klangmotor, verbunden
per OSC — genau wie bei **Sonic Pi** (Ruby-artige Steuersprache → OSC → eingebetteter
SuperCollider-Server) und **TidalCycles + SuperDirt** (Pattern-Sprache → OSC → SuperCollider-
Sample-Player/Mixer).

## Grober Zuschnitt

- **musicnet** (Rails) — Suchen/Finden/Queue befüllen, bleibt wie es ist.
- **musicnet-player** (neues Repo, SuperCollider) — Klangmotor: Track laden/abspielen,
  Lautstärke, Filter, Vorhören/Cue-Kanal. Hört per `OSCdef` auf eigene Adressen
  (z.B. `/deck1/play`, `/deck1/volume`).
- Verbindung: Rails kann OSC direkt verschicken (z.B. Gem `osc-ruby`), kein extra
  Bridge-Server nötig, da OSC schlicht UDP ist.
- Vorhören/Cue: braucht ein Audio-Interface mit genug Ausgangskanälen (Hauptmix an die PA,
  Cue-Kanal separat an Kopfhörer).
- Die JITLib-Deck-Logik aus Soundgarden (`boot.scd`/`fx.scd`/`set_template.scd` — Track laden,
  FX-Kette, klickfreies Umschalten per `Ndef`) ist eine gute Ausgangsbasis für den
  SuperCollider-seitigen Klangmotor.

## Nächster Schritt, sobald es losgeht

Neues Repo `musicnet-player` anlegen (aktuell nicht möglich — GitHub-Anbindung dieser Session
hat keine Berechtigung zum Anlegen neuer Repositories), IDD-Grundgerüst (`.intents/`,
`AGENTS.md`/`CLAUDE.md`) und SuperCollider-Grundgerüst analog zu Soundgarden aufsetzen.
