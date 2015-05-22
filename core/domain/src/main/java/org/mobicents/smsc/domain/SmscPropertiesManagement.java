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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javolution.text.TextBuilder;
import javolution.xml.XMLBinding;
import javolution.xml.XMLObjectReader;
import javolution.xml.XMLObjectWriter;
import javolution.xml.stream.XMLStreamException;

import org.apache.log4j.Logger;
import org.mobicents.smsc.smpp.GenerateType;
import org.mobicents.smsc.smpp.SmppEncoding;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 * 
 */
public class SmscPropertiesManagement implements SmscPropertiesManagementMBean {
	private static final Logger logger = Logger.getLogger(SmscPropertiesManagement.class);

	private static final String DEFAULT_TON = "defaultTon";
	private static final String DEFAULT_NPI = "defaultNpi";
    private static final String SMPP_ENCODING_FOR_UCS2 = "smppEncodingForUCS2";
    private static final String SMPP_ENCODING_FOR_GSM7 = "smppEncodingForGsm7";
	private static final String ESME_DEFAULT_CLUSTER_NAME = "esmeDefaultCluster";
    private static final String GENERATE_RECEIPT_CDR = "generateReceiptCdr";
    private static final String RECEIPTS_DISABLING = "receiptsDisabling";
    private static final String GENERATE_CDR = "generateCdr";
    private static final String DELIVERY_PAUSE = "deliveryPause";
    private static final String DEFAULT_VALIDITY_PERIOD_HOURS = "defaultValidityPeriodHours";
    private static final String MAX_VALIDITY_PERIOD_HOURS = "maxValidityPeriodHours";


	private static final String TAB_INDENT = "\t";
	private static final String CLASS_ATTRIBUTE = "type";
	private static final XMLBinding binding = new XMLBinding();
	private static final String PERSIST_FILE_NAME = "smscproperties.xml";

	private static SmscPropertiesManagement instance;

	private final String name;

	private String persistDir = null;

	private final TextBuilder persistFile = TextBuilder.newInstance();

	private int defaultTon = 1;
	private int defaultNpi = 1;

	private SmppEncoding smppEncodingForGsm7 = SmppEncoding.Utf8;
    // Encoding type at SMPP part for data coding schema==8 (UCS2)
    // 0-UTF8, 1-UNICODE
    private SmppEncoding smppEncodingForUCS2 = SmppEncoding.Utf8;

	// if destinationAddress does not match to any esme (any ClusterName) or
	// a message will be routed to defaultClusterName (only for
	// DatabaseSmsRoutingRule)
	// (if it is specified)
	private String esmeDefaultClusterName;

	// true: we generate CDR for both receipt and regular messages
	// false: we generate CDR only for regular messages
    private boolean generateReceiptCdr = false;
    // true: generating of receipts will be disabled for all messages
    private boolean receiptsDisabling = false;

    // generating CDR's option
    private GenerateType generateCdr = new GenerateType(true, true, true);

    // if set to true:
    // SMSC does not try to deliver any messages from cassandra database to SS7
    // / ESMEs / SIP
    // SMSC accepts any incoming messages from SS7 / ESMEs / SIP (and storing
    // them into a database)
    private boolean deliveryPause = false;

    // this flag is not a storable option but a flag
    // this flag is set to true when Schedule RA is inactivated or inactivating
    // and is set to false when Schedule RA is activated
    private boolean smscStopped = true;

    private int defaultValidityPeriodHours = 3 * 24;
    private int maxValidityPeriodHours = 10 * 24;


    private SmscPropertiesManagement(String name) {
		this.name = name;
		binding.setClassAttribute(CLASS_ATTRIBUTE);
	}

	public static SmscPropertiesManagement getInstance(String name) {
		if (instance == null) {
			instance = new SmscPropertiesManagement(name);
		}
		return instance;
	}

