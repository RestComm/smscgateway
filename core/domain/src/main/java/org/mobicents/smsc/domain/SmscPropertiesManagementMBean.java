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

package org.mobicents.smsc.domain;

import java.util.Map;

import org.mobicents.protocols.ss7.indicator.GlobalTitleIndicator;
import org.mobicents.smsc.smpp.GenerateType;
import org.mobicents.smsc.smpp.SmppEncoding;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public interface SmscPropertiesManagementMBean {

	public String getServiceCenterGt();

    public String getServiceCenterGt(int networkId);

	public void setServiceCenterGt(String serviceCenterGt);
	
	public void setServiceCenterGt(int networkId, String serviceCenterGt);
	
	public Map<Integer, String> getNetworkIdVsServiceCenterGt();

	public int getServiceCenterSsn();

	public void setServiceCenterSsn(int serviceCenterSsn);

	public int getHlrSsn();

	public void setHlrSsn(int hlrSsn);

	public int getMscSsn();

	public void setMscSsn(int mscSsn);

	public int getMaxMapVersion();

	public void setMaxMapVersion(int maxMapVersion);

	public int getDefaultValidityPeriodHours();

	public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours);

	public int getMaxValidityPeriodHours();

	public void setMaxValidityPeriodHours(int maxValidityPeriodHours);

	public int getDefaultTon();

	public void setDefaultTon(int defaultTon);

	public int getDefaultNpi();

	public void setDefaultNpi(int defaultNpi);

	public int getSubscriberBusyDueDelay();

	public void setSubscriberBusyDueDelay(int subscriberBusyDueDelay);

	public int getFirstDueDelay();

	public void setFirstDueDelay(int firstDueDelay);

	public int getSecondDueDelay();

	public void setSecondDueDelay(int secondDueDelay);

	public int getMaxDueDelay();

	public void setMaxDueDelay(int maxDueDelay);

	public int getDueDelayMultiplicator();

	public void setDueDelayMultiplicator(int dueDelayMultiplicator);

	public int getMaxMessageLengthReducer();

	public void setMaxMessageLengthReducer(int maxMessageLengReducer);

    public SmppEncoding getSmppEncodingForGsm7();

    public void setSmppEncodingForGsm7(SmppEncoding smppEncodingForGsm7);

	public SmppEncoding getSmppEncodingForUCS2();

	public void setSmppEncodingForUCS2(SmppEncoding smppEncodingForUCS2);

    public String getDbHosts();

    public void setDbHosts(String dbHosts);

    public int getDbPort();

    public void setDbPort(int dbPort);

	public String getKeyspaceName();

	public void setKeyspaceName(String keyspaceName);

	public String getClusterName();

	public void setClusterName(String clusterName);

	public long getFetchPeriod();

	public void setFetchPeriod(long fetchPeriod);

	public int getFetchMaxRows();

	public void setFetchMaxRows(int fetchMaxRows);

	public int getMaxActivityCount();

	public void setMaxActivityCount(int maxActivityCount);

	public String getEsmeDefaultClusterName();

	public void setEsmeDefaultClusterName(String val);

//	public boolean getSMSHomeRouting();
//	public void setSMSHomeRouting(boolean isSMSHomeRouting);

	public int getReviseSecondsOnSmscStart();

	public void setReviseSecondsOnSmscStart(int reviseSecondsOnSmscStart);

	public int getProcessingSmsSetTimeout();

	public void setProcessingSmsSetTimeout(int processingSmsSetTimeout);

	public boolean getGenerateReceiptCdr();

	public void setGenerateReceiptCdr(boolean generateReceiptCdr);

    public MoChargingType getMoCharging();

    public void setMoCharging(MoChargingType moCharging);

    public MoChargingType getHrCharging();

    public void setHrCharging(MoChargingType hrCharging);

    public StoreAndForwordMode getStoreAndForwordMode();

    public void setStoreAndForwordMode(StoreAndForwordMode storeAndForwordMode);

	public ChargingType getTxSmppChargingType();

	public void setTxSmppChargingType(ChargingType txSmppCharging);

	public ChargingType getTxSipChargingType();

	public void setTxSipChargingType(ChargingType txSmppCharging);

    public GlobalTitleIndicator getGlobalTitleIndicator();

    public void setGlobalTitleIndicator(GlobalTitleIndicator globalTitleIndicator);

    public int getTranslationType();

    public void setTranslationType(int translationType);

    public int getCorrelationIdLiveTime();

    public void setCorrelationIdLiveTime(int correlationIdLiveTime);

	public String getDiameterDestRealm();

	public void setDiameterDestRealm(String diameterDestRealm);

	public String getDiameterDestHost();

	public void setDiameterDestHost(String diameterDestHost);

	public int getDiameterDestPort();

	public void setDiameterDestPort(int diameterDestPort);

	public String getDiameterUserName();

	public void setDiameterUserName(String diameterUserName);

    public int getRemovingLiveTablesDays();

    public void setRemovingLiveTablesDays(int removingLiveTablesDays);

    public int getRemovingArchiveTablesDays();

    public void setRemovingArchiveTablesDays(int removingArchiveTablesDays);

    public boolean isDeliveryPause();

    public boolean isSmscStopped();

    public void setSkipUnsentMessages(int skipUnsentMessages);
    
    public int getSkipUnsentMessages();

    public void setDeliveryPause(boolean deliveryPause);

    public GenerateType getGenerateCdr();

    public void setGenerateCdr(GenerateType generateCdr);

    public int getGenerateCdrInt();
    
    public void setGenerateCdrInt(int generateCdr);
    
    public GenerateType getGenerateArchiveTable();

    public void setGenerateArchiveTable(GenerateType generateArchiveTable);

    public int getGenerateArchiveTableInt();
    
    public void setGenerateArchiveTableInt(int generateArchiveTable); 
    
    public boolean getReceiptsDisabling();
    
    public void setReceiptsDisabling(boolean receiptsDisabling);

    public boolean getOrigNetworkIdForReceipts();

    public void setOrigNetworkIdForReceipts(boolean origNetworkIdForReceipts);

    public String getHrHlrNumber();

    public void setHrHlrNumber(String hrHlrNumber);

    public int getNationalLanguageSingleShift();

    public void setNationalLanguageSingleShift(int nationalLanguageSingleShift);

    public int getNationalLanguageLockingShift();

    public void setNationalLanguageLockingShift(int nationalLanguageLockingShift);
    
    public int getSriResponseLiveTime();
    
    public void setSriResponseLiveTime(int sriresponselivetime);

}
