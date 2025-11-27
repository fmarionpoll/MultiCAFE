# MultiCAFE Code Quality Recommendations

## Executive Summary

This document provides prioritized recommendations for improving the architecture, design, and error handling in the MultiCAFE plugin. These recommendations are based on a comprehensive code review of the codebase.

---

## High Priority Recommendations

### 1. Improve Error Handling

**Current Issues:**
- 147 instances of `System.out.println`, `printStackTrace`, `TODO`, `FIXME`
- Generic exception catching (`catch (Exception e)`)
- Silent failures with no user notification
- Inconsistent error reporting

**Recommendations:**

#### 1.1 Implement Proper Exception Hierarchy
```java
// Create custom exceptions
public class ExperimentLoadException extends Exception {
    public ExperimentLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class ExperimentSaveException extends Exception {
    public ExperimentSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### 1.2 Replace Generic Exception Catching
**Before:**
```java
} catch (Exception e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
}
```

**After:**
```java
} catch (FormatException e) {
    logger.error("Failed to save kymograph: " + filename, e);
    showErrorToUser("Failed to save kymograph: " + e.getMessage());
    return false;
} catch (IOException e) {
    logger.error("IO error saving kymograph: " + filename, e);
    showErrorToUser("IO error: " + e.getMessage());
    return false;
}
```

#### 1.3 Add User-Friendly Error Messages
- Show meaningful dialogs for user-facing errors
- Log technical details separately
- Provide actionable error messages

#### 1.4 Implement Centralized Logging
- Replace `System.out.println` with proper logging framework (SLF4J recommended)
- Use appropriate log levels: ERROR, WARN, INFO, DEBUG
- Structure logs for easy debugging

**Implementation Steps:**
1. Add SLF4J dependency to `pom.xml`
2. Create `Logger` utility class
3. Replace all `System.out.println` calls
4. Replace all `printStackTrace()` calls
5. Add user notification dialogs for critical errors

---

### 2. Encapsulate Public Fields

**Current Issues:**
- Public fields throughout codebase (e.g., `public SequenceKymos seqKymos = null;`)
- Direct field access from multiple classes
- No null safety guarantees
- Difficult to refactor

**Recommendations:**

#### 2.1 Convert Public Fields to Private with Accessors
**Before:**
```java
public class Experiment {
    public SequenceKymos seqKymos = null;
    public Capillaries capillaries = new Capillaries();
}
```

**After:**
```java
public class Experiment {
    private SequenceKymos seqKymos;
    private Capillaries capillaries = new Capillaries();
    
    public SequenceKymos getSeqKymos() {
        if (seqKymos == null) {
            seqKymos = new SequenceKymos();
        }
        return seqKymos;
    }
    
    public void setSeqKymos(SequenceKymos seqKymos) {
        this.seqKymos = seqKymos;
    }
    
    public Capillaries getCapillaries() {
        return capillaries;
    }
}
```

#### 2.2 Add Null Safety Checks
- Use `Objects.requireNonNull()` for required parameters
- Return `Optional<>` for nullable returns
- Validate at method boundaries

**Implementation Steps:**
1. Identify all public fields
2. Convert to private with getters/setters
3. Add null checks in getters
4. Update all call sites
5. Add validation in setters

---

### 3. Break Down Large Classes

**Current Issues:**
- `Experiment.java`: 1085+ lines (too many responsibilities)
- `Capillaries.java`: 677+ lines
- `Cages.java`: 700+ lines
- Hard to maintain, test, and understand

**Recommendations:**

#### 3.1 Split Experiment Class
Break `Experiment` into focused classes:

```java
// ExperimentData.java - Core data storage
public class ExperimentData {
    private String boxID;
    private String experiment;
    private String strain;
    // ... other fields
}

// ExperimentPersistence.java - Save/load operations
public class ExperimentPersistence {
    public boolean save(ExperimentData data, String directory);
    public ExperimentData load(String directory);
}

