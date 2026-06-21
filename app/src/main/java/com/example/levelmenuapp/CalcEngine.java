package com.example.levelmenuapp;

import java.util.Locale;

/**
 * Чистая логика калькулятора (без зависимостей от Android — это делает её
 * легко тестируемой).
 *
 * Состояние (минимальное и однозначное):
 *   accumulator   — накопленное значение (левый операнд для отложенной операции
 *                   или последний показанный результат);
 *   pendingOp     — отложенная бинарная операция (null если ни одна не нажата);
 *   currentInput  — текущая строка ввода пользователя ("" если ввод не начат);
 *   justEvaluated — true только сразу после нажатия «=» / унарной операции,
 *                   т.е. accumulator содержит свежий результат и следующая
 *                   введённая цифра должна начать новое выражение;
 *   errorState    — true при ошибочной операции (1/0, √отрицательного и т.п.).
 *
 * Поведение — типичный «школьный» калькулятор: операции применяются строго
 * слева направо, без приоритетов. При нажатии новой бинарной операции с уже
 * введённым вторым операндом промежуточный результат вычисляется и продолжает
 * накапливаться.
 */
public class CalcEngine {

    public static final String OP_ADD = "+";
    public static final String OP_SUB = "\u2212"; // U+2212 MINUS SIGN
    public static final String OP_MUL = "\u00D7"; // U+00D7 MULTIPLICATION SIGN
    public static final String OP_DIV = "\u00F7"; // U+00F7 DIVISION SIGN

    public static final String OP_SQRT       = "\u221A";    // √
    public static final String OP_SQUARE     = "x\u00B2";   // x²
    public static final String OP_RECIPROCAL = "1/x";
    public static final String OP_PI         = "\u03C0";    // π

    private double accumulator = 0;
    private String pendingOp = null;
    private String currentInput = "";
    private boolean justEvaluated = false;
    private boolean errorState = false;

    private static final int MAX_DIGITS = 12;

    // ===== Getters / setters (используются Activity для save/restore) =====

    public double getAccumulator() { return accumulator; }
    public void setAccumulator(double v) { accumulator = v; }

    public String getPendingOp() { return pendingOp; }
    public void setPendingOp(String v) { pendingOp = v; }

    public String getCurrentInputState() { return currentInput; }
    public void setCurrentInputState(String v) { currentInput = (v == null) ? "" : v; }

    public boolean isJustEvaluated() { return justEvaluated; }
    public void setJustEvaluated(boolean v) { justEvaluated = v; }

    public boolean isErrorState() { return errorState; }
    public void setErrorState(boolean v) { errorState = v; }

    // ===== Базовые операции =====

    /** Сбрасывает всё состояние. */
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
        if (currentInput.equals("-")) return "-0";
        if (!currentInput.isEmpty()) return currentInput;
        return formatNumber(accumulator);
    }

    public void inputDigit(char d) {
        if (errorState) clearAll();
        if (justEvaluated) {
            // После «=» / унарной операции новая цифра начинает выражение заново.
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
        } else if (currentInput.equals("-")) {
            currentInput = "";
        } else if (currentInput.startsWith("-")) {
            currentInput = currentInput.substring(1);
        } else {
            currentInput = "-" + currentInput;
        }
    }

    /** Удаляет последний символ — реализует требование ЛР №5 о «стирании некорректного ввода». */
    public void backspace() {
        if (errorState) {
            clearAll();
            return;
        }
        if (justEvaluated) {
            // После «=» backspace просто снимает флаг — следующее нажатие цифры
            // продолжит работу с тем же accumulator.
            justEvaluated = false;
            return;
        }
        if (!currentInput.isEmpty()) {
            currentInput = currentInput.substring(0, currentInput.length() - 1);
        }
    }

    /**
     * Применяет бинарную операцию (+ − × ÷). Если уже была отложенная операция
     * и введён второй операнд — сначала вычисляет промежуточный результат.
     * Возвращает false при ошибке (деление на 0 в накапливаемой цепочке).
     */
    public boolean applyOp(String op) {
        if (errorState) return false;

        if (!currentInput.isEmpty() && !currentInput.equals("-")) {
            // Завершаем ввод текущего операнда и применяем отложенную операцию (если была).
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
        }
        // Если currentInput пуст:
        //   - есть отложенная операция → пользователь просто меняет её на новую
        //     (заменяем pendingOp, accumulator не трогаем);
        //   - нет отложенной (например, сразу после старта) → берём accumulator
        //     как левый операнд (это 0 по умолчанию или последний результат).

        pendingOp = op;
        justEvaluated = false;
        return true;
    }

    /** Выполняет «=». Возвращает false при ошибке. */
    public boolean evaluate() {
        if (errorState) return false;
        if (pendingOp == null) {
            // Нет отложенной операции — если есть введённый операнд, переносим
            // его в accumulator, иначе оставляем как есть.
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

    /** Процент: преобразует текущее значение на дисплее в значение/100. */
    public boolean applyPercent() {
        if (errorState) return false;
        double v;
        if (!currentInput.isEmpty() && !currentInput.equals("-")) {
            v = parseInput();
            currentInput = "";
        } else {
            v = accumulator;
        }
        if (pendingOp != null && (pendingOp.equals(OP_ADD) || pendingOp.equals(OP_SUB))) {
            // Классическое поведение «a + b%» = a + a*(b/100)
            double delta = accumulator * (v / 100.0);
            if (pendingOp.equals(OP_SUB)) delta = -delta;
            accumulator = accumulator + delta;
            pendingOp = null;
        } else if (pendingOp != null && (pendingOp.equals(OP_MUL) || pendingOp.equals(OP_DIV))) {
            // «a × b%» = a × (b/100)
            try {
                accumulator = compute(accumulator, pendingOp, v / 100.0);
            } catch (ArithmeticException e) {
                errorState = true;
                return false;
            }
            pendingOp = null;
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
    String formatNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            errorState = true;
            return "Ошибка";
        }
        if (Math.abs(v) >= 1e12) {
            return String.format(Locale.US, "%.6e", v);
        }
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format(Locale.US, "%.0f", v);
        }
        String s = String.format(Locale.US, "%.10f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
