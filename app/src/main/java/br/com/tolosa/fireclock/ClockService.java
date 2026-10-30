package br.com.tolosa.fireclock;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windows;
    private TextView clock;
    private SharedPreferences prefs;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (clock == null) return;
            clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            // Check the wall clock regularly, including after time/fuso adjustments.
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("clock", "Relógio ativo",
                NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        startForeground(1, new Notification.Builder(this, "clock")
                .setContentTitle("Relógio Fire TV ativo")
                .setContentText("Abra o app para desativar ou ajustar.")
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentIntent(open).setOngoing(true).build());
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        windows = getSystemService(WindowManager.class);
        prefs = getSharedPreferences("clock", MODE_PRIVATE);
        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        clock.setPadding(dp(10), dp(4), dp(10), dp(4));
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x99000000);
        background.setCornerRadius(dp(6));
        clock.setBackground(background);
        clock.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        try {
            windows.addView(clock, params());
            prefs.registerOnSharedPreferenceChangeListener(this);
            handler.post(tick);
        } catch (RuntimeException e) {
            // Revocation or a device policy may reject the overlay.
            clock = null;
            stopSelf();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private WindowManager.LayoutParams params() {
        clock.setTextSize(prefs.getInt("size", 24));
        int corner = prefs.getInt("corner", 0);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                -2, -2, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = (corner == 0 || corner == 3 ? Gravity.TOP : Gravity.BOTTOM)
                | (corner < 2 ? Gravity.RIGHT : Gravity.LEFT);
        p.x = dp(24);
        p.y = dp(24);
        return p;
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        if (clock != null) {
            try { windows.updateViewLayout(clock, params()); }
            catch (RuntimeException e) { stopSelf(); }
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int id) {
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(this);
        if (clock != null) {
            try { windows.removeView(clock); } catch (IllegalArgumentException ignored) { }
            clock = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
