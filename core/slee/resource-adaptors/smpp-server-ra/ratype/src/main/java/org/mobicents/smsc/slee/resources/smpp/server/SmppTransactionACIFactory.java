package org.mobicents.smsc.slee.resources.smpp.server;

public interface SmppTransactionACIFactory {
	javax.slee.ActivityContextInterface getActivityContextInterface(SmppTransaction txn);
}
