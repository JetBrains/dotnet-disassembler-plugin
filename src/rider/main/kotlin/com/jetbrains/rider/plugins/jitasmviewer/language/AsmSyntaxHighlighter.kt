package com.jetbrains.rider.plugins.jitasmviewer.language

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class AsmSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        val LABEL = TextAttributesKey.createTextAttributesKey(
            "ASM_LABEL",
            DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
        )

        val OPCODE = TextAttributesKey.createTextAttributesKey(
            "ASM_OPCODE",
            DefaultLanguageHighlighterColors.KEYWORD
        )

        val OPERAND = TextAttributesKey.createTextAttributesKey(
            "ASM_OPERAND",
            DefaultLanguageHighlighterColors.PARAMETER
        )

        val COMMENT = TextAttributesKey.createTextAttributesKey(
            "ASM_COMMENT",
            DefaultLanguageHighlighterColors.LINE_COMMENT
        )

        // Common ASM opcodes (x64, ARM64, x86)
        internal val OPCODES = setOf(
            // Data movement
            "mov", "movz", "movk", "movn", "movt", "movw", "movs", "movabs", "movsb", "movsw", "movsd", "movsq",
            "ldr", "ldrb", "ldrh", "ldrsb", "ldrsh", "ldrsw", "ldp",
            "str", "strb", "strh", "stp",
            "lea", "leaq", "ld", "st",

            // Arithmetic
            "add", "adds", "adc", "adcs", "sub", "subs", "sbc", "sbcs",
            "mul", "madd", "msub", "smull", "umull", "smulh", "umulh",
            "div", "sdiv", "udiv",
            "imul", "idiv",
            "inc", "dec", "neg", "negs",

            // Logical
            "and", "ands", "orr", "eor", "eon", "bic", "bics", "orn",
            "or", "xor", "not", "mvn",
            "tst", "teq",

            // Shifts and rotates
            "lsl", "lsr", "asr", "ror",
            "shl", "shr", "sal", "sar", "rol",
            "sxtb", "sxth", "sxtw", "uxtb", "uxth",

            // Comparison
            "cmp", "cmn", "cmpxchg", "test",

            // Conditional
            "csel", "csinc", "csinv", "csneg", "cset", "csetm",
            "cmove", "cmovne", "cmovg", "cmovge", "cmovl", "cmovle", "cmova", "cmovae", "cmovb", "cmovbe",
            "sete", "setne", "setg", "setge", "setl", "setle", "seta", "setae", "setb", "setbe",

            // Branches and jumps
            "b", "bl", "blr", "br", "ret", "eret",
            "cbz", "cbnz", "tbz", "tbnz",
            "b.eq", "b.ne", "b.cs", "b.cc", "b.mi", "b.pl", "b.vs", "b.vc",
            "b.hi", "b.ls", "b.ge", "b.lt", "b.gt", "b.le", "b.al",
            "jmp", "je", "jne", "jz", "jnz", "jg", "jge", "jl", "jle",
            "ja", "jae", "jb", "jbe", "js", "jns", "jo", "jno", "jp", "jnp",
            "call", "callq", "retq", "retn", "jmpq",

            // Stack
            "push", "pop", "pushq", "popq",
            "enter", "leave",

            // Address calculation
            "adr", "adrp",

            // Synchronization
            "dmb", "dsb", "isb",
            "ldaxr", "stlxr", "ldar", "stlr",
            "ldapr", "ldaprb", "ldaprh",
            "ldaxp", "stlxp", "ldxr", "stxr", "ldxp", "stxp",
            "ldlarb", "ldlarh", "ldlar", "stllrb", "stllrh", "stllr",

            // Misc
            "nop", "brk", "hlt", "wfe", "wfi", "yield",
            "int", "syscall", "sysenter", "sysexit",
            "clz", "cls", "rbit", "rev", "rev16", "rev32",
            "bswap", "cpuid", "rdtsc",

            // Floating point
            "fadd", "fsub", "fmul", "fdiv", "fsqrt", "fabs", "fneg",
            "fcmp", "fcsel", "fmov", "fcvt", "fcvtzs", "fcvtzu", "scvtf", "ucvtf",
            "fmadd", "fmsub", "fnmadd", "fnmsub",
            "fmax", "fmin", "fmaxnm", "fminnm",

            // SIMD
            "dup", "ins", "umov", "smov",
            "addv", "saddw", "uaddw",
            "movd", "movq", "movaps", "movups", "movdqa", "movdqu",
            "paddd", "psubd", "pxor", "por", "pand"
        )
    }

    override fun getHighlightingLexer() = AsmLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        return when (tokenType) {
            AsmTokenTypes.LABEL -> arrayOf(LABEL)
            AsmTokenTypes.OPCODE -> arrayOf(OPCODE)
            AsmTokenTypes.OPERAND -> arrayOf(OPERAND)
            AsmTokenTypes.COMMENT -> arrayOf(COMMENT)
            else -> emptyArray()
        }
    }
}

