package com.example.levelmenuapp;

import android.os.SystemClock;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * «Умный» выход из приложения (ЛР №6).
 *
 * Реализует две формы подтверждения выхода:
 *
 * 1) Для системной кнопки «Назад» на корневом экране — двойное нажатие
 *    в течение 2000 мс ({@link #attach(AppCompatActivity)}): первое
 *    нажатие показывает Toast-подсказку, повторное в течение интервала —
 *    закрывает Activity.
 *
 * 2) Для пункта меню «Выход» — модальный диалог с явным вопросом
 *    «Вы точно хотите выйти?» и кнопками «Да / Отмена»
 *    ({@link #showExitConfirmDialog(AppCompatActivity)}). Используется
 *    на всех экранах приложения.
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

    /**
     * Показывает диалог подтверждения выхода. По «Да» — закрывает всё
     * приложение через {@link AppCompatActivity#finishAffinity()}, по
     * «Отмена» — просто закрывает диалог.
     */
    public static void showExitConfirmDialog(AppCompatActivity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.exit_dialog_title)
                .setMessage(R.string.exit_dialog_message)
                .setPositiveButton(R.string.exit_dialog_yes,
                        (dialog, which) -> activity.finishAffinity())
                .setNegativeButton(R.string.exit_dialog_no,
                        (dialog, which) -> dialog.dismiss())
                .setCancelable(true)
                .show();
    }
}
