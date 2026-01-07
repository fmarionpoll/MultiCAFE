# Persistence Refactoring Testing Plan

## Overview

This document outlines the testing strategy for verifying that the persistence refactoring (restricting legacy read operations to private methods within persistence classes) works correctly. The refactoring ensures that legacy read operations are only accessible internally within persistence classes, while the public API only exposes new format methods with internal fallback logic.

## Testing Objectives

1. Verify new format persistence (v2_ files) works correctly for save and load operations
2. Verify legacy format fallback logic works when legacy files are present
3. Verify data integrity across format transitions
4. Verify error handling and edge cases
5. Verify backward compatibility with existing data

## Phase 1: New Format Testing

### Objective
Verify that new format persistence works correctly and generates proper persistent data.

### Test Cases

#### TC-1.1: Clean New Format Save
- **Setup**: Start with fresh experiment data (clear all analysis data)
- **Steps**:
  1. Run analysis procedures to generate data
  2. Save experiment data
  3. Verify files are created in correct locations:
     - Descriptions: `results/v2_CapillariesArray.csv`
     - Descriptions: `results/v2_CagesArray.csv`
     - Descriptions: `results/v2_SpotsArray.csv`
     - Measures: `results/bin60/v2_CapillariesArrayMeasures.csv`
     - Measures: `results/bin60/v2_CagesArrayMeasures.csv`
     - Measures: `results/bin60/v2_SpotsArrayMeasures.csv`
     - Experiment: `results/v2_Experiment.xml`
  4. Verify file contents match expected v2 format structure
  5. Verify no legacy format files are created (no files without v2_ prefix)

#### TC-1.2: New Format Load
- **Setup**: Experiment with v2_ format files (from TC-1.1)
- **Steps**:
  1. Close and reload experiment
  2. Verify all data loads correctly from v2_ files
  3. Verify data integrity:
     - Capillaries descriptions and measures
     - Cages descriptions and measures
     - Spots descriptions and measures
     - Experiment properties
  4. Verify no fallback to legacy format occurs

#### TC-1.3: Full Cycle Test
- **Setup**: Fresh experiment
- **Steps**:
  1. Load experiment
  2. Run analysis procedures
  3. Save experiment (new format)
  4. Close experiment
  5. Reload experiment
  6. Verify all data is intact and matches original

#### TC-1.4: Format Verification
- **Setup**: Experiment with v2_ format files
- **Steps**:
  1. Inspect CSV files to verify format structure:
     - Verify DESCRIPTION sections
     - Verify CAPILLARIES/CAGE/SPOTS sections
     - Verify section headers and separators
  2. Verify XML files use correct schema
  3. Verify no legacy format elements are present

## Phase 2: Legacy Format Fallback Testing

### Objective
Verify that legacy format files can be read and that fallback logic works correctly.

### Test Cases

#### TC-2.1: Legacy Format Detection
- **Setup**: Replace v2_ files with legacy format files:
  - `CapillariesArray.csv` (legacy CSV)
  - `MCcapillaries.xml` (legacy XML)
  - `CagesArray.csv` (legacy CSV)
  - `MCdrosotrack.xml` (legacy XML)
  - `SpotsArray.csv` (legacy CSV)
  - `MCexperiment.xml` (legacy XML)
- **Steps**:
  1. Load experiment
  2. Verify legacy files are detected and loaded
  3. Verify fallback logic executes (check logs if available)
  4. Verify data loads correctly from legacy format
  5. Verify data integrity matches expected values

#### TC-2.2: Multiple Legacy Formats
- **Setup**: Test different legacy format versions:
  - v0 format (oldest)
  - v1 format
  - v2 legacy format (pre-v2_)
  - Old CSV formats
  - Old XML formats
- **Steps**:
  1. For each legacy format version:
     - Replace files with specific legacy version
     - Load experiment
     - Verify loads successfully
     - Verify data integrity

#### TC-2.3: Fallback Order Verification
- **Setup**: Directory with both v2_ and legacy files
- **Steps**:
  1. Verify v2_ format is tried first
  2. Verify legacy format is only used if v2_ not found
  3. Verify correct file is loaded (should be v2_ if both exist)

#### TC-2.4: Mixed Format Scenarios
- **Setup**: Mixed data - some in new format, some in legacy
  - Example: Capillaries in v2_ format, Cages in legacy format
- **Steps**:
  1. Load experiment
  2. Verify each component loads from correct format
  3. Verify all data loads successfully
  4. Verify data integrity

## Phase 3: Migration Testing

### Objective
Verify that data can be migrated from legacy to new format.

### Test Cases

#### TC-3.1: Legacy Load and Save
- **Setup**: Experiment with only legacy files
- **Steps**:
  1. Load experiment (from legacy)
  2. Verify data loads correctly
  3. Save experiment
  4. Verify v2_ format files are created
  5. Verify legacy files remain (not overwritten)
  6. Reload experiment
  7. Verify loads from v2_ format (new files take precedence)

#### TC-3.2: Format Upgrade Verification
- **Setup**: Experiment with legacy files
- **Steps**:
  1. Load from legacy
  2. Save (creates v2_ files)
  3. Delete legacy files
  4. Reload experiment
  5. Verify loads successfully from v2_ format only

#### TC-3.3: Data Consistency After Migration
- **Setup**: Experiment with legacy files
- **Steps**:
  1. Load from legacy and capture data values
  2. Save to new format
  3. Reload from new format
  4. Verify data values match original (no data loss)

## Phase 4: Error Handling Testing

