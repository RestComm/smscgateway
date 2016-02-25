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

import java.util.Date;

import org.apache.log4j.Logger;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class ArchiveSms implements ArchiveSmsMBean {

	private static final Logger logger = Logger.getLogger(ArchiveSms.class);

	private final String name;

	private static ArchiveSms instance = null;

	private ArchiveSms(String name) {
		this.name = name;
	}

	protected static ArchiveSms getInstance(String name) {
		if (instance == null) {
			instance = new ArchiveSms(name);
		}
		return instance;
	}

	public static ArchiveSms getInstance() {
		return instance;
	}

	public String getName() {
		return name;
	}

	public void start() throws Exception {
		try {

		} catch (Exception e) {
			logger.error("Error initializing cassandra database for ArchiveSms", e);
		}
	}

	public void stop() {
	}

	@Override
	public void makeCdrDatabaseManualExport(Date timeFrom, Date timeTo) {
		// TODO Auto-generated method stub

	}

}
