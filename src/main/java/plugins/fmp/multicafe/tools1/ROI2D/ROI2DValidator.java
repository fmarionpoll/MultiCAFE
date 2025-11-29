package plugins.fmp.multicafe.tools1.ROI2D;

import java.awt.geom.Point2D;
import java.util.List;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;

/**
 * Validation utility class for ROI2D operations.
 * Provides comprehensive input validation and parameter checking methods.
 * 
 * @author MultiSPOTS96 Team
 * @version 2.0
 */
public final class ROI2DValidator {
    
    // Prevent instantiation
    private ROI2DValidator() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Validates that an ROI is not null.
     * 
     * @param roi The ROI to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the ROI is null
     */
    public static void validateNotNull(ROI roi, String parameterName) throws ROI2DValidationException {
        if (roi == null) {
            throw new ROI2DValidationException(parameterName, null, Constants.ErrorMessages.NULL_ROI);
        }
    }
    
    /**
     * Validates that a sequence is not null.
     * 
     * @param sequence The sequence to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the sequence is null
     */
    public static void validateNotNull(Sequence sequence, String parameterName) throws ROI2DValidationException {
        if (sequence == null) {
            throw new ROI2DValidationException(parameterName, null, Constants.ErrorMessages.NULL_SEQUENCE);
        }
    }
    
    /**
     * Validates that a polygon is not null.
     * 
     * @param polygon The polygon to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the polygon is null
     */
    public static void validateNotNull(Polygon2D polygon, String parameterName) throws ROI2DValidationException {
        if (polygon == null) {
            throw new ROI2DValidationException(parameterName, null, Constants.ErrorMessages.NULL_POLYGON);
        }
    }
    
    /**
     * Validates that a points list is not null or empty.
     * 
     * @param points The points list to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the points list is null or empty
     */
    public static void validateNotNullOrEmpty(List<Point2D> points, String parameterName) throws ROI2DValidationException {
        if (points == null) {
            throw new ROI2DValidationException(parameterName, null, Constants.ErrorMessages.NULL_POINTS);
        }
        if (points.isEmpty()) {
            throw new ROI2DValidationException(parameterName, points.size(), Constants.ErrorMessages.EMPTY_POINTS);
        }
    }
    
    /**
     * Validates that a polygon has the required number of sides.
     * 
     * @param polygon The polygon to validate
     * @param requiredSides The required number of sides
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the polygon doesn't have the required number of sides
     */
    public static void validatePolygonSides(Polygon2D polygon, int requiredSides, String parameterName) 
            throws ROI2DValidationException {
        validateNotNull(polygon, parameterName);
        if (polygon.npoints != requiredSides) {
            throw new ROI2DValidationException(parameterName, polygon.npoints, 
                String.format(Constants.ErrorMessages.INVALID_POLYGON_SIDES, requiredSides, polygon.npoints));
        }
    }
    
    /**
     * Validates grid dimensions are positive and within limits.
     * 
     * @param columns Number of columns
     * @param rows Number of rows
     * @throws ROI2DValidationException If dimensions are invalid
     */
    public static void validateGridDimensions(int columns, int rows) throws ROI2DValidationException {
        if (columns < Constants.Grid.MIN_GRID_COLUMNS || columns > Constants.Grid.MAX_GRID_COLUMNS) {
            throw new ROI2DValidationException("columns", columns, 
                String.format("Must be between %d and %d", 
                    Constants.Grid.MIN_GRID_COLUMNS, Constants.Grid.MAX_GRID_COLUMNS));
        }
        if (rows < Constants.Grid.MIN_GRID_ROWS || rows > Constants.Grid.MAX_GRID_ROWS) {
            throw new ROI2DValidationException("rows", rows, 
                String.format("Must be between %d and %d", 
                    Constants.Grid.MIN_GRID_ROWS, Constants.Grid.MAX_GRID_ROWS));
        }
    }
    
    /**
     * Validates that there are sufficient points for ellipse fitting.
     * 
     * @param pointCount The number of points available
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If there are insufficient points
     */
    public static void validateEllipsePoints(int pointCount, String parameterName) throws ROI2DValidationException {
        if (pointCount < Constants.Geometry.MIN_ELLIPSE_POINTS) {
            throw new ROI2DValidationException(parameterName, pointCount,
                String.format(Constants.ErrorMessages.INSUFFICIENT_ELLIPSE_POINTS, 
                    Constants.Geometry.MIN_ELLIPSE_POINTS, pointCount));
        }
    }
    
    /**
     * Validates that a threshold value is within valid range.
     * 
     * @param threshold The threshold value to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the threshold is out of range
     */
    public static void validateThreshold(int threshold, String parameterName) throws ROI2DValidationException {
        if (threshold < Constants.Measurement.MIN_SPOT_THRESHOLD || 
            threshold > Constants.Measurement.MAX_SPOT_THRESHOLD) {
            throw new ROI2DValidationException(parameterName, threshold,
                String.format(Constants.ErrorMessages.INVALID_THRESHOLD_RANGE,
                    Constants.Measurement.MIN_SPOT_THRESHOLD,
                    Constants.Measurement.MAX_SPOT_THRESHOLD,
                    threshold));
        }
    }
    
    /**
     * Validates that a scale factor is positive.
     * 
     * @param scaleFactor The scale factor to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the scale factor is not positive
     */
    public static void validatePositiveScaleFactor(double scaleFactor, String parameterName) throws ROI2DValidationException {
        if (scaleFactor <= 0.0) {
            throw new ROI2DValidationException(parameterName, scaleFactor,
                String.format(Constants.ErrorMessages.INVALID_SCALE_FACTOR, scaleFactor));
        }
    }
    
    /**
     * Validates that interpolation intervals are valid.
     * 
     * @param intervals The number of intervals
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If intervals are invalid
     */
    public static void validateInterpolationIntervals(int intervals, String parameterName) throws ROI2DValidationException {
        if (intervals < Constants.Measurement.MIN_INTERPOLATION_INTERVALS) {
            throw new ROI2DValidationException(parameterName, intervals,
                String.format("Must be at least %d", Constants.Measurement.MIN_INTERPOLATION_INTERVALS));
        }
    }
    
    /**
     * Validates that an array index is within bounds.
     * 
     * @param index The index to validate
     * @param arrayLength The length of the array
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the index is out of bounds
     */
    public static void validateArrayIndex(int index, int arrayLength, String parameterName) throws ROI2DValidationException {
        if (index < 0 || index >= arrayLength) {
            throw new ROI2DValidationException(parameterName, index,
                String.format(Constants.ErrorMessages.ARRAY_INDEX_OUT_OF_BOUNDS, index, arrayLength));
        }
    }
    
    /**
     * Validates that an ROI is of type ROI2D.
     * 
     * @param roi The ROI to validate
     * @param parameterName The name of the parameter for error reporting
     * @throws ROI2DValidationException If the ROI is not a ROI2D
     */
    public static void validateROI2D(ROI roi, String parameterName) throws ROI2DValidationException {
        validateNotNull(roi, parameterName);
        if (!(roi instanceof ROI2D)) {
            throw new ROI2DValidationException(parameterName, roi.getClass().getSimpleName(),
                String.format(Constants.ErrorMessages.UNSUPPORTED_ROI_TYPE, roi.getClass().getSimpleName()));
        }
    }
} 