package br.com.tolosa.fireclock;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 24);
        TextView title = new TextView(this);
        title.setText("Relógio Fire TV");
        title.setTextSize(28);
        layout.addView(title);
        status = new TextView(this);
        status.setTextSize(18);
        layout.addView(status);
        addButton(layout, "1. Permitir exibição sobre outros apps", v -> permission());
        addButton(layout, "Ativar relógio", v -> {
            if (!Settings.canDrawOverlays(this)) { permission(); return; }
            startForegroundService(new Intent(this, ClockService.class));
            status.setText("Ativação solicitada. Pressione Home para verificar o relógio.");
        });
        addButton(layout, "Desativar relógio", v -> {
            stopService(new Intent(this, ClockService.class));
            status.setText("Relógio desativado.");
        });
        addButton(layout, "Mudar canto da tela", v -> {
            int corner = (getPreferencesValue("corner", 0) + 1) % 4;
            getSharedPreferences("clock", MODE_PRIVATE).edit().putInt("corner", corner).apply();
            status.setText("Canto: " + new String[]{"superior direito", "inferior direito",
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
        status.setText(Settings.canDrawOverlays(this)
                ? "Permissão concedida. Você pode ativar o relógio."
                : "Primeiro, autorize a exibição sobre outros apps.");
    }

    private void permission() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            status.setText("Este Fire TV não oferece essa tela. Consulte a autorização por ADB no README.");
        }
    }
}
