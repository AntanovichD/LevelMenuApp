package com.example.levelmenuapp;

import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Главный экран приложения.
 *
 * ЛР №2 (вариант 1) — в макете используется только ConstraintLayout.
 * ЛР №3 — обработчики кнопок уровней, лог, текст в ресурсах.
 * ЛР №4 — options-меню, динамическое добавление компонентов прямо в корневой
 *          ConstraintLayout (через ConstraintLayout.LayoutParams), всплывающие
 *          сообщения (Toast), контекстное меню для смены цвета компонента.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LevelMenu";

    private TextView tvInfo;
    private ConstraintLayout rootLayout;

    private int componentCounter = 0;
    /** ID последнего добавленного компонента (для привязки следующего сверху). */
    private int lastComponentId = View.NO_ID;
    /** Список ID всех динамических компонентов — нужен для очистки рабочей области. */
    private final List<Integer> dynamicComponentIds = new ArrayList<>();
    /** Конкретный View, над которым было вызвано контекстное меню. */
    private View contextTargetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        rootLayout = findViewById(R.id.rootLayout);

        // Edge-to-edge (targetSdk 35+): при targetSdk = 36 содержимое окна
        // расползается под статус-бар и встроенный ActionBar. Чтобы порядок
        // на экране был «ActionBar → tvInfo → кнопки → … → Назад», вручную
        // добавляем корневому ConstraintLayout отступы под системные панели
        // и высоту ActionBar (берётся из атрибута темы actionBarSize).
        final TypedValue tv = new TypedValue();
        final int actionBarHeight;
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    tv.data, getResources().getDisplayMetrics());
        } else {
            actionBarHeight = 0;
        }
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    systemBars.left,
                    systemBars.top + actionBarHeight,
                    systemBars.right,
                    systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** ЛР №3: обработчик нажатия на кнопку уровня. */
    public void onLevelClick(View view) {
        Button button = (Button) view;
        String levelText = button.getText().toString();
        int levelNumber = Integer.parseInt(levelText);

        tvInfo.setText(getString(R.string.level_pressed, levelNumber));
        Log.d(TAG, "Нажата кнопка уровня: " + levelText);
    }

    /** ЛР №3: обработчик нажатия на кнопку «Назад». */
    public void onBackClick(View view) {
        tvInfo.setText(getString(R.string.choose_level));
        Log.d(TAG, "Нажата кнопка 'Назад'");
    }

    // ===== ЛР №4: options-меню =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.add_component) {
            addNewComponent();
            return true;
        } else if (id == R.id.clear_components) {
            clearComponents();
            return true;
        } else if (id == R.id.exit_app) {
            Log.d(TAG, "Выход из приложения через меню");
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Динамически создаёт новый TextView-компонент и добавляет его прямо в
     * корневой ConstraintLayout с программно заданными constraint'ами.
     * Никакие дополнительные ViewGroup-контейнеры не используются.
     */
    private void addNewComponent() {
        componentCounter++;

        TextView component = new TextView(this);
        component.setId(View.generateViewId());
        component.setText(getString(R.string.component_name, componentCounter));
        component.setTextSize(16);
        component.setGravity(Gravity.CENTER);
        component.setPadding(24, 24, 24, 24);
        component.setBackgroundColor(ContextCompat.getColor(this, R.color.component_default));

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                0, // 0dp ширина — растягивается между start и end constraint
                ViewGroup.LayoutParams.WRAP_CONTENT);

        // Привязываем новый компонент:
        //  • верх — к нижнему краю предыдущего динамического компонента,
        //    либо к нижнему краю подсказки workAreaHint, если это первый компонент;
        //  • левый/правый край — к границам родителя.
        int topAnchorId = (lastComponentId == View.NO_ID)
                ? R.id.workAreaHint
                : lastComponentId;
        lp.topToBottom = topAnchorId;
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.leftMargin = 32;
        lp.rightMargin = 32;
        lp.topMargin = 12;
        component.setLayoutParams(lp);

        // ЛР №4: регистрируем компонент для вызова контекстного меню (долгое нажатие).
        registerForContextMenu(component);

        rootLayout.addView(component);

        lastComponentId = component.getId();
        dynamicComponentIds.add(component.getId());

        Toast.makeText(this,
                getString(R.string.toast_component_added, componentCounter),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Добавлен новый компонент №" + componentCounter);
    }

    /** Удаляет все динамически добавленные компоненты, оставляя статичную разметку. */
    private void clearComponents() {
        for (int id : dynamicComponentIds) {
            View v = rootLayout.findViewById(id);
            if (v != null) {
                rootLayout.removeView(v);
            }
        }
        dynamicComponentIds.clear();
        lastComponentId = View.NO_ID;
        componentCounter = 0;

        Toast.makeText(this, R.string.toast_work_area_cleared, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Рабочая область очищена");
    }

    // ===== ЛР №4: контекстное меню для динамических компонентов =====

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        contextTargetView = v;
        menu.setHeaderTitle(R.string.context_menu_title);
        getMenuInflater().inflate(R.menu.component_context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (contextTargetView == null) {
            return super.onContextItemSelected(item);
        }
        int id = item.getItemId();
        int colorRes;
        String colorName;
        if (id == R.id.color_red) {
            colorRes = R.color.component_red;
            colorName = "Красный";
        } else if (id == R.id.color_green) {
            colorRes = R.color.component_green;
            colorName = "Зелёный";
        } else if (id == R.id.color_blue) {
            colorRes = R.color.component_blue;
            colorName = "Синий";
        } else if (id == R.id.color_default) {
            colorRes = R.color.component_default;
            colorName = "По умолчанию";
        } else {
            return super.onContextItemSelected(item);
        }

        contextTargetView.setBackgroundColor(ContextCompat.getColor(this, colorRes));
        String label = (contextTargetView instanceof TextView)
                ? ((TextView) contextTargetView).getText().toString()
                : contextTargetView.toString();
        Log.d(TAG, "Изменён цвет компонента \"" + label + "\" на: " + colorName);
        return true;
    }
}
