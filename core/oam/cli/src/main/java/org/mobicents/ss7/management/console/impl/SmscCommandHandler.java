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
public class SmscCommandHandler extends CommandHandlerWithHelp {

	static final Tree commandTree = new Tree("smsc");
	static {
		Node parent = commandTree.getTopNode();
		
		Node sip = parent.addChild("sip");
		sip.addChild("modify");
		sip.addChild("show");

		Node set = parent.addChild("set");
        set.addChild("defaultton");
        set.addChild("defaultnpi");
        set.addChild("defaultvalidityperiodhours");
        set.addChild("deliverypause");
        set.addChild("esmedefaultcluster");
        set.addChild("generatereceiptcdr");
        set.addChild("generatecdr");
        Node smppencodingforgsm7 = set.addChild("smppencodingforgsm7");
        smppencodingforgsm7.addChild("utf8");
        smppencodingforgsm7.addChild("unicode");
        Node smppencodingforucs2 = set.addChild("smppencodingforucs2");
        smppencodingforucs2.addChild("utf8");
        smppencodingforucs2.addChild("unicode");

		Node get = parent.addChild("get");
        get.addChild("defaultton");
        get.addChild("defaultnpi");
        get.addChild("defaultvalidityperiodhours");
        get.addChild("deliverypause");
        get.addChild("esmedefaultcluster");
        get.addChild("generatereceiptcdr");
        get.addChild("generatecdr");

        Node remove = parent.addChild("remove");
        remove.addChild("esmedefaultcluster");

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
