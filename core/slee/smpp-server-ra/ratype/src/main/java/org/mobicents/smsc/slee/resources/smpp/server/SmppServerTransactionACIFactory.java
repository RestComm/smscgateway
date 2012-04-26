package org.mobicents.smsc.slee.resources.smpp.server;

public interface SmppServerTransactionACIFactory {
	javax.slee.ActivityContextInterface getActivityContextInterface(SmppServerTransaction txn);
}
