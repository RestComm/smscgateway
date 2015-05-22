/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.tools.smppsimulator.bootstrap;

//import gnu.getopt.Getopt;
//import gnu.getopt.LongOpt;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.mobicents.smsc.tools.smppsimulator.SmppSimulatorForm;

/**
 * @author <a href="mailto:amit.bhayani@jboss.com">amit bhayani</a>
 * @author sergey vetyutnev
 */
public class Main {

	private final static String APP_NAME = "SMPP Simulator";

	private final static String HOME_DIR = "SIMULATOR_HOME";
	private final static String LOG4J_URL = "/conf/log4j.properties";
	private final static String LOG4J_URL_XML = "/conf/log4j.xml";
	public static final String SIMULATOR_HOME = "simulator.home.dir";
	public static final String SIMULATOR_DATA = "simulator.data.dir";
	private static int index = 0;

	private static Logger logger;

	private String command = null;
	private String appName = "main";
	private int rmiPort = -1;
	private int httpPort = -1;

	public static void main(String[] args) throws Throwable {
		String homeDir = getHomeDir(args);
		System.setProperty(SIMULATOR_HOME, homeDir);
		System.setProperty(SIMULATOR_DATA, homeDir + File.separator + "data" + File.separator);

		if (!initLOG4JProperties(homeDir) && !initLOG4JXml(homeDir)) {
			System.err.println("Failed to initialize loggin, no configuration. Defaults are used.");
		}

		logger = Logger.getLogger(Main.class);
		logger.info("log4j configured");

		Main main = new Main();

		main.processCommandLine(args);
		main.boot();
	}

	private void processCommandLine(String[] args) {
	}

	private void genericHelp() {
		System.out.println("usage: " + APP_NAME + "<command> [options]");
		System.out.println();
		System.out.println("command:");
		System.out.println("    core      Start the SS7 simulator core");
		System.out.println("    gui       Start the SS7 simulator gui");
		System.out.println();
		System.out.println("see 'run <command> help' for more information on a specific command:");
		System.out.println();
		System.exit(0);
	}

	private void coreHelp() {
		System.out.println("core: Starts the simulator core");
		System.out.println();
		System.out.println("usage: " + APP_NAME + " core [options]");
		System.out.println();
		System.out.println("options:");
		System.out.println("    -n, --name=<simulator name>		Simulator name. If not passed default is main");
		System.out.println("    -t, --http=<http port>			Http port for core");
		System.out.println("    -r, --rmi=<rmi port>			RMI port for core");
		System.out.println();
		System.exit(0);
	}

	private void guiHelp() {
		System.out.println("gui: Starts the simulator gui");
		System.out.println();
		System.out.println("usage: " + APP_NAME + " gui [options]");
		System.out.println();
		System.out.println("options:");
		System.out.println("    -n, --name=<simulator name>   Simulator name. If not passed default is main");
		System.out.println();
		System.exit(0);
	}

	private static boolean initLOG4JProperties(String homeDir) {
		String Log4jURL = homeDir + LOG4J_URL;

		try {
			URL log4jurl = getURL(Log4jURL);
			InputStream inStreamLog4j = log4jurl.openStream();
			Properties propertiesLog4j = new Properties();
			try {
				propertiesLog4j.load(inStreamLog4j);
				PropertyConfigurator.configure(propertiesLog4j);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			// e.printStackTrace();
			System.err.println("Failed to initialize LOG4J with properties file.");
			return false;
		}
		return true;
	}

	private static boolean initLOG4JXml(String homeDir) {
		String Log4jURL = homeDir + LOG4J_URL_XML;

		try {
			URL log4jurl = getURL(Log4jURL);
			DOMConfigurator.configure(log4jurl);
		} catch (Exception e) {
			// e.printStackTrace();
			System.err.println("Failed to initialize LOG4J with xml file.");
			return false;
		}
		return true;
	}

	/**
	 * Gets the Media Server Home directory.
	 * 
	 * @param args
	 *            the command line arguments
	 * @return the path to the home directory.
	 */
	private static String getHomeDir(String args[]) {
		if (System.getenv(HOME_DIR) == null) {
			if (args.length > index) {
				return args[index++];
			} else {
				return ".";
			}
		} else {
			return System.getenv(HOME_DIR);
		}
	}

	protected void boot() throws Throwable {
		// if (this.command == null) {
		// System.out.println("No command passed");
		// this.genericHelp();
		// } else if (this.command.equals("gui")) {
		// EventQueue.invokeLater(new MainGui(appName));
		// } else if (this.command.equals("core")) {
		// MainCore mainCore = new MainCore();
		// mainCore.start(appName, httpPort, rmiPort);
		// }

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					// SmppSimulatorForm window = new
					// SmppSimulatorForm(initPar);
					SmppSimulatorForm window = new SmppSimulatorForm();
					window.getJFrame().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	public static URL getURL(String url) throws Exception {
		File file = new File(url);
		if (file.exists() == false) {
			throw new IllegalArgumentException("No such file: " + url);
		}
		return file.toURI().toURL();
	}

	protected void registerShutdownThread() {
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownThread()));
	}

	private class ShutdownThread implements Runnable {

		public void run() {
			System.out.println("Shutting down");

		}
	}
}