object AsmTokenTypes {
    val LABEL = IElementType("ASM_LABEL", null)
    val OPCODE = IElementType("ASM_OPCODE", null)
    val OPERAND = IElementType("ASM_OPERAND", null)
    val COMMENT = IElementType("ASM_COMMENT", null)
    val WHITESPACE = IElementType("ASM_WHITESPACE", null)
}

class AsmLexer : com.intellij.lexer.LexerBase() {
    private var buffer: CharSequence = ""
    private var startOffset = 0
    private var endOffset = 0
    private var currentOffset = 0
    private var currentTokenType: IElementType? = null
    private var lineStart = true

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.startOffset = startOffset
        this.endOffset = endOffset
        this.currentOffset = startOffset
        this.lineStart = true
        advance()
    }

    override fun getState() = if (lineStart) 1 else 0

    override fun getTokenType() = currentTokenType

    override fun getTokenStart() = startOffset

    override fun getTokenEnd() = currentOffset

    override fun advance() {
        if (currentOffset >= endOffset) {
            currentTokenType = null
            return
        }

        startOffset = currentOffset
        val ch = buffer[currentOffset]

        when {
            ch == ';' -> {
                while (currentOffset < endOffset && buffer[currentOffset] != '\n') {
                    currentOffset++
                }
                currentTokenType = AsmTokenTypes.COMMENT
            }

            ch == '\n' -> {
                currentOffset++
                lineStart = true
                currentTokenType = AsmTokenTypes.WHITESPACE
            }

            ch.isWhitespace() -> {
                while (currentOffset < endOffset &&
                       buffer[currentOffset].isWhitespace() &&
                       buffer[currentOffset] != '\n') {
                    currentOffset++
                }
                currentTokenType = AsmTokenTypes.WHITESPACE
            }

            lineStart && isIdentifierStart(ch) -> {
                val colonPos = findColonInLine()
                if (colonPos > currentOffset) {
                    currentOffset = colonPos + 1
                    currentTokenType = AsmTokenTypes.LABEL
                    lineStart = false
                } else {
                    parseOpcodeOrOperand()
                }
            }

            else -> parseOpcodeOrOperand()
        }
    }

    private fun parseOpcodeOrOperand() {
        lineStart = false
        val wordEnd = findWordEnd(currentOffset)

        if (wordEnd > currentOffset) {
            val word = buffer.subSequence(currentOffset, wordEnd).toString().lowercase()
            if (AsmSyntaxHighlighter.OPCODES.contains(word)) {
                currentOffset = wordEnd
                currentTokenType = AsmTokenTypes.OPCODE
                return
            }
        }

        while (currentOffset < endOffset) {
            val ch = buffer[currentOffset]
            if (ch == '\n' || ch == ';') break
            currentOffset++
        }

        while (currentOffset > startOffset && buffer[currentOffset - 1].isWhitespace()) {
            currentOffset--
        }

        currentTokenType = if (currentOffset > startOffset) AsmTokenTypes.OPERAND else AsmTokenTypes.WHITESPACE
    }

    private fun isIdentifierStart(ch: Char) = ch.isLetterOrDigit() || ch == '_' || ch == '.'

    private fun findColonInLine(): Int {
        var pos = currentOffset

        while (pos < endOffset && isIdentifierStart(buffer[pos])) {
            pos++
        }

        return if (pos in (currentOffset + 1)..<endOffset && buffer[pos] == ':') pos else currentOffset
    }

    private fun findWordEnd(start: Int): Int {
        var pos = start
        while (pos < endOffset && isIdentifierStart(buffer[pos])) {
            pos++
        }
        return pos
    }

    override fun getBufferSequence() = buffer

    override fun getBufferEnd() = endOffset
}
