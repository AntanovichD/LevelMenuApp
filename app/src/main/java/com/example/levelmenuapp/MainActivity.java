package com.example.levelmenuapp;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "LevelMenu";

    private TextView tvInfo;
    private LinearLayout workArea;
    private int componentCounter = 0;
    private View contextTargetView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvInfo = findViewById(R.id.tvInfo);
        workArea = findViewById(R.id.workArea);
    }

    /** Обработчик нажатия на кнопку уровня (ЛР №3). */
    public void onLevelClick(View view) {
        Button button = (Button) view;
        String levelText = button.getText().toString();
        int levelNumber = Integer.parseInt(levelText);

        tvInfo.setText(getString(R.string.level_pressed, levelNumber));
        Log.d(TAG, "Нажата кнопка уровня: " + levelText);
    }

    /** Обработчик нажатия на кнопку «Назад» (ЛР №3). */
    public void onBackClick(View view) {
        tvInfo.setText(getString(R.string.choose_level));
        Log.d(TAG, "Нажата кнопка 'Назад'");
    }

    // ----- ЛР №4: options-меню -----

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

    /** Динамически добавляет новый компонент в рабочую область. */
    private void addNewComponent() {
        componentCounter++;

        TextView component = new TextView(this);
        component.setText(getString(R.string.component_name, componentCounter));
        component.setTextSize(16);
        component.setGravity(Gravity.CENTER);
        component.setPadding(24, 24, 24, 24);
        component.setBackgroundColor(ContextCompat.getColor(this, R.color.component_default));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 8;
        params.bottomMargin = 8;
        component.setLayoutParams(params);

        // Регистрируем компонент для вызова контекстного меню (долгое нажатие)
        registerForContextMenu(component);

        workArea.addView(component);

        Toast.makeText(this,
                getString(R.string.toast_component_added, componentCounter),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Добавлен новый компонент №" + componentCounter);
    }

    /** Удаляет все динамически добавленные компоненты из рабочей области. */
    private void clearComponents() {
        View hint = workArea.findViewById(R.id.workAreaHint);
        workArea.removeAllViews();
        if (hint != null) {
            workArea.addView(hint);
        }
        componentCounter = 0;

        Toast.makeText(this, R.string.toast_work_area_cleared, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Рабочая область очищена");
    }

    // ----- ЛР №4: контекстное меню для динамических компонентов -----

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
