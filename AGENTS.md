# AGENTS.md

Dieses Dokument enthält verbindliche Anweisungen für Coding-Agents, die in diesem Repository
arbeiten. Es ist die eigene, für Soundgarden angepasste Fassung des Intent-Driven-Development-
Ansatzes (IDD), inspiriert von `hrigu/musicnet` — bewusst ohne dessen Rails-spezifische Teile
(rspec/rubocop, Domain-Resource-Action-Web-Architektur, Devise/OmniAuth-Auth) und ohne das dort
eingebettete, nie zu diesem Projekt gehörende Phoenix/Elixir-Beispiel.

## Sprache

- Antworten in der Sprache des Prompts (i.d.R. Deutsch).
- Code-Kommentare auf Deutsch.
- Commit-Messages auf Deutsch: Titelzeile kurz und präzise (Imperativ, Präsens), optionaler Body
  mit Warum/Impact in 1–3 Sätzen.

## Arbeitsstil

- Präzise, direkt, umsetzungsorientiert. Kleine, fokussierte Diffs.
- Kein Refactoring unbeteiligten Codes ohne expliziten Auftrag.
- Nichts Zerstörerisches (`git reset --hard`, `rm -rf`, force-push, etc.) ohne explizite Anweisung.
- Vor riskanten/schwer rückgängig zu machenden Aktionen: nachfragen statt annehmen.

## Intent Driven Development (IDD)

Alle nicht-trivialen Änderungen **müssen** zuerst als Intent geplant werden (Why/What/How),
bevor Code entsteht. Ausnahme: triviale Einzeiler ohne Entwurfscharakter (Tippfehler,
offensichtliche Korrekturen).

### Intent-Struktur

- H1-Titel: `<Nummer> - <Typ> (<Domain>): <Titel>`, z.B.
  `5 - Feature (Spatial-Audio): ATK-basiertes HRTF für das Insekt`.
- Typ: `Feature`, `Enhancement`, `Bug`, `Test`, `Research`.
- Domains in diesem Projekt (bei Bedarf erweiterbar):
  - `Live-Set` — `boot/boot.scd`/`experiments/live_coding_rig/fx.scd`/
    `experiments/live_coding_rig/set_template.scd`, der DJ-Live-Coding-Workflow.
  - `Boot-Tracks` — Track-Erkennung/-Loading-Logik.
  - `Spatial-Audio` — `Movable`/`Listener`/`SoundObject`, binaurale Klangobjekte.
- Pflicht-Abschnitte:
  - **1. Why** — Objective, Context, optional Depends On/Related To Intents.
  - **2.0 What** — Gherkin-Szenarien (bei Feature/Enhancement/Bug/Test) oder eine kurze
    Beschreibung des Ergebnisses (bei Research-Intents, die noch keine Umsetzung festlegen).
  - **3.0 How** — Implementation Context + Tasks/Subtasks als Checkliste (`[ ]`/`[x]`).
- Optional: **Challenges & Solutions** — nur nachträglich ergänzt, wenn beim Umsetzen etwas
  nicht wie geplant lief (siehe Intent 1 als Beispiel).

### Ablage & Tracking

- `.intents/{todo,work-in-progress,completed}/` — ein Intent wandert `todo` →
  `work-in-progress` (sobald Arbeit beginnt) → `completed` (sobald alle Tasks `[x]` sind).
- Dateiname: `<Nummer>.<typ>_<domain>_<slug>.md` (Typ/Domain/Slug klein geschrieben,
  Bindestrich-getrennt).
- `<n>.last_intent_created` — leere Marker-Datei, trackt die zuletzt vergebene Nummer nur über
  den Dateinamen (bei jedem neuen Intent per `git mv` hochzählen).
- Jeder Übergang (Intent erstellt/aktualisiert/verschoben) wird **vor** der nächsten Aktion
  committet.

### Protokoll

1. Vor einem neuen Intent: `.intents/todo/` und `.intents/work-in-progress/` auf bereits
   existierende Intents zum selben Thema prüfen; ggf. aktualisieren statt duplizieren.
2. Intent-Entwurf dem Nutzer zur Freigabe vorlegen — **erst nach Zustimmung** in `todo/`
   speichern und committen.
3. Umsetzung: ein Parent-Task nach dem anderen. **Vor jedem Parent-Task Bestätigung des
   Nutzers einholen** (Sub-Tasks laufen ohne Rückfrage durch, aber immer nur eine Sub-Task pro
   Vorschlag).
4. Sub-Task fertig → sofort `[x]` markieren.
5. Alle Sub-Tasks eines Parent-Tasks fertig: falls die betroffene Logik testbar ist (siehe
   TDD-Abschnitt unten), `run_tests.scd` muss grün sein; danach Parent-Task `[x]` markieren.
