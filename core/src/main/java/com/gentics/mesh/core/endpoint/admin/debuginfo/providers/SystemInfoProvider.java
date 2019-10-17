package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
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
	public LoadLevel loadLevel() {
		return LoadLevel.LIGHT;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		SystemInfo info = new SystemInfo();
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

		info.systemLoadAverage = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
		info.heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		info.nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
		info.jvmArguments = runtimeMXBean.getInputArguments();

		return vertx.fileSystem().rxFsProps(".")
			.map(fs -> {
				DiskSpace diskSpace = new DiskSpace();
				diskSpace.totalSpace = fs.totalSpace();
				diskSpace.unallocatedSpace = fs.unallocatedSpace();
				diskSpace.usableSpace = fs.usableSpace();
				info.diskSpace = diskSpace;

				return DebugInfoEntry.fromString("systemInfo.json", JsonUtil.toJson(info));
			})
			.toFlowable();
	}

	public static class SystemInfo {
		public double systemLoadAverage;
		public MemoryUsage heapMemoryUsage;
		public MemoryUsage nonHeapMemoryUsage;
		public DiskSpace diskSpace;
		public List<String> jvmArguments;
	}

	private static class DiskSpace {
		public long totalSpace;
		public long unallocatedSpace;
		public long usableSpace;

		public float getPercentUsedSpace() {
			return 100f * usableSpace / totalSpace;
		}
	}

}