// ExperimentTimeManager.java - Time calculations
public class ExperimentTimeManager {
    public long calculateTimeIntervals(...);
    public long[] buildTimeArray(...);
}

// Experiment.java - Main coordinator (much smaller)
public class Experiment {
    private ExperimentData data;
    private ExperimentPersistence persistence;
    private ExperimentTimeManager timeManager;
    // Delegates to above classes
}
```

#### 3.2 Extract Responsibilities
- **File I/O**: Move to persistence classes
- **Time Calculations**: Move to time manager
- **Validation**: Move to validator classes
- **Business Logic**: Move to service classes

**Implementation Steps:**
1. Identify responsibilities in large classes
2. Create new focused classes
3. Move methods to appropriate classes
4. Update references
5. Test thoroughly

---

## Medium Priority Recommendations

### 4. Introduce Service Layer

**Current Issue:**
- Business logic mixed with data classes and UI
- Direct manipulation of `Experiment` from multiple places
- Difficult to test and maintain

**Recommendation:**

Create a service layer to encapsulate business operations:

```java
public class ExperimentService {
    private ExperimentRepository repository;
    private ExperimentValidator validator;
    
    public Experiment loadExperiment(String directory) throws ExperimentLoadException {
        validateDirectory(directory);
        Experiment exp = repository.load(directory);
        validateExperiment(exp);
        return exp;
    }
    
    public void saveExperiment(Experiment exp) throws ExperimentSaveException {
        validateExperiment(exp);
        repository.save(exp);
    }
    
    public void processExperiment(Experiment exp) {
        // Coordinate processing operations
    }
}
```

**Benefits:**
- Separation of concerns
- Easier testing
- Centralized business logic
- Better error handling

---

### 5. Add Logging Framework

**Current Issues:**
- Mix of `System.out.println` and `printStackTrace`
- No log levels
- No structured logging
- Difficult to debug production issues

**Recommendation:**

#### 5.1 Add SLF4J Dependency
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>1.7.36</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.36</version>
</dependency>
```

#### 5.2 Create Logger Utility
```java
public class Logger {
    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger("MultiCAFE");
    
    public static void error(String message, Throwable t) {
        logger.error(message, t);
    }
    
    public static void warn(String message) {
        logger.warn(message);
    }
    
    public static void info(String message) {
        logger.info(message);
    }
    
    public static void debug(String message) {
        logger.debug(message);
    }
}
```

#### 5.3 Replace All Print Statements
- Replace `System.out.println` with `Logger.info()`
- Replace `printStackTrace()` with `Logger.error()`
- Use appropriate log levels

---

### 6. Improve Null Safety

**Current Issues:**
- Inconsistent null checks
- Public nullable fields
- Potential NPEs throughout codebase

**Recommendations:**

#### 6.1 Use Optional for Nullable Returns
```java
// Before:
public SequenceKymos getSeqKymos() {
    return seqKymos;  // Can return null
}

// After:
public Optional<SequenceKymos> getSeqKymos() {
    return Optional.ofNullable(seqKymos);
}
```

#### 6.2 Add Validation at Boundaries
```java
public void setSeqKymos(SequenceKymos seqKymos) {
    this.seqKymos = Objects.requireNonNull(seqKymos, 
        "SequenceKymos cannot be null");
}
```

#### 6.3 Document Null Contracts
```java
/**
 * Gets the kymograph sequence.
 * @return the sequence, never null (creates new if needed)
 */
public SequenceKymos getSeqKymos() {
    if (seqKymos == null) {
        seqKymos = new SequenceKymos();
    }
    return seqKymos;
}
```

---

## Low Priority Recommendations

### 7. Add Unit Tests

**Current State:**
- No unit tests found in codebase
- No test infrastructure
- Difficult to verify correctness

**Recommendations:**

#### 7.1 Set Up Testing Infrastructure
```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

#### 7.2 Start with Critical Business Logic
- Test `Experiment` loading/saving
- Test time calculations
- Test data transformations
- Test error handling paths

#### 7.3 Example Test Structure
```java
public class ExperimentTest {
    @Test
    public void testLoadExperiment_Success() {
        // Test successful load
    }
    
