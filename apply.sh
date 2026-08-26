#!/usr/bin/env bash
set -e
git add app/src/main/res/layout/widget_next_event.xml         app/src/main/java/de/andreas/naechstertermin/NextEventWidgetProvider.java
git commit -m "Live Countdown korrigiert" || true
git push
echo "FERTIG"
