package org.sqlite.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;
import org.sqlite.driver.DriverInfo;
import org.sqlite.driver.JDBC;

import java.util.Dictionary;
import java.util.Hashtable;

public class DbDriverActivator implements BundleActivator {
	private ServiceRegistration<?> registration;

	@Override
	public void start(BundleContext context) throws Exception {
		if (!JDBC.isRegistered()) {
			JDBC.register();
		}
		if (dataSourceFactoryExists()) {
			registerDataSourceFactory(context);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
		if (JDBC.isRegistered()) {
			JDBC.deregister();
		}
	}

	private void registerDataSourceFactory(BundleContext context) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS, JDBC.class.getName());
		properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_NAME, DriverInfo.DRIVER_NAME);
		properties.put(DataSourceFactory.OSGI_JDBC_DRIVER_VERSION, DriverInfo.DRIVER_VERSION);
		registration = context.registerService(DataSourceFactory.class,
			new OsgiDataSourceFactory(), properties);
	}

	private static boolean dataSourceFactoryExists() {
		try {
			Class.forName("org.osgi.service.jdbc.DataSourceFactory");
			return true;
		} catch (ClassNotFoundException ignored) {
			// DataSourceFactory does not exist => no reason to register the service
		}
		return false;
	}
}
