package org.wikibrain.cookbook.wikiwalker.ui;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates an equidistant set of points on an ellipse.
 * @author Shilad Sen
 */
public class Ellipse {
    private static final int NUM_ITERATIONS = 20;
    private Point2D center;
    private double width;
    private double height;

    /**
     * Creates a new ellipse with a particular center and dimension
     */
    public Ellipse(double x, double y, double width, double height) {
        this.center = new Point2D.Double(x, y);
        this.width = width;
        this.height = height;
    }

    /**
     * Generates numPoints points equidistant along the circumference of the ellipse
     * between angle theta1 and theta2.
     *
     * @param theta1
     * @param theta2
     * @param numPoints
     * @return
     */
    public List<Point2D> generatePoints(double theta1, double theta2, int numPoints) {
        List<Double> thetas = new ArrayList<Double>();
        for (int i = 0; i < numPoints; i++) {
            thetas.add(theta1 + i * (theta2 - theta1) / (numPoints - 1));
        }
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            adjustThetas(thetas);
        }
        List<Point2D> points = new ArrayList<Point2D>();
        for (Double theta : thetas) {
            points.add(getPoint(theta));
        }
        return points;
    }

    /**
     * incrementally adjust each point.
     * @param thetas
     */
    private void adjustThetas(List<Double> thetas) {
        for (int i = 1; i < thetas.size() - 1; i++) {
            double t0 = thetas.get(i-1);
            double t1 = thetas.get(i);
            double t2 = thetas.get(i+1);
            double distBefore = arcLength(t0, t1);
            double distAfter = arcLength(t1, t2);
            double adjustmentRatio = (distAfter - distBefore) / 2 / (distBefore + distAfter);
            thetas.set(i, t1 + (t2 - t0) * adjustmentRatio);
        }
    }

    /**
     * Returns the arc length between two points on the circumference, as approximated by Euclidean distance.
     * @param theta1
     * @param theta2
     * @return
     */
    public double arcLength(double theta1, double theta2) {
        return getPoint(theta1).distance(getPoint(theta2));
    }

    /**
     * Returns the point associated with a particular angle.
     * @param theta
     * @return
     */
    public Point2D getPoint(double theta) {
        return new Point2D.Double(
                center.getX() + Math.cos(theta) * width / 2,
                center.getY() - Math.sin(theta) * height / 2);
    }

    public static void main(String args[]) {
        Ellipse ellipse = new Ellipse(100, 300, 200, 100);
        System.out.println("thetas are: " + ellipse.generatePoints(Math.PI / 2, Math.PI * 3 / 2, 20));
    }
}
