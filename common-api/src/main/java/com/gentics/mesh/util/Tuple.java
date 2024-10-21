package com.gentics.mesh.util;

/**
 * Class for pairing objects
 *
 * @param <V1>
 * @param <V2>
 */
public class Tuple<V1, V2> {
	/**
	 * Create an instance
	 * 
	 * @param v1
	 *            first object
	 * @param v2
	 *            second object
	 * @return instance
	 */
	public static <V1, V2> Tuple<V1, V2> tuple(V1 v1, V2 v2) {
		return new Tuple<>(v1, v2);
	}

	private final V1 v1;
	private final V2 v2;

	/**
	 * Create an instance
	 * 
	 * @param v1
	 *            first object
	 * @param v2
	 *            second object
	 */
	public Tuple(V1 v1, V2 v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	/**
	 * Get the first object
	 * 
	 * @return first object
	 */
	public V1 v1() {
		return v1;
	}

	/**
	 * Get the second object
	 * 
	 * @return second object
	 */
	public V2 v2() {
		return v2;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		@SuppressWarnings("unchecked")
		Tuple<V1, V2> tuple = (Tuple<V1, V2>) o;

		if (v1 != null ? !v1.equals(tuple.v1) : tuple.v1 != null) {
			return false;
		}
		if (v2 != null ? !v2.equals(tuple.v2) : tuple.v2 != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = v1 != null ? v1.hashCode() : 0;
		result = 31 * result + (v2 != null ? v2.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Tuple [v1=" + v1 + ", v2=" + v2 + "]";
	}
}
