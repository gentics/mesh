package com.gentics.mesh.core.endpoint.admin.consistency.check;

public class OrientDBFieldCheck extends FieldCheck {

	 {
			String uuid = list.getUuid();
			// Check whether the list has a parent container
			Iterable<? extends NodeGraphFieldContainerImpl> nodeContainers = list.in(HAS_LIST).has(NodeGraphFieldContainerImpl.class)
				.frameExplicit(NodeGraphFieldContainerImpl.class);

			Iterable<? extends MicronodeImpl> micronodeContainers = list.in(HAS_LIST).has(MicronodeImpl.class)
				.frameExplicit(MicronodeImpl.class);
			if (!nodeContainers.iterator().hasNext() && !micronodeContainers.iterator().hasNext()) {
				InconsistencyInfo info = new InconsistencyInfo()
					.setDescription("Found dangling list of type {" + type + "} with uuid {" + uuid + "}. No parent container found.")
					.setElementUuid(uuid).setSeverity(LOW);
				if (attemptRepair) {
					list.delete();
					info.setRepaired(true)
						.setRepairAction(RepairAction.DELETE);
				}
				result.addInconsistency(info);
			}

		}
}
