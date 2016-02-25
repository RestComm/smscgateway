/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
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

package org.mobicents.smsc.slee.common.ra;

import java.util.concurrent.ConcurrentHashMap;

import javax.slee.EventTypeID;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.ResourceAdaptorContext;

/**
 * 
 * @author amit bhayani
 * @author baranowb
 */
public class EventIDCache {

	private ConcurrentHashMap<String, FireableEventType> eventIds = new ConcurrentHashMap<String, FireableEventType>();
	private final String eventVendor;
	private final String eventVersion;

	private Tracer tracer;
	private EventLookupFacility eventLookupFacility;
	public EventIDCache(ResourceAdaptorContext raContext, final String vendor, final String version) {
		this.tracer = raContext.getTracer(EventIDCache.class.getSimpleName());
		this.eventLookupFacility = raContext.getEventLookupFacility();
		this.eventVendor = vendor;
		this.eventVersion = version;
	}

	public FireableEventType getEventId(String eventName) {

		FireableEventType eventType = eventIds.get(eventName);
		if (eventType == null) {
			try {
				eventType = eventLookupFacility.getFireableEventType(new EventTypeID(eventName, this.eventVendor,
				        this.eventVersion));
			} catch (Throwable e) {
				this.tracer.severe("Error while looking up for SMPP Server Events", e);
			}
			if (eventType != null) {
				this.eventIds.put(eventName, eventType);
			}
		}
		return eventType;
	}
}
