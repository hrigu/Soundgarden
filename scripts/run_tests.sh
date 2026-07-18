#!/bin/bash
# scripts/run_tests.sh — headless Ausführung aller sclang-Unit-Tests.
#
# Das Skript umgeht das in AGENTS.md dokumentierte Gotcha des hängenden sclang-Prozesses:
# run_tests.scd ruft am Ende kein 0.exit auf; bei einem unbehandelten Laufzeitfehler bleibt
# der Prozess sonst lebendig auf der interaktiven Konsole stehen. Deshalb wird hier eine
# temporäre Kopie angelegt und explizit 0.exit angehängt.
#
# Verwendung:
#   ./scripts/run_tests.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCLANG="/Applications/SuperCollider.app/Contents/MacOS/sclang"
TEMP_SCRIPT="$(mktemp /tmp/sg_run_tests_XXXXXX.scd)"

cleanup() {
  rm -f "${TEMP_SCRIPT}"
}
trap cleanup EXIT

if [[ ! -x "${SCLANG}" ]]; then
  echo "Fehler: sclang nicht gefunden unter ${SCLANG}" >&2
  exit 1
fi

cp "${REPO_ROOT}/run_tests.scd" "${TEMP_SCRIPT}"
echo "0.exit;" >> "${TEMP_SCRIPT}"

# Ausgabe direkt ins Terminal, nicht durch tail/head pipen — sonst ist ein
# hängender Prozess von einem noch laufenden Testlauf nicht unterscheidbar.
"${SCLANG}" "${TEMP_SCRIPT}"
