package org.mobicents.smsc.ihub;

import org.mobicents.protocols.ss7.map.MAPStackImpl;
import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.sccp.impl.SccpStackImpl;

public class MAPSimulator {

	// MAP
	private MAPStackImpl mapStack;
	private MAPProvider mapProvider;

	// SCCP
	private SccpStackImpl sccpStack;

	// SSn
	private int ssn;

	private MAPListener mapListener = null;

	public MAPSimulator() {

	}

	public SccpStackImpl getSccpStack() {
		return sccpStack;
	}

	public void setSccpStack(SccpStackImpl sccpStack) {
		this.sccpStack = sccpStack;
	}

	public int getSsn() {
		return ssn;
	}

	public void setSsn(int ssn) {
		this.ssn = ssn;
	}

	public void start() {
		// Create MAP Stack and register listener
		this.mapStack = new MAPStackImpl(this.sccpStack.getSccpProvider(), this.getSsn());
		this.mapProvider = this.mapStack.getMAPProvider();

		this.mapListener = new MAPListener(this);

		this.mapProvider.addMAPDialogListener(this.mapListener);
		this.mapProvider.getMAPServiceSms().addMAPServiceListener(this.mapListener);

		this.mapProvider.getMAPServiceSms().acivate();

		this.mapStack.start();

	}

	public void stop() {
		this.mapStack.stop();
	}

}
