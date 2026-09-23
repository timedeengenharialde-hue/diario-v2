package br.lukas.diariosintomas;

import android.content.Context;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// Recebe o estado do app (humor, doses, lembretes) e atualiza o widget
@CapacitorPlugin(name = "GatoWidget")
public class WidgetPlugin extends Plugin {
    @PluginMethod
    public void update(PluginCall call) {
        String state = call.getString("state", "{}");
        Context ctx = getContext();
        ctx.getSharedPreferences(GatoWidget.PREFS, Context.MODE_PRIVATE).edit().putString("state", state).apply();
        GatoWidget.updateAll(ctx);
        call.resolve();
    }
}
