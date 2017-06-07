/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.ss7.management.console.impl;

import org.mobicents.ss7.management.console.CommandContext;
import org.mobicents.ss7.management.console.CommandHandlerWithHelp;
import org.mobicents.ss7.management.console.Tree;
import org.mobicents.ss7.management.console.Tree.Node;

/**
 * @author amit bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscCommandHandler extends CommandHandlerWithHelp {

	static final Tree commandTree = new Tree("smsc");
	static {
		Node parent = commandTree.getTopNode();

		Node sip = parent.addChild("sip");
		sip.addChild("modify");
		sip.addChild("show");

		Node set = parent.addChild("set");
		set.addChild("scgt");
		set.addChild("scssn");
		set.addChild("hlrssn");
		set.addChild("mscssn");
        Node gti = set.addChild("gti");
        set.addChild("tt");
        set.addChild("maxmapv");
        set.addChild("defaultvalidityperiodhours");
        set.addChild("maxvalidityperiodhours");
        set.addChild("defaultton");
        set.addChild("defaultnpi");
        set.addChild("subscriberbusyduedelay");
        set.addChild("firstduedelay");
        set.addChild("secondduedelay");
        set.addChild("maxduedelay");
        set.addChild("duedelaymultiplicator");
        set.addChild("maxmessagelengthreducer");
//        set.addChild("smshomerouting");
        set.addChild("correlationidlivetime");
        set.addChild("revisesecondsonsmscstart");
        set.addChild("processingsmssettimeout");
        set.addChild("generatereceiptcdr");
        set.addChild("generatecdr");
        set.addChild("generatearchivetable");
        Node storeAndForwordMode = set.addChild("storeandforwordmode");
        Node mocharging = set.addChild("mocharging");
        Node hrcharging = set.addChild("hrcharging");
        Node txsmppcharging = set.addChild("txsmppcharging");
        Node txsipcharging = set.addChild("txsipcharging");
        set.addChild("diameterdestrealm");
        set.addChild("diameterdesthost");
        set.addChild("diameterdestport");
        set.addChild("diameterusername");
        set.addChild("removinglivetablesdays");
        set.addChild("removingarchivetablesdays");
        set.addChild("hrhlrnumber");
        set.addChild("hrsribypass");
        set.addChild("sriresponselivetime");
        set.addChild("nationallanguagesingleshift");
        set.addChild("nationallanguagelockingshift");
        set.addChild("httpdefaultsourceton");
        set.addChild("httpdefaultsourcenpi");
        set.addChild("httpdefaultdestton");
        set.addChild("httpdefaultdestnpi");
        set.addChild("httpdefaultnetworkid");
        set.addChild("httpdefaultmessagingmode");
        set.addChild("httpdefaultrddeliveryreceipt");
        set.addChild("httpdefaultrdintermediatenotification");
        set.addChild("httpdefaultdatacoding");
        set.addChild("modefaultmessagingmode");
        set.addChild("hrdefaultmessagingmode");
        set.addChild("sipdefaultmessagingmode");
        set.addChild("vpprolong");

        txsmppcharging.addChild("None");
        txsmppcharging.addChild("Selected");
        txsmppcharging.addChild("All");
        
        txsipcharging.addChild("None");
        txsipcharging.addChild("Selected");
        txsipcharging.addChild("All");

        gti.addChild("0001");
        gti.addChild("0001");
        gti.addChild("0011");
        gti.addChild("0100");

        mocharging.addChild("accept");
        mocharging.addChild("reject");
        mocharging.addChild("diameter");

        hrcharging.addChild("accept");
        hrcharging.addChild("reject");
        hrcharging.addChild("diameter");

        storeAndForwordMode.addChild("normal");
        storeAndForwordMode.addChild("fast");

        Node smppencodingforgsm7 = set.addChild("smppencodingforgsm7");
        smppencodingforgsm7.addChild("utf8");
        smppencodingforgsm7.addChild("unicode");
        smppencodingforgsm7.addChild("gsm7");
        Node smppencodingforucs2 = set.addChild("smppencodingforucs2");
        smppencodingforucs2.addChild("utf8");
        smppencodingforucs2.addChild("unicode");
        smppencodingforucs2.addChild("gsm7");
        set.addChild("dbhosts");
        set.addChild("dbport");
        set.addChild("keyspacename");
        set.addChild("clustername");
        set.addChild("fetchperiod");
        set.addChild("fetchmaxrows");
        set.addChild("maxactivitycount");
//        set.addChild("cdrdatabaseexportduration");
        set.addChild("esmedefaultcluster");
        set.addChild("deliverypause");
        set.addChild("receiptsdisabling");
        set.addChild("incomereceiptsprocessing");
        set.addChild("orignetworkidforreceipts");
        set.addChild("cassandrauser");
        set.addChild("cassandrapass");

		Node get = parent.addChild("get");
		get.addChild("scgt");
		get.addChild("scssn");
		get.addChild("hlrssn");
		get.addChild("mscssn");
        get.addChild("gti");
        get.addChild("tt");
		get.addChild("maxmapv");
        get.addChild("defaultvalidityperiodhours");
        get.addChild("maxvalidityperiodhours");
        get.addChild("defaultton");
        get.addChild("defaultnpi");
        get.addChild("subscriberbusyduedelay");
        get.addChild("firstduedelay");
        get.addChild("secondduedelay");
        get.addChild("maxduedelay");
        get.addChild("duedelaymultiplicator");
        get.addChild("maxmessagelengthreducer");
//        get.addChild("smshomerouting");
        get.addChild("correlationidlivetime");
        get.addChild("revisesecondsonsmscstart");
        get.addChild("processingsmssettimeout");
        get.addChild("generatereceiptcdr");
        get.addChild("generatecdr");
        get.addChild("generatearchivetable");
        get.addChild("storeandforwordmode");
        get.addChild("mocharging");
        get.addChild("hrcharging");
        get.addChild("txsmppcharging");
        get.addChild("txsipcharging");
        get.addChild("diameterdestrealm");
        get.addChild("diameterdesthost");
        get.addChild("diameterdestport");
        get.addChild("diameterusername");
        get.addChild("removinglivetablesdays");
        get.addChild("removingarchivetablesdays");
        get.addChild("hrhlrnumber");
        get.addChild("hrsribypass");
        get.addChild("sriresponselivetime");
        get.addChild("nationallanguagesingleshift");
        get.addChild("nationallanguagelockingshift");
        get.addChild("httpdefaultsourceton");
        get.addChild("httpdefaultsourcenpi");
        get.addChild("httpdefaultdestton");
        get.addChild("httpdefaultdestnpi");
        get.addChild("httpdefaultnetworkid");
        get.addChild("httpdefaultmessagingmode");
        get.addChild("httpdefaultrddeliveryreceipt");
        get.addChild("httpdefaultrdintermediatenotification");
        get.addChild("httpdefaultdatacoding");
        get.addChild("modefaultmessagingmode");
        get.addChild("hrdefaultmessagingmode");
        get.addChild("sipdefaultmessagingmode");
        get.addChild("vpprolong");

        Node smppencodingforgsm72 = get.addChild("smppencodingforgsm7");
        Node smppencodingforucs22 = get.addChild("smppencodingforucs2");
        get.addChild("dbhosts");
        get.addChild("dbport");
        get.addChild("keyspacename");
        get.addChild("clustername");
        get.addChild("fetchperiod");
        get.addChild("fetchmaxrows");
        get.addChild("maxactivitycount");
//        get.addChild("cdrdatabaseexportduration");
        get.addChild("esmedefaultcluster");
        get.addChild("deliverypause");
        get.addChild("receiptsdisabling");
        get.addChild("incomereceiptsprocessing");
        get.addChild("orignetworkidforreceipts");
        get.addChild("cassandrauser");
        get.addChild("cassandrapass");

        Node remove = parent.addChild("remove");
        remove.addChild("esmedefaultcluster");
        remove.addChild("hrhlrnumber");
        remove.addChild("hrsribypass");

		Node smppServer = parent.addChild("smppserver");

        Node databaseRule = parent.addChild("databaserule");
        databaseRule.addChild("update");
        databaseRule.addChild("delete");
        databaseRule.addChild("get");
        databaseRule.addChild("getrange");
        
        Node mapcache = parent.addChild("mapcache");
        mapcache.addChild("get");
        mapcache.addChild("set");
        mapcache.addChild("clear");

        Node stat = parent.addChild("stat");
        stat.addChild("get");

        Node updateccmccmnstable = parent.addChild("updateccmccmnstable");

        Node hrccmccmnc = parent.addChild("hrccmccmnc");
        hrccmccmnc.addChild("add");
        hrccmccmnc.addChild("modify");
        hrccmccmnc.addChild("remove");
        hrccmccmnc.addChild("show");

        Node mproc = parent.addChild("mproc");
        mproc.addChild("add");
        mproc.addChild("modify");
        mproc.addChild("remove");
        mproc.addChild("show");

        Node httpuser = parent.addChild("httpuser");
        httpuser.addChild("add");
        httpuser.addChild("modify");
        httpuser.addChild("remove");
        httpuser.addChild("show");

        Node skipUnsentMessages = parent.addChild("skipunsentmessages");

	};

	public SmscCommandHandler() {
		super(commandTree, CONNECT_MANDATORY_FLAG);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.ss7.management.console.CommandHandler#isValid(java.lang
	 * .String)
	 */
	@Override
	public void handle(CommandContext ctx, String commandLine) {
		// TODO Validate command
		if (commandLine.contains("--help")) {
			this.printHelp(commandLine, ctx);
			return;
		}

		ctx.sendMessage(commandLine);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.ss7.management.console.CommandHandler#isAvailable(org.mobicents
	 * .ss7.management.console.CommandContext)
	 */
	@Override
	public boolean isAvailable(CommandContext ctx) {
		if (!ctx.isControllerConnected()) {
			ctx.printLine("The command is not available in the current context. Please connnect first");
			return false;
		}
		return true;
	}

}