6. Alle Tasks eines Intents fertig → Intent nach `completed/` verschieben, committen.
7. Nutzer-Kommandos: "weiter"/"ja"/"ok" → fortfahren; "skip X" → Task/Intent überspringen;
   "ändern/anpassen X" → gemeinsam überarbeiten statt einfach weiterzumachen.

### TDD — wo sinnvoll, nicht dogmatisch

Anders als bei einer Web-App lässt sich in einem Audio-/Live-Performance-Projekt nicht alles
sinnvoll test-first entwickeln:

- **Reine sclang-Logik ohne Server-/Audio-Abhängigkeit** (z.B. `BootTrackDetection`): rot/grün —
  erst ein `UnitTest` in `tests/`, der fehlschlägt, dann die Implementierung in `classes/`, dann
  grün. Ausführung über `run_tests.scd`.
- **SynthDefs, Klangverhalten, Live-Set-Arbeit**: kein erzwungenes TDD. Verifikation über
  Hörtest (Kopfhörer bei binauralen Sachen) — "klingt es richtig" lässt sich nicht sinnvoll als
  Vorher-Test formulieren.
- Produktiv-Klassen kommen nach `classes/`, Test-Klassen (`UnitTest`-Subklassen) nach `tests/`;
  `boot/load_classes.scd` muss vor dem ersten Start gelaufen sein, bevor sie verfügbar sind:
  `LanguageConfig.addIncludePath` für beide Ordner (rekursiv, persistiert in `sclang_conf.yaml`)
  + `thisProcess.recompile` für die laufende Sitzung. Einmalig beim Erst-Setup nötig — danach
  bindet SuperCollider beide Ordner bei jedem Start automatisch mit ein.
- Spatial-Audio-Klassen kommen nach `classes/sg/{sounds,soundobjects,spatial}/` (reine
  Ordnerkonvention, SuperCollider selbst hat keine echten Namespaces — siehe CLAUDE.md
  Architektur), Tests spiegelbildlich nach `tests/sg/{soundobjects,spatial}/`. Andere Domains
  (z.B. `BootTrackDetection`, Live-Set) liegen direkt in `classes/`/`tests/`, nicht unter `sg`.

### Headless-Testlauf vor dem Push (falls sclang in der Agent-Umgebung verfügbar ist)

Reine sclang-Logik-Tests (kein Server nötig, siehe oben) lassen sich ohne Audio-Hardware
ausführen: `sclang -l <conf.yaml> <script.scd>`, wobei die Konfiguration `classes/` und
`tests/` als `includePaths` einträgt und das Skript alle `Test*`-Klassen `.run` aufruft,
gefolgt von `0.exit`. In Umgebungen mit `QT_QPA_PLATFORM=offscreen` und
`QTWEBENGINE_DISABLE_SANDBOX=1` als Env-Variablen (nötig für headless/als root). Ein
Coding-Agent mit sclang-Zugriff **soll** dies vor jedem Push ausführen, wenn testbare
Logik geändert wurde — echtes Feedback statt reinem Code-Lesen. Ersetzt nicht den
Hörtest für SynthDefs/Klangverhalten, den nur der Nutzer selbst machen kann.

## Commit-Konventionen

- Intent-Erstellung:
  `git commit -m "Intent N Planning - <Typ> (<Domain>): <Titel>" -m "Intent planned tasks:" -m "- ..."`
- Entwicklungsarbeit:
  `git commit -m "<typ> (<domain>): <Änderung>. Intent: N, Task: M" -m "- wichtige Änderung 1" -m "- wichtige Änderung 2"`
- Keine Commit-Trailer, kein "Co-authored-by".
- Nicht automatisch committen, außer der Nutzer verlangt es explizit oder hat das für die
  laufende Session bereits so vereinbart.

## Bewusst nicht übernommen aus dem musicnet-Vorbild

- Rubocop/rspec-Befehle, Rails-Domain-Resource-Action-Web-Architektur, Devise/OmniAuth-Auth-
  Kapitel, Dependencies-Usage-Rules, MCP-Server-Kapitel — nicht anwendbar auf ein
  SuperCollider-Projekt ohne Web-Layer.
- Das in `musicnet/AGENTS.md` eingebettete Intent-Beispiel (Phoenix/Elixir `mix phx.gen.live`,
  `mix ecto.migrate` etc.) — gehörte nie zu einem der beiden Projekte. Ersetzt durch die echten,
  projekteigenen Beispiele in `.intents/completed/` und `.intents/todo/`.
