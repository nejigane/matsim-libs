/* *********************************************************************** *
 * project: org.matsim.*
 * CalcPaidToll.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.AgentWait2LinkEventImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.roadpricing.RoadPricingScheme.Cost;

/**
 * Calculates the toll agents pay during a simulation by analyzing events. To
 * fully function, add an instance of this class as EventHandler to your Events
 * object.
 * <br/>
 * This class and its methods will pass the person to the toll calculation ...
 * ... but the toll calculation must in fact use the person.  Standard RoadPricingScheme objects
 * which are read from xml will not and in the current design cannot do this.  kai, mar'12
 *
 * @author mrieser
 */
public class CalcPaidToll implements LinkEnterEventHandler, AgentWait2LinkEventHandler,  AgentArrivalEventHandler {

	static class AgentTollInfo {
		public double toll = 0.0;
		public boolean insideCordonArea = true;
	}

	final RoadPricingSchemeI scheme;
	final TreeMap<Id, AgentTollInfo> agents = new TreeMap<Id, AgentTollInfo>();
	private final Network network;
	private final Population population ;

	private TollBehaviourI handler = null;

	/**
     * Design comments:<ul>
     * <li> yy I don't think that we really need Population.  kai, mar'12
     * <li> yyyy However, would need EventsManager if toll is sent to agent at arrival. kai, mar'12
     * </ul>
	 */
	public CalcPaidToll(final Network network, final RoadPricingSchemeI scheme, Population population) {
		super();
		this.network = network;
		this.population = population ;
		this.scheme = scheme;
		if (RoadPricingScheme.TOLL_TYPE_DISTANCE.equals(scheme.getType())) {
			this.handler = new DistanceTollBehaviour();
		} else if (RoadPricingScheme.TOLL_TYPE_AREA.equals(scheme.getType())) {
			this.handler = new AreaTollBehaviour();
			Logger.getLogger(this.getClass()).warn("area toll does not work if you have different toll amounts on different " +
					"links.  Make sure you get what you want.  kai, mar'12");
		} else if (RoadPricingScheme.TOLL_TYPE_CORDON.equals(scheme.getType())) {
			this.handler = new CordonTollBehaviour();
			Logger.getLogger(this.getClass()).warn("cordon toll only charges at transition from untolled to tolled. " +
					"Make sure this is what you want. kai, mar'12") ;
		} else if (RoadPricingScheme.TOLL_TYPE_LINK.equals(scheme.getType())) {
			this.handler = new LinkTollBehaviour();
		} else {
			throw new IllegalArgumentException("RoadPricingScheme of type \"" + scheme.getType() + "\" is not supported.");
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		Link link = this.network.getLinks().get(event.getLinkId());
		Person person = this.population.getPersons().get(event.getPersonId()) ;
		this.handler.handleEvent(event, link, person);
	}

	@Override
	public void handleEvent(final AgentWait2LinkEvent event) {
		Link link = this.network.getLinks().get(event.getLinkId());
		Person person = this.population.getPersons().get(event.getPersonId()) ;
		this.handler.handleEvent(event, link, person);
	}
	
	@Override
	public void handleEvent(AgentArrivalEvent event) {
		
	}



	private static int cnt = 0 ;
	/**
	 * Sends {@link AgentMoneyEventImpl}s for all agents that must pay a toll.
	 * This method should usually be called at the end before of an iteration.
	 *
	 * <strong>Important note: </strong>Do not call this method twice without
	 * calling {@link #reset(int)} in between. Otherwise the toll-disutility
	 * may be added twice to the agents' score!
	 *
	 * @param time the current time the generated events are associated with
	 * @param events the {@link EventsManager} collection, the generated events are sent to for processing
	 */
	public void sendMoneyEvents(final double time, final EventsManager events) {
		for (Map.Entry<Id, AgentTollInfo> entries : this.agents.entrySet()) {
			events.processEvent(new AgentMoneyEventImpl(time, entries.getKey(), -entries.getValue().toll));
			if ( cnt < 10 ) {
				cnt++ ;
				Logger.getLogger(this.getClass()).info("toll paid: " + entries.getValue().toll ) ;
				if (cnt==10 ) {
					Logger.getLogger(this.getClass()).info(Gbl.FUTURE_SUPPRESSED) ;
				}
			}

		}
	}

	@Override
	public void reset(final int iteration) {
		this.agents.clear();
	}

	/**
	 * Returns the toll the specified agent has paid in the course of the
	 * simulation so far.
	 *
	 * @param agentId
	 * @return The toll paid by the specified agent, 0.0 if no toll was paid.
	 */
	public double getAgentToll(final Id agentId) {
		AgentTollInfo info = this.agents.get(agentId);
		if (info == null) {
			return 0.0;
		}
		return info.toll;
	}

	/**
	 * @return The toll paid by all the agents.
	 */
	public double getAllAgentsToll() {
		double tolls = 0;
		for (AgentTollInfo ai : this.agents.values()) {
			tolls += (ai == null) ? 0.0 : ai.toll;
		}
		return tolls;
	}

	/**
	 * @return The Number of all the Drawees.
	 */
	public int getDraweesNr() {
		int dwCnt = 0;
		for (AgentTollInfo ai : this.agents.values()) {
			if ((ai != null) && (ai.toll > 0.0)) {
					dwCnt++;
			}
		}
		return dwCnt;
	}

	/**
	 * A simple interface to implement different toll schemes.
	 */
	private interface TollBehaviourI {
		public void handleEvent(PersonEvent event, Link link, Person person );
	}

	/**
	 * Handles the calculation of the distance toll. If an agent enters a link at
	 * a time the toll has to be paid, the toll amount is added to the agent. The
	 * agent does not have to pay the toll for a link if it starts on the link,
	 * as it may have paid already when arriving on the link.
	 */
	class DistanceTollBehaviour implements TollBehaviourI {
		@Override
		public void handleEvent(final PersonEvent event, final Link link, Person person) {
			if (event instanceof AgentWait2LinkEventImpl) {
				/* we do not handle wait2link-events for distance toll, because the agent
				 * must not pay twice for this link, and he (likely) paid already when
				 * arriving at this link.  */
				return;
			}
			Cost cost = CalcPaidToll.this.scheme.getLinkCostInfo(link.getId(),
					event.getTime(), person.getId());
			if (cost != null) {
				double newToll = link.getLength() * cost.amount;
				AgentTollInfo info = CalcPaidToll.this.agents.get(event.getPersonId());
				if (info == null) {
					info = new AgentTollInfo();
					CalcPaidToll.this.agents.put(event.getPersonId(), info);
				}
				info.toll += newToll;
			}
		}
	}

	class LinkTollBehaviour implements TollBehaviourI {
		@Override
		public void handleEvent(final PersonEvent event, final Link link, Person person) {
			if (event instanceof AgentWait2LinkEventImpl) {
				/* we do not handle wait2link-events for link toll, because the agent
				 * should not pay twice for this link, and he (likely) paid already when
				 * arriving at this link.  */
				return;
			}
			Cost cost = CalcPaidToll.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), person.getId());
			if (cost != null) {
				AgentTollInfo info = CalcPaidToll.this.agents.get(event.getPersonId());
				if (info == null) {
					info = new AgentTollInfo();
					CalcPaidToll.this.agents.put(event.getPersonId(), info);
				}
				info.toll += cost.amount;
			}
		}
	}

	/** Handles the calculation of the area toll. Whenever the agent is seen on
	 * one of the tolled link, the constant toll amount has to be paid once.
	 * <br/>
	 * Design comments:<ul>
	 * <li> This implementation becomes a problem if someone tries to implement more than one area which do
	 * not share the same flat fee.  kai, mar'12
	 * </ul> 
	 */
	class AreaTollBehaviour implements TollBehaviourI {
		@Override
		public void handleEvent(final PersonEvent event, final Link link, Person person) {
			Cost cost = CalcPaidToll.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), person.getId());
			if (cost != null) {
				AgentTollInfo info = CalcPaidToll.this.agents.get(event.getPersonId());
				if (info == null) {
					info = new AgentTollInfo();
					CalcPaidToll.this.agents.put(event.getPersonId(), info);
					info.toll = cost.amount;
				}
			}
		}
	}

	/**
	 * Handles the calculation of the cordon toll. An agent has only to pay if he
	 * crosses the cordon from the outside to the inside.
	 */
	class CordonTollBehaviour implements TollBehaviourI {
		@Override
		public void handleEvent(final PersonEvent event, final Link link, Person person) {
			Cost cost = CalcPaidToll.this.scheme.getLinkCostInfo(link.getId(), event.getTime(), person.getId());
			if (cost != null) {
				// this is a link inside the toll area.
				// [[I guess this assumes that all links inside the cordon are listed in the toll scheme, similar to an area
				// toll.  Conventionally, one would not do it in this way, but one would just name those links where
				// the cordon toll is charged.  kai, mar'12]]
				AgentTollInfo info = CalcPaidToll.this.agents.get(event.getPersonId());
				if (info == null) {
					// no information about this agent, so it did not yet pay the toll
					// [[yyyy this would refer to any toll, so if we have two cordons, it does not work.  kai, mar'12]]
					info = new AgentTollInfo();
					CalcPaidToll.this.agents.put(event.getPersonId(), info);
					info.toll = 0.0; // we start in the area, do not toll
					// info.insideCordonArea is implicitly initialized with `true'. kai, mar'12
				} else if (!info.insideCordonArea) {
					// agent was outside before, now inside the toll area --> agent has to pay
					info.insideCordonArea = true;
					info.toll += cost.amount;
				}
				// else: agent was already in toll area, does not have to pay again (this implementation is a bit unusual!)
			} else {
				// this is a link outside the toll area; just need to memorize that the agent is outside the toll area.
				AgentTollInfo info = CalcPaidToll.this.agents.get(event.getPersonId());
				if (info == null) {
					info = new AgentTollInfo();
					CalcPaidToll.this.agents.put(event.getPersonId(), info);
				}
				info.insideCordonArea = false;
			}
		}
	}

}
