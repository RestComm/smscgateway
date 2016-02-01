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

package org.mobicents.smsc.mproc.testimpl;

import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.smsc.mproc.MProcMessage;
import org.mobicents.smsc.mproc.MProcRuleBaseImpl;
import org.mobicents.smsc.mproc.PostArrivalProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public class MProcRuleTestImpl extends MProcRuleBaseImpl {

    @Override
    public String getRuleClassName() {
        return MProcRuleFactoryTestImpl.CLASS_NAME;
    }


    private static final String PAR1 = "par1";
    private static final String PAR2 = "par2";
    private String par1, par2;

    @Override
    public void setInitialRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);
        if (args.length != 2) {
            throw new Exception("parametersString must contains 2 parameters");
        }
        par1 = args[0];
        par2 = args[1];
    }

    @Override
    public void updateRuleParameters(String parametersString) throws Exception {
        String[] args = splitParametersString(parametersString);
        if (args.length != 2) {
            throw new Exception("parametersString must contains 2 parameters");
        }
        par1 = args[0];
        par2 = args[1];
    }

    @Override
    public String getRuleParameters() {
        return par1 + " " + par2;
    }


    @Override
    public boolean isForPostArrivalState() {
        return true;
    }


    @Override
    public boolean matchesPostArrival(MProcMessage message) {
        if (message.getDestAddr().startsWith(par1))
            return true;
        else
            return false;
    }

    @Override
    public boolean matchesPostImsiRequest(MProcMessage message) {
        return false;
    }

    @Override
    public boolean matchesPostDelivery(MProcMessage message) {
        return false;
    }

    @Override
    public void onPostArrival(PostArrivalProcessor factory, MProcMessage message) throws Exception {
        String destAddr = this.par2 + message.getDestAddr();
        factory.updateMessageDestAddr(message, destAddr);
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleTestImpl> M_PROC_RULE_TEST_XML = new XMLFormat<MProcRuleTestImpl>(
            MProcRuleTestImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleTestImpl mProcRule) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.read(xml, mProcRule);

            mProcRule.par1 = xml.getAttribute(PAR1, "");
            mProcRule.par2 = xml.getAttribute(PAR2, "");
        }

        @Override
        public void write(MProcRuleTestImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            M_PROC_RULE_BASE_XML.write(mProcRule, xml);

            xml.setAttribute(PAR1, mProcRule.par1);
            xml.setAttribute(PAR2, mProcRule.par2);
        }
    };

}
