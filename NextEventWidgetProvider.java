package de.andreas.naechstertermin;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NextEventWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_REFRESH =
            "de.andreas.naechstertermin.REFRESH";

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {

        updateAll(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_REFRESH.equals(intent.getAction())) {
            AppWidgetManager manager =
                    AppWidgetManager.getInstance(context);

            int[] ids = manager.getAppWidgetIds(
                    new android.content.ComponentName(
                            context,
                            NextEventWidgetProvider.class));

            updateAll(context, manager, ids);
        }
    }

    public static void updateAll(
            Context context,
            AppWidgetManager manager,
            int[] ids) {

        for (int id : ids) {
            updateWidget(context, manager, id);
        }
    }

    private static void updateWidget(
            Context context,
            AppWidgetManager manager,
            int appWidgetId) {

        RemoteViews views =
                new RemoteViews(
                        context.getPackageName(),
                        R.layout.widget_next_event);

        if (context.checkSelfPermission(
                Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {

            views.setTextViewText(
                    R.id.eventTitle,
                    "Kalenderzugriff fehlt");

            views.setTextViewText(
                    R.id.eventDate,
                    "App öffnen und Zugriff erlauben");

            views.setViewVisibility(
                    R.id.countdown,
                    View.GONE);

            views.setViewVisibility(
                    R.id.location,
                    View.GONE);

            views.setViewVisibility(
                    R.id.details,
                    View.GONE);

            views.setOnClickPendingIntent(
                    R.id.widgetRoot,
                    openAppIntent(context));

            manager.updateAppWidget(
                    appWidgetId,
                    views);

            return;
        }

        EventInfo event =
                findNextEvent(context);

        if (event == null) {

            views.setTextViewText(
                    R.id.eventTitle,
                    "Kein nächster Termin");

            views.setTextViewText(
                    R.id.eventDate,
                    "In den nächsten 30 Tagen nichts gefunden");

            views.setViewVisibility(
                    R.id.countdown,
                    View.GONE);

            views.setViewVisibility(
                    R.id.location,
                    View.GONE);

            views.setViewVisibility(
                    R.id.details,
                    View.GONE);

            views.setOnClickPendingIntent(
                    R.id.widgetRoot,
                    openAppIntent(context));

            manager.updateAppWidget(
                    appWidgetId,
                    views);

            return;
        }

        long now =
                System.currentTimeMillis();

        long diff =
                Math.max(
                        0L,
                        event.startMillis - now);

        views.setTextViewText(
                R.id.eventTitle,
                event.title);

        views.setTextViewText(
                R.id.eventDate,
                formatDate(
                        event.startMillis,
                        event.allDay));

        long chronometerBase =
                SystemClock.elapsedRealtime()
                        + diff;

        views.setChronometer(
                R.id.countdown,
                chronometerBase,
                "⏳ Noch %s",
                true);

        views.setChronometerCountDown(
                R.id.countdown,
                true);

        views.setViewVisibility(
                R.id.countdown,
                View.VISIBLE);

        if (event.location != null
                && !event.location.trim().isEmpty()) {

            views.setTextViewText(
                    R.id.location,
                    "📍 " + event.location.trim());

            views.setViewVisibility(
                    R.id.location,
                    View.VISIBLE);

        } else {

            views.setViewVisibility(
                    R.id.location,
                    View.GONE);
        }

        if (event.description != null
                && !event.description.trim().isEmpty()) {

            views.setTextViewText(
                    R.id.details,
                    "ℹ️ " + cleanDescription(
                            event.description));

            views.setViewVisibility(
                    R.id.details,
                    View.VISIBLE);

        } else {

            views.setViewVisibility(
                    R.id.details,
                    View.GONE);
        }

        views.setOnClickPendingIntent(
                R.id.widgetRoot,
                openCalendarAt(
                        context,
                        event.startMillis));

        manager.updateAppWidget(
                appWidgetId,
                views);

        scheduleRefreshAtEventStart(
                context,
                event.startMillis + 2000L);
    }

    private static EventInfo findNextEvent(
            Context context) {

        long now =
                System.currentTimeMillis();

        long end =
                now
                + 30L
                * 24L
                * 60L
                * 60L
                * 1000L;

        String[] projection =
                new String[] {
                        CalendarContract.Instances.TITLE,
                        CalendarContract.Instances.BEGIN,
                        CalendarContract.Instances.EVENT_LOCATION,
                        CalendarContract.Instances.DESCRIPTION,
                        CalendarContract.Instances.ALL_DAY,
                        CalendarContract.Instances.STATUS
                };

        ContentResolver resolver =
                context.getContentResolver();

        Cursor cursor = null;

        try {

            cursor =
                    CalendarContract.Instances.query(
                            resolver,
                            projection,
                            now,
                            end);

            EventInfo best = null;

            while (cursor != null
                    && cursor.moveToNext()) {

                String title =
                        cursor.getString(0);

                long begin =
                        cursor.getLong(1);

                String location =
                        cursor.getString(2);

                String description =
                        cursor.getString(3);

                boolean allDay =
                        cursor.getInt(4) != 0;

                int status =
                        cursor.getInt(5);

                if (begin < now) {
                    continue;
                }

                if (status
                        == CalendarContract.Events.STATUS_CANCELED) {
                    continue;
                }

                if (best == null
                        || begin < best.startMillis) {

                    best =
                            new EventInfo(
                                    title == null
                                            || title.trim().isEmpty()
                                            ? "Termin"
                                            : title.trim(),
                                    begin,
                                    location,
                                    description,
                                    allDay);
                }
            }

            return best;

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static String formatDate(
            long millis,
            boolean allDay) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        allDay
                                ? "EEEE · dd.MM.yyyy"
                                : "EEEE · dd.MM.yyyy · HH:mm",
                        Locale.GERMANY);

        return format.format(
                new Date(millis));
    }

    private static String cleanDescription(
            String text) {

        return text
                .replace("\\r", " ")
                .replace("\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static PendingIntent openAppIntent(
            Context context) {

        Intent intent =
                new Intent(
                        context,
                        MainActivity.class);

        return PendingIntent.getActivity(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openCalendarAt(
            Context context,
            long millis) {

        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW);

        intent.setData(
                Uri.parse(
                        "content://com.android.calendar/time/"
                                + millis));

        return PendingIntent.getActivity(
                context,
                11,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void scheduleRefreshAtEventStart(
            Context context,
            long when) {

        Intent intent =
                new Intent(
                        context,
                        NextEventWidgetProvider.class);

        intent.setAction(
                ACTION_REFRESH);

        PendingIntent pi =
                PendingIntent.getBroadcast(
                        context,
                        12,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarm =
                (AlarmManager)
                        context.getSystemService(
                                Context.ALARM_SERVICE);

        if (alarm != null) {

            alarm.set(
                    AlarmManager.RTC_WAKEUP,
                    when,
                    pi);
        }
    }

    private static class EventInfo {

        final String title;
        final long startMillis;
        final String location;
        final String description;
        final boolean allDay;

        EventInfo(
                String title,
                long startMillis,
                String location,
                String description,
                boolean allDay) {

            this.title = title;
            this.startMillis = startMillis;
            this.location = location;
            this.description = description;
            this.allDay = allDay;
        }
    }
}
