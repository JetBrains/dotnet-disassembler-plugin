# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## 0.1.1

### Fixed
- Fixed statistics collection
- Updated plugin description

## 0.1.0

### Added
- View disassembly for C# methods in .NET 6.0+ projects
- Multiple codegen modes: standard JIT, ReadyToRun (crossgen2), NativeAOT (ilc)
- Snapshot and diff view to compare assembly output after code changes
- Configurable compiler settings: tiered compilation, PGO, diff-friendly output, build options
- Tool window for viewing disassembly
- Context action for quick access from editor
- Syntax highlighting for x86/x64 and ARM64 assembly
