package com.gentics.mesh.metric;

public class SyncMetric implements Metric {
	private final Operation operation;
	private final Meter meter;
	private final String type;

	public SyncMetric(String type, Operation operation, Meter meter) {
		this.operation = operation;
		this.meter = meter;
		this.type = type;
	}

	@Override
	public String key() {
		return String.format("mesh_index_sync_%s_%s_%s", type, operation.name().toLowerCase(), meter.name().toLowerCase());
	}

	@Override
	public String description() {
		return null;
	}

	public enum Operation {
		INSERT,
		UPDATE,
		DELETE,
	}

	public enum Meter {
		PENDING,
		SYNCED
	}
}
