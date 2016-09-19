package org.mobicents.smsc.slee.services.http.server.tx;

import org.junit.Ignore;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

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

    // test constants
    private static final String ENCODING_UCS2 = "UCS-2";
    private static final String ENCODING_UTF8 = "UTF8";

    private static final String FORMAT_STRING = "String";
    private static final String FORMAT_JSON = "json";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    private static final String SENDER_ID_DEFAULT = "Sender_id_1231241243";
    private static final String MESSAGE_DEFAULT = "SMS message ;P";

    private static final String PASSWORD_DEFAULT = "password";
    private static final String USER_DEFAULT = "user_4321";

    private static final String[] TO_MULTIPLE = {"123456789", "111222333", "123123123"};
    private static final String[] TO_ONE = {"123456789"};

    private static final String URL_SEND_MESSAGE = "http://test.pl/restcomm";
    private static final String URL_SEND_MESSAGE_FAKE = "http://test.pl/sendMessageFake";
    private static final String URL_GET_MESSAGE_ID_STATUS = "http://test.pl/getStatus";

    private static final String MESSAGE_ID = "123456789";

    private static final String MSG_ARABIC = "الأَبْجَدِيَّة العَرَبِيَّة";

    @BeforeMethod
    public void setUpClass() throws Exception {
        System.out.println("setUpClass");

        this.pers = new PersistenceRAInterfaceProxy();
        this.cassandraDbInited = this.pers.testCassandraAccess();
        if (!this.cassandraDbInited)
            return;
//        if (!this.cassandraDbInited)
//            Assert.fail("Cassandra DB is not inited");
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

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //-SEND MESSAGE FUNCTIONALITY TESTS
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    @Test
    public void sendMessageGETStringSuccessTest() throws Exception {
        System.out.println("sendMessageGETStringSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"), FORMAT_STRING, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);
        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();
        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_STRING, true), "Response is not valid");
    }

    @Test
    public void sendMessageGETStringErrorTest() throws UnsupportedEncodingException {
        System.out.println("sendMessageGETStringErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"), FORMAT_STRING, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, false), "Response is not valid");
    }

    @Test
    public void wrongServiceTest(){
        System.out.println("sendMessageGETStringErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE_FAKE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_STRING, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, false), "Response is not valid");
    }

    @Test
    public void sendMessageGETJsonSuccessTest(){
        System.out.println("sendMessageGETJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true), "Response is not valid");
    }

    @Test
    public void sendMessageGETJsonErrorTest(){
        System.out.println("sendMessageGETJsonErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, false), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTStringSuccessTest(){
        System.out.println("sendMessagePOSTStringSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_STRING, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTStringErrorTest(){
        System.out.println("sendMessagePOSTStringErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_STRING, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, false), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTJsonSuccessTest(){
        System.out.println("sendMessagePOSTJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTJsonErrorTest(){
        System.out.println("sendMessagePOSTJsonErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON, ENCODING_UCS2, SENDER_ID_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, false), "Response is not valid");
    }

    @Test
    public void sendArabicMessageGETJsonSuccessTest() throws UnsupportedEncodingException {
        System.out.println("sendArabicMessageGETJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        String urlEncoded = null;
        urlEncoded = URLEncoder.encode(MSG_ARABIC, "UTF-8");

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, urlEncoded, FORMAT_JSON, ENCODING_UTF8, SENDER_ID_DEFAULT, TO_MULTIPLE);
//        request.setContentType("application/x-www-form-urlencoded");
//        request.setCharacterEncoding("UTF-8");
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true), "Response is not valid");
    }

    @Test
    public void sendArabicMessageGETStringSuccessTest() throws UnsupportedEncodingException {
        System.out.println("sendArabicMessageGETStringSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        String urlEncoded = null;
        urlEncoded = URLEncoder.encode(MSG_ARABIC, "UTF-8");

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, urlEncoded, FORMAT_STRING, ENCODING_UTF8, SENDER_ID_DEFAULT, TO_MULTIPLE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true), "Response is not valid");
    }

    @Test
    public void sendArabicMessagePOSTSuccessTest() throws UnsupportedEncodingException {
            System.out.println("sendArabicMessagePOSTSuccessTest");
            if (!this.cassandraDbInited) {
//                Assert.fail("Cassandra DB is not inited");
                return;
            }
            //  prepare
            ActivityContextInterface aci = new HttpActivityContextInterface();
            MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

            String urlEncoded = null;
            urlEncoded = URLEncoder.encode(MSG_ARABIC, "UTF-8");

            MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, urlEncoded, FORMAT_STRING, ENCODING_UTF8, SENDER_ID_DEFAULT, TO_MULTIPLE);
            event.setRequest(request);

            MockHttpServletResponse response = new MockHttpServletResponse();
            event.setResponse(response);

            // perform the action
            this.sbb.onHttpPost(event, aci);

            MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

            printResponseData(resp);

            Assert.assertTrue(isValid(resp, FORMAT_STRING, true), "Response is not valid");
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //-END OF SEND MESSAGE FUNCTIONALITY TESTS
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //-GET STATUS FUNCTIONALITY TESTS
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    @Ignore
    @Test
    public void getStatusGETSuccessTest(){
        System.out.println("getStatusGETSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_GET, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_ID, FORMAT_STRING);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        //TODO set the persistence

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true), "Response is not valid");
    }

    @Test
    public void getStatusGETErrorTest(){
        System.out.println("getStatusGETErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_GET, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_ID, FORMAT_STRING);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, false), "Response is not valid");
    }

    @Ignore
    @Test
    public void getStatusPOSTTest(){
//        Assert.fail("Test not yet implemented");
    }

    @Test
    public void getStatusPOSTStringErrorTest(){
        System.out.println("getStatusPOSTStringErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_GET, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, PASSWORD_DEFAULT, null, FORMAT_STRING);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, false), "Response is not valid");
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //-END OF GET STATUS FUNCTIONALITY TESTS
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    private boolean isValid(MockHttpServletResponse resp, String format, boolean expectedStatus) {
        System.out.println("Validating the response");
        if(200 != resp.getStatus()){
            System.out.println("Status is not 200 - OK. actual status is: "+resp.getStatus());
            return false;
        }
        try {
            if (FORMAT_STRING.equalsIgnoreCase(format)) {
                System.out.println("Validating the String response.");
                if (expectedStatus) {
                    Assert.assertTrue(resp.getContentAsString().contains("Success"), "Content does not contain success status.");
                } else {
                    Assert.assertTrue(resp.getContentAsString().contains("Error"), "Content does not contain error status.");
                }
            } else if (FORMAT_JSON.equalsIgnoreCase(format)) {
                System.out.println("Validating the Json response.");
                final String content = resp.getContentAsString();
                Assert.assertNotNull(content, "Response content is null");
                if (expectedStatus){
                    Assert.assertTrue(content.startsWith("{\"Success\""), "Content does not contain success status.");
                } else {
                    Assert.assertTrue(content.startsWith("{\"Error\""), "Content does not contain Error status.");
                }
                Assert.assertTrue(content.endsWith("}"), "Json does not end with '}'");
                Assert.assertTrue(content.contains(","), "Json does not contain ','");
            } else {
                System.out.println("Unknown format: " + format);
                return false;
            }
            System.out.println("Response is valid");
            return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.out.println(e.toString());
            return false;
        }
    }

    private void printResponseData(MockHttpServletResponse resp) {
        System.out.println("Header names: "+resp.getHeaderNames());
        System.out.println("Content-Type: "+resp.getHeader("Content-Type"));
        System.out.println("Buffer size: "+resp.getBufferSize());
        System.out.println("Content length: "+resp.getContentLength());
        try {
            System.out.println("Status: "+resp.getStatus());
            System.out.println("Content as String: "+resp.getContentAsString());
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported exception.");
            e.printStackTrace();
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

    private MockHttpServletRequest buildSendMessageRequest(String method, String url, String userId, String password, String msg, String format, String encodingStr, String senderId, String[] to) {
        MockHttpServletRequest req = new MockHttpServletRequest();

        req.setMethod(method);
        req.setParameter("userid", userId);
        req.setParameter("password", password);
        req.setParameter("msg", msg);
        req.setParameter("sender", senderId);
        req.setParameter("to", to);
        req.setParameter("format", format);
        req.setParameter("encoding", encodingStr);
        req.setRequestURI(url);
        return req;
    }

    private MockHttpServletRequest buildGetMessageIdStatusRequest(String method, String url, String userId, String password, String msgId, String format) {
        MockHttpServletRequest req = new MockHttpServletRequest();

        req.setMethod(method);
        req.setParameter("userid", userId);
        req.setParameter("password", password);
        req.setParameter("msgid", msgId);
        if(format != null ){
            req.setParameter("format", format);
        }
        req.setRequestURI(url);
        return req;
    }
}
