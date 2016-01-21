package org.mobicents.protocols.smpp.load;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServer;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.SmppProcessingException;

public class SlowServer extends TestHarness {

	private static Logger logger = Logger.getLogger(SlowServer.class);
	private static final long DELAY_BEFORE_RESPONSE = 3000;
	
    private int port = 2775;
    private int maxConnectionSize = 10;
    private boolean nonBlockingSocketsEnabled = true;
    private int defaultRequestExpiryTimeout = 30000;
    private int defaultWindowMonitorInterval = 15000;
    private int defaultWindowSize = 5;
    private long defaultWindowWaitTimeout = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
    private boolean defaultSessionCountersEnabled = true;
    private boolean jmxEnabled = true;

	public SlowServer() {

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception  {
		SlowServer ss = new SlowServer();
		ss.test(args);
	}

	private void test(String[] args) throws Exception {
		this.port = Integer.parseInt(args[0]);
        this.maxConnectionSize = Integer.parseInt(args[1]);
        this.nonBlockingSocketsEnabled = Boolean.parseBoolean(args[2]);
        this.defaultRequestExpiryTimeout = Integer.parseInt(args[3]);
        this.defaultWindowMonitorInterval = Integer.parseInt(args[4]);
        this.defaultWindowSize = Integer.parseInt(args[5]);
        this.defaultWindowWaitTimeout = Long.parseLong(args[6]);
        this.defaultSessionCountersEnabled = Boolean.parseBoolean(args[7]);
        this.jmxEnabled = Boolean.parseBoolean(args[8]);

        if (port < 1) {
            throw new Exception("port cannot be less than 1");
        }
        
        if (maxConnectionSize < 1) {
            throw new Exception("maxConnectionSize cannot be less than 1");
        }
        
        if (defaultRequestExpiryTimeout < 1) {
            throw new Exception("defaultRequestExpiryTimeout to send cannot be less than 1");
        }       
        
        if (defaultWindowMonitorInterval < 1) {
            throw new Exception("defaultWindowMonitorInterval cannot be less than 1");
        }

        if (defaultWindowSize < 1) {
            throw new Exception("defaultWindowSize cannot be less than 1");
        }
        
		logger.info("port=" + port);
		logger.info("maxConnectionSize=" + maxConnectionSize);
		logger.info("nonBlockingSocketsEnabled=" + nonBlockingSocketsEnabled);
		logger.info("defaultRequestExpiryTimeout=" + defaultRequestExpiryTimeout);
		logger.info("defaultWindowMonitorInterval=" + defaultWindowMonitorInterval);
		logger.info("defaultWindowSize=" + defaultWindowSize);
		logger.info("defaultWindowWaitTimeout=" + defaultWindowWaitTimeout);
		logger.info("defaultSessionCountersEnabled=" + defaultSessionCountersEnabled);
		logger.info("jmxEnabled=" + jmxEnabled);
		
		SmppServerConfiguration configuration = new SmppServerConfiguration();
		configuration.setPort(port);
        configuration.setMaxConnectionSize(maxConnectionSize);
        configuration.setNonBlockingSocketsEnabled(nonBlockingSocketsEnabled);
        configuration.setDefaultRequestExpiryTimeout(defaultRequestExpiryTimeout);
        configuration.setDefaultWindowMonitorInterval(defaultWindowMonitorInterval);
        configuration.setDefaultWindowSize(defaultWindowSize);
        configuration.setDefaultWindowWaitTimeout(defaultWindowWaitTimeout);
        configuration.setDefaultSessionCountersEnabled(defaultSessionCountersEnabled);
        configuration.setJmxEnabled(jmxEnabled);
        
		SmppServer smppServer = new DefaultSmppServer(configuration, new DefaultSmppServerHandler());

		logger.info("About to start SMPP slow server");
		smppServer.start();
		logger.info("SMPP slow server started");

		System.out.println("Press any key to stop server");
		System.in.read();

		logger.info("SMPP server stopping");
		smppServer.stop();
		logger.info("SMPP server stopped");
	}

	public static class DefaultSmppServerHandler implements SmppServerHandler {
		@Override
		public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration,
				final BaseBind bindRequest) throws SmppProcessingException {
			// this name actually shows up as thread context....
			sessionConfiguration.setName("Application.SMPP." + sessionId);
		}

		@Override
		public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse)
				throws SmppProcessingException {
			logger.info("Session created: " + session);
			// need to do something it now (flag we're ready)
			session.serverReady(new SlowSmppSessionHandler());
		}

		@Override
		public void sessionDestroyed(Long sessionId, SmppServerSession session) {
			logger.info("Session destroyed: " + session);
		}

	}

	public static class SlowSmppSessionHandler extends DefaultSmppSessionHandler {
		@Override
		public PduResponse firePduRequestReceived(PduRequest pduRequest) {
			try {
				Thread.sleep(DELAY_BEFORE_RESPONSE);
			} catch (Exception e) {
			}

			// ignore for now (already logged)
			return pduRequest.createResponse();
		}
	}

}
