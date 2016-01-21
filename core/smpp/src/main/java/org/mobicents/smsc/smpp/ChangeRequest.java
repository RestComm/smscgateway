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
package org.mobicents.smsc.smpp;

/**
 * @author Amit Bhayani
 * 
 */
public final class ChangeRequest {

	public static final int CONNECT = 0;
	public static final int ENQUIRE_LINK = 2;

	private int type;
	private Esme esme;
	private long executionTime;

	/**
	 * 
	 */
	public ChangeRequest(Esme esme, int type, long executionTime) {
		this.esme = esme;
		this.type = type;
		this.executionTime = executionTime;
	}

	/**
	 * @return the type
	 */
	protected int getType() {
		return type;
	}

	/**
	 * @return the esme
	 */
	protected Esme getEsme() {
		return esme;
	}

	/**
	 * @return the executionTime
	 */
	protected long getExecutionTime() {
		return executionTime;
	}

}
