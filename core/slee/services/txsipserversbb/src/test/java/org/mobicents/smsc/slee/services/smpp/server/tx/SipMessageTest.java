package org.mobicents.smsc.slee.services.smpp.server.tx;

import java.util.ArrayList;
import java.util.Properties;

import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;

public class SipMessageTest implements SipListener {

	private static SipProvider sipProvider;

	private static AddressFactory addressFactory;

	private static MessageFactory messageFactory;

	private static HeaderFactory headerFactory;

	private static SipStack sipStack;

	private ContactHeader contactHeader;

	private ListeningPoint udpListeningPoint;

	private ClientTransaction inviteTid;

	private Dialog dialog;

	public SipMessageTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SipMessageTest test = new SipMessageTest();
		test.init();

	}

	private void test() {
		SipFactory sipFactory = SipFactory.getInstance();
	}

	public void init() {
		SipFactory sipFactory = null;
		sipStack = null;
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		Properties properties = new Properties();
		// If you want to try TCP transport change the following to
		String transport = "udp";
		String peerHostPort = "127.0.0.1:5060";
		properties.setProperty("javax.sip.OUTBOUND_PROXY", peerHostPort + "/" + transport);
		// If you want to use UDP then uncomment this.
		properties.setProperty("javax.sip.STACK_NAME", "shootist");

		// The following properties are specific to nist-sip
		// and are not necessarily part of any other jain-sip
		// implementation.
		// You can set a max message size for tcp transport to
		// guard against denial of service attack.
		properties.setProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE", "1048576");
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "shootistdebuglog.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "shootistlog.txt");

		// Drop the client connection after we are done with the transaction.
		properties.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
		// Set to 0 in your production code for max speed.
		// You need 16 for logging traces. 32 for debug + traces.
		// Your code will limp at 32 but it is best for debugging.
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "DEBUG");

		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);
			System.out.println("createSipStack " + sipStack);
		} catch (PeerUnavailableException e) {
			// could not find
			// gov.nist.jain.protocol.ip.sip.SipStackImpl
			// in the classpath
			e.printStackTrace();
			System.err.println(e.getMessage());
			System.exit(0);
		}

		try {
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
			udpListeningPoint = sipStack.createListeningPoint("127.0.0.1", 5066, "udp");
			sipProvider = sipStack.createSipProvider(udpListeningPoint);
			SipMessageTest listener = this;
			sipProvider.addSipListener(listener);

			String fromName = "123456789";
			String fromSipAddress = "127.0.0.1:5066";
			String fromDisplayName = "123456789";

			String toSipAddress = "127.0.0.1:5060";
			String toUser = "6666";
			String toDisplayName = "6666";

			// create >From Header
			SipURI fromAddress = addressFactory.createSipURI(fromName, fromSipAddress);

			Address fromNameAddress = addressFactory.createAddress(fromAddress);
			fromNameAddress.setDisplayName(fromDisplayName);
			FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");

			// create To Header
			SipURI toAddress = addressFactory.createSipURI(toUser, toSipAddress);
			Address toNameAddress = addressFactory.createAddress(toAddress);
			toNameAddress.setDisplayName(toDisplayName);
			ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

			// create Request URI
			SipURI requestURI = addressFactory.createSipURI(toUser, peerHostPort);

			// Create ViaHeaders

			ArrayList viaHeaders = new ArrayList();
			ViaHeader viaHeader = headerFactory.createViaHeader("127.0.0.1", sipProvider.getListeningPoint(transport)
					.getPort(), transport, null);

			// add via headers
			viaHeaders.add(viaHeader);

			// Create ContentTypeHeader
			ContentTypeHeader contentTypeHeader = headerFactory.createContentTypeHeader("text", "plain");

			// Create a new CallId header
			CallIdHeader callIdHeader = sipProvider.getNewCallId();

			// Create a new Cseq header
			CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1L, Request.MESSAGE);

			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

			// Create the request.
			Request request = messageFactory.createRequest(requestURI, Request.MESSAGE, callIdHeader, cSeqHeader,
					fromHeader, toHeader, viaHeaders, maxForwards);
			// Create contact headers
			String host = "127.0.0.1";

			SipURI contactUrl = addressFactory.createSipURI(fromName, host);
			contactUrl.setPort(udpListeningPoint.getPort());

			// Create the contact name address.
			SipURI contactURI = addressFactory.createSipURI(fromName, host);
			contactURI.setPort(sipProvider.getListeningPoint(transport).getPort());

			Address contactAddress = addressFactory.createAddress(contactURI);

			// Add the contact address.
			contactAddress.setDisplayName(fromName);

			contactHeader = headerFactory.createContactHeader(contactAddress);
			request.addHeader(contactHeader);

			// UDH Header
			Header udhHeader = headerFactory.createHeader("X-SMS-UDH", "06050413011301");
			request.addHeader(udhHeader);

			// Add the extension header.
			Header extensionHeader = headerFactory.createHeader("My-Header", "my header value");
			request.addHeader(extensionHeader);

			String s = this.getBody();
			byte[] contents = s.getBytes();
			request.setContent(contents, contentTypeHeader);

			// Create the client transaction.
			inviteTid = sipProvider.getNewClientTransaction(request);

			// send the request out.
			inviteTid.sendRequest();

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
	}

	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {
		System.out.println("dialogTerminatedEvent");
	}

	@Override
	public void processIOException(IOExceptionEvent exceptionEvent) {
		System.out.println("IOException happened for " + exceptionEvent.getHost() + " port = "
				+ exceptionEvent.getPort());

	}

	@Override
	public void processRequest(RequestEvent event) {
		System.out.println("processRequest " + event);
	}

	@Override
	public void processResponse(ResponseEvent event) {
		System.out.println("processResponse " + event);
	}

	@Override
	public void processTimeout(TimeoutEvent event) {
		System.out.println("processTimeout " + event);
	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {
		System.out.println("processTransactionTerminated " + arg0);
	}

	private String getBody() {
		String s = "31631d370615423f687645077a032943210239674016267176461371106d0918230b4100037b6a7e7d49530161702151292927417970401b6b5f445c112b61575359184a1970090b132c7e5765500f15615b41107111110131112257706b43020e6c512b71264151514257502b23042771615b11520772200a0c6308224c63282a6c046236";

		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return new String(data);
	}

}
