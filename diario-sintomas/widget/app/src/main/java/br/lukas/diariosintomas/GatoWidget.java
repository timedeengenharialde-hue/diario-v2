package br.lukas.diariosintomas;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

// Widget da tela inicial: gato muda de cara pelo humor e na hora do remédio
public class GatoWidget extends AppWidgetProvider {
    static final String PREFS = "gato_widget";
    static final String TICK = "br.lukas.diariosintomas.GATO_TICK";
    static final long MIN = 60000L, H = 3600000L;

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        updateAll(ctx);
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        super.onReceive(ctx, intent);
        if (TICK.equals(intent.getAction())) updateAll(ctx);
    }

    static void updateAll(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        int[] ids = mgr.getAppWidgetIds(new ComponentName(ctx, GatoWidget.class));
        if (ids == null || ids.length == 0) return;

        long now = System.currentTimeMillis();
        String mood = "neutral";
        String text = "Como você está?";
        long next = now + 30 * MIN;

        try {
            JSONObject s = new JSONObject(ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("state", "{}"));
            JSONObject lemb = s.optJSONObject("lemb");
            JSONObject last = s.optJSONObject("last");
            String[] meds = {"rit", "esc"};
            String[] nomes = {"Ritalina", "Escitalopram"};
            String pend = null;

            for (int m = 0; m < 2; m++) {
                JSONArray hs = lemb == null ? null : lemb.optJSONArray(meds[m]);
                long ld = last == null ? 0 : last.optLong(meds[m], 0);
                if (hs == null) continue;
                for (int i = 0; i < hs.length(); i++) {
                    String[] p = hs.optString(i, "").split(":");
                    if (p.length < 2) continue;
                    int hh = Integer.parseInt(p[0].trim()), mm = Integer.parseInt(p[1].trim());
                    for (int d = -1; d <= 1; d++) {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(now);
                        c.add(Calendar.DAY_OF_YEAR, d);
                        c.set(Calendar.HOUR_OF_DAY, hh);
                        c.set(Calendar.MINUTE, mm);
                        c.set(Calendar.SECOND, 0);
                        c.set(Calendar.MILLISECOND, 0);
                        long r = c.getTimeInMillis();
                        // Pendente: passou do horário, até 3 h depois, sem dose registrada desde 90 min antes
                        boolean pendente = now >= r && now < r + 3 * H && ld < r - 90 * MIN;
                        if (pendente && pend == null) pend = nomes[m];
                        if (r > now) next = Math.min(next, r);
                        if (now >= r && r + 3 * H > now) next = Math.min(next, r + 3 * H);
                    }
                }
            }

            if (pend != null) {
                mood = "remedio";
                text = "Hora de tomar " + pend + "!";
            } else {
                String m = s.optString("mood", "neutral");
                long t = s.optLong("moodT", 0);
                if (!"neutral".equals(m) && now - t < 12 * H) {
                    mood = m;
                    text = s.optString("frase", "");
                    next = Math.min(next, t + 12 * H);
                }
            }
        } catch (Exception e) {
            // estado inválido: mostra o gato neutro
        }

        int img;
        switch (mood) {
            case "happy": img = R.drawable.gato_happy; break;
            case "alert": img = R.drawable.gato_alert; break;
            case "angry": img = R.drawable.gato_angry; break;
            case "sleep": img = R.drawable.gato_sleep; break;
            case "sad": img = R.drawable.gato_sad; break;
            case "remedio": img = R.drawable.gato_remedio; break;
            default: img = R.drawable.gato_neutral;
        }

        Intent open = new Intent(ctx, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        for (int id : ids) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.gato_widget);
            v.setImageViewResource(R.id.gato_img, img);
            v.setTextViewText(R.id.gato_txt, text);
            v.setOnClickPendingIntent(R.id.gato_root, pi);
            mgr.updateAppWidget(id, v);
        }

        // Agenda a próxima troca de cara (horário do remédio, fim da janela, fim das 12 h do humor)
        Intent tick = new Intent(ctx, GatoWidget.class).setAction(TICK);
        PendingIntent tp = PendingIntent.getBroadcast(ctx, 1, tick, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.setAndAllowWhileIdle(AlarmManager.RTC, Math.max(next, now + MIN), tp);
    }
}
