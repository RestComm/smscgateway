package org.mobicents.smsc.slee.services.http.server.tx;

import org.mobicents.slee.ChildRelationExt;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.library.SmsSet;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.resources.persistence.TraceProxy;
import org.mobicents.smsc.slee.resources.scheduler.SchedulerRaSbbInterface;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import java.util.Date;

//import org.mobicents.smsc.slee.resources.persistence.TraceProxy;

/**
 * Created by tpalucki on 05.09.16.
 * @author Tomasz Pałucki
 */
public class TxHttpServerSbbTest {
    private TxHttpServerSbbProxy sbb;
    private PersistenceRAInterfaceProxy pers;
    private boolean cassandraDbInited;

    private static String sMsg = "??????Hel";
    private static String sMsg_2 = "Msg 2";
    private static String sMsg_3 = "Msg 3";
    private static String sMsg_4 = "Msg 4 []";
    private static byte[] msgUtf8, msgUtf8_2, msgUtf8_3;
    private static byte[] msgUcs2;
    private static byte[] msgGsm7;
    private static byte[] udhCode;
    private byte[] msg_ref_num = { 0, 10 };
    private Date scheduleDeliveryTime;

    private static final int WAITING_TIME = 1000;

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.pers = new PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.pers.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
        this.pers.start();


        this.sbb = new TxHttpServerSbbProxy(this.pers);

        SmscPropertiesManagement.getInstance("Test");
        SmscPropertiesManagement.getInstance().setSmscStopped(false);
        SmscPropertiesManagement.getInstance().setStoreAndForwordMode(StoreAndForwordMode.normal);
        MProcManagement.getInstance("Test");
    }

    @AfterMethod
    public void tearDownClass() throws Exception {
        System.out.println("tearDownClass");
    }

//    @Test(groups = { "TxHttpServer" })
    @Test
    public void sendMessageGETTest() throws Exception {
        System.out.println("Test: sendMessageTest");
//        if (!this.cassandraDbInited) {
//            return;
//        }
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        this.sbb.onHttpGet(event, aci);

        Assert.fail();
    }

    //    @Test(groups = { "TxHttpServer" })
    @Test
    public void sendMessagePOSTTest() throws Exception {
        System.out.println("Test: sendMessageTest");
//        if (!this.cassandraDbInited) {
//            return;
//        }
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        this.sbb.onHttpPost(event, aci);

        Assert.fail();
    }

//    @Test(groups = { "TxHttpServer" })
    @Test
    public void getMessageIdSatusTest(){
        System.out.println("Test: getMessageIdSatusTest");

        // TODO test not yet implemented
        Assert.fail();
    }

    private class TxHttpServerSbbProxy extends TxHttpServerSbb {

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
    }

    private class HttpActivityContextInterface implements ActivityContextInterface {

        public HttpActivityContextInterface() {
        }

        @Override
        public void attach(SbbLocalObject arg0) throws NullPointerException, SLEEException {
            // TODO Auto-generated method stub
        }

        @Override
        public void detach(SbbLocalObject arg0) throws NullPointerException, SLEEException {
            // TODO Auto-generated method stub
        }

        @Override
        public Object getActivity() throws SLEEException {
            // TODO Auto-generated method stub
            return this;
        }

        @Override
        public boolean isAttached(SbbLocalObject arg0) throws NullPointerException, SLEEException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public boolean isEnding() throws SLEEException {
            // TODO Auto-generated method stub
            return false;
        }
    }

    private class SchedulerResourceAdaptorProxy implements SchedulerRaSbbInterface {

        @Override
        public void injectSmsOnFly(SmsSet smsSet, boolean callFromSbb) throws Exception {
            // TODO Auto-generated method stub
        }

        @Override
        public void injectSmsDatabase(SmsSet smsSet) throws Exception {
            // TODO Auto-generated method stub
        }

        @Override
        public void setDestCluster(SmsSet smsSet) {
            // TODO Auto-generated method stub
        }
    }
}
