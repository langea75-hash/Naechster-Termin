#!/usr/bin/env bash
set -e
git add app/src/main/res/drawable/calendar_badge_background.xml         app/src/main/res/drawable/calendar_month_background.xml         app/src/main/res/drawable/countdown_background.xml
git commit -m "Fehlende Widget-Hintergruende hinzugefuegt" || true
git push
echo "FERTIG"
