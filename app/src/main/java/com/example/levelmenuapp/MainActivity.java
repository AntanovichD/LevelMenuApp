package com.example.levelmenuapp;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private TextView tvInfo;
    private static final String TAG = "LevelMenu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Устанавливаем Toolbar как ActionBar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvInfo = findViewById(R.id.tvInfo);
    }

    public void onLevelClick(View view) {
        Button button = (Button) view;
        String levelText = button.getText().toString();

        tvInfo.setText(getString(R.string.level_pressed, Integer.parseInt(levelText)));

        Log.d(TAG, "Нажата кнопка уровня: " + levelText);
    }

    public void onBackClick(View view) {
        tvInfo.setText(getString(R.string.choose_level));
        Log.d(TAG, "Нажата кнопка 'Назад'");
    }

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
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void addNewComponent() {
        // Получаем текущее количество кнопок
        GridLayout grid = findViewById(R.id.gridLevels);
        int buttonCount = grid.getChildCount();

        // Создаем новую кнопку с номером
        Button newButton = new Button(this);
        newButton.setText(String.valueOf(buttonCount + 1));
        newButton.setTextSize(18);
        newButton.setOnClickListener(this::onLevelClick);

        // Устанавливаем параметры для GridLayout
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        params.setMargins(6, 6, 6, 6);
        newButton.setLayoutParams(params);

        // Добавляем кнопку в GridLayout
        grid.addView(newButton);

        Log.d(TAG, "Добавлен новый компонент: " + (buttonCount + 1));
    }

    private void clearComponents() {
        GridLayout grid = findViewById(R.id.gridLevels);
        grid.removeAllViews();
        Log.d(TAG, "Рабочая область очищена");
    }
}