### Objective
Verify that error conditions are handled gracefully.

### Test Cases

#### TC-4.1: Missing Files
- **Setup**: Experiment directory with no persistence files
- **Steps**:
  1. Attempt to load experiment
  2. Verify fails gracefully (returns false, no exceptions)
  3. Verify appropriate error messages/logging

#### TC-4.2: Corrupted Files
- **Setup**: Experiment directory with corrupted/invalid files
- **Steps**:
  1. Attempt to load experiment
  2. Verify handles errors gracefully
  3. Verify fallback to other formats if applicable
  4. Verify appropriate error messages/logging

#### TC-4.3: Empty Directories
- **Setup**: Empty experiment directory
- **Steps**:
  1. Attempt to load experiment
  2. Verify handles empty directory correctly
  3. Verify can save new data to empty directory

#### TC-4.4: Invalid File Formats
- **Setup**: Files with wrong format/structure
- **Steps**:
  1. Attempt to load experiment
  2. Verify handles format errors gracefully
  3. Verify fallback logic works if multiple formats present

## Phase 5: Integration Testing

### Objective
Verify complete workflows and integration with analysis procedures.

### Test Cases

#### TC-5.1: Complete Workflow - New Format
- **Setup**: Fresh experiment
- **Steps**:
  1. Load experiment
  2. Run capillary analysis
  3. Run cage analysis
  4. Run spot analysis
  5. Save experiment
  6. Close experiment
  7. Reload experiment
  8. Verify all analysis results are preserved
  9. Verify can continue analysis from saved state

#### TC-5.2: Complete Workflow - Legacy Format
- **Setup**: Experiment with legacy files
- **Steps**:
  1. Load experiment (from legacy)
  2. Run analysis procedures
  3. Save experiment (creates v2_ files)
  4. Verify analysis results are saved correctly
  5. Reload and verify analysis results

#### TC-5.3: Multiple Experiments
- **Setup**: Multiple experiment directories
- **Steps**:
  1. Load experiment 1 (new format)
  2. Load experiment 2 (legacy format)
  3. Verify both load correctly
  4. Verify no interference between experiments

#### TC-5.4: Chain Experiments (if applicable)
- **Setup**: Chained experiments
- **Steps**:
  1. Load chained experiments
  2. Verify persistence works for all experiments in chain
  3. Verify chain relationships are preserved

## Phase 6: Backward Compatibility Testing

### Objective
Verify that existing workflows continue to work with refactored code.

### Test Cases

#### TC-6.1: Existing Data Compatibility
- **Setup**: Real-world experiment directories with existing data
- **Steps**:
  1. Load experiments with various formats
  2. Verify all load successfully
  3. Verify data integrity
  4. Verify can save without errors

#### TC-6.2: API Compatibility
- **Steps**:
  1. Verify public API methods still work:
     - `loadCapillariesArrayDescription()`
     - `load_CapillariesArrayMeasures()`
     - `loadCagesArrayDescription()`
     - `loadCagesArrayMeasures()`
     - `loadSpotsArrayDescription()`
     - `loadSpotsArrayMeasures()`
  2. Verify legacy methods are not accessible externally
  3. Verify domain class methods use new format APIs

## Test Data Preparation

### Required Test Datasets

1. **Clean Dataset**: Fresh experiment with no persistence files
2. **New Format Dataset**: Experiment with v2_ format files only
3. **Legacy Format Datasets**: Experiments with various legacy formats:
   - v0 XML format
   - v1 XML format
   - v2 legacy XML format
   - Legacy CSV formats
4. **Mixed Format Dataset**: Experiment with mix of new and legacy files
5. **Corrupted Dataset**: Experiment with invalid/corrupted files

### Test Data Backup

- Always backup original data before testing
- Use separate test directories to avoid modifying production data
- Keep reference copies of all test datasets

## Success Criteria

### New Format Testing
- ✅ All v2_ format files are created correctly
- ✅ Data saves to correct locations
- ✅ Data loads correctly from v2_ files
- ✅ No legacy format files are created during save

### Legacy Format Testing
- ✅ Legacy files are detected and loaded
- ✅ Fallback logic executes in correct order (v2_ first, then legacy)
- ✅ Data integrity is maintained when loading from legacy
- ✅ All legacy format versions are supported

### Migration Testing
- ✅ Legacy data can be loaded and saved to new format
- ✅ No data loss during migration
- ✅ Legacy files are preserved (not overwritten)
- ✅ After migration, new format is used

### Error Handling
- ✅ Missing files handled gracefully
- ✅ Corrupted files handled gracefully
- ✅ Appropriate error messages/logging
- ✅ No crashes or unhandled exceptions

### Integration Testing
- ✅ Complete workflows function correctly
- ✅ Analysis procedures work with new persistence
- ✅ Multiple experiments can be handled
- ✅ Existing workflows continue to work

## Execution Order

1. **Phase 1**: New Format Testing (validate new format works)
2. **Phase 2**: Legacy Format Fallback Testing (validate backward compatibility)
3. **Phase 3**: Migration Testing (validate format transitions)
4. **Phase 4**: Error Handling Testing (validate robustness)
5. **Phase 5**: Integration Testing (validate complete workflows)
6. **Phase 6**: Backward Compatibility Testing (validate existing data)

## Notes

- Test in a controlled environment with backup data
- Document any issues found during testing
- Verify log messages indicate correct format usage
- Pay special attention to file locations and naming conventions
- Test with realistic data volumes (not just minimal test data)

