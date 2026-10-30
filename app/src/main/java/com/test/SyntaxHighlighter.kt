package com.test

import android.text.Editable
import android.text.Spanned
import android.text.style.ForegroundColorSpan

object SyntaxHighlighter {

    private const val MAX_LENGTH = 300_000

    private const val KEYWORD = 0xFFC586C0.toInt()
    private const val TYPE = 0xFF4EC9B0.toInt()
    private const val LITERAL = 0xFF569CD6.toInt()
    private const val STRING = 0xFFCE9178.toInt()
    private const val COMMENT = 0xFF6A9955.toInt()
    private const val NUMBER = 0xFFB5CEA8.toInt()

    private val TOKEN = Regex(
        """"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|//[^\n]*|#[^\n]*|/\*[\s\S]*?\*/|\b\d+(?:\.\d+)?\b|\b[A-Za-z_][A-Za-z0-9_]*\b"""
    )

    private val KEYWORDS = hashSetOf(
        "fun", "val", "var", "class", "object", "interface", "enum", "typealias",
        "if", "else", "when", "for", "while", "do", "return", "break", "continue",
        "in", "is", "as", "null", "true", "false", "this", "super",
        "new", "delete", "public", "private", "protected", "static", "final",
        "void", "int", "long", "float", "double", "boolean", "char", "byte", "short",
        "function", "const", "let", "import", "from", "package", "export", "default",
        "async", "await", "try", "catch", "finally", "throw", "throws", "switch",
        "case", "struct", "impl", "use", "mod", "pub", "fn", "mut", "match", "loop",
        "extends", "implements", "abstract", "override", "open", "data", "sealed",
        "companion", "init", "get", "set", "by", "it", "and", "or", "not",
        "def", "elif", "lambda", "with", "yield", "global", "nonlocal", "assert",
        "del", "pass", "raise", "except", "typeof", "instanceof", "of", "namespace",
        "template", "typename", "using", "virtual", "inline", "static_cast",
        "auto", "register", "volatile", "extern", "sizeof", "typedef", "union",
        "go", "defer", "chan", "select", "range", "fallthrough", "map", "func"
    )

    private val LITERALS = hashSetOf(
        "true", "false", "null", "None", "True", "False", "nil", "undefined", "void"
    )

    fun apply(text: Editable) {
        for (span in text.getSpans(0, text.length, ForegroundColorSpan::class.java)) {
            text.removeSpan(span)
        }
        if (text.length > MAX_LENGTH) return

        for (match in TOKEN.findAll(text)) {
            val color = colorFor(match.value) ?: continue
            text.setSpan(
                ForegroundColorSpan(color),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun colorFor(token: String): Int? = when {
        token.startsWith("//") || token.startsWith("#") || token.startsWith("/*") -> COMMENT
        token.startsWith("\"") || token.startsWith("'") -> STRING
        token[0].isDigit() -> NUMBER
        token in LITERALS -> LITERAL
        token in KEYWORDS -> KEYWORD
        token[0].isUpperCase() && token.length > 1 -> TYPE
        else -> null
    }
}
