# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## 0.1.0
### Added
- Initial experimental release
- Display JIT-compiled assembly for C# methods in .NET 6.0+ projects
- Configurable JIT compilation options:
  - Tiered compilation toggle
  - PGO (Profile-Guided Optimization)
  - Diffable output mode
  - Multiple codegen modes: standard JIT, ReadyToRun (crossgen2), NativeAOT (ilc)
  - Unloadable context support
- Build configuration options (dotnet build/publish, --no-restore flag)
- Assembly code syntax highlighting
- Snapshot and diff view to compare assembly after code changes
- Context action for quick access from editor
- Tool window with real-time assembly display

### Notes
- Based on the [Disasmo](https://github.com/EgorBo/Disasmo) project by Egor Bogatov
- Only works with .NET Core/6+ projects (not .NET Framework)
