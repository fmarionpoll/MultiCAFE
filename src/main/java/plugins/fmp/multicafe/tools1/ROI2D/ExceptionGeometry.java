package plugins.fmp.multicafe.tools1.ROI2D;

/**
 * Exception thrown when geometric operations fail.
 */
public class ExceptionGeometry extends ExceptionROI2D {

	private static final long serialVersionUID = 1L;

	private final String geometryOperation;

	public ExceptionGeometry(String geometryOperation, String message) {
		super(String.format("Geometry operation '%s' failed: %s", geometryOperation, message));
		this.geometryOperation = geometryOperation;
	}

	public ExceptionGeometry(String geometryOperation, String message, Throwable cause) {
		super(String.format("Geometry operation '%s' failed: %s", geometryOperation, message), cause);
		this.geometryOperation = geometryOperation;
	}

	public String getGeometryOperation() {
		return geometryOperation;
	}
}
