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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Базовый класс для экранов калькулятора. Содержит логику работы с {@link CalcEngine},
 * общие обработчики кнопок (вызываются через android:onClick), меню навигации между
 * экранами (ЛР №5 — «переключение между экранами в мобильном приложении») и анимации
 * (ЛР №5 — «анимация при выводе ответа»).
 *
 * Также демонстрирует жизненный цикл Activity — все методы логируются под тегом
 * {@link #TAG} (см. ЛР №5, контрольный вопрос 8).
 */
public abstract class BaseCalcActivity extends AppCompatActivity {

    protected static final String TAG = "Calc";

    protected final CalcEngine engine = new CalcEngine();
    protected TextView display;
    protected ConstraintLayout rootLayout;

    /** Возвращает ресурс макета конкретного экрана. */
    protected abstract int getLayoutResource();

    /** Заголовок ActionBar для конкретного экрана. */
    protected abstract int getTitleResource();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, getClass().getSimpleName() + ".onCreate");
        setContentView(getLayoutResource());
        setTitle(getTitleResource());

        display = findViewById(R.id.display);
        rootLayout = findViewById(R.id.rootLayout);

        applyInsets();
        if (savedInstanceState != null) {
            engine.restoreFrom(savedInstanceState.getBundle("engine"));
        }
        updateDisplay();
    }

    /**
     * Edge-to-edge (targetSdk 35+): вручную учитываем системные панели и высоту
     * встроенного ActionBar, чтобы содержимое не уезжало под статус-бар.
     */
    private void applyInsets() {
        if (rootLayout == null) return;
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle b = new Bundle();
        engine.saveTo(b);
        outState.putBundle("engine", b);
    }

    @Override protected void onStart()   { super.onStart();   Log.d(TAG, getClass().getSimpleName() + ".onStart"); }
    @Override protected void onResume()  { super.onResume();  Log.d(TAG, getClass().getSimpleName() + ".onResume"); }
    @Override protected void onPause()   { super.onPause();   Log.d(TAG, getClass().getSimpleName() + ".onPause"); }
    @Override protected void onStop()    { super.onStop();    Log.d(TAG, getClass().getSimpleName() + ".onStop"); }
    @Override protected void onRestart() { super.onRestart(); Log.d(TAG, getClass().getSimpleName() + ".onRestart"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.d(TAG, getClass().getSimpleName() + ".onDestroy"); }

    // ===== Обновление дисплея с анимацией =====

    protected void updateDisplay() {
        if (display != null) display.setText(engine.getDisplay());
    }

    /** Анимация вывода ответа (ЛР №5: «анимация ... при выводе ответа»). */
    protected void updateDisplayAnimated() {
        updateDisplay();
        if (display != null) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.result_pop);
            display.startAnimation(anim);
        }
    }

    /** Лёгкая «промежуточная» анимация при вводе цифры / нажатии операции. */
    protected void blinkDisplay() {
        if (display != null) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.digit_blink);
            display.startAnimation(anim);
        }
    }

    // ===== Обработчики кнопок (вызываются через android:onClick из XML) =====

    public void onDigitClick(View v) {
        String txt = ((Button) v).getText().toString();
        char d = txt.charAt(0);
        engine.inputDigit(d);
        updateDisplay();
        blinkDisplay();
    }

    public void onDotClick(View v) {
        engine.inputDot();
        updateDisplay();
        blinkDisplay();
    }

    public void onSignClick(View v) {
        engine.toggleSign();
        updateDisplay();
    }

    public void onBackspaceClick(View v) {
        engine.backspace();
        updateDisplay();
    }

    public void onClearClick(View v) {
        engine.clearAll();
        updateDisplay();
        blinkDisplay();
    }

    public void onOpClick(View v) {
        String op = ((Button) v).getText().toString();
        boolean ok = engine.applyOp(op);
        if (!ok) {
            updateDisplay();
            showError(R.string.error_div_zero);
            return;
        }
        updateDisplay();
        blinkDisplay();
    }

    public void onEqualsClick(View v) {
        boolean ok = engine.evaluate();
        if (!ok) {
            updateDisplay();
            showError(R.string.error_div_zero);
            return;
        }
        updateDisplayAnimated();
    }

    public void onPercentClick(View v) {
        boolean ok = engine.applyPercent();
        if (!ok) {
            updateDisplay();
            showError(R.string.error_invalid);
            return;
        }
        updateDisplayAnimated();
    }

    public void onUnaryClick(View v) {
        String op = ((Button) v).getText().toString();
        boolean ok = engine.unary(op);
        if (!ok) {
            updateDisplay();
            showError(R.string.error_invalid);
            return;
        }
        updateDisplayAnimated();
    }

    private void showError(int messageRes) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Ошибка вычисления: " + getString(messageRes));
    }

    // ===== Меню навигации между экранами =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.calc_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_start) {
            // Возврат на стартовый экран — просто закрываем текущий, он лежит в стеке.
            finishWithBackAnimation();
            return true;
        } else if (id == R.id.menu_standard) {
            if (this instanceof StandardCalcActivity) return true; // уже здесь
            replaceWith(StandardCalcActivity.class);
            return true;
        } else if (id == R.id.menu_extended) {
            if (this instanceof ExtendedCalcActivity) return true;
            replaceWith(ExtendedCalcActivity.class);
            return true;
        } else if (id == R.id.menu_exit) {
            // Здесь — обычное закрытие без подтверждения (умный выход на StartActivity).
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Закрывает текущий экран и стартует другой с анимацией перехода (ЛР №5). */
    protected void replaceWith(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    protected void finishWithBackAnimation() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
