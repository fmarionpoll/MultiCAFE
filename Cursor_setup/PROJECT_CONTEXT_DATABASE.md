# MultiCAFE — Project Context Database

This document is the authoritative reference for the MultiCAFE codebase: architecture, conventions, and key decisions. It is intended for both humans and tools (including Cursor AI) to keep changes consistent.

## Purpose
- MultiCAFE is an ICY plugin/project that analyzes experiments composed of cages, spots, and capillaries, producing measurements and visualizations (charts, tables, exports).
- Core goals: robust data processing, reproducible results, and maintainable UI/tooling for analysis workflows.

## Repository map (high-level)
- `pom.xml`: Maven build.
- `src/main/java/plugins/fmp/multicafe/`:
  - `MultiCAFE.java`: plugin entry point.
  - `fmp_experiment/`: experiment model (Experiment, Cage, Spot, Capillary, persistence).
  - `fmp_tools/`: utilities and tools (charts, results, export, ROI helpers, etc.).
  - `dlg/`: UI dialogs/frames/panels.
- `cursor/`: internal design notes, refactor sessions, experiments (documentation only; not runtime code).

## Domain model (core objects)
### Experiment
- Represents one experiment dataset (images + metadata + derived measurements).
- Provides access to cages and camera timing information.
  - Timing used in charting: `exp.getSeqCamData().getTimeManager().getCamImagesTime_Minutes()`.

### Cage
- Contains/relates to:
  - **Spots**: via `cage.spotsArray` (`SpotsArray` → list of `Spot`).
  - **Capillaries**: via `cage.getCapillaries()` (`Capillaries` → list of `Capillary`).
  - **Properties**: `CageProperties` (ID, position, number of flies, etc.).

### Spot
- Spot measurements are represented by `SpotMeasure` and selected by `EnumResults`.
- Current chart usage typically uses:
  - `spot.getMeasurements(resultType)` → `SpotMeasure`
  - `SpotMeasure.getValueAt(i)` and `SpotMeasure.getCount()`

### Capillary
- Capillary measurements are represented by `CapillaryMeasure` (levels/gulps/derived) and selected by `EnumResults`.
- Current chart usage typically uses:
  - `cap.getMeasurements(resultType)` → `CapillaryMeasure`
  - `CapillaryMeasure.getValueAt(i)` and `CapillaryMeasure.getNPoints()`
- Capillary charting may apply scaling (e.g., pixels→volume conversion) based on `cap.getVolume()`/`cap.getPixels()`.

## Results & charting pipeline (current)
### Data production
- Measurements are stored on Spots/Capillaries and computed elsewhere (e.g., computations in `fmp_experiment/cages/*Computation.java`, services, etc.).
### Charting
- The charting stack uses JFreeChart (`XYSeries`, `XYSeriesCollection`, `XYPlot`).
- Historically, `ChartCageBuild` performed:
  - Spot-series building from `cage.spotsArray`
  - Capillary-series building from `cage.getCapillaries()` including LR variants (Sum/PI)
  - Plot renderer styling and background/grid behavior based on `nflies`

## Conventions
### General
- Prefer small, single-purpose classes over “god utility” classes.
- Avoid redundant comments: document intent/constraints, not obvious code.
- Prefer immutable/explicit data transfer objects for metadata; avoid encoding structured metadata in ad-hoc strings.

### Charting conventions
- **Separate responsibilities**:
  - **Series building** (domain data → `XYSeriesCollection`)
  - **Plot building/styling** (`XYPlot` renderer setup, colors/strokes, background/grid)
- **Do not parse series metadata from string descriptions** when avoidable.
  - If legacy APIs require `XYSeries.setDescription()`, centralize encode/decode in one place and keep the format stable.
- Keep output behavior stable unless a refactor explicitly intends to change chart appearance or values.

## Known technical debt (selected)
- Some chart styling depends on parsing `XYSeries.getDescription()` with colon-delimited tokens (brittle).
- Background/grid logic is duplicated in places (e.g., chart builder vs UI panel reacting to fly count changes).
- Some older APIs blur the boundary between “results computation” and “chart presentation”.

## Decision log
### 2025-12 — Split cage chart builders by data source
Problem:
- `ChartCageBuild` mixes Spot and Capillary data generation, LR logic, and renderer styling.

Decision:
- Introduce dedicated builders for Spot-series and Capillary-series.
- Introduce a dedicated plot/styling factory/helper.
- Keep a thin facade for backward compatibility during migration.

Rationale:
- Spot vs Capillary have different measurement models and special cases (e.g., LR Sum/PI).
- A domain-level shared base class would be artificial; instead use a chart-layer interface for uniform call sites.

Non-goals:
- No change in computed values or chart appearance unless explicitly requested.


