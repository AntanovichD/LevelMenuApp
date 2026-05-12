package com.example.levelmenuapp;

import android.os.Bundle;

import java.util.Locale;

/**
 * Чистая логика калькулятора (без зависимостей от Android UI).
 *
 * Состояние:
 *   accumulator   — накопленное значение (левый операнд для отложенной операции);
 *   pendingOp     — отложенная бинарная операция (null, "+", "−", "×", "÷");
 *   currentInput  — текущая строка ввода пользователя ("", "12", "-3.4", "-");
 *   justEvaluated — флаг «только что показали результат, новая цифра начинает заново»;
 *   errorState    — флаг «была ошибка, следующая цифра / C сбрасывают состояние».
 *
 * Поведение соответствует поведению типового «инженерного» калькулятора:
 * нажатие новой операции после ввода второго операнда вычисляет накопленный
 * результат и продолжает вычисление; «=» выводит результат и переводит в режим
 * ожидания новой операции; унарные операции применяются к текущему значению на
 * дисплее. Все строки операций совпадают с текстом кнопок (см. strings.xml).
 */
public class CalcEngine {

    public static final String OP_ADD = "+";
    public static final String OP_SUB = "\u2212"; // U+2212 MINUS SIGN
    public static final String OP_MUL = "\u00D7"; // U+00D7 MULTIPLICATION SIGN
    public static final String OP_DIV = "\u00F7"; // U+00F7 DIVISION SIGN

    public static final String OP_SQRT = "\u221A";       // √
    public static final String OP_SQUARE = "x\u00B2";    // x²
    public static final String OP_RECIPROCAL = "1/x";
    public static final String OP_PI = "\u03C0";         // π

    private double accumulator = 0;
    private String pendingOp = null;
    private String currentInput = "";
    private boolean justEvaluated = false;
    private boolean errorState = false;

    private static final int MAX_DIGITS = 12;

    /** Сохранение состояния (например, при повороте экрана). */
    public void saveTo(Bundle b) {
        b.putDouble("accumulator", accumulator);
        b.putString("pendingOp", pendingOp);
        b.putString("currentInput", currentInput);
        b.putBoolean("justEvaluated", justEvaluated);
        b.putBoolean("errorState", errorState);
    }

    public void restoreFrom(Bundle b) {
        if (b == null) return;
        accumulator = b.getDouble("accumulator", 0);
        pendingOp = b.getString("pendingOp", null);
        currentInput = b.getString("currentInput", "");
        justEvaluated = b.getBoolean("justEvaluated", false);
        errorState = b.getBoolean("errorState", false);
    }

    /** Сбрасывает всё. */
    public void clearAll() {
        accumulator = 0;
        pendingOp = null;
        currentInput = "";
        justEvaluated = false;
        errorState = false;
    }

    /** Что показывать на дисплее. */
    public String getDisplay() {
        if (errorState) return "Ошибка";
        if (currentInput.isEmpty() || justEvaluated) {
            return formatNumber(accumulator);
        }
        if (currentInput.equals("-")) return "-0";
        return currentInput;
    }

    public boolean isErrorState() {
        return errorState;
    }

    public void inputDigit(char d) {
        if (errorState) clearAll();
        if (justEvaluated) {
            // Новый ввод после показа результата начинает выражение заново.
            accumulator = 0;
            pendingOp = null;
            currentInput = "";
            justEvaluated = false;
        }
        String stripped = currentInput.startsWith("-") ? currentInput.substring(1) : currentInput;
        int digits = stripped.replace(".", "").length();
        if (digits >= MAX_DIGITS) return;
        if (currentInput.isEmpty() || currentInput.equals("-")) {
            currentInput = currentInput + d;
        } else if (currentInput.equals("0")) {
            currentInput = String.valueOf(d);
        } else if (currentInput.equals("-0")) {
            currentInput = "-" + d;
        } else {
            currentInput = currentInput + d;
        }
    }

    public void inputDot() {
        if (errorState) clearAll();
        if (justEvaluated) {
            accumulator = 0;
            pendingOp = null;
            currentInput = "0.";
            justEvaluated = false;
            return;
        }
        if (currentInput.isEmpty() || currentInput.equals("-")) {
            currentInput = currentInput + "0.";
        } else if (!currentInput.contains(".")) {
            currentInput = currentInput + ".";
        }
    }

    public void toggleSign() {
        if (errorState) return;
        if (justEvaluated) {
            accumulator = -accumulator;
            return;
        }
        if (currentInput.isEmpty()) {
            currentInput = "-";
        } else if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
    }

