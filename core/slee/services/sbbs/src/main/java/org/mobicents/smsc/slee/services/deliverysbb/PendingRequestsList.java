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

package org.mobicents.smsc.slee.services.deliverysbb;

import java.io.Serializable;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class PendingRequestsList implements Serializable {

    private static final long serialVersionUID = -2086858224861060482L;

    private int unconfirmedCnt;
    private int[] sequenceNumbers;
    private int[][] sequenceNumbersExtra;
    private boolean[] confirmations;
    private boolean[][] confirmationsExtra;

    public PendingRequestsList() {
    }

    public PendingRequestsList(int[] sequenceNumbers, int[][] sequenceNumbersExtra) {
        this.sequenceNumbers = sequenceNumbers;
        this.sequenceNumbersExtra = sequenceNumbersExtra;
        this.confirmations = new boolean[sequenceNumbers.length];
        this.confirmationsExtra = new boolean[sequenceNumbers.length][];
        this.unconfirmedCnt = sequenceNumbers.length;

        for (int i1 = 0; i1 < this.sequenceNumbers.length; i1++) {
            if (this.sequenceNumbersExtra[i1] != null)
                this.confirmationsExtra[i1] = new boolean[this.sequenceNumbersExtra[i1].length];
        }
    }

    public ConfirmMessageInSendingPool confirm(int sequenceNumber) {
        ConfirmMessageInSendingPool res = new ConfirmMessageInSendingPool();
        for (int i1 = 0; i1 < sequenceNumbers.length; i1++) {
            if (this.sequenceNumbers[i1] == sequenceNumber && !this.confirmations[i1]) {
                this.confirmations[i1] = true;
                res.sequenceNumberFound = true;
                res.msgNum = i1;
                if (this.sequenceNumbersExtra[i1] != null)
                    res.splittedMessage = true;
                if (isSent(i1)) {
                    res.confirmed = true;
                    this.unconfirmedCnt--;
                    return res;
                } else
                    return res;
            }
            if (this.sequenceNumbersExtra[i1] != null) {
                for (int i2 = 0; i2 < this.sequenceNumbersExtra[i1].length; i2++) {
                    if (this.sequenceNumbersExtra[i1][i2] == sequenceNumber && !this.confirmationsExtra[i1][i2]) {
                        this.confirmationsExtra[i1][i2] = true;
                        res.sequenceNumberFound = true;
                        res.msgNum = i1;
                        res.splittedMessage = true;
                        if (isSent(i1)) {
                            res.confirmed = true;
                            this.unconfirmedCnt--;
                            return res;
                        } else
                            return res;
                    }
                }
            }
        }
        return res;
    }

    public boolean isSent(int number) {
        if (number < 0 || number >= this.confirmations.length)
            return false;
        else {
            if (this.confirmationsExtra[number] != null) {
                for (int i2 = 0; i2 < this.confirmationsExtra[number].length; i2++) {
                    if (!this.confirmationsExtra[number][i2])
                        return false;
                }
            }
            return this.confirmations[number];
        }
    }

    public int getRecordCount() {
        return this.confirmations.length;
    }

    public int getUnconfurnedCnt() {
        return this.unconfirmedCnt;
    }

}
