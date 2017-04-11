package org.mobicents.smsc.slee.services.http.server.tx;

import org.junit.Ignore;
import org.mobicents.smsc.domain.HttpUsersManagement;
import org.mobicents.smsc.domain.MProcManagement;
import org.mobicents.smsc.domain.SmscPropertiesManagement;
import org.mobicents.smsc.domain.StoreAndForwordMode;
import org.mobicents.smsc.slee.resources.persistence.PersistenceRAInterfaceProxy;
import org.mobicents.smsc.slee.services.http.server.tx.enums.RequestParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletResponse;
import javax.slee.ActivityContextInterface;
import javax.slee.SLEEException;
import javax.slee.SbbLocalObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

/**
 * Created by tpalucki on 05.09.16.
 *
 * @author Tomasz Pałucki
 */
public class TxHttpServerSbbTest {

    private TxHttpServerSbbProxy sbb;
    private PersistenceRAInterfaceProxy pers;
    private boolean cassandraDbInited;

    // test constants
    private static final String ENCODING_UCS2 = "UCS2";
    private static final String ENCODING_GSM7 = "GSM7";

    private static final String BODY_ENCODING_UTF8 = "UTF8";
    private static final String BODY_ENCODING_UTF16 = "UTF16";

    private static final String FORMAT_STRING = "String";
    private static final String FORMAT_JSON = "json";

    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";

    private static final String SENDER_ID_DEFAULT = "1231241243";
    private static final String SENDER_ALPHANUMERIC = "ABC1231241243";
    private static final String MESSAGE_DEFAULT = "SMS message ;P";
    private static final String MESSAGE_TEXT_WITH_LINK = "SMS=message&emot=;P&a=b";

    private static final String SENDER_TON_DEFAULT = "1";
    private static final String SENDER_NPI_DEFAULT = "1";

    private static final String SENDER_TON_INCORRECT = "9d";
    private static final String SENDER_NPI_INCORRECT = "20";

    private static final String PASSWORD_DEFAULT = "password";
    private static final String USER_DEFAULT = "user_4321";

    private static final String USER_INCORRECT = "user_6584";
    private static final String PASSWORD_INCORRECT = "password345";

    private static final String[] TO_MULTIPLE = {"123456789", "111222333", "123123123"};
    private static final String[] TO_ONE = {"123456789"};
    private static final String[] TO_INCORRECT = {"123tr4"};
    private static final String[] TO_EMPTY = {""};
    private static final String[] TO_ONE_ALPHANUMERIC = {"ABC"};
    private static final String[] TO_MULTIPLE_PLUS_ALPHANUMERIC = {"123456789", "111222333", "123123123", "ABC"};
    private static final String[] TO_MULTIPLE_PLUS_EMPTY = {"123456789", "111222333", "123123123", "", "   "};

    private static final String URL_SEND_MESSAGE = "http://test.pl/restcomm/sendSms";
    private static final String URL_SEND_MESSAGE_FAKE = "http://test.pl/sendMessageFake";
    private static final String URL_GET_MESSAGE_ID_STATUS = "http://test.pl/restcomm/msgQuery";

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

        SmscPropertiesManagement.getInstance("Test");
        SmscPropertiesManagement.getInstance().setSmscStopped(false);
        SmscPropertiesManagement.getInstance().setStoreAndForwordMode(StoreAndForwordMode.normal);

        MProcManagement.getInstance("Test");

        HttpUsersManagement usersManagement = HttpUsersManagement.getInstance("Test");
        if(usersManagement.getHttpUserByName("user_4321") == null) {
            //add if not exists
            usersManagement.createHttpUser("user_4321", "password", 0);
        }