    /** Удаляет последний символ. Реализует требование ЛР №5 о «стирании некорректного ввода». */
    public void backspace() {
        if (errorState) {
            clearAll();
            return;
        }
        if (justEvaluated) {
            // После показа результата ⌫ просто сбрасывает «только что вычислено»,
            // а текущий ввод остаётся пустым — следующая цифра начнёт заново.
            justEvaluated = false;
            return;
        }
        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        }
    }

    /**
     * Применяет бинарную операцию (+ − × ÷). Если уже была отложенная операция —
     * сначала вычисляет промежуточный результат. Возвращает false при ошибке.
     */
    public boolean applyOp(String op) {
        if (errorState) return false;
        if (!currentInput.isEmpty() && !currentInput.equals("-")) {
            double rhs = parseInput();
            if (pendingOp != null) {
                try {
                    accumulator = compute(accumulator, pendingOp, rhs);
                } catch (ArithmeticException e) {
                    errorState = true;
                    return false;
                }
            } else {
                accumulator = rhs;
            }
            currentInput = "";
            justEvaluated = true;
        } else if (justEvaluated) {
            // Продолжаем работать с показанным результатом.
        } else if (pendingOp == null && !justEvaluated) {
            // Пользователь нажал операцию без ввода — берём 0.
            accumulator = 0;
            justEvaluated = true;
        }
        pendingOp = op;
        return true;
    }

    /** Выполняет «=». Возвращает false при ошибке (например, деление на 0). */
    public boolean evaluate() {
        if (errorState) return false;
        if (pendingOp == null) {
            // Нет отложенной операции — если есть текущий ввод, просто переносим в accumulator.
            if (!currentInput.isEmpty() && !currentInput.equals("-")) {
                accumulator = parseInput();
                currentInput = "";
            }
            justEvaluated = true;
            return true;
        }
        double rhs;
        if (currentInput.isEmpty() || currentInput.equals("-")) {
            // Повторение операции с тем же операндом — типичное поведение калькуляторов.
            rhs = accumulator;
        } else {
            rhs = parseInput();
        }
        try {
            accumulator = compute(accumulator, pendingOp, rhs);
        } catch (ArithmeticException e) {
            errorState = true;
            return false;
        }
        pendingOp = null;
        currentInput = "";
        justEvaluated = true;
        return true;
    }

    /** Применяет процент: преобразует текущий ввод/значение в значение/100. */
    public boolean applyPercent() {
        if (errorState) return false;
        double v;
        if (!currentInput.isEmpty() && !currentInput.equals("-")) {
            v = parseInput();
            currentInput = "";
        } else {
            v = accumulator;
        }
        // Поведение «классического» калькулятора:
        //   если есть отложенный +/− → процент относительно accumulator;
        //   иначе — просто v/100.
        if (pendingOp != null && (pendingOp.equals(OP_ADD) || pendingOp.equals(OP_SUB))) {
            accumulator = accumulator + accumulator * (v / 100.0) * (pendingOp.equals(OP_SUB) ? -1 : 1);
            pendingOp = null;
        } else if (pendingOp != null) {
            accumulator = v / 100.0;
        } else {
            accumulator = v / 100.0;
        }
        justEvaluated = true;
        return true;
    }

    /** Унарные операции для расширенного калькулятора (√, x², 1/x, π). */
    public boolean unary(String op) {
        if (errorState) return false;
        double v;
        if (!currentInput.isEmpty() && !currentInput.equals("-")) {
            v = parseInput();
            currentInput = "";
        } else {
            v = accumulator;
        }
        double r;
        try {
            switch (op) {
                case OP_SQRT:
                    if (v < 0) throw new ArithmeticException("sqrt of negative");
                    r = Math.sqrt(v);
                    break;
                case OP_SQUARE:
                    r = v * v;
                    if (Double.isInfinite(r)) throw new ArithmeticException("overflow");
                    break;
                case OP_RECIPROCAL:
                    if (v == 0) throw new ArithmeticException("1/0");
                    r = 1.0 / v;
                    break;
                case OP_PI:
                    r = Math.PI;
                    break;
                default:
                    return false;
            }
        } catch (ArithmeticException e) {
            errorState = true;
            return false;
        }
        accumulator = r;
        justEvaluated = true;
        return true;
    }

    private double parseInput() {
        if (currentInput.isEmpty() || currentInput.equals("-")) return 0;
        try {
            return Double.parseDouble(currentInput);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double compute(double a, String op, double b) {
        switch (op) {
            case OP_ADD: return a + b;
            case OP_SUB: return a - b;
            case OP_MUL: return a * b;
            case OP_DIV:
                if (b == 0) throw new ArithmeticException("div by zero");
                return a / b;
            default:
                throw new IllegalArgumentException("Unknown op: " + op);
        }
    }

    /** Форматирование числа для дисплея: целые без точки, дробные — без хвостовых нулей. */
    private String formatNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            errorState = true;
            return "Ошибка";
        }
        if (Math.abs(v) >= 1e12) {
            // Очень большие — экспоненциальная запись
            return String.format(Locale.US, "%.6e", v);
        }
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format(Locale.US, "%.0f", v);
        }
        String s = String.format(Locale.US, "%.10f", v);
        // Удаляем хвостовые нули и лишнюю точку.
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
