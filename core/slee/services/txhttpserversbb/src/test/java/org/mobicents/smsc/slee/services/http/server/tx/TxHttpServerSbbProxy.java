package org.mobicents.smsc.slee.services.http.server.tx;

import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;

import javax.slee.ActivityContextInterface;
import javax.slee.CreateException;
import javax.slee.RolledBackContext;

/**
 * Created by tpalucki on 14.09.16.
 */
public class TxHttpServerSbbProxy extends TxHttpServerSbb {

    private PersistenceRAInterfaceProxy cassandraSbb;

    public TxHttpServerSbbProxy(PersistenceRAInterfaceProxy cassandraSbb) {
        this.cassandraSbb = cassandraSbb;
        this.logger = new TraceProxy();
        this.scheduler = new SchedulerResourceAdaptorProxy();
        TxHttpServerSbb.smscPropertiesManagement = SmscPropertiesManagement.getInstance("Test");
    }

    @Override
    public PersistenceRAInterfaceProxy getStore() {
        return cassandraSbb;
    }

//        public void setSmppServerSessions(SmppSessions smppServerSessions) {
//            this.smppServerSessions = smppServerSessions;
//        }
//
//        protected Sms createSmsEvent(BaseSm event, Esme origEsme, TargetAddress ta, PersistenceRAInterface store)
//                throws SmscProcessingException {
//            return super.createSmsEvent(event, origEsme, ta, store);
//        }

    //        @Override
    public ChildRelationExt getChargingSbb() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void unsetSbbContext() {

    }

    @Override
    public void sbbCreate() throws CreateException {

    }

    @Override
    public void sbbPostCreate() throws CreateException {

    }

    @Override
    public void sbbActivate() {

    }

    @Override
    public void sbbPassivate() {

    }

    @Override
    public void sbbLoad() {

    }

    @Override
    public void sbbStore() {

    }

    @Override
    public void sbbRemove() {

    }

    @Override
    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface aci) {

    }

    @Override
    public void sbbRolledBack(RolledBackContext context) {

    }
}
