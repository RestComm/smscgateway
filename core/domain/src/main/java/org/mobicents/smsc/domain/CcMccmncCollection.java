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

package org.mobicents.smsc.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javolution.util.FastMap;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.mobicents.protocols.ss7.map.primitives.ArrayListSerializingBase;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CcMccmncCollection {

    private static final String CC_MCCMNC = "ccMccmnc";
    private static final String CC_MCCMNC_LIST = "ccMccmncList";

    private CcMccmncMap<String, CcMccmncImpl> ccMccmncMap = new CcMccmncMap<String, CcMccmncImpl>();
    private static CcMccmncComparator ccMccmncComparator = new CcMccmncComparator();

    public CcMccmnc findMccmnc(String countryCode) {
        for (FastMap.Entry<String, CcMccmncImpl> e = this.ccMccmncMap.head(), end = this.ccMccmncMap.tail(); (e = e.getNext()) != end;) {
            CcMccmncImpl ccMccmnc = e.getValue();
            if (ccMccmnc.getCountryCode().equals(""))
                return ccMccmnc;
            if (countryCode.startsWith(ccMccmnc.getCountryCode())) {
                return ccMccmnc;
            }
        }
        return null;
    }

    public void addCcMccmnc(CcMccmncImpl ccMccmnc) throws Exception {
        if (ccMccmnc == null) {
            throw new Exception(String.format(SMSCOAMMessages.NULL_ARGUMENT, "ccMccmnc"));
        }
        if (ccMccmnc.getCountryCode() == null) {
            throw new Exception(String.format(SMSCOAMMessages.NULL_ARGUMENT, "ccMccmnc.getCountryCode()"));
        }

        synchronized (this) {
            if (this.ccMccmncMap.containsKey(ccMccmnc.getCountryCode())) {
                throw new Exception(String.format(SMSCOAMMessages.CC_MCCMNC_IS_PRESENT, ccMccmnc.getCountryCode()));
            }

            CcMccmncImpl[] ccMccmncArray = new CcMccmncImpl[(this.ccMccmncMap.size() + 1)];
            int count = 0;

            for (FastMap.Entry<String, CcMccmncImpl> e = this.ccMccmncMap.head(), end = this.ccMccmncMap.tail(); (e = e.getNext()) != end;) {
                CcMccmncImpl ccMccmncTemp1 = (CcMccmncImpl) e.getValue();
                ccMccmncArray[count++] = ccMccmncTemp1;
            }

            // add latest rule
            ccMccmncArray[count++] = ccMccmnc;

            // Sort
            Arrays.sort(ccMccmncArray, ccMccmncComparator);

            CcMccmncMap<String, CcMccmncImpl> newCcMccmnc = new CcMccmncMap<String, CcMccmncImpl>();
            for (int i = 0; i < ccMccmncArray.length; i++) {
                CcMccmncImpl ccMccmncTemp = ccMccmncArray[i];
                newCcMccmnc.put(ccMccmncTemp.getCountryCode(), ccMccmncTemp);
            }
            this.ccMccmncMap = newCcMccmnc;
        }
    }

    public Map<String, CcMccmncImpl> getCcMccmncMap() {
        return this.ccMccmncMap;
    }

    public void modifyCcMccmnc(String countryCode, String mccMnc, String smsc) throws Exception {
        if (countryCode == null) {
            throw new Exception(String.format(SMSCOAMMessages.NULL_ARGUMENT, "countryCode"));
        }

        synchronized (this) {
            CcMccmncImpl el = this.ccMccmncMap.get(countryCode);
            if (el == null) {
                throw new Exception(String.format(SMSCOAMMessages.CC_MCCMNC_NOT_FOUND, countryCode));
            }

            el.setMccMnc(mccMnc);
            el.setSmsc(smsc);
        }
    }

    public void removeCcMccmnc(String countryCode) throws Exception {

        if (countryCode == null) {
            throw new Exception(String.format(SMSCOAMMessages.NULL_ARGUMENT, "countryCode"));
        }

        synchronized (this) {
            if (!this.ccMccmncMap.containsKey(countryCode)) {
                throw new Exception(String.format(SMSCOAMMessages.CC_MCCMNC_NOT_FOUND, countryCode));
            }

            CcMccmncMap<String, CcMccmncImpl> newCcMccmnc = new CcMccmncMap<String, CcMccmncImpl>();
            newCcMccmnc.putAll(this.ccMccmncMap);
            newCcMccmnc.remove(countryCode);
            this.ccMccmncMap = newCcMccmnc;
        }
    }

    public CcMccmnc getCcMccmnc(String countryCode) {
        return this.ccMccmncMap.get(countryCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("CcMccmncCollection=[");
        int i1 = 0;
        for (FastMap.Entry<String, CcMccmncImpl> e = this.ccMccmncMap.head(), end = this.ccMccmncMap.tail(); (e = e.getNext()) != end;) {
            if (i1 == 0)
                i1 = 1;
            else
                sb.append(", ");
            CcMccmncImpl ccMccmns = e.getValue();
            sb.append(ccMccmns.toString());
        }
        sb.append("]");

        return sb.toString();
    }

    /**
     * XML Serialization/Deserialization
     */
    protected static final XMLFormat<CcMccmncCollection> CC_MCCMNC_COLLECTION_XML = new XMLFormat<CcMccmncCollection>(CcMccmncCollection.class) {

        @Override
        public void read(javolution.xml.XMLFormat.InputElement xml, CcMccmncCollection ccMccmnsCollection) throws XMLStreamException {
            CcMccmnsCollection_CcMccmns al = xml.get(CC_MCCMNC_LIST, CcMccmnsCollection_CcMccmns.class);
            if (al != null) {
                CcMccmncImpl[] ccMccmncArray = new CcMccmncImpl[(al.getData().size())];
                al.getData().toArray(ccMccmncArray);
                Arrays.sort(ccMccmncArray, ccMccmncComparator);

                CcMccmncMap<String, CcMccmncImpl> newCcMccmnc = new CcMccmncMap<String, CcMccmncImpl>();
                for (int i = 0; i < ccMccmncArray.length; i++) {
                    CcMccmncImpl ccMccmncTemp = ccMccmncArray[i];
                    newCcMccmnc.put(ccMccmncTemp.getCountryCode(), ccMccmncTemp);
                }
                ccMccmnsCollection.ccMccmncMap = newCcMccmnc;
            }
        }

        @Override
        public void write(CcMccmncCollection ccMccmnsCollection, javolution.xml.XMLFormat.OutputElement xml) throws XMLStreamException {
            ArrayList<CcMccmncImpl> all = new ArrayList<CcMccmncImpl>(ccMccmnsCollection.ccMccmncMap.values());
            CcMccmnsCollection_CcMccmns al = new CcMccmnsCollection_CcMccmns(all);
            xml.add(al, CC_MCCMNC_LIST, CcMccmnsCollection_CcMccmns.class);
        }
    };

    public static class CcMccmnsCollection_CcMccmns extends ArrayListSerializingBase<CcMccmncImpl> {

        public CcMccmnsCollection_CcMccmns() {
            super(CC_MCCMNC, CcMccmncImpl.class);
        }

        public CcMccmnsCollection_CcMccmns(ArrayList<CcMccmncImpl> data) {
            super(CC_MCCMNC, CcMccmncImpl.class, data);
        }

    }

}