    @Test
    public void testLoadExperiment_FileNotFound() {
        // Test error handling
    }
    
    @Test
    public void testLoadExperiment_InvalidFormat() {
        // Test validation
    }
}
```

---

### 8. Refactor Technical Debt

**Current Issues:**
- Method named `ugly_checkOffsetValues()` indicates technical debt
- Dead/commented code
- 147 TODO/FIXME comments

**Recommendations:**

#### 8.1 Rename Poorly Named Methods
```java
// Before:
private void ugly_checkOffsetValues() {
    // ...
}

// After:
/**
 * Validates and normalizes time offset values.
 * Ensures all time values are non-negative.
 */
private void normalizeTimeOffsets() {
    camImageFirst_ms = Math.max(0, camImageFirst_ms);
    camImageLast_ms = Math.max(0, camImageLast_ms);
    kymoFirst_ms = Math.max(0, kymoFirst_ms);
    kymoLast_ms = Math.max(0, kymoLast_ms);
    kymoBin_ms = (kymoBin_ms < 0) ? DEFAULT_KYMO_BIN_MS : kymoBin_ms;
}
```

#### 8.2 Remove Dead Code
- Delete commented-out code
- Remove unused methods
- Clean up unused imports

#### 8.3 Address TODO Items
- Prioritize TODO items
- Either implement or remove
- Document why items are deferred

---

### 9. Improve Documentation

**Current Issues:**
- Minimal JavaDoc comments
- Complex algorithms lack explanation
- No architectural documentation

**Recommendations:**

#### 9.1 Add JavaDoc to Public APIs
```java
/**
 * Loads an experiment from the specified directory.
 * 
 * @param directory the experiment directory path
 * @return the loaded experiment
 * @throws ExperimentLoadException if the experiment cannot be loaded
 *         (file not found, invalid format, etc.)
 */
public Experiment loadExperiment(String directory) throws ExperimentLoadException {
    // ...
}
```

#### 9.2 Document Complex Algorithms
- Explain kymograph generation algorithm
- Document level detection logic
- Explain fly tracking algorithms

#### 9.3 Create Architectural Diagrams
- Package structure diagram
- Class relationship diagrams
- Data flow diagrams

---

## Implementation Priority

### Phase 1 (Immediate - 1-2 months)
1. ✅ Improve error handling (critical for scientific software)
2. ✅ Encapsulate public fields (prevents bugs)
3. ✅ Add logging framework (essential for debugging)

### Phase 2 (Short-term - 3-4 months)
4. ✅ Break down large classes (improves maintainability)
5. ✅ Introduce service layer (improves architecture)
6. ✅ Improve null safety (prevents runtime errors)

### Phase 3 (Long-term - 6+ months)
7. ✅ Add unit tests (improves confidence)
8. ✅ Refactor technical debt (improves code quality)
9. ✅ Improve documentation (improves maintainability)

---

## Quick Wins

These can be implemented quickly with high impact:

1. **Replace printStackTrace with logging** (1-2 days)
2. **Add null checks in critical paths** (2-3 days)
3. **Rename poorly named methods** (1 day)
4. **Add JavaDoc to public methods** (ongoing)
5. **Remove dead/commented code** (1 day)

---

## Success Metrics

Track improvement with these metrics:

1. **Error Handling**: 
   - Zero silent failures
   - All errors logged appropriately
   - User-friendly error messages

2. **Code Quality**:
   - No classes > 500 lines
   - All public fields encapsulated
   - Zero TODO/FIXME in production code

3. **Testing**:
   - >80% code coverage for business logic
   - All error paths tested
   - Integration tests for critical workflows

4. **Documentation**:
   - 100% JavaDoc coverage for public APIs
   - Architectural documentation complete
   - User guide updated

---

## Notes

- These recommendations are based on a comprehensive code review
- Implementation should be done incrementally
- Prioritize based on impact and effort
- Consider user impact when making changes
- Maintain backward compatibility where possible

