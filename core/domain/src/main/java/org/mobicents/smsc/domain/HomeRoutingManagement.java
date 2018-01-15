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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;

/**
 *
 * @author sergey vetyutnev
 *
 */
public class HomeRoutingManagement implements HomeRoutingManagementMBean {
    private static final Logger logger = Logger.getLogger(HomeRoutingManagement.class);

    private static final String TAB_INDENT = "\t";
    private static final String CLASS_ATTRIBUTE = "type";
    private static final XMLBinding binding = new XMLBinding();
    private static final String CC_MCCMNC_PERSIST_FILE_NAME = "cc_mccmnc.xml";
    private static final String CORR_ID_FILE_NAME = "corrid.xml";
    private static final String CC_MCCMNS_COLLECTION = "CcMccmncCollection";
    private static final String CC_CORR_ID = "CorrId";
    private static final long MAX_CORRELATION_ID = 10000000000L;
    private static final long CORR_ID_LAG = 1000;

    private static HomeRoutingManagement instance;

    private final String name;
    private String persistDir = null;
    private final TextBuilder persistFile = TextBuilder.newInstance();
    private final TextBuilder persistFileCorrId = TextBuilder.newInstance();

    private CcMccmncCollection ccMccmncCollection;
    private long correlationId = 0;
    private long loadedCorrelationId = -1;
//    private int ccMccmnsTableVersionActual = 1;
//    private int ccMccmnsTableVersionLoaded = 0;

    private HomeRoutingManagement(String name) {
        this.name = name;
        binding.setClassAttribute(CLASS_ATTRIBUTE);
    }

    public static HomeRoutingManagement getInstance(String name) {
        if (instance == null) {
            instance = new HomeRoutingManagement(name);
        }
        return instance;
    }

    public static HomeRoutingManagement getInstance() {
        return instance;
    }

