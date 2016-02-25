/**
 * 
 */
package org.mobicents.smsc.domain;

import java.util.List;

import org.mobicents.smsc.cassandra.PersistenceException;
import org.mobicents.smsc.cassandra.SmsRoutingRuleType;
import org.mobicents.smsc.library.DbSmsRoutingRule;

/**
 * @author Amit Bhayani
 * 
 */
public interface DatabaseSmsRoutingRuleMBean extends SmsRoutingRule {

	public void updateDbSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address, int networkId, String systemId)
			throws PersistenceException;

	public void deleteDbSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address, int networkId)
			throws PersistenceException;

	public DbSmsRoutingRule getSmsRoutingRule(SmsRoutingRuleType dbSmsRoutingRuleType, String address, int networkId)
			throws PersistenceException;

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(SmsRoutingRuleType dbSmsRoutingRuleType)
			throws PersistenceException;

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(SmsRoutingRuleType dbSmsRoutingRuleType, String lastAdress)
			throws PersistenceException;

}
