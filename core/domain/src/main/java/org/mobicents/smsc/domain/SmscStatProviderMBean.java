/**
 * 
 */
package org.mobicents.smsc.domain;

import java.util.Date;

/**
 * @author Amit Bhayani
 *
 */
public interface SmscStatProviderMBean {
	
	int getMessageInProcess();
	
	int getDueSlotProcessingLag();
	
	long getMessageScheduledTotal();
	
	long getCurrentMessageId();
	
	Date getSmscStartTime();
	
}
