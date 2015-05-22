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

package org.mobicents.smsc.domain;

import org.mobicents.smsc.smpp.GenerateType;
import org.mobicents.smsc.smpp.SmppEncoding;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public interface SmscPropertiesManagementMBean {

	public int getDefaultTon();

	public void setDefaultTon(int defaultTon);

	public int getDefaultNpi();

	public void setDefaultNpi(int defaultNpi);

    public SmppEncoding getSmppEncodingForGsm7();

    public void setSmppEncodingForGsm7(SmppEncoding smppEncodingForGsm7);

	public SmppEncoding getSmppEncodingForUCS2();

	public void setSmppEncodingForUCS2(SmppEncoding smppEncodingForUCS2);

	public String getEsmeDefaultClusterName();

	public void setEsmeDefaultClusterName(String val);

	public boolean getGenerateReceiptCdr();

	public void setGenerateReceiptCdr(boolean generateReceiptCdr);
    
    public boolean getReceiptsDisabling();
    
    public void setReceiptsDisabling(boolean receiptsDisabling);

    public GenerateType getGenerateCdr();

    public void setGenerateCdr(GenerateType generateCdr);

    public boolean isDeliveryPause();

    public boolean isSmscStopped();

    public void setDeliveryPause(boolean deliveryPause);

    public int getDefaultValidityPeriodHours();

    public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours);

    public int getMaxValidityPeriodHours();

    public void setMaxValidityPeriodHours(int maxValidityPeriodHours);

}
