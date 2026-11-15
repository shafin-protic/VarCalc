package com.example.engine

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.roundToLong

sealed interface MathNode {
  data class NumberNode(val text: String) : MathNode
  data class VariableNode(val name: String) : MathNode
  data class PercentNode(val inner: MathNode) : MathNode
  data class UnaryNode(val op: Char, val inner: MathNode) : MathNode
  data class BinaryNode(val left: MathNode, val op: Char, val right: MathNode) : MathNode
  data class FractionNode(val numerator: MathNode, val denominator: MathNode) : MathNode
  data class ParenNode(val inner: MathNode) : MathNode
  data object PlaceholderNode : MathNode
}

sealed interface EvalResult {
  data class Success(
    val value: Double,
    val usedVariables: Set<String>,
  ) : EvalResult

  data class Error(val message: String) : EvalResult
}

data class FunctionFormula(
  val outputVar: String,
  val expression: String,
)

private sealed interface Token {
  data class Number(val text: String) : Token
  data class Ident(val name: String) : Token
  data class Op(val ch: Char) : Token
  data object LParen : Token
  data object RParen : Token
  data object Percent : Token
}

object MathEngine {

  private fun tokenize(raw: String): List<Token> {
    val expr = raw.replace('×', '*').replace('÷', '/').replace('−', '-')
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < expr.length) {
      val c = expr[i]
      when {
        c.isWhitespace() -> i++
        c.isDigit() || c == '.' -> {
          val start = i
          var dotCount = if (c == '.') 1 else 0
          i++
          while (i < expr.length) {
            val nc = expr[i]
            if (nc.isDigit()) {
              i++
            } else if (nc == '.' && dotCount == 0) {
              dotCount++
              i++
            } else {
              break
            }
          }
          tokens.add(Token.Number(expr.substring(start, i)))
        }
        c.isLetter() || c == '_' -> {
          val start = i
          i++
          while (i < expr.length && (expr[i].isLetterOrDigit() || expr[i] == '_')) {
            i++
          }
          tokens.add(Token.Ident(expr.substring(start, i)))
        }
        c == '(' -> {
          tokens.add(Token.LParen)
          i++
        }
        c == ')' -> {
          tokens.add(Token.RParen)
          i++
        }
        c == '%' -> {
          tokens.add(Token.Percent)
          i++
        }
        c == '+' || c == '-' || c == '*' || c == '/' -> {
          tokens.add(Token.Op(c))
          i++
        }
        else -> i++
      }
    }
    return insertImplicitMultiplication(tokens)
  }

  private fun insertImplicitMultiplication(raw: List<Token>): List<Token> {
    if (raw.size <= 1) return raw
    val out = ArrayList<Token>(raw.size + 4)
    for (idx in raw.indices) {
      val curr = raw[idx]
      if (idx > 0) {
        val prev = raw[idx - 1]
        val prevEndsOperand =
          prev is Token.Number || prev is Token.Ident || prev is Token.RParen || prev is Token.Percent
        val currStartsOperand =
          curr is Token.Number || curr is Token.Ident || curr is Token.LParen
        if (prevEndsOperand && currStartsOperand) {
          out.add(Token.Op('*'))
        }
      }
      out.add(curr)
    }
    return out
  }

  /**
   * Lenient AST builder used for live mathematical display (including stacked fractions
   * while the user is mid-expression, e.g., "5/" or "(a+b)/").
   */
  fun parseForDisplay(expression: String): MathNode {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) return MathNode.PlaceholderNode
    val tokens = tokenize(trimmed)
    if (tokens.isEmpty()) return MathNode.PlaceholderNode
    val parser = DisplayParser(tokens)
    return parser.parseExpression()
  }

  fun extractIdentifiers(expression: String): Set<String> {
    return tokenize(expression)
      .filterIsInstance<Token.Ident>()
      .map { it.name }
      .toSet()
  }

  fun evaluate(
    expression: String,
    variables: Map<String, Double>,
    ansValue: Double? = null,
    smartPercentage: Boolean = true,
  ): EvalResult {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) return EvalResult.Error("Empty expression")
    val tokens = tokenize(trimmed)
    if (tokens.isEmpty()) return EvalResult.Error("Invalid expression")
    val usedVars = mutableSetOf<String>()
    val parser = StrictEvalParser(tokens, variables, ansValue, smartPercentage, usedVars)
    return try {
      val value = parser.parseExpression()
      if (parser.hasRemaining()) {
        EvalResult.Error("Unexpected token")
      } else if (value.isNaN() || value.isInfinite()) {
        EvalResult.Error("Math error")
      } else {
        EvalResult.Success(value, usedVars)
      }
    } catch (e: EvalException) {
      EvalResult.Error(e.message ?: "Syntax error")
    }
  }

  /**
   * Evaluates multiple chained function formulas (e.g. x=(a+b)/2*5%, y=5%*x, z=(a+b)/c)
   * resolving dependencies between outputs automatically.
   */
  fun evaluateFunctions(
    formulas: List<FunctionFormula>,
    inputs: Map<String, Double>,
    smartPercentage: Boolean = true,
  ): Map<String, EvalResult> {
    val results = linkedMapOf<String, EvalResult>()
    val resolvedVars = inputs.toMutableMap()
    val formulaMap = formulas.associateBy { it.outputVar }
    val visiting = mutableSetOf<String>()
    val visited = mutableSetOf<String>()

    fun resolve(varName: String): EvalResult {
      results[varName]?.let { return it }
      if (varName in visiting) {
        val err = EvalResult.Error("Circular reference: $varName")
        results[varName] = err
        return err
      }
      val formula = formulaMap[varName]
      if (formula == null) {
        val inputVal = resolvedVars[varName]
        return if (inputVal != null) {
          EvalResult.Success(inputVal, emptySet())
        } else {
          EvalResult.Error("Undefined: $varName")
        }
      }
      if (formula.expression.isBlank()) {
        val err = EvalResult.Error("No formula for $varName")
        results[varName] = err
        return err
      }

      visiting.add(varName)
      val referenced = extractIdentifiers(formula.expression)
      for (dep in referenced) {
        if (dep != varName && dep in formulaMap && dep !in visited) {
          val depRes = resolve(dep)
          if (depRes is EvalResult.Error) {
            visiting.remove(varName)
            visited.add(varName)
            results[varName] = depRes
            return depRes
          }
        } else if (dep == varName) {
          visiting.remove(varName)
          visited.add(varName)
          val err = EvalResult.Error("Self reference: $varName")
          results[varName] = err
          return err
        }
      }
      visiting.remove(varName)

      val eval = evaluate(
        expression = formula.expression,
        variables = resolvedVars,
        ansValue = null,
        smartPercentage = smartPercentage,
      )
      if (eval is EvalResult.Success) {
        resolvedVars[varName] = eval.value
      }
      visited.add(varName)
      results[varName] = eval
      return eval
    }

    for (f in formulas) {
      resolve(f.outputVar)
    }
    return results
  }

  fun formatNumber(
    value: Double,
    decimalPlaces: Int = 2,
    trimTrailingZeros: Boolean = false,
  ): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
    val clean = if (abs(value) < 1e-12) 0.0 else value
    val isWhole = abs(clean - clean.roundToLong().toDouble()) < 1e-9 && abs(clean) < 1e14
    if (isWhole) {
      return clean.roundToLong().toString()
    }
    val safePlaces = decimalPlaces.coerceIn(0, 8)
    val bd = BigDecimal.valueOf(clean).setScale(safePlaces, RoundingMode.HALF_UP)
    val normalized = if (bd.compareTo(BigDecimal.ZERO) == 0) {
      BigDecimal.ZERO.setScale(safePlaces, RoundingMode.HALF_UP)
    } else {
      bd
    }
    return if (trimTrailingZeros) {
      normalized.stripTrailingZeros().toPlainString()
    } else {
      normalized.toPlainString()
    }
  }

  private class DisplayParser(private val tokens: List<Token>) {
    private var pos = 0

    fun parseExpression(): MathNode = parseAddSub()

    private fun parseAddSub(): MathNode {
      var left = parseMulDiv()
      while (pos < tokens.size) {
        val t = tokens[pos]
        if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
          pos++
          val right = if (pos < tokens.size && tokens[pos] !is Token.RParen) {
            parseMulDiv()
          } else {
            MathNode.PlaceholderNode
          }
          left = MathNode.BinaryNode(left, t.ch, right)
        } else {
          break
        }
      }
      return left
    }

    private fun parseMulDiv(): MathNode {
      var left = parseUnary()
      while (pos < tokens.size) {
        val t = tokens[pos]
        if (t is Token.Op && (t.ch == '*' || t.ch == '/')) {
          pos++
          val right = if (pos < tokens.size && tokens[pos] !is Token.RParen) {
            parseUnary()
          } else {
            MathNode.PlaceholderNode
          }
          left = if (t.ch == '/') {
            MathNode.FractionNode(unwrapParens(left), unwrapParens(right))
          } else {
            MathNode.BinaryNode(left, '*', right)
          }
        } else {
          break
        }
      }
      return left
    }

    private fun unwrapParens(node: MathNode): MathNode {
      return if (node is MathNode.ParenNode) node.inner else node
    }

    private fun parseUnary(): MathNode {
      if (pos < tokens.size) {
        val t = tokens[pos]
        if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
          pos++
          val inner = if (pos < tokens.size && tokens[pos] !is Token.RParen) {
            parseUnary()
          } else {
            MathNode.PlaceholderNode
          }
          return MathNode.UnaryNode(t.ch, inner)
        }
      }
      return parsePostfix()
    }

    private fun parsePostfix(): MathNode {
      var node = parsePrimary()
      while (pos < tokens.size && tokens[pos] is Token.Percent) {
        pos++
        node = MathNode.PercentNode(node)
      }
      return node
    }

    private fun parsePrimary(): MathNode {
      if (pos >= tokens.size) return MathNode.PlaceholderNode
      return when (val t = tokens[pos]) {
        is Token.Number -> {
          pos++
          MathNode.NumberNode(t.text)
        }
        is Token.Ident -> {
          pos++
          MathNode.VariableNode(t.name)
        }
        is Token.LParen -> {
          pos++
          val inner = if (pos < tokens.size && tokens[pos] !is Token.RParen) {
            parseExpression()
          } else {
            MathNode.PlaceholderNode
          }
          if (pos < tokens.size && tokens[pos] is Token.RParen) {
            pos++
          }
          MathNode.ParenNode(inner)
        }
        else -> {
          pos++
          MathNode.PlaceholderNode
        }
      }
    }
  }

  private class EvalException(message: String) : RuntimeException(message)

  private data class TermVal(val value: Double, val isPercent: Boolean)

  private class StrictEvalParser(
    private val tokens: List<Token>,
    private val variables: Map<String, Double>,
    private val ansValue: Double?,
    private val smartPercentage: Boolean,
    private val usedVars: MutableSet<String>,
  ) {
    private var pos = 0

    fun hasRemaining(): Boolean = pos < tokens.size

    fun parseExpression(): Double {
      var left = parseMulDiv()
      while (pos < tokens.size) {
        val t = tokens[pos]
        if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
          pos++
          val right = parseMulDiv()
          val delta = if (smartPercentage && right.isPercent) {
            left.value * right.value
          } else {
            right.value
          }
          val combined = if (t.ch == '+') left.value + delta else left.value - delta
          left = TermVal(combined, isPercent = false)
        } else {
          break
        }
      }
      return left.value
    }

    private fun parseMulDiv(): TermVal {
      var left = parseUnary()
      while (pos < tokens.size) {
        val t = tokens[pos]
        if (t is Token.Op && (t.ch == '*' || t.ch == '/')) {
          pos++
          val right = parseUnary()
          val value = if (t.ch == '*') {
            left.value * right.value
          } else {
            if (abs(right.value) < 1e-15) throw EvalException("Division by zero")
            left.value / right.value
          }
          left = TermVal(value, isPercent = false)
        } else {
          break
        }
      }
      return left
    }

    private fun parseUnary(): TermVal {
      if (pos < tokens.size) {
        val t = tokens[pos]
        if (t is Token.Op && (t.ch == '+' || t.ch == '-')) {
          pos++
          val inner = parseUnary()
          val signed = if (t.ch == '-') -inner.value else inner.value
          return TermVal(signed, inner.isPercent)
        }
      }
      return parsePostfix()
    }

    private fun parsePostfix(): TermVal {
      var node = parsePrimary()
      while (pos < tokens.size && tokens[pos] is Token.Percent) {
        pos++
        node = TermVal(node.value / 100.0, isPercent = true)
      }
      return node
    }

    private fun parsePrimary(): TermVal {
      if (pos >= tokens.size) throw EvalException("Incomplete expression")
      return when (val t = tokens[pos]) {
        is Token.Number -> {
          pos++
          val normalized = if (t.text == ".") "0.0" else if (t.text.startsWith(".")) "0${t.text}" else t.text
          val num = normalized.toDoubleOrNull() ?: throw EvalException("Invalid number: ${t.text}")
          TermVal(num, isPercent = false)
        }
        is Token.Ident -> {
          pos++
          if (t.name.equals("ANS", ignoreCase = true)) {
            val ans = ansValue ?: throw EvalException("ANS is empty")
            TermVal(ans, isPercent = false)
          } else {
            val v = variables[t.name]
              ?: variables.entries.firstOrNull { it.key.equals(t.name, ignoreCase = false) }?.value
              ?: throw EvalException("Undefined variable: ${t.name}")
            usedVars.add(t.name)
            TermVal(v, isPercent = false)
          }
        }
        is Token.LParen -> {
          pos++
          val inside = parseExpression()
          if (pos >= tokens.size || tokens[pos] !is Token.RParen) {
            throw EvalException("Missing closing ')'")
          }
          pos++
          TermVal(inside, isPercent = false)
        }
        else -> throw EvalException("Unexpected token")
      }
    }
  }
}
