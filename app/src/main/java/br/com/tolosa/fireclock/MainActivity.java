package br.com.tolosa.fireclock;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;
    private TextView diagnostics;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            updateDiagnostics();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        TextView title = new TextView(this);
        title.setText("Relógio Fire TV · 0.1.1");
        title.setTextSize(28);
        layout.addView(title);
        status = new TextView(this);
        status.setTextSize(18);
        layout.addView(status);
        diagnostics = new TextView(this);
        diagnostics.setTextSize(16);
        diagnostics.setTextIsSelectable(true);
        layout.addView(diagnostics);
        android.widget.TextClock preview = new android.widget.TextClock(this);
        preview.setFormat24Hour("'Prévia local: ' HH:mm");
        preview.setFormat12Hour("'Prévia local: ' HH:mm");
        preview.setTextSize(28);
        layout.addView(preview);
        addButton(layout, "1. Permitir exibição sobre outros apps", v -> permission());
        addButton(layout, "Ativar relógio", v -> {
            if (!Settings.canDrawOverlays(this)) {
                status.setText("Relógio não ativado: falta a permissão. Veja o diagnóstico abaixo.");
                updateDiagnostics();
                return;
            }
            getSharedPreferences("clock", MODE_PRIVATE).edit().remove("error").apply();
            try {
                startForegroundService(new Intent(this, ClockService.class));
                status.setText("Ativação solicitada. Confira o estado abaixo antes de pressionar Home.");
            } catch (RuntimeException e) {
                getSharedPreferences("clock", MODE_PRIVATE).edit()
                    .putString("error", e.getClass().getSimpleName() + ": " + e.getMessage()).apply();
            }
            updateDiagnostics();
        });
        addButton(layout, "Desativar relógio", v -> {
            stopService(new Intent(this, ClockService.class));
            status.setText("Relógio desativado.");
        });
        addButton(layout, "Mudar canto da tela", v -> {
            int corner = (getPreferencesValue("corner", 0) + 1) % 4;
            getSharedPreferences("clock", MODE_PRIVATE).edit().putInt("corner", corner).apply();
            status.setText((ClockService.running ? "Canto: " : "Relógio desativado. Canto salvo: ") + new String[]{"superior direito", "inferior direito",
                    "inferior esquerdo", "superior esquerdo"}[corner]);
        });
        addButton(layout, "Mudar tamanho", v -> {
            int size = getPreferencesValue("size", 24) == 24 ? 32 : 24;
            getSharedPreferences("clock", MODE_PRIVATE).edit().putInt("size", size).apply();
            status.setText("Tamanho: " + size);
        });
        TextView help = new TextView(this);
        help.setText("Horário e fuso seguem o dispositivo. Alguns apps podem ocultar sobreposições. "
                + "Após reiniciar o Fire TV, abra este app e ative novamente.");
        help.setTextSize(16);
        layout.addView(help);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private int getPreferencesValue(String key, int fallback) {
        return getSharedPreferences("clock", MODE_PRIVATE).getInt(key, fallback);
    }

    private void addButton(LinearLayout layout, String text, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(text);
        button.setOnClickListener(action);
        layout.addView(button, new LinearLayout.LayoutParams(-1, -2));
    }

    @Override public void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override public void onPause() {
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private void updateDiagnostics() {
        boolean allowed = Settings.canDrawOverlays(this);
        String error = getSharedPreferences("clock", MODE_PRIVATE).getString("error", "");
        diagnostics.setText("Permissão: " + (allowed ? "CONCEDIDA" : "NÃO CONCEDIDA")
                + "\nSobreposição: " + (ClockService.running ? "ANEXADA ao sistema" : "INATIVA")
                + (error.isEmpty() ? "" : "\nFalha: " + error)
                + (!allowed ? "\nSe a tela de permissão não funcionar no Fire TV, execute pelo computador com ADB autorizado:\n"
                    + "adb shell appops set br.com.tolosa.fireclock SYSTEM_ALERT_WINDOW allow"
                    + "\nDepois volte ao app e selecione Ativar relógio."
                    : "\nSe estiver anexada mas invisível, teste na tela inicial; o app de vídeo pode ocultá-la.")
                + "\nA prévia local não confirma a exibição sobre outros apps.");
    }

    private void permission() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException e) {
            status.setText("Este Fire TV não oferece essa tela. Consulte a autorização por ADB no README.");
        }
    }
}
