package com.example

import com.example.engine.EvalResult
import com.example.engine.FunctionFormula
import com.example.engine.MathEngine
import com.example.engine.MathNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MathEngineTest {

  @Test
  fun `evaluates arithmetic and percentages accurately`() {
    val res1 = MathEngine.evaluate("(10+20)/2*5%", emptyMap()) as EvalResult.Success
    assertEquals(0.75, res1.value, 1e-9)

    val res2 = MathEngine.evaluate("100+10%", emptyMap(), smartPercentage = true) as EvalResult.Success
    assertEquals(110.0, res2.value, 1e-9)

    val res3 = MathEngine.evaluate("200-15%", emptyMap(), smartPercentage = true) as EvalResult.Success
    assertEquals(170.0, res3.value, 1e-9)
  }

  @Test
  fun `evaluates chained function formulas with custom text variables`() {
    val formulas = listOf(
      FunctionFormula("y", "5%*x"),
      FunctionFormula("x", "(man+distance)/2*5%"),
      FunctionFormula("z", "(man+distance)/c"),
    )
    val inputs = mapOf("man" to 10.0, "distance" to 30.0, "c" to 8.0)
    val results = MathEngine.evaluateFunctions(formulas, inputs)

    val x = results["x"] as EvalResult.Success
    val y = results["y"] as EvalResult.Success
    val z = results["z"] as EvalResult.Success

    assertEquals(1.0, x.value, 1e-9)
    assertEquals(0.05, y.value, 1e-9)
    assertEquals(5.0, z.value, 1e-9)
  }

  @Test
  fun `parses division into stacked fraction AST`() {
    val ast = MathEngine.parseForDisplay("(a+b)/2")
    assertTrue(ast is MathNode.FractionNode)
    val frac = ast as MathNode.FractionNode
    assertTrue(frac.numerator is MathNode.BinaryNode)
    assertTrue(frac.denominator is MathNode.NumberNode)
  }

  @Test
  fun `formats fractional numbers to 2 decimals by default`() {
    assertEquals("3.33", MathEngine.formatNumber(10.0 / 3.0, decimalPlaces = 2))
    assertEquals("2.50", MathEngine.formatNumber(5.0 / 2.0, decimalPlaces = 2))
    assertEquals("2.5", MathEngine.formatNumber(5.0 / 2.0, decimalPlaces = 2, trimTrailingZeros = true))
    assertEquals("4", MathEngine.formatNumber(8.0 / 2.0, decimalPlaces = 2))
  }
}
