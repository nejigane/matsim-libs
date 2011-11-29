/* *********************************************************************** *
 * project: org.matsim.*
 * AgentWait2LinkEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;

public class AgentWait2LinkEventImpl extends AgentEventImpl implements AgentWait2LinkEvent {

	public static final String EVENT_TYPE = "wait2link";
	public static final String ATTRIBUTE_VEHICLE = "vehicle";
	private final Id vehicleId;

	public AgentWait2LinkEventImpl(final double time, final Id agentId, final Id linkId, Id vehicleId) {
		super(time, agentId, linkId, null);
		this.vehicleId = vehicleId;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id getVehicleId() {
		return vehicleId;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		if (this.vehicleId != null) {
			attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		}
		return attr;
	}
	
}
