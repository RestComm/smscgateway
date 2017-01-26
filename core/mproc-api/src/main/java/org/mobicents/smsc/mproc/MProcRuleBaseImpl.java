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

package org.mobicents.smsc.mproc;

import javolution.util.FastList;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
*
* @author sergey vetyutnev
*
*/
public abstract class MProcRuleBaseImpl implements MProcRule {

    private static final String ID = "id";
    
    private int id;

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int val) {
        this.id = val;
    }

    @Override
    public boolean isForPostHrSriState() {
        return false;
    }

    @Override
    public boolean isForPostArrivalState() {
        return false;
    }

    @Override
    public boolean isForPostPreDeliveryState() {
        return false;
    }

    @Override
    public boolean isForPostImsiRequestState() {
        return false;
    }

    @Override
    public boolean isForPostDeliveryState() {
        return false;
    }

    @Override
    public boolean isForPostDeliveryTempFailureState() {
        return false;
    }

    @Override
    public boolean matchesPostHrSri(MProcMessage messageDest) {
        return false;
    }

    @Override
    public boolean matchesPostArrival(MProcMessage messageDest) {
        return false;
    }

    @Override
    public boolean matchesPostPreDelivery(MProcMessage messageDest) {
        return false;
    }

    @Override
    public boolean matchesPostImsiRequest(MProcMessage messageDest) {
        return false;
    }

    @Override
    public boolean matchesPostDelivery(MProcMessage messageDest) {
        return false;
    }

    @Override
    public boolean matchesPostDeliveryTempFailure(MProcMessage message) {
        return false;
    }

    @Override
    public void onPostHrSri(final MProcRuleRaProvider anMProcRuleRa, PostHrSriProcessor factory, MProcMessage message)
            throws Exception {
    }

    @Override
    public void onPostArrival(final MProcRuleRaProvider anMProcRuleRa, PostArrivalProcessor factory, MProcMessage message)
            throws Exception {
    }

    @Override
    public void onPostPreDelivery(final MProcRuleRaProvider anMProcRuleRa, PostPreDeliveryProcessor factory,
            MProcMessage message) throws Exception {
    }

    @Override
    public void onPostImsiRequest(final MProcRuleRaProvider anMProcRuleRa, PostImsiProcessor factory, MProcMessage messages)
            throws Exception {
    }

    @Override
    public void onPostDelivery(final MProcRuleRaProvider anMProcRuleRa, PostDeliveryProcessor factory, MProcMessage message)
            throws Exception {
    }

    @Override
    public void onPostDeliveryTempFailure(final MProcRuleRaProvider anMProcRuleRa, PostDeliveryTempFailureProcessor factory,
            MProcMessage message) throws Exception {
    }

    /**
     * splitting of a message and removing of empty substrings. Space is a splitter between parameters instances.
     *
     * @param parametersString source parameters String
     * @return a list of parameters
     */
    protected String[] splitParametersString(String parametersString) {
        String[] args0 = parametersString.split(" ");
        FastList<String> al1 = new FastList<String>();
        for (int i1 = 0; i1 < args0.length; i1++) {
            String s = args0[i1];
            if (s != null && s.length() > 0)
                al1.add(s);
        }
        String[] args = new String[al1.size()];
        al1.toArray(args);
        return args;
    }

    protected void writeParameter(StringBuilder sb, int parNumber, String name, Object value, String paramaterSplitter,
            String valueSplitter) {
        if (parNumber > 0)
            sb.append(paramaterSplitter);
        sb.append(name);
        sb.append(valueSplitter);
        sb.append(value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("mProc=[class=");
        sb.append(getRuleClassName());
        sb.append(", id=");
        sb.append(id);
        sb.append(", ");
        sb.append(this.getRuleParameters());
        sb.append("]");

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<MProcRuleBaseImpl> M_PROC_RULE_BASE_XML = new XMLFormat<MProcRuleBaseImpl>(MProcRuleBaseImpl.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, MProcRuleBaseImpl mProcRule) throws XMLStreamException {
            mProcRule.id = xml.getAttribute(ID, -1);
        }

        @Override
        public void write(MProcRuleBaseImpl mProcRule, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            xml.setAttribute(ID, mProcRule.id);
        }
    };

}