    public String getName() {
        return name;
    }

    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }

    @Override
    public synchronized NextCorrelationIdResult getNextCorrelationId(String msisdn) {
        long corrId = doGetNextCorrelationId();
        CcMccmnc ccMccmncValue = getCcMccmncValue(msisdn);
        String mccmnc;
        if (ccMccmncValue == null) {
            logger.warn("Found no entry in CcMccmncCollection for msisdn: " + msisdn);
            mccmnc = "";
        } else {
            mccmnc = ccMccmncValue.getMccMnc();
        }

        String corrIdS = String.valueOf(corrId);
        StringBuilder sb = new StringBuilder();
        int len = mccmnc.length() + corrIdS.length();
        if (len <= 15) {
            sb.append(mccmnc);
            for (int i1 = len; i1 < 15; i1++) {
                sb.append("0");
            }
            sb.append(corrIdS);
        } else {
            sb.append(mccmnc);
            sb.append(corrIdS.substring(corrIdS.length() - (15 - mccmnc.length())));
        }

        NextCorrelationIdResult res = new NextCorrelationIdResult();
        res.setCorrelationId(sb.toString());
        if (ccMccmncValue != null)
            res.setSmscAddress(ccMccmncValue.getSmsc());
        return res;
    }

    @Override
    public void updateCcMccmncTable() {
//        ccMccmnsTableVersionActual++;
        this.load();
    }

    protected synchronized long doGetNextCorrelationId() {
        correlationId++;
        if (correlationId >= MAX_CORRELATION_ID)
            correlationId = 1;

        // TODO: properly implement it with provided MSISDN -> IMSI recoding
        // table
        if (correlationId - loadedCorrelationId >= CORR_ID_LAG || correlationId < loadedCorrelationId || loadedCorrelationId < 0) {
            this.storeCorrId();
        }

        return correlationId;
    }

    protected CcMccmnc getCcMccmncValue(String countryCode) {
        checkCcMccmncTable();
        return ccMccmncCollection.findMccmnc(countryCode);
    }

    protected void checkCcMccmncTable() {
//        if (ccMccmncCollection != null && ccMccmnsTableVersionLoaded == ccMccmnsTableVersionActual)
//            return;
//        load();
    }

    @Override
    public void addCcMccmnc(String countryCode, String mccMnc, String smsc) throws Exception {
        CcMccmncImpl ccMccmnc = new CcMccmncImpl(countryCode, mccMnc, smsc);
        ccMccmncCollection.addCcMccmnc(ccMccmnc);
        this.store();
    }

    @Override
    public void modifyCcMccmnc(String countryCode, String mccMnc, String smsc) throws Exception {
        ccMccmncCollection.modifyCcMccmnc(countryCode, mccMnc, smsc);
        this.store();
    }

    @Override
    public void removeCcMccmnc(String countryCode) throws Exception {
        ccMccmncCollection.removeCcMccmnc(countryCode);
        this.store();
    }

    @Override
    public CcMccmnc getCcMccmnc(String countryCode) {
        return ccMccmncCollection.getCcMccmnc(countryCode);
    }

    @Override
    public Map<String, CcMccmncImpl> getCcMccmncMap() {
        return ccMccmncCollection.getCcMccmncMap();
    }


    public void start() throws Exception {
        this.persistFile.clear();

        if (persistDir != null) {
            this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_").append(CC_MCCMNC_PERSIST_FILE_NAME);
        } else {
            persistFile.append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY, System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator)
                    .append(this.name).append("_").append(CC_MCCMNC_PERSIST_FILE_NAME);
        }

        this.persistFileCorrId.clear();

        if (persistDir != null) {
            this.persistFileCorrId.append(persistDir).append(File.separator).append(this.name).append("_").append(CORR_ID_FILE_NAME);
        } else {
            persistFileCorrId.append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY, System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator)
                    .append(this.name).append("_").append(CORR_ID_FILE_NAME);
        }

        logger.info(String.format("Loading home routing properties from %s", persistFile.toString()));
        this.load();

        logger.info(String.format("Loading home routing corrId from %s", persistFileCorrId.toString()));
        this.loadCorrId();
    }

    public void stop() throws Exception {
//        this.store();
        if (loadedCorrelationId != -1 && correlationId != loadedCorrelationId) {
            this.storeCorrId();
        }
    }

    /**
     * Persist
     */
    public void store() {
        try {
            XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
            writer.setBinding(binding);
            writer.setIndentation(TAB_INDENT);

            writer.write(ccMccmncCollection, CC_MCCMNS_COLLECTION, CcMccmncCollection.class);

            writer.close();
        } catch (Exception e) {
            logger.error("Error while persisting the ccMccmncCollection in file", e);
        }
    }

    public void storeCorrId() {
        try {
            XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFileCorrId.toString()));
            writer.setBinding(binding);
            writer.setIndentation(TAB_INDENT);

            writer.write(correlationId, CC_CORR_ID, Long.class);

            writer.close();
        } catch (Exception e) {
            logger.error("Error while persisting the home routing corrId value in file", e);
        }

        loadedCorrelationId = correlationId;
    }

    public synchronized void load() {
//        if (ccMccmncCollection != null && ccMccmnsTableVersionLoaded == ccMccmnsTableVersionActual)
//            return;

        ccMccmncCollection = new CcMccmncCollection();
        XMLObjectReader reader = null;
        try {
            FileInputStream fis = new FileInputStream(persistFile.toString());
            try {
                System.out.println("fis.available():" + fis.available());
            } catch (IOException e) {
                e.printStackTrace();
            }
            reader = XMLObjectReader.newInstance(fis);
            try {
                reader.setBinding(binding);
                ccMccmncCollection = reader.read(CC_MCCMNS_COLLECTION, CcMccmncCollection.class);

                logger.info("Successfully loaded CcMccmnsCollection: " + persistFile);
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException ex) {
            logger.warn("CcMccmnsCollection: file not found: " + persistFile.toString());
            try {
                this.store();
            } catch (Exception e) {
            }
        } catch (XMLStreamException ex) {
            logger.error("Error while loading CcMccmnsCollection from file" + persistFile.toString(), ex);
        }
//        ccMccmnsTableVersionLoaded = ccMccmnsTableVersionActual;
    }

    public synchronized void loadCorrId() {
        XMLObjectReader reader = null;
        try {
            reader = XMLObjectReader.newInstance(new FileInputStream(persistFileCorrId.toString()));
            try {
                reader.setBinding(binding);
                correlationId = reader.read(CC_CORR_ID, Long.class);
                correlationId += CORR_ID_LAG;
                loadedCorrelationId = -1;

                logger.info("Successfully loaded home routing corrId: " + persistFile);
            } finally {
                reader.close();
            }
        } catch (FileNotFoundException ex) {
            logger.warn("home routing corrId value: file not found: " + persistFile.toString());
            logger.warn("CcMccmnsCollection: file not found: " + persistFile.toString());
            try {
                this.storeCorrId();
            } catch (Exception e) {
            }
        } catch (XMLStreamException ex) {
            logger.error("Error while loading home routing corrId value from file" + persistFile.toString(), ex);
        }
    }

}
