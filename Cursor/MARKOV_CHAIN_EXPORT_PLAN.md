# Replace TRANSITIONS with MARKOV_CHAIN Export

## Overview
Replace the existing TRANSITIONS export with a MARKOV_CHAIN export that:
1. Uses NBGULPS data (0/1 per interval) from L and R capillaries
2. Computes 4 states: Ls (L=1, R=0), Rs (L=0, R=1), LR (L=1, R=1), N (L=0, R=0)
3. Calculates 16 transitions between states from interval t-1 to t
4. Exports 20 measures per cage: 16 transition counts + 4 state counts (Ls, Rs, LR, N)

## Implementation Steps

### 1. Update EnumXLSExport
- **File**: `src/main/java/plugins/fmp/multicafe/tools/toExcel/EnumXLSExport.java`
- Replace `TRANSITIONS` enum value with `MARKOV_CHAIN("markov_chain", "n observ", ...)`
- Keep same unit and measure type

### 2. Update XLSExportOptions
- **File**: `src/main/java/plugins/fmp/multicafe/tools/toExcel/XLSExportOptions.java`
- Rename `transitions` boolean field to `markovChain`

### 3. Update UI Component
- **File**: `src/main/java/plugins/fmp/multicafe/dlg/excel/Gulps.java`
- Update checkbox label from "transitions L-R-LR-N" to "markov chain"
- Update variable name from `transitionsCheckBox` to `markovChainCheckBox`

### 4. Update Export Classes
- **File**: `src/main/java/plugins/fmp/multicafe/tools/toExcel/XLSExportGulpsResults.java`
- Replace `options.transitions` with `options.markovChain`
- Replace `EnumXLSExport.TRANSITIONS` with `EnumXLSExport.MARKOV_CHAIN`

- **File**: `src/main/java/plugins/fmp/multicafe/tools/toExcel/XLSExportCapillariesResults.java`
- Update switch statement to use `MARKOV_CHAIN` instead of `TRANSITIONS`

### 5. Implement Markov Chain Computation
- **File**: `src/main/java/plugins/fmp/multicafe/tools/toExcel/XLSResultsFromCapillaries.java`
- Add new method `buildMarkovChain(XLSExportOptions options)` that:
  1. Groups capillaries by cage ID
  2. For each cage, finds L and R capillaries
  3. Gets NBGULPS data (0/1 arrays) for L and R
  4. Computes states array: for each interval, determine Ls/Rs/LR/N
  5. Computes 16 transition counts between states
  6. Creates 20 XLSResults rows per cage (one per measure)
  7. Each row name: use acronym directly (e.g., "Ls", "Rs", "LR", "N", "Ls-Ls", "Rs-Ls", etc.)

- Update `buildDataForPass2()` to call `buildMarkovChain()` for `MARKOV_CHAIN` export type

### 6. Handle Export Structure
- The export should create rows per cage (not per capillary)
- Each cage gets 20 rows (16 transitions + 4 states)
- Row naming: Use the acronym directly as the capillary name
  - States: "Ls", "Rs", "LR", "N"
  - Transitions: "Ls-Ls", "Rs-Ls", "LR-Ls", "N-Ls", "Ls-Rs", "Rs-Rs", "LR-Rs", "N-Rs", "Ls-LR", "Rs-LR", "LR-LR", "N-LR", "Ls-N", "Rs-N", "LR-N", "N-N"

### 7. Update UI Binding
- **File**: `src/main/java/plugins/fmp/multicafe/dlg/excel/MCExcel_.java` (if it exists)
- Update any references from `transitions` to `markovChain` when reading checkbox state

## Key Implementation Details

### State Computation Logic
```java
// For each time interval t:
int gulpL = (L_capillary_NBGULPS[t] > 0) ? 1 : 0;
int gulpR = (R_capillary_NBGULPS[t] > 0) ? 1 : 0;
int state;
if (gulpL == 1 && gulpR == 0) state = Ls;
else if (gulpL == 0 && gulpR == 1) state = Rs;
else if (gulpL == 1 && gulpR == 1) state = LR;
else state = N; // both 0
```

### Transition Counting
```java
// For each interval t (starting from t=1):
int prevState = states[t-1];
int currState = states[t];
transitions[prevState][currState]++;
```

### Export Format
- Each cage produces 20 rows
- Each row has the same time dimension as the original data
- Values are counts (for transitions) or state indicators (for states: Ls, Rs, LR, N)
- Row names are the acronyms directly (e.g., "Ls", "Rs-Ls", etc.) - written in the capillary column

## Implementation Todos

1. Replace TRANSITIONS with MARKOV_CHAIN in EnumXLSExport.java
2. Rename transitions to markovChain in XLSExportOptions.java
3. Update checkbox label and variable name in Gulps.java
4. Update XLSExportGulpsResults.java and XLSExportCapillariesResults.java to use MARKOV_CHAIN
5. Implement buildMarkovChain() method in XLSResultsFromCapillaries.java with state computation and transition counting
6. Update buildDataForPass2() to call buildMarkovChain() for MARKOV_CHAIN export type
7. Update UI binding code (MCExcel_.java) to read markovChain checkbox instead of transitions

