package plugins.fmp.multicafe.tools;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector2d;

import icy.file.Saver;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.sequence.Sequence;
import icy.type.geom.Polygon2D;
import plugins.fmp.multicafe.experiment.Experiment;
import plugins.fmp.multicafe.experiment.SequenceCamData;
import plugins.fmp.multicafe.experiment.cages.Cage;
import plugins.fmp.multicafe.experiment.capillaries.Capillary;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class ImageRegistrationFeatures extends ImageRegistration {

	@Override
	public boolean runRegistration(Experiment exp, int referenceFrame, int startFrame, int endFrame, boolean reverse) {
		if (!doBackup(exp))
			return false;

		SequenceCamData seqCamData = exp.getSeqCamData();
		Sequence seq = seqCamData.getSeq();

		List<Point2D> referencePoints = extractPoints(exp);
		List<Point2D> currentPoints = new ArrayList<Point2D>();
		for (Point2D p : referencePoints)
			currentPoints.add((Point2D) p.clone());

		int step = reverse ? -1 : 1;
		int start = reverse ? endFrame : startFrame;
		int end = reverse ? startFrame : endFrame;

		int t = start;
		while ((reverse && t >= end) || (!reverse && t <= end)) {
			if (t == referenceFrame) {
				t += step;
				continue;
			}

			int t_prev = t - step;
			if (t_prev < 0 || t_prev >= seq.getSizeT()) {
				t += step;
				continue;
			}

			IcyBufferedImage imgPrev = seq.getImage(t_prev, 0);
			IcyBufferedImage imgCurr = seq.getImage(t, 0);

			trackPoints(imgPrev, imgCurr, currentPoints);

			AffineTransform transform = computeAffineTransform(referencePoints, currentPoints);

			try {
				AffineTransform inverse = transform.createInverse();

				// IcyBufferedImageUtil does not have a direct getAffineTransformedImage method.
				// Use Java2D to apply the affine transform.
				IcyBufferedImage newImg = new IcyBufferedImage(imgCurr.getWidth(), imgCurr.getHeight(),
						imgCurr.getSizeC(), imgCurr.getDataType_());

				for (int c = 0; c < imgCurr.getSizeC(); c++) {
					java.awt.image.BufferedImage awtSrc = imgCurr.getImage(c);
					java.awt.image.BufferedImage awtDst = new java.awt.image.BufferedImage(awtSrc.getWidth(),
							awtSrc.getHeight(), awtSrc.getType());
					java.awt.Graphics2D g2 = awtDst.createGraphics();

					// Apply inverse transform to align current image with reference
					// This handles translation, rotation, and scaling (x and y independently)
					g2.setTransform(inverse);
					g2.drawImage(awtSrc, 0, 0, null);
					g2.dispose();

					// Convert transformed BufferedImage back to IcyBufferedImage
					IcyBufferedImage tempWrapper = IcyBufferedImage.createFrom(awtDst);
					newImg.setDataXY(c, tempWrapper.getDataXY(0));
				}

				String filename = seqCamData.getFileNameFromImageList(t);
				File file = new File(filename);
				Saver.saveImage(newImg, file, true);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			t += step;
		}
		return true;
	}

	private List<Point2D> extractPoints(Experiment exp) {
		List<Point2D> points = new ArrayList<>();
		for (Capillary cap : exp.getCapillaries().getCapillariesList()) {
			Point2D p1 = cap.getCapillaryROIFirstPoint();
			if (p1 != null)
				points.add(p1);
			Point2D p2 = cap.getCapillaryROILastPoint();
			if (p2 != null)
				points.add(p2);
		}
		for (Cage cage : exp.getCages().getCageList()) {
			if (cage.getCageRoi2D() instanceof ROI2DPolygon) {
				Polygon2D poly = ((ROI2DPolygon) cage.getCageRoi2D()).getPolygon2D();
				for (Point2D p : poly.getPoints()) {
					points.add(p);
				}
			}
		}
		return points;
	}

	private void trackPoints(IcyBufferedImage imgPrev, IcyBufferedImage imgCurr, List<Point2D> points) {
		int width = 32;
		int half = width / 2;

		for (int i = 0; i < points.size(); i++) {
			Point2D p = points.get(i);
			int x = (int) p.getX();
			int y = (int) p.getY();

			if (x - half < 0 || y - half < 0 || x + half >= imgPrev.getWidth() || y + half >= imgPrev.getHeight())
				continue;

			IcyBufferedImage patchPrev = IcyBufferedImageUtil.getSubImage(imgPrev,
					new java.awt.Rectangle(x - half, y - half, width, width));
			IcyBufferedImage patchCurr = IcyBufferedImageUtil.getSubImage(imgCurr,
					new java.awt.Rectangle(x - half, y - half, width, width));

			Vector2d translation = new Vector2d();
			int n = 0;
			for (int c = 0; c < imgPrev.getSizeC(); c++) {
				translation.add(GaspardRigidRegistration.findTranslation2D(patchCurr, c, patchPrev, c));
				n++;
			}
			translation.scale(1.0 / n);

			double newX = p.getX() - translation.x;
			double newY = p.getY() - translation.y;

			p.setLocation(newX, newY);
		}
	}

	/**
	 * Computes a full affine transform that handles:
	 * - Translation (tx, ty)
	 * - Rotation (θ)
	 * - Scaling in X and Y (sx, sy) - independent scaling
	 * - Shear (if present)
	 * 
	 * The transform maps source points to destination points using least squares:
	 * u = a*x + b*y + c  (x-coordinate transformation)
	 * v = d*x + e*y + f  (y-coordinate transformation)
	 * 
	 * Where the AffineTransform matrix is:
	 * [a  b  c]   [m00  m01  m02]
	 * [d  e  f] = [m10  m11  m12]
	 * [0  0  1]   [  0    0    1]
	 */
	private AffineTransform computeAffineTransform(List<Point2D> srcPoints, List<Point2D> dstPoints) {
		int n = srcPoints.size();
		if (n < 3)
			return new AffineTransform();

		double sumX = 0, sumY = 0, sumX2 = 0, sumY2 = 0, sumXY = 0;
		double sumU = 0, sumV = 0, sumUX = 0, sumUY = 0, sumVX = 0, sumVY = 0;

		for (int i = 0; i < n; i++) {
			double x = srcPoints.get(i).getX();
			double y = srcPoints.get(i).getY();
			double u = dstPoints.get(i).getX();
			double v = dstPoints.get(i).getY();

			sumX += x;
			sumY += y;
			sumX2 += x * x;
			sumY2 += y * y;
			sumXY += x * y;

			sumU += u;
			sumV += v;
			sumUX += u * x;
			sumUY += u * y;

			sumVX += v * x;
			sumVY += v * y;
		}

		// Solve for transform parameters using least squares
		// Matrix equation: A * [a, b, c]^T = [sumUX, sumUY, sumU]^T
		//                  A * [d, e, f]^T = [sumVX, sumVY, sumV]^T
		double[][] A = { { sumX2, sumXY, sumX }, { sumXY, sumY2, sumY }, { sumX, sumY, (double) n } };

		double[] B_u = { sumUX, sumUY, sumU };
		double[] B_v = { sumVX, sumVY, sumV };

		double[] sol_u = solve3x3(A, B_u);
		double[] sol_v = solve3x3(A, B_v);

		if (sol_u == null || sol_v == null)
			return new AffineTransform();

		// AffineTransform(m00, m10, m01, m11, m02, m12)
		// m00 = a (x-scale and rotation), m01 = b (x-shear)
		// m10 = d (y-shear), m11 = e (y-scale and rotation)
		// m02 = c (x-translation), m12 = f (y-translation)
		return new AffineTransform(sol_u[0], sol_v[0], sol_u[1], sol_v[1], sol_u[2], sol_v[2]);
	}

	private double[] solve3x3(double[][] A, double[] B) {
		double det = det3x3(A);
		if (Math.abs(det) < 1e-9)
			return null;

		double detX = det3x3(replaceCol(A, B, 0));
		double detY = det3x3(replaceCol(A, B, 1));
		double detZ = det3x3(replaceCol(A, B, 2));

		return new double[] { detX / det, detY / det, detZ / det };
	}

	private double det3x3(double[][] m) {
		return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
				+ m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
	}

	private double[][] replaceCol(double[][] m, double[] col, int c) {
		double[][] res = new double[3][3];
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				res[i][j] = (j == c) ? col[i] : m[i][j];
			}
		}
		return res;
	}

}
