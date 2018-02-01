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

	public void updateDbSmsRoutingRule(String dbSmsRoutingRuleType, String address, int networkId, String systemId)
			throws PersistenceException;

	public void deleteDbSmsRoutingRule(String dbSmsRoutingRuleType, String address, int networkId)
			throws PersistenceException;

	public DbSmsRoutingRule getSmsRoutingRule(String dbSmsRoutingRuleType, String address, int networkId)
			throws PersistenceException;

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String dbSmsRoutingRuleType)
			throws PersistenceException;

	public List<DbSmsRoutingRule> getSmsRoutingRulesRange(String dbSmsRoutingRuleType, String lastAdress)
			throws PersistenceException;

}
