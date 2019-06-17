package com.gentics.mesh.metric;

import com.codahale.metrics.Counter;

public class ResettableCounter extends Counter {

	public void reset() {
		dec(getCount());
	}
}
