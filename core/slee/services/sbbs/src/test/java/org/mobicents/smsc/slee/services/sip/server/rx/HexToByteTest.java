/**
 * 
 */
package org.mobicents.smsc.slee.services.sip.server.rx;

import org.testng.annotations.Test;

/**
 * @author Amit Bhayani
 *
 */
public class HexToByteTest {
	
	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * 
	 */
	public HexToByteTest() {
		// TODO Auto-generated constructor stub
	}
	
	@Test(groups = { "Base" })
	public void testEncodeDecode() throws Exception {
		String udh = "06050413011301";
		byte[] rawData = hexStringToByteArray(udh);
		
		String recreatedUdh = hexStringToByteArray(rawData);
		
		org.testng.Assert.assertEquals(udh, recreatedUdh);
	}
	
	private byte[] hexStringToByteArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return data;
	}
	
	private String hexStringToByteArray(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
	
	

}
