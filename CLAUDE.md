# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JIT Assembly Viewer is a JetBrains Rider plugin that displays JIT-compiled assembly code for C# methods. It uses a dual-stack architecture: Kotlin frontend for Rider IDE integration and C# backend for .NET analysis via the RD (Remote Development) protocol.

## Build Commands

```bash
# Build the plugin
./gradlew buildPlugin

# Compile .NET components only
./gradlew compileDotNet

# Run all Kotlin/JVM tests
./gradlew check

# Run .NET tests (Windows only)
./gradlew testDotNet

# Run Rider with the plugin for testing
./gradlew runIde

# Regenerate protocol models after changing protocol/src/main/kotlin/model/rider/AsmViewerModel.kt
./gradlew rdgen

# Verify plugin structure
./gradlew verifyPlugin
```

## Architecture

### Dual-Stack Design

- **Frontend (Kotlin)**: `src/rider/main/kotlin/` - Rider UI, tool windows, syntax highlighting
- **Backend (C#)**: `src/dotnet/ReSharperPlugin.JitAsmViewer/` - JIT disassembly, project analysis
- **Protocol**: `protocol/src/main/kotlin/model/rider/AsmViewerModel.kt` - RD protocol definitions

### Key Components

**Frontend:**
- `AsmViewerHost.kt` - Main plugin service, manages lifecycle and protocol connection
- `AsmViewerHostUi.kt` - UI layer for tool window management
- `AsmContentPanel.kt` - Assembly code display panel
- `AsmSyntaxHighlighter.kt` - x86/ARM64 syntax highlighting

**Backend:**
- `AsmViewerHost.cs` - Solution-level component managing backend state
- `JitDisasm/JitCodegenProvider.cs` - Core JIT disassembly logic
- `JitDisasm/LoaderAppManager.cs` - Manages temporary loader applications
- `JitDisasmAdapters/` - Factory pattern adapters for JIT configuration

**Protocol:**
- `AsmViewerModel.kt` defines the RD protocol model
- Generated files: `AsmViewerModel.Generated.kt` (Kotlin) and `AsmViewerModel.Generated.cs` (C#)

### Communication Pattern

Frontend and backend communicate via RD protocol with reactive properties for visibility, loading state, configuration options, and assembly content.

## Testing

- **Kotlin tests**: `src/test/kotlin/` - Run with `./gradlew check`
- **.NET tests**: `src/dotnet/ReSharperPlugin.JitAsmViewer.Tests/` - Run with `./gradlew testDotNet` (Windows)
- **Test data**: `src/test/resources/testData/`

## Requirements

- Java 21
- .NET SDK 8.x
- Gradle 8.8 (wrapper included)
- Target Rider version: 2025.3
