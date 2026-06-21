package com.example.levelmenuapp;

/** Расширенный калькулятор: добавлены √, x², 1/x, π к базовому набору. */
public class ExtendedCalcActivity extends BaseCalcActivity {

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_extended_calc;
    }

    @Override
    protected int getTitleResource() {
        return R.string.title_extended;
    }
}