	public static SmscPropertiesManagement getInstance() {
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

	public int getDefaultTon() {
		return defaultTon;
	}

	public void setDefaultTon(int defaultTon) {
		this.defaultTon = defaultTon;
		this.store();
	}

	public int getDefaultNpi() {
		return defaultNpi;
	}

	public void setDefaultNpi(int defaultNpi) {
		this.defaultNpi = defaultNpi;
		this.store();
	}

	@Override
	public SmppEncoding getSmppEncodingForGsm7() {
		return smppEncodingForGsm7;
	}

	@Override
	public void setSmppEncodingForGsm7(SmppEncoding smppEncodingForGsm7) {
		this.smppEncodingForGsm7 = smppEncodingForGsm7;
		this.store();
	}

    @Override
    public SmppEncoding getSmppEncodingForUCS2() {
        return smppEncodingForUCS2;
    }

    @Override
    public void setSmppEncodingForUCS2(SmppEncoding smppEncodingForUCS2) {
        this.smppEncodingForUCS2 = smppEncodingForUCS2;
        this.store();
    }

	@Override
	public String getEsmeDefaultClusterName() {
		return esmeDefaultClusterName;
	}

	@Override
	public void setEsmeDefaultClusterName(String val) {
		esmeDefaultClusterName = val;
		this.store();
	}

	public boolean getGenerateReceiptCdr() {
		return this.generateReceiptCdr;
	}

	public void setGenerateReceiptCdr(boolean generateReceiptCdr) {
		this.generateReceiptCdr = generateReceiptCdr;
		this.store();
	}

    public boolean getReceiptsDisabling() {
        return this.receiptsDisabling;
    }

    public void setReceiptsDisabling(boolean receiptsDisabling) {
        this.receiptsDisabling = receiptsDisabling;
        this.store();
    }

    @Override
    public boolean isDeliveryPause() {
        return deliveryPause;
    }

    @Override
    public void setDeliveryPause(boolean deliveryPause) {
        this.deliveryPause = deliveryPause;
        this.store();
    }

    @Override
    public boolean isSmscStopped() {
        return smscStopped;
    }

    public void setSmscStopped(boolean smscStopped) {
        this.smscStopped = smscStopped;
    }

    public GenerateType getGenerateCdr() {
        return generateCdr;
    }

    public void setGenerateCdr(GenerateType generateCdr) {
        this.generateCdr = generateCdr;
        this.store();
    }
    
    public int getGenerateCdrInt() {
        return this.generateCdr.getValue();
    }    
    
    public void setGenerateCdrInt(int generateCdr) {
        this.generateCdr = new GenerateType(generateCdr);
        this.store();
    }

    public int getDefaultValidityPeriodHours() {
        return defaultValidityPeriodHours;
    }

    public void setDefaultValidityPeriodHours(int defaultValidityPeriodHours) {
        this.defaultValidityPeriodHours = defaultValidityPeriodHours;
        this.store();
    }

    public int getMaxValidityPeriodHours() {
        return maxValidityPeriodHours;
    }

    public void setMaxValidityPeriodHours(int maxValidityPeriodHours) {
        this.maxValidityPeriodHours = maxValidityPeriodHours;
        this.store();
    }

	public void start() throws Exception {

		this.persistFile.clear();

		if (persistDir != null) {
			this.persistFile.append(persistDir).append(File.separator).append(this.name).append("_")
					.append(PERSIST_FILE_NAME);
		} else {
			persistFile
					.append(System.getProperty(SmscManagement.SMSC_PERSIST_DIR_KEY,
							System.getProperty(SmscManagement.USER_DIR_KEY))).append(File.separator).append(this.name)
					.append("_").append(PERSIST_FILE_NAME);
		}

		logger.info(String.format("Loading SMSC Properties from %s", persistFile.toString()));

		try {
			this.load();
		} catch (FileNotFoundException e) {
			logger.warn(String.format("Failed to load the SMSC configuration file. \n%s", e.getMessage()));
		}

	}

	public void stop() throws Exception {
		this.store();
	}

	/**
	 * Persist
	 */
	public void store() {

		// TODO : Should we keep reference to Objects rather than recreating
		// everytime?
		try {
			XMLObjectWriter writer = XMLObjectWriter.newInstance(new FileOutputStream(persistFile.toString()));
			writer.setBinding(binding);
			// Enables cross-references.
			// writer.setReferenceResolver(new XMLReferenceResolver());
			writer.setIndentation(TAB_INDENT);

            writer.write(this.defaultValidityPeriodHours, DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
            writer.write(this.maxValidityPeriodHours, MAX_VALIDITY_PERIOD_HOURS, Integer.class);
			writer.write(this.defaultTon, DEFAULT_TON, Integer.class);
			writer.write(this.defaultNpi, DEFAULT_NPI, Integer.class);

            writer.write(this.deliveryPause, DELIVERY_PAUSE, Boolean.class);

			writer.write(this.esmeDefaultClusterName, ESME_DEFAULT_CLUSTER_NAME, String.class);
            writer.write(this.smppEncodingForGsm7.toString(), SMPP_ENCODING_FOR_GSM7, String.class);
            writer.write(this.smppEncodingForUCS2.toString(), SMPP_ENCODING_FOR_UCS2, String.class);

            writer.write(this.generateReceiptCdr, GENERATE_RECEIPT_CDR, Boolean.class);
            writer.write(this.receiptsDisabling, RECEIPTS_DISABLING, Boolean.class);
            writer.write(this.generateCdr.getValue(), GENERATE_CDR, Integer.class);

			writer.close();
		} catch (Exception e) {
			logger.error("Error while persisting the SMSC state in file", e);
		}
	}

	/**
	 * Load and create LinkSets and Link from persisted file
	 * 
	 * @throws Exception
	 */
	public void load() throws FileNotFoundException {

		XMLObjectReader reader = null;
		try {
			reader = XMLObjectReader.newInstance(new FileInputStream(persistFile.toString()));

			reader.setBinding(binding);

            Integer dvp = reader.read(DEFAULT_VALIDITY_PERIOD_HOURS, Integer.class);
            if (dvp != null)
                this.defaultValidityPeriodHours = dvp;
            Integer mvp = reader.read(MAX_VALIDITY_PERIOD_HOURS, Integer.class);
            if (mvp != null)
                this.maxValidityPeriodHours = mvp;
			Integer dTon = reader.read(DEFAULT_TON, Integer.class);
			if (dTon != null)
				this.defaultTon = dTon;
			Integer dNpi = reader.read(DEFAULT_NPI, Integer.class);
			if (dNpi != null)
				this.defaultNpi = dNpi;

            Boolean valB = reader.read(DELIVERY_PAUSE, Boolean.class);
            if (valB != null) {
                this.deliveryPause = valB.booleanValue();
            }

			this.esmeDefaultClusterName = reader.read(ESME_DEFAULT_CLUSTER_NAME, String.class);

			String vals = reader.read(SMPP_ENCODING_FOR_GSM7, String.class);
            if (vals != null)
                this.smppEncodingForGsm7 = Enum.valueOf(SmppEncoding.class, vals);

            vals = reader.read(SMPP_ENCODING_FOR_UCS2, String.class);
            if (vals != null)
                this.smppEncodingForUCS2 = Enum.valueOf(SmppEncoding.class, vals);

            valB = reader.read(GENERATE_RECEIPT_CDR, Boolean.class);
            if (valB != null) {
                this.generateReceiptCdr = valB.booleanValue();
            }
            valB = reader.read(RECEIPTS_DISABLING, Boolean.class);
            if (valB != null) {
                this.receiptsDisabling = valB.booleanValue();
            }

			reader.close();
		} catch (XMLStreamException ex) {
			logger.error("Error while loading the SMSC state from file", ex);
		}
	}
}
