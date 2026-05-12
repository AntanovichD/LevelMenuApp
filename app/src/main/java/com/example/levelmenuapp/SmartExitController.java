package com.example.levelmenuapp;

import android.os.SystemClock;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

/**
 * «Умный» выход из приложения (ЛР №6).
 * При первом нажатии «Назад» показывает Toast «Нажмите ещё раз для выхода»
 * и запоминает момент нажатия. При повторном нажатии в течение 2000 мс
 * закрывает Activity. Используется на корневом экране (StartActivity).
 */
public final class SmartExitController {

    private static final long DOUBLE_BACK_INTERVAL_MS = 2000L;

    private long lastBackPressedAt = 0L;

    private SmartExitController() {}

    public static void attach(AppCompatActivity activity) {
        final SmartExitController self = new SmartExitController();
        activity.getOnBackPressedDispatcher().addCallback(activity, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long now = SystemClock.elapsedRealtime();
                // Метод замера времени нажатия — SystemClock.elapsedRealtime()
                // (см. ЛР №6, контрольный вопрос 3).
                if (now - self.lastBackPressedAt < DOUBLE_BACK_INTERVAL_MS) {
                    activity.finish();
                } else {
                    self.lastBackPressedAt = now;
                    Toast.makeText(activity, R.string.exit_confirm, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
