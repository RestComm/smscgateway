package org.mobicents.protocols.smpp.timers;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.SmppProcessingException;

public class DefaultSmppServerHandler implements SmppServerHandler {
    
    private static Logger logger = Logger.getLogger(DefaultSmppServerHandler.class);
    
    private DefaultSmppSessionHandler handler;
    private SmppServerSession session;
    
    public DefaultSmppServerHandler(DefaultSmppSessionHandler handler) {
        this.handler = handler;
    }
    
    public SmppServerSession getSession() {
        return session;
    }
    
    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
        // test name change of sessions
        // this name actually shows up as thread context....
        sessionConfiguration.setName("Application.SMPP." + sessionConfiguration.getSystemId());

        //throw new SmppProcessingException(SmppConstants.STATUS_BINDFAIL, null);
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        logger.info("Session created: "+ session);
        // need to do something it now (flag we're ready)
        session.serverReady(handler);
        this.session = session;
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        logger.info("Session destroyed: "+ session);
        
        // make sure it's really shutdown
        session.destroy();
    }
}
