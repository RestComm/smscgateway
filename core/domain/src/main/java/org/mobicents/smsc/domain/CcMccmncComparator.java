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

import java.util.Comparator;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class CcMccmncComparator implements Comparator<CcMccmncImpl> {

    @Override
    public int compare(CcMccmncImpl ccMccmnc1, CcMccmncImpl ccMccmnc2) {
        String cc1 = ccMccmnc1.getCountryCode();
        String cc2 = ccMccmnc2.getCountryCode();

        if (cc1 == null)
            cc1 = "";
        if (cc2 == null)
            cc2 = "";
        int len1 = cc1.length();
        int len2 = cc2.length();

        if (len1 > len2)
            return -1;
        else if (len1 < len2)
            return 1;
        else
            return 0;
    }
}
