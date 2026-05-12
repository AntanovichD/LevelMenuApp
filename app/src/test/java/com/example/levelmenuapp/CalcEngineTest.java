package com.example.levelmenuapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit-тесты для {@link CalcEngine}.
 * Запуск: ./gradlew :app:testDebugUnitTest
 *
 * Покрытие: базовые арифметические операции, цепочки операций, деление на 0,
 * унарные операции (√, x², 1/x), процент, смена знака, ввод точки, backspace,
 * восстановление после ошибки.
 */
public class CalcEngineTest {

    /** Помощник: «нажимаем» цифру за цифрой. */
    private static void type(CalcEngine e, String digits) {
        for (char c : digits.toCharArray()) {
            if (c == '.') e.inputDot();
            else e.inputDigit(c);
        }
    }

    // ===== 4 базовые операции =====

    @Test
    public void test_5_plus_3_eq_8() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "3");
        assertTrue(e.evaluate());
        assertEquals("8", e.getDisplay());
    }

    @Test
    public void test_12_minus_5_eq_7() {
        CalcEngine e = new CalcEngine();
        type(e, "12");
        e.applyOp(CalcEngine.OP_SUB);
        type(e, "5");
        assertTrue(e.evaluate());
        assertEquals("7", e.getDisplay());
    }

    @Test
    public void test_7_times_8_eq_56() {
        CalcEngine e = new CalcEngine();
        type(e, "7");
        e.applyOp(CalcEngine.OP_MUL);
        type(e, "8");
        assertTrue(e.evaluate());
        assertEquals("56", e.getDisplay());
    }

    @Test
    public void test_45_div_9_eq_5() {
        CalcEngine e = new CalcEngine();
        type(e, "45");
        e.applyOp(CalcEngine.OP_DIV);
        type(e, "9");
        assertTrue(e.evaluate());
        assertEquals("5", e.getDisplay());
    }

    @Test
    public void test_dot_arithmetic_1_5_plus_2_5_eq_4() {
        CalcEngine e = new CalcEngine();
        type(e, "1.5");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "2.5");
        assertTrue(e.evaluate());
        assertEquals("4", e.getDisplay());
    }

    // ===== Цепочки операций =====

    @Test
    public void test_chain_2_plus_3_times_4_eq_20() {
        // Левая ассоциативность без приоритетов: (2+3)*4 = 20
        CalcEngine e = new CalcEngine();
        type(e, "2");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "3");
        e.applyOp(CalcEngine.OP_MUL); // вычисляет 2+3=5, продолжает с *
        type(e, "4");
        assertTrue(e.evaluate());
        assertEquals("20", e.getDisplay());
    }

    @Test
    public void test_continue_after_equals() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "3");
        e.evaluate();
        assertEquals("8", e.getDisplay());
        // После «=» нажимаем «+ 2 =» — должно быть 10
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "2");
        assertTrue(e.evaluate());
        assertEquals("10", e.getDisplay());
    }

    @Test
    public void test_new_number_after_equals() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "3");
        e.evaluate();
        // После «=» нажимаем цифру 7 — выражение должно начаться заново с 7
        type(e, "7");
        assertEquals("7", e.getDisplay());
        e.applyOp(CalcEngine.OP_MUL);
        type(e, "2");
        e.evaluate();
        assertEquals("14", e.getDisplay());
    }

    @Test
    public void test_op_change_replaces_pending() {
        // 5 + × 3 = 15 (вторая операция заменяет первую)
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_ADD);
        e.applyOp(CalcEngine.OP_MUL);
        type(e, "3");
        e.evaluate();
        assertEquals("15", e.getDisplay());
    }

    @Test
    public void test_equals_with_same_operand_repeats() {
        // 5 + = → 5+5=10
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_ADD);
        assertTrue(e.evaluate());
        assertEquals("10", e.getDisplay());
    }

    // ===== Деление на 0 =====

    @Test
    public void test_div_by_zero_sets_error() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_DIV);
        type(e, "0");
        assertFalse(e.evaluate());
        assertTrue(e.isErrorState());
        assertEquals("Ошибка", e.getDisplay());
    }

    @Test
    public void test_error_recovers_on_digit() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.applyOp(CalcEngine.OP_DIV);
        type(e, "0");
        e.evaluate();
        assertTrue(e.isErrorState());
        // Новая цифра должна сбросить ошибку и начать новое выражение
        type(e, "3");
        assertFalse(e.isErrorState());
        assertEquals("3", e.getDisplay());
    }

    // ===== Унарные операции =====

    @Test
    public void test_sqrt_81_eq_9() {
        CalcEngine e = new CalcEngine();
        type(e, "81");
        assertTrue(e.unary(CalcEngine.OP_SQRT));
        assertEquals("9", e.getDisplay());
    }

    @Test
    public void test_sqrt_negative_is_error() {
        CalcEngine e = new CalcEngine();
        type(e, "9");
        e.toggleSign(); // -9
        assertFalse(e.unary(CalcEngine.OP_SQRT));
        assertTrue(e.isErrorState());
    }

    @Test
    public void test_square_5_eq_25() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        assertTrue(e.unary(CalcEngine.OP_SQUARE));
        assertEquals("25", e.getDisplay());
    }

    @Test
    public void test_reciprocal_4_eq_0_25() {
        CalcEngine e = new CalcEngine();
        type(e, "4");
        assertTrue(e.unary(CalcEngine.OP_RECIPROCAL));
        assertEquals("0.25", e.getDisplay());
    }

    @Test
    public void test_reciprocal_0_is_error() {
        CalcEngine e = new CalcEngine();
        type(e, "0");
        assertFalse(e.unary(CalcEngine.OP_RECIPROCAL));
        assertTrue(e.isErrorState());
    }

    @Test
    public void test_pi() {
        CalcEngine e = new CalcEngine();
        assertTrue(e.unary(CalcEngine.OP_PI));
        // 3.1415926536
        assertTrue(e.getDisplay().startsWith("3.14159265"));
    }

    // ===== Процент =====

    @Test
    public void test_simple_percent_50() {
        CalcEngine e = new CalcEngine();
        type(e, "50");
        assertTrue(e.applyPercent());
        assertEquals("0.5", e.getDisplay());
    }

    @Test
    public void test_percent_in_addition_200_plus_10pct_eq_220() {
        // Поведение «школьного» калькулятора: 200 + 10% = 220
        CalcEngine e = new CalcEngine();
        type(e, "200");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "10");
        e.applyPercent();
        assertEquals("220", e.getDisplay());
    }

    // ===== Backspace, sign, dot =====

    @Test
    public void test_backspace_removes_last_char() {
        CalcEngine e = new CalcEngine();
        type(e, "123");
        e.backspace();
        assertEquals("12", e.getDisplay());
        e.backspace();
        assertEquals("1", e.getDisplay());
        e.backspace();
        // currentInput пуст → отображаем accumulator (0)
        assertEquals("0", e.getDisplay());
    }

    @Test
    public void test_toggle_sign() {
        CalcEngine e = new CalcEngine();
        type(e, "5");
        e.toggleSign();
        assertEquals("-5", e.getDisplay());
        e.toggleSign();
        assertEquals("5", e.getDisplay());
    }

    @Test
    public void test_toggle_sign_after_result() {
        CalcEngine e = new CalcEngine();
        type(e, "10");
        e.evaluate();
        e.toggleSign();
        assertEquals("-10", e.getDisplay());
    }

    @Test
    public void test_dot_starts_with_zero() {
        CalcEngine e = new CalcEngine();
        e.inputDot();
        assertEquals("0.", e.getDisplay());
        e.inputDigit('5');
        assertEquals("0.5", e.getDisplay());
    }

    @Test
    public void test_dot_only_once() {
        CalcEngine e = new CalcEngine();
        type(e, "1.5");
        e.inputDot(); // должен игнорироваться
        e.inputDigit('7');
        assertEquals("1.57", e.getDisplay());
    }

    @Test
    public void test_clear_all_resets_state() {
        CalcEngine e = new CalcEngine();
        type(e, "123");
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "456");
        e.clearAll();
        assertEquals("0", e.getDisplay());
        assertFalse(e.isErrorState());
    }

    @Test
    public void test_negative_input_via_sign_then_digit() {
        CalcEngine e = new CalcEngine();
        e.toggleSign();      // -
        e.inputDigit('5');   // -5
        assertEquals("-5", e.getDisplay());
        e.applyOp(CalcEngine.OP_ADD);
        type(e, "3");
        e.evaluate();
        assertEquals("-2", e.getDisplay());
    }

    @Test
    public void test_leading_zero_replaced_by_digit() {
        CalcEngine e = new CalcEngine();
        e.inputDigit('0');
        e.inputDigit('5');
        // "0" не должно превратиться в "05" — должно стать "5"
        assertEquals("5", e.getDisplay());
    }

    @Test
    public void test_multiple_dots_with_zero_prefix() {
        CalcEngine e = new CalcEngine();
        e.inputDigit('0');
        e.inputDot();
        e.inputDigit('5');
        assertEquals("0.5", e.getDisplay());
    }

    // ===== Длинные сценарии =====

    @Test
    public void test_long_chain_1_plus_2_plus_3_plus_4_plus_5_eq_15() {
        CalcEngine e = new CalcEngine();
        type(e, "1");
        for (int i = 2; i <= 5; i++) {
            e.applyOp(CalcEngine.OP_ADD);
            type(e, String.valueOf(i));
        }
        e.evaluate();
        assertEquals("15", e.getDisplay());
    }

    @Test
    public void test_mixed_chain_10_minus_3_times_2_div_2_eq_7() {
        // (((10-3)*2)/2) = 7
        CalcEngine e = new CalcEngine();
        type(e, "10");
        e.applyOp(CalcEngine.OP_SUB);
        type(e, "3");
        e.applyOp(CalcEngine.OP_MUL);
        type(e, "2");
        e.applyOp(CalcEngine.OP_DIV);
        type(e, "2");
        e.evaluate();
        assertEquals("7", e.getDisplay());
    }
}