        this.sbb = new TxHttpServerSbbProxy(this.pers);

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

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"),
                FORMAT_STRING, ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);
        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();
        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_STRING, true, 1), "Response is not valid");
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

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"),
                FORMAT_STRING, ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void sendMessageGETForbiddenTest() throws UnsupportedEncodingException {
        System.out.println("sendMessageGETForbiddenTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"),
                FORMAT_STRING, ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.setForbidden(true);
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();
        printResponseData(resp);
        Assert.assertFalse(isValid(resp, FORMAT_STRING, false), "Response is not valid");
        this.sbb.setForbidden(false);
    }

    @Test
    public void sendMessageGETStringIncorrectTonTest() throws Exception {
        System.out.println("sendMessageGETStringIncorrectTonTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"),
                FORMAT_STRING, ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_INCORRECT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);
        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();
        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_STRING, false, 1), "Response is not valid");
    }

    @Test
    public void sendMessageGETStringIncorrectNpiTest() throws Exception {
        System.out.println("sendMessageGETStringIncorrectNpiTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, URLEncoder.encode(MESSAGE_DEFAULT, "UTF-8"),
                FORMAT_STRING, ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_INCORRECT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);
        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();
        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_STRING, false, 1), "Response is not valid");
    }

    @Test
    public void incorrectToAdressTest() {
        System.out.println("incorrectToAdressTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_INCORRECT);
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
    public void emptyToAdressTest() {
        System.out.println("emptyToAdressTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_EMPTY);
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
    public void wrongServiceTest() {
        System.out.println("wrongServiceTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE_FAKE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_STRING,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
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
    public void sendMessageGETJsonSuccessTest() {
        System.out.println("sendMessageGETJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true, 1), "Response is not valid");
    }

    @Test
    public void sendMessageGETJsonErrorTest() {
        System.out.println("sendMessageGETJsonErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void sendMessageGETJsonWrongUsernameTest() {
        System.out.println("sendMessageGETJsonWrongUsernameTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_INCORRECT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void sendMessageGETJsonWrongPasswordTest() {
        System.out.println("sendMessageGETJsonWrongPasswordTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_INCORRECT, PASSWORD_INCORRECT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void sendMessagePOSTStringSuccessTest() {
        System.out.println("sendMessagePOSTStringSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_STRING,
                ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true, 1), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTStringWithMessageLinkSuccessTest() {
        System.out.println("sendMessagePOSTStringWithMessageLinkSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_TEXT_WITH_LINK, FORMAT_STRING,
                ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true, 1), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTStringErrorTest() {
        System.out.println("sendMessagePOSTStringErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, null, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_STRING,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void sendMessagePOSTJsonSuccessTest() {
        System.out.println("sendMessagePOSTJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true, 1), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTJsonToMultipleSuccessTest() {
        System.out.println("sendMessagePOSTJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true, 3), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTJsonWrongRecipientsAmountErrorTest() {
        System.out.println("sendMessagePOSTJsonWrongRecipientsAmountErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_GSM7, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertFalse(isValid(resp, FORMAT_JSON, true, 4), "Response is not valid");
    }

    @Test
    public void sendMessagePOSTJsonErrorTest() {
        System.out.println("sendMessagePOSTJsonErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, null, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
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

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, urlEncoded, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE);
//        request.setContentType("application/x-www-form-urlencoded");
//        request.setCharacterEncoding("UTF-8");
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_JSON, true, 3), "Response is not valid");
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

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, urlEncoded, FORMAT_STRING,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true, 3), "Response is not valid");
    }

    @Test
    public void sendArabicMessagePOSTSuccessStringTest() throws UnsupportedEncodingException {
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

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, urlEncoded, FORMAT_STRING,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertTrue(isValid(resp, FORMAT_STRING, true, 3), "Response is not valid");
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
    public void getStatusGETSuccessTest() {
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

        // perform the action
        this.sbb.onHttpGet(event, aci);
        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();
        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_STRING, true), "Response is not valid");
    }

    @Test
    public void getStatusGETJsonSuccessTest() {
        System.out.println("getStatusGETJsonSuccessTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_GET, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_ID, FORMAT_JSON);
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
    public void getStatusGETJsonErrorTest() {
        System.out.println("getStatusGETJsonErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_GET, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, PASSWORD_DEFAULT, null, FORMAT_JSON);
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
    public void getStatusGETPasswordNullErrorTest() {
        System.out.println("getStatusGETErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_GET, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, null, MESSAGE_ID, FORMAT_STRING);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);

        Assert.assertEquals(resp.getStatus(), HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void getStatusPOSTStringErrorTest() {
        System.out.println("getStatusPOSTStringErrorTest");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildGetMessageIdStatusRequest(METHOD_POST, URL_GET_MESSAGE_ID_STATUS, USER_DEFAULT, PASSWORD_DEFAULT, null, FORMAT_STRING);
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
    public void alphanumericSenderAddressTest_GET() {
        System.out.println("alphanumericSenderAddressTest_GET");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ALPHANUMERIC, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE);
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
    public void alphanumericToAddressTest_GET() {
        System.out.println("alphanumericToAddressTest_GET");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE_ALPHANUMERIC);
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
    public void alphanumericToAddressTest_POST() {
        System.out.println("alphanumericToAddressTest_POST");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_ONE_ALPHANUMERIC);
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
    public void multiplePlusAlphanumericToAddressTest_GET() {
        System.out.println("multiplePlusAlphanumericToAddressTest_GET");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE_PLUS_ALPHANUMERIC);
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
    public void multiplePlusEmptyToAddressTest_GET() {
        System.out.println("multiplePlusEmptyToAddressTest_GET");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_GET, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE_PLUS_EMPTY);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpGet(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_JSON, true, 3), "Response is not valid");
    }

    @Test
    public void multiplePlusEmptyToAddressTest_POST() {
        System.out.println("multiplePlusEmptyToAddressTest_GET");
        if (!this.cassandraDbInited) {
//            Assert.fail("Cassandra DB is not inited");
            return;
        }
        //  prepare
        ActivityContextInterface aci = new HttpActivityContextInterface();
        MockHttpServletRequestEvent event = new MockHttpServletRequestEvent();

        MockHttpServletRequest request = buildSendMessageRequest(METHOD_POST, URL_SEND_MESSAGE, USER_DEFAULT, PASSWORD_DEFAULT, MESSAGE_DEFAULT, FORMAT_JSON,
                ENCODING_UCS2, BODY_ENCODING_UTF8, SENDER_ID_DEFAULT, SENDER_TON_DEFAULT, SENDER_NPI_DEFAULT, TO_MULTIPLE_PLUS_EMPTY);
        event.setRequest(request);

        MockHttpServletResponse response = new MockHttpServletResponse();
        event.setResponse(response);

        // perform the action
        this.sbb.onHttpPost(event, aci);

        MockHttpServletResponse resp = (MockHttpServletResponse) event.getResponse();

        printResponseData(resp);
        Assert.assertTrue(isValid(resp, FORMAT_JSON, true, 3), "Response is not valid");
    }

    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------
    //-END OF GET STATUS FUNCTIONALITY TESTS
    //------------------------------------------------------------------------------------------------------------------
    //------------------------------------------------------------------------------------------------------------------

    private boolean isValid(MockHttpServletResponse resp, String format, boolean expectedStatus) {
        return isValid(resp, format, expectedStatus, null);
    }

    private boolean isValid(MockHttpServletResponse resp, String format, boolean expectedStatus, Integer count) {
        System.out.println("Validating the response");
        if (200 != resp.getStatus()) {
            System.out.println("Status is not 200 - OK. actual status is: " + resp.getStatus());
            return false;
        }
        try {
            final String content = resp.getContentAsString();
            if (FORMAT_STRING.equalsIgnoreCase(format)) {
                System.out.println("Validating the String response.");
                if (expectedStatus) {
                    Assert.assertTrue(content.contains("Success"), "Content does not contain success status.");
                } else {
                    Assert.assertTrue(content.contains("Error"), "Content does not contain error status.");
                }
            } else if (FORMAT_JSON.equalsIgnoreCase(format)) {
                System.out.println("Validating the Json response.");

                Assert.assertNotNull(content, "Response content is null");
                if (expectedStatus) {
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
            if (count != null && content.split(",").length - 1 != count) {
                System.out.println("Response contains wrong number of recipients");
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
        System.out.println("Header names: " + resp.getHeaderNames());
        System.out.println("Content-Type: " + resp.getHeader("Content-Type"));
        System.out.println("Buffer size: " + resp.getBufferSize());
        System.out.println("Content length: " + resp.getContentLength());
        try {
            System.out.println("Status: " + resp.getStatus());
            System.out.println("Content as String: " + resp.getContentAsString());
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

    private MockHttpServletRequest buildSendMessageRequest(String method, String url, String userId, String password, String msg, String format, String smscEncoding,
                                                           String messageBodyEncoding, String sender, String senderTon, String senderNpi, String[] to) {
        MockHttpServletRequest req = new MockHttpServletRequest();

        req.setMethod(method);
        if(!method.equals("POST")) {
            req.setParameter(RequestParameter.USER_ID.getName(), userId);
            req.setParameter(RequestParameter.PASSWORD.getName(), password);
            req.setParameter(RequestParameter.MESSAGE_BODY.getName(), msg);
            req.setParameter(RequestParameter.SENDER.getName(), sender);
            req.setParameter(RequestParameter.TO.getName(), Arrays.toString(to).replaceAll("\\[", "").replaceAll("\\]", ""));
            req.setParameter(RequestParameter.FORMAT.getName(), format);
            req.setParameter(RequestParameter.SMSC_ENCODING.getName(), smscEncoding);
            req.setParameter(RequestParameter.MESSAGE_BODY_ENCODING.getName(), messageBodyEncoding);
            req.setParameter(RequestParameter.SENDER_TON.getName(), senderTon);
            req.setParameter(RequestParameter.SENDER_NPI.getName(), senderNpi);
        } else {
            String params = RequestParameter.USER_ID.getName() + "=" + getValue(userId) +
                    "&" + RequestParameter.PASSWORD.getName() + "=" + getValue(password) +
                    "&" + RequestParameter.MESSAGE_BODY.getName() + "=" + getValue(msg) +
                    "&" + RequestParameter.SENDER.getName() + "=" + getValue(sender) +
                    "&" + RequestParameter.TO.getName() + "=" + Arrays.toString(to).replaceAll("\\[", "").replaceAll("\\]", "") ;
            if (format != null) {
                params += "&" + RequestParameter.FORMAT.getName() + "=" + getValue(format);
            }
            if (smscEncoding != null) {
                params += "&" + RequestParameter.SMSC_ENCODING.getName() + "=" + getValue(smscEncoding);
            }
            if (messageBodyEncoding != null) {
                params += "&" + RequestParameter.MESSAGE_BODY_ENCODING.getName() + "=" + getValue(messageBodyEncoding);
            }
            if (senderTon != null) {
                params += "&" + RequestParameter.SENDER_TON.getName() + "=" + getValue(senderTon);
            }
            if (senderNpi != null) {
                params += "&" + RequestParameter.SENDER_NPI.getName() + "=" + getValue(senderNpi);
            }

            try {
                req.setContent(params.getBytes("UTF8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        req.setRequestURI(url);
        return req;
    }

    private String getValue(String str){
        return str==null ? "" : str;
    }

    private MockHttpServletRequest buildGetMessageIdStatusRequest(String method, String url, String userId, String password, String msgId, String format) {
        MockHttpServletRequest req = new MockHttpServletRequest();

        req.setMethod(method);
        req.setParameter(RequestParameter.USER_ID.getName(), userId);
        req.setParameter(RequestParameter.PASSWORD.getName(), password);
        req.setParameter(RequestParameter.MESSAGE_ID.getName(), msgId);
        if (format != null) {
            req.setParameter(RequestParameter.FORMAT.getName(), format);
        }
        req.setRequestURI(url);
        return req;
    }
}
