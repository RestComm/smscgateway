/**
 * 
 */
package org.mobicents.smsc.slee.services.sip.server.rx;

import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author Amit Bhayani
 * 
 */
public class SipTest {

	private static AddressFactory addressFactory;

	/**
	 * 
	 */
	public SipTest() {
		// TODO Auto-generated constructor stub
	}

	@BeforeClass
	public void setup() throws Exception {
		SipFactory sipFactory = SipFactory.getInstance();
		addressFactory = sipFactory.createAddressFactory();
	}

	@Test(groups = { "Base" })
	public void testSipUri() throws Exception {
		String user = "*135%23";
		String fromDisplayName = user;
		String fromSipAddress = "127.0.0.1:5065";

		SipURI fromAddress = addressFactory.createSipURI(user, fromSipAddress);
		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		fromNameAddress.setDisplayName(fromDisplayName);

		System.out.println(fromNameAddress);
	}

}
