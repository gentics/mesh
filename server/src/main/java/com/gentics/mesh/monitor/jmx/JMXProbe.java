package com.gentics.mesh.monitor.jmx;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * Liveness probe. Connects to the locally running JVM over the JMX port, fetches the Liveness mbean and checks the "live" flag.
 */
public class JMXProbe {
	public final static String JMX_PORT = "9999";

	/**
	 * Main method
	 * @param args arguments
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + JMX_PORT + "/jmxrmi");
		JMXConnector connector = JMXConnectorFactory.connect(url);
		MBeanServerConnection remote = connector.getMBeanServerConnection();
		ObjectName objectName = new ObjectName(LivenessMBean.NAME);

		if ("true".equals(remote.getAttribute(objectName, "Live").toString())) {
			System.exit(0);
		} else {
			System.exit(1);
		}
	}
}
