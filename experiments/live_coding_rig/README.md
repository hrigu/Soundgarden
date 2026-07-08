# Live-Coding-Rig (`fx.scd` + `set_template.scd`)

Der ursprüngliche Zweck von Soundgarden: bestehende elektronische Tracks während einer
Fusion-Tanzparty live umformen (Filter, Echo, Bitcrush, Stutter, Reverb) – ohne Aussetzer, rein
per Tastatur (`Cmd+Enter` je Block), kein MIDI-Controller nötig.

## Vorher

1. `boot/load_classes.scd` – nur beim Erst-Setup nötig (persistiert in `sclang_conf.yaml`,
   danach automatisch bei jedem Start eingebunden, siehe Haupt-`README.md`).
2. `boot/boot.scd` – bootet den Server und lädt alle Dateien aus `sounds/` in `~tracks`.

## Bedienung

1. **`fx.scd`** ausführen – definiert `~fxChains`, eine Sammlung wiederverwendbarer FX-Bausteine.
   Danach nicht mehr angefasst werden muss, solange keine neue Kette gebraucht wird.
2. **`set_template.scd`** öffnen und Block für Block während des Sets ausführen:
   - **Track starten** – `Ndef(\track, { ... })` mit einem Buffer aus `~tracks`.
   - **Durch die FX-Kette schicken** – `Ndef(\fx, { ~fxChains[\clean].(Ndef.ar(\track, 2)) })`,
     dann `Ndef(\fx).play`.
   - **FX-Kette wechseln** – `Ndef(\fx, { ~fxChains[\filterSweep].(...) })` (oder `\echo`,
     `\bitcrush`, `\stutterGate`, `\reverb`) einfach neu ausführen.
   - **Parameter live verstellen** – `Ndef(\fx).set(\cutoff, 12000)` usw., während der Synth
     weiterläuft.
   - **Nächster Track** – `Ndef(\track, { ... })` mit anderem Buffer neu ausführen.

Der Trick: `Ndef` (JITLib) überblendet bei jeder Neudefinition automatisch (`fadeTime`) – kein
Klicken, kein Stottern, obwohl der laufende Synth komplett neu geschrieben wird. Das gilt sowohl
für `Ndef(\track)` (Crossfade zwischen Tracks) als auch für `Ndef(\fx)` (Wechsel der Effektkette).

## Was man damit machen kann

- **Build-up/Drop**: `\filterSweep`, Cutoff von tief nach hoch fahren.
- **Break**: `\stutterGate`, Gate-Rate variieren (schnell = zackig, langsam = stotternd).
- **Übergang zwischen Tracks**: `\echo` mit hohem `mix`/`decay`, während im Hintergrund schon der
  nächste Track in `Ndef(\track)` startet (Crossfade via `fadeTime`).
- **Lo-Fi-Charakter**: `\bitcrush` (nur Core-UGens, kein `sc3-plugins` nötig).
- **Ausklang**: `\reverb`, dann `Ndef(\track).clear`/`Ndef(\fx).clear` als Panic/Reset.
- **Eigene Ketten ergänzen**: neuer Eintrag in `~fxChains` in `fx.scd`, gleiche Signatur
  (`{ |sig| ... }`, gibt bearbeitetes Stereo-Signal zurück) – sofort in `set_template.scd`
  nutzbar.

`set_template.scd` selbst ist eine Vorlage, kein Skript, das man von oben nach unten durchlaufen
lässt – die Blockreihenfolge im File zeigt nur ein Beispiel-Set; welcher Block wann drankommt,
entscheidet sich live während der Party.
