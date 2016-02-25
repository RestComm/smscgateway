/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
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

package org.mobicents.smsc.mproc.impl;

import org.mobicents.smsc.mproc.MProcRuleFactory;
import org.mobicents.smsc.mproc.MProcRule;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcRuleFactoryDefault implements MProcRuleFactory {

    public static final String RULE_CLASS_NAME = "mproc";

    @Override
    public String getRuleClassName() {
        return RULE_CLASS_NAME;
    }

    @Override
    public MProcRule createMProcRuleInstance() {
        return new MProcRuleDefaultImpl();
    }

}
