# Refactoring Log: multiCAFE

## Summary
Identified as a source project for the extraction of common routines to the new `fmpTools` library.

## Operations Realized

### 1. Code Extraction Source
*   Contributed to the extraction of the `tools` package to `fmpTools`.
*   Common logic for `Experiment` and `SequenceCamData` was analyzed to create shared abstractions in `fmpTools`.

### 2. Future Steps
*   Update `pom.xml` to depend on `fmpTools`.
*   Remove extracted classes from `plugins.fmp.multicafe.tools`.
*   Refactor `Experiment` and `SequenceCamData` to use the new shared managers (`ImageLoader`, `TimeManager`) from `fmpTools`.

