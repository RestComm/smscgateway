package org.mobicents.smsc.tools.smppsimulator;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;

import com.cloudhopper.smpp.type.SmppProcessingException;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class DefaultSmppServerHandler implements SmppServerHandler {

    SmppTestingForm frm;

    public DefaultSmppServerHandler(SmppTestingForm frm) {
        this.frm = frm;
    }

    @Override
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException {
        if (this.frm.getSmppSession() != null) {
            throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
        }

//        sessionConfiguration.setAddressRange(bindRequestAddressRange);
//
//        sessionConfiguration.setCountersEnabled(esme.isCountersEnabled());

        sessionConfiguration.setName("Test SMPP session");
    }

    @Override
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        if (this.frm.getSmppSession() != null) {
            throw new SmppProcessingException(SmppConstants.STATUS_INVBNDSTS);
        }

        this.frm.addMessage("Session created", session.getConfiguration().getSystemId());
        this.frm.setSmppSession(session);
        session.serverReady(new ClientSmppSessionHandler(this.frm));
    }

    @Override
    public void sessionDestroyed(Long sessionId, SmppServerSession session) {
        this.frm.addMessage("Session destroyed", session.getConfiguration().getSystemId());
        this.frm.setSmppSession(null);

        session.destroy();
    }

}
