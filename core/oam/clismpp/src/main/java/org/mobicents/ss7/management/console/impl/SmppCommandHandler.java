/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
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
public class SmppCommandHandler extends CommandHandlerWithHelp {

	static final Tree commandTree = new Tree("smpp");
	static {
		Node parent = commandTree.getTopNode();

		Node esme = parent.addChild("esme");
		esme.addChild("create");
		esme.addChild("modify");
		esme.addChild("delete");
		esme.addChild("start");
		esme.addChild("stop");
		esme.addChild("show");

		Node smppServer = parent.addChild("smppserver");

		Node smppServerSet = smppServer.addChild("set");
		smppServerSet.addChild("port");
		smppServerSet.addChild("bindtimeout");
		smppServerSet.addChild("systemid");
		smppServerSet.addChild("autonegotiateversion");
		smppServerSet.addChild("interfaceversion");
		smppServerSet.addChild("maxconnectionsize");
		smppServerSet.addChild("defaultwindowsize");
		smppServerSet.addChild("defaultwindowwaittimeout");
		smppServerSet.addChild("defaultrequestexpirytimeout");
		smppServerSet.addChild("defaultwindowmonitorinterval");
		smppServerSet.addChild("defaultsessioncountersenabled");

		Node smppServerGet = smppServer.addChild("get");
		smppServerGet.addChild("port");
		smppServerGet.addChild("bindtimeout");
		smppServerGet.addChild("systemid");
		smppServerGet.addChild("autonegotiateversion");
		smppServerGet.addChild("interfaceversion");
		smppServerGet.addChild("maxconnectionsize");
		smppServerGet.addChild("defaultwindowsize");
		smppServerGet.addChild("defaultwindowwaittimeout");
		smppServerGet.addChild("defaultrequestexpirytimeout");
		smppServerGet.addChild("defaultwindowmonitorinterval");
		smppServerGet.addChild("defaultsessioncountersenabled");

	};

	public SmppCommandHandler() {
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
