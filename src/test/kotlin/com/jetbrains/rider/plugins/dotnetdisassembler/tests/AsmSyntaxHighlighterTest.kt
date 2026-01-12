package com.jetbrains.rider.plugins.dotnetdisassembler.tests

import com.jetbrains.rider.plugins.dotnetdisassembler.language.AsmLexer
import com.jetbrains.rider.plugins.dotnetdisassembler.language.AsmSyntaxHighlighter
import com.jetbrains.rider.plugins.dotnetdisassembler.language.AsmTokenTypes
import org.testng.Assert.*
import org.testng.annotations.Test

class AsmSyntaxHighlighterTest {

    class BasicTokenTypes {

        @Test
        fun testTokenizeComment() {
            // Arrange
            val lexer = AsmLexer()
            val code = "; This is a comment"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.COMMENT)
            assertEquals(lexer.tokenText, code)
        }

        @Test
        fun testTokenizeLabel() {
            // Arrange
            val lexer = AsmLexer()
            val code = "G_M000_IG01:"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.LABEL)
            assertEquals(lexer.tokenText, code)
        }

        @Test
        fun testTokenizeLabelWithDot() {
            // Arrange
            val lexer = AsmLexer()
            val code = ".L001:"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.LABEL)
            assertEquals(lexer.tokenText, code)
        }

        @Test
        fun testTokenizeSimpleInstruction() {
            // Arrange
            val lexer = AsmLexer()
            val code = "mov eax, 5"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "mov")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPERAND)
            assertEquals(lexer.tokenText, "eax, 5")
        }

        @Test
        fun testTokenizeNop() {
            // Arrange
            val lexer = AsmLexer()
            val code = "nop"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "nop")
        }
    }

    class ComplexInstructions {

        @Test
        fun testTokenizeLabelWithInstruction() {
            // Arrange
            val lexer = AsmLexer()
            val code = "loop_start:\n    mov rax, rbx"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.LABEL)
            assertTrue(lexer.tokenText.startsWith("loop_start:"))

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "mov")
        }

        @Test
        fun testTokenizeInstructionWithComment() {
            // Arrange
            val lexer = AsmLexer()
            val code = "add eax, ebx ; Add registers"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "add")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPERAND)
            assertEquals(lexer.tokenText, "eax, ebx")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.COMMENT)
            assertTrue(lexer.tokenText.startsWith(";"))
        }

        @Test
        fun testTokenizeMultipleInstructions() {
            // Arrange
            val lexer = AsmLexer()
            val code = "push rbp\nmov rbp, rsp\npop rbp"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "push")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPERAND)
            assertEquals(lexer.tokenText, "rbp")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "mov")
        }

        @Test
        fun testTokenizeEmptyLines() {
            // Arrange
            val lexer = AsmLexer()
            val code = "mov eax, 5\n\npop rbp"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)

            lexer.advance()
            lexer.advance()
            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
        }
    }

    class Operands {

        @Test
        fun testTokenizeComplexOperands() {
            // Arrange
            val lexer = AsmLexer()
            val code = "mov qword ptr [rsp+8H], rax"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "mov")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPERAND)
            assertTrue(lexer.tokenText.contains("qword ptr"))
            assertTrue(lexer.tokenText.contains("[rsp+8H]"))
        }

        @Test
        fun testTokenizeMemoryAddress() {
            // Arrange
            val lexer = AsmLexer()
            val code = "lea rax, [rbp-10H]"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)
            assertEquals(lexer.tokenText, "lea")

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPERAND)
            assertTrue(lexer.tokenText.contains("[rbp-10H]"))
        }

        @Test
        fun testTokenizeHexNumbers() {
            // Arrange
            val lexer = AsmLexer()
            val code = "mov eax, 0x12345678"

            // Act
            lexer.start(code, 0, code.length, 0)

            // Assert
            assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.WHITESPACE)

            lexer.advance()
            assertEquals(lexer.tokenType, AsmTokenTypes.OPERAND)
            assertTrue(lexer.tokenText.contains("0x12345678"))
        }
    }

    class ArchitectureSpecific {

        @Test
        fun testTokenizeX64Instructions() {
            // Arrange
            val instructions = listOf("movabs", "pushq", "popq", "callq", "retq", "jmpq", "leaq")

            for (instruction in instructions) {
                // Arrange
                val lexer = AsmLexer()
                val code = "$instruction rax, rbx"

                // Act
                lexer.start(code, 0, code.length, 0)

                // Assert
                assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE, "Failed for instruction: $instruction")
                assertEquals(lexer.tokenText, instruction)
            }
        }

        @Test
        fun testTokenizeARM64Instructions() {
            // Arrange
            val instructions = listOf("ldr", "str", "ldp", "stp", "adrp", "bl", "ret")

            for (instruction in instructions) {
                // Arrange
                val lexer = AsmLexer()
                val code = "$instruction x0, x1"

                // Act
                lexer.start(code, 0, code.length, 0)

                // Assert
                assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE, "Failed for instruction: $instruction")
                assertEquals(lexer.tokenText, instruction)
            }
        }

        @Test
        fun testTokenizeConditionalJumps() {
            // Arrange
            val jumps = listOf("je", "jne", "jz", "jnz", "jg", "jge", "jl", "jle")

            for (jump in jumps) {
                // Arrange
                val lexer = AsmLexer()
                val code = "$jump label_001"

                // Act
                lexer.start(code, 0, code.length, 0)

                // Assert
                assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE, "Failed for jump: $jump")
                assertEquals(lexer.tokenText, jump)
            }
        }

        @Test
        fun testTokenizeFloatingPointInstructions() {
            // Arrange
            val instructions = listOf("fadd", "fsub", "fmul", "fdiv", "fsqrt", "fcmp")

            for (instruction in instructions) {
                // Arrange
                val lexer = AsmLexer()
                val code = "$instruction xmm0, xmm1"

                // Act
                lexer.start(code, 0, code.length, 0)

                // Assert
                assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE, "Failed for instruction: $instruction")
                assertEquals(lexer.tokenText, instruction)
            }
        }

        @Test
        fun testTokenizeSIMDInstructions() {
            // Arrange
            val instructions = listOf("movaps", "movups", "movdqa", "movdqu", "paddd", "pxor")

            for (instruction in instructions) {
                // Arrange
                val lexer = AsmLexer()
                val code = "$instruction xmm0, xmm1"

                // Act
                lexer.start(code, 0, code.length, 0)

                // Assert
                assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE, "Failed for instruction: $instruction")
                assertEquals(lexer.tokenText, instruction)
            }
        }
    }

    class OpcodeSetValidation {

        @Test
        fun testOpcodeSet() {
            // Arrange
            val commonOpcodes = listOf("mov", "add", "sub", "jmp", "call", "ret", "push", "pop")
            val nonOpcodes = listOf("rax", "eax", "invalid")

            // Act & Assert
            for (opcode in commonOpcodes) {
                assertTrue(AsmSyntaxHighlighter.OPCODES.contains(opcode), "Expected '$opcode' to be in OPCODES set")
            }

            for (nonOpcode in nonOpcodes) {
                assertFalse(AsmSyntaxHighlighter.OPCODES.contains(nonOpcode), "Expected '$nonOpcode' NOT to be in OPCODES set")
            }
        }

        @Test
        fun testCaseInsensitiveOpcodes() {
            // Arrange
            val lexer = AsmLexer()
            val testCases = listOf("MOV eax, 5", "MoV eax, 5", "mov eax, 5")

            // Act & Assert
            for (code in testCases) {
                lexer.start(code, 0, code.length, 0)
                assertEquals(lexer.tokenType, AsmTokenTypes.OPCODE, "Failed for: $code")
            }
        }
    }

    class HighlighterApi {

        @Test
        fun testGetTokenHighlights() {
            // Arrange
            val highlighter = AsmSyntaxHighlighter()

            // Act
            val opcodeHighlights = highlighter.getTokenHighlights(AsmTokenTypes.OPCODE)
            val labelHighlights = highlighter.getTokenHighlights(AsmTokenTypes.LABEL)
            val operandHighlights = highlighter.getTokenHighlights(AsmTokenTypes.OPERAND)
            val commentHighlights = highlighter.getTokenHighlights(AsmTokenTypes.COMMENT)
            val unknownHighlights = highlighter.getTokenHighlights(null)

            // Assert
            assertEquals(opcodeHighlights.size, 1)
            assertEquals(opcodeHighlights[0], AsmSyntaxHighlighter.OPCODE)

            assertEquals(labelHighlights.size, 1)
            assertEquals(labelHighlights[0], AsmSyntaxHighlighter.LABEL)

            assertEquals(operandHighlights.size, 1)
            assertEquals(operandHighlights[0], AsmSyntaxHighlighter.OPERAND)

            assertEquals(commentHighlights.size, 1)
            assertEquals(commentHighlights[0], AsmSyntaxHighlighter.COMMENT)

            assertEquals(unknownHighlights.size, 0)
        }
    }

    class IntegrationScenarios {

        @Test
        fun testRealJitOutput() {
            // Arrange
            val lexer = AsmLexer()
            val code = """
                ; Assembly listing for method Program:Main()
                G_M000_IG01:
                       push     rbp
                       mov      rbp, rsp
                G_M000_IG02:
                       mov      ecx, 0x2A ; 42
                       call     [System.Console:WriteLine(int)]
                G_M000_IG03:
                       pop      rbp
                       ret
            """.trimIndent()

            // Act
            lexer.start(code, 0, code.length, 0)

            var opcodeCount = 0
            var labelCount = 0
            var commentCount = 0

            while (lexer.tokenType != null) {
                when (lexer.tokenType) {
                    AsmTokenTypes.OPCODE -> opcodeCount++
                    AsmTokenTypes.LABEL -> labelCount++
                    AsmTokenTypes.COMMENT -> commentCount++
                }
                lexer.advance()
            }

            // Assert
            assertTrue(opcodeCount >= 4, "Expected at least 4 opcodes (push, mov, call, pop, ret), got $opcodeCount")
            assertTrue(labelCount >= 2, "Expected at least 2 labels, got $labelCount")
            assertTrue(commentCount >= 1, "Expected at least 1 comment, got $commentCount")
        }
    }
}
