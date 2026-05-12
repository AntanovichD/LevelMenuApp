package com.example.levelmenuapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Стартовый экран приложения (ЛР №5: «3 экрана: начальный, стандартный, расширенный»).
 * Реализует «умный» выход (ЛР №6) — двойное нажатие Back для закрытия.
 */
public class StartActivity extends AppCompatActivity {

    private static final String TAG = "Calc";

    private ConstraintLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "StartActivity.onCreate");
        setContentView(R.layout.activity_start);
        setTitle(R.string.app_name);

        rootLayout = findViewById(R.id.rootLayout);
        applyInsets();

        findViewById(R.id.btnStandard).setOnClickListener(v -> openCalc(StandardCalcActivity.class));
        findViewById(R.id.btnExtended).setOnClickListener(v -> openCalc(ExtendedCalcActivity.class));

        SmartExitController.attach(this);
    }

    private void applyInsets() {
        final TypedValue tv = new TypedValue();
        final int actionBarHeight;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, getResources().getDisplayMetrics());
        } else {
            actionBarHeight = 0;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top + actionBarHeight, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "StartActivity.onResume");
        // ЛР №5: лёгкая fade-in анимация при возвращении на стартовый экран.
        View title = findViewById(R.id.tvTitle);
        if (title != null) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.result_pop);
            title.startAnimation(anim);
        }
    }

    @Override protected void onStart()   { super.onStart();   Log.d(TAG, "StartActivity.onStart"); }
    @Override protected void onPause()   { super.onPause();   Log.d(TAG, "StartActivity.onPause"); }
    @Override protected void onStop()    { super.onStop();    Log.d(TAG, "StartActivity.onStop"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, "StartActivity.onRestart"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, "StartActivity.onDestroy"); }

    private void openCalc(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        // ЛР №5: анимация переключения между экранами.
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    // Меню навигации: со стартового экрана можно перейти на любой калькулятор.
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calc_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_start) {
            return true;
        } else if (id == R.id.menu_standard) {
            openCalc(StandardCalcActivity.class);
            return true;
        } else if (id == R.id.menu_extended) {
            openCalc(ExtendedCalcActivity.class);
            return true;
        } else if (id == R.id.menu_exit) {
            // На стартовом экране пункт «Выход» — обычное завершение (без подтверждения),
            // т.к. пользователь явно выбрал этот пункт.
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
