package com.gentics.mesh.monitor.jmx;

import java.lang.management.ManagementFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.StandardMBean;

/**
 * Liveness mbean
 */
public class Liveness extends StandardMBean implements LivenessMBean {
	/**
	 * Singleton instance
	 */
	protected static Liveness instance;

	/**
	 * Initialize the liveness mbean. This will create the mbean (if not yet created) and will register it at the mbean server
	 * @throws NotCompliantMBeanException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MalformedObjectNameException
	 */
	public static void init() throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException {
		if (instance == null) {
			instance = new Liveness();
		}
	}

	/**
	 * Set the liveness to "false"
	 */
	public static void setFalse() {
		if (instance != null) {
			instance.setLive(false);
		}
	}

	/**
	 * Liveness flag
	 */
	protected boolean live = true;

	/**
	 * Create the instance and register at the mbean server
	 * @throws NotCompliantMBeanException
	 * @throws InstanceAlreadyExistsException
	 * @throws MBeanRegistrationException
	 * @throws MalformedObjectNameException
	 */
	protected Liveness() throws NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException, MalformedObjectNameException {
		super(LivenessMBean.class);
		ManagementFactory.getPlatformMBeanServer().registerMBean(this, new ObjectName(NAME));
	}

	@Override
	public boolean isLive() {
		return live;
	}

	/**
	 * Set the liveness flag
	 * @param live flag
	 */
	public void setLive(boolean live) {
		this.live = live;
	}
}
