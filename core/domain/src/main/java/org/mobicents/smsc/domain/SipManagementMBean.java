/**
 * 
 */
package org.mobicents.smsc.domain;

import java.util.List;

/**
 * @author Amit Bhayani
 * 
 */
public interface SipManagementMBean {

	List<Sip> getSips();

	Sip getSipByName(String sipName);

	Sip getSipByClusterName(String sipClusterName);

}
