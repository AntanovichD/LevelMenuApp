package com.example.levelmenuapp;

/** Стандартный калькулятор: 4 базовые операции + % и смена знака. */
public class StandardCalcActivity extends BaseCalcActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_standard_calc;
    }

    @Override
    protected int getTitleResource() {
        return R.string.title_standard;
    }
}
