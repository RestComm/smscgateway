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
	
	long getMessagesPendingInDatabase();
	
	long getCurrentMessageId();
	
	Date getSmscStartTime();
	
}
