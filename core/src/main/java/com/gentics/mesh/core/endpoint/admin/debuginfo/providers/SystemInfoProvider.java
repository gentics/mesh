package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import static com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoUtil.humanReadableByteCount;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.json.JsonUtil;

import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;

@Singleton
public class SystemInfoProvider implements DebugInfoProvider {

	private final Vertx vertx;

	@Inject
	public SystemInfoProvider(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public String name() {
		return "systemInfo";
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		SystemInfo info = new SystemInfo();
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

		info.systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
		info.heapMemoryUsage = new ReadableMemoryUsage(memoryMXBean.getHeapMemoryUsage());
		info.nonHeapMemoryUsage = new ReadableMemoryUsage(memoryMXBean.getNonHeapMemoryUsage());
		info.jvmArguments = runtimeMXBean.getInputArguments();

		return vertx.fileSystem().rxFsProps(".")
			.map(fs -> {
				DiskSpace diskSpace = new DiskSpace();
				diskSpace.totalSpace = humanReadableByteCount(fs.totalSpace());
				diskSpace.unallocatedSpace = humanReadableByteCount(fs.unallocatedSpace());
				diskSpace.usableSpace = humanReadableByteCount(fs.usableSpace());
				info.diskSpace = diskSpace;

				return DebugInfoBufferEntry.fromString("systemInfo.json", JsonUtil.toJson(info));
			})
			.toFlowable();
	}

	public static class SystemInfo {
		public double systemLoadAverage;
		public ReadableMemoryUsage heapMemoryUsage;
		public ReadableMemoryUsage nonHeapMemoryUsage;
		public DiskSpace diskSpace;
		public List<String> jvmArguments;
	}

	private static class DiskSpace {
		public String totalSpace;
		public String unallocatedSpace;
		public String usableSpace;
	}

	private static class ReadableMemoryUsage {
		private final MemoryUsage memoryUsage;

		private ReadableMemoryUsage(MemoryUsage memoryUsage) {
			this.memoryUsage = memoryUsage;
		}

		public String getInit() {
			return humanReadableByteCount(memoryUsage.getInit());
		}

		public String getUsed() {
			return humanReadableByteCount(memoryUsage.getUsed());
		}

		public String getCommitted() {
			return humanReadableByteCount(memoryUsage.getCommitted());
		}

		public String getMax() {
			return humanReadableByteCount(memoryUsage.getMax());
		}
	}

}

