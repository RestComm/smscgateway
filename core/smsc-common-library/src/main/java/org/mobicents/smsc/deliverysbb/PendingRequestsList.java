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

package org.mobicents.smsc.deliverysbb;

import java.io.Serializable;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class PendingRequestsList implements Serializable {

    private static final long serialVersionUID = -2086858224861060482L;

    private int unconfurnedCnt;
    private int[] sequenceNumbers;
    private boolean[] confirmations;

    public PendingRequestsList() {
    }

    public PendingRequestsList(int[] sequenceNumbers) {
        this.sequenceNumbers = sequenceNumbers;
        this.confirmations = new boolean[sequenceNumbers.length];
        this.unconfurnedCnt = sequenceNumbers.length;
    }

    public int confirm(int sequenceNumber) {
        this.unconfurnedCnt--;
        for (int i1 = 0; i1 < sequenceNumbers.length; i1++) {
            if (this.sequenceNumbers[i1] == sequenceNumber && !this.confirmations[i1]) {
                this.confirmations[i1] = true;
                return i1;
            }
        }
        return -1;
    }

    public boolean isSent(int number) {
        if (number < 0 || number >= this.confirmations.length)
            return false;
        else
            return this.confirmations[number];
    }

    public int getRecordCount() {
        return this.confirmations.length;
    }

    public int getUnconfurnedCnt() {
        return this.unconfurnedCnt;
    }

}
