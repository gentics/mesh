package com.gentics.mesh.core.rest.node.field;

/**
 * Point which can represent a point or image dimensions.
 */
public class Point {

	public int x;
	public int y;

	/**
	 * Construct a new point.
	 * 
	 * @param x
	 * @param y
	 */
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Point() {
	}

	/**
	 * Return x component.
	 * 
	 * @return
	 */
	public int getX() {
		return x;
	}

	/**
	 * Return y component.
	 * 
	 * @return
	 */
	public int getY() {
		return y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Point) {
			Point point = (Point) obj;
			return point.x == x && point.y == y;
		} else {
			return super.equals(obj);
		}
	}

	/**
	 * Checks whether the point could fit within the area size.
	 * 
	 * @param imageSize
	 * @return
	 */
	public boolean isWithinBoundsOf(Point areaSize) {
		// Check whether the point is outside of the area.
		if (x <= 0 || y <= 0) {
			return false;
		}
		// Check whether the point is outside of the area.
		if (x > areaSize.x || y > areaSize.y) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return x + ":" + y;
	}

	/**
	 * Return the ration of x to y.
	 * 
	 * @return
	 */
	public double getRatio() {
		return (double) getX() / (double) getY();
	}
}
