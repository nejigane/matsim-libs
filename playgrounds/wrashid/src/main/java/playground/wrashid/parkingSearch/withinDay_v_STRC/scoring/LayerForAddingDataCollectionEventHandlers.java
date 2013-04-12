/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.withinDay_v_STRC.scoring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.ExperimentalBasicWithindayAgent;
import org.matsim.core.mobsim.qsim.agents.PlanBasedWithinDayAgent;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;

import playground.christoph.parking.core.mobsim.ParkingInfrastructure;
import playground.wrashid.lib.obj.IntegerValueHashMap;
import playground.wrashid.parkingSearch.withinDay_v_STRC.WithinDayParkingController;
import playground.wrashid.parkingSearch.withinDay_v_STRC.util.ParkingAgentsTracker_v2;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureDurationOfLastParkingOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureFirstCarDepartureTimeOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureLastActivityDurationOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureParkingWalkTimesDuringDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CapturePreviousActivityDurationDuringDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.CaptureWalkDurationOfFirstAndLastOfDay;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.UpdateEndTimeOfPreviousActivity;
import playground.wrashid.parkingSearch.withindayFW.parkingTracker.UpdateLastParkingArrivalTime;

public class LayerForAddingDataCollectionEventHandlers extends ParkingAgentsTracker_v2 {

	protected HashMap<Id, Double> mostRecentDepartureTime;

	protected HashMap<Id, Id> lastParkingFacilityIdOfDay;

	protected HashMap<Id, Double> firstParkingWalkTimeOfDay;
	protected HashMap<Id, Double> lastParkingWalkTimeOfDay;

	protected HashMap<Id, Double> firstParkingDepartureTimeOfDay;
	protected HashMap<Id, Double> lastParkingArrivalTimeOfDay;

	protected HashMap<Id, Double> endTimeOfFirstActivityOfDay;
	protected HashMap<Id, Double> startTimeOfLastActivityOfDay;

	protected HashMap<Id, Double> walkDurationFromParking;
	protected HashMap<Id, Double> walkDurationToParking;

	protected HashMap<Id, Double> parkingArrivalTime;
	protected HashMap<Id, Double> parkingDepartureTime;

	protected HashMap<Id, Double> startTimeOfFirstActivityAfterParkingCar;
	protected HashMap<Id, Double> endTimeOfLastActivityBeforeLeavingWithCar;

	protected Map<Id, Integer> firstParkingActivityPlanElemIndex;
	protected Map<Id, Integer> lastParkingActivityPlanElemIndex;
	
	protected HashMap<Id, Double> parkingSearchStartTime;
	
	protected Integer currentPlanElementIndex;

	public LayerForAddingDataCollectionEventHandlers(Scenario scenario, ParkingInfrastructure parkingInfrastructure,
			double distance, WithinDayParkingController controler) {
		super(scenario, parkingInfrastructure, distance, controler);

	}

	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {
		super.notifyMobsimInitialized(e);

		initializeFirstAndLastParkingActPlanElemIndex();

	}

	@Override
	public void reset(int iter) {
		super.reset(iter);
		
		lastParkingFacilityIdOfDay = new HashMap<Id, Id>();

		mostRecentDepartureTime = new HashMap<Id, Double>();
		firstParkingWalkTimeOfDay = new HashMap<Id, Double>();
		lastParkingWalkTimeOfDay = new HashMap<Id, Double>();

		firstParkingDepartureTimeOfDay = new HashMap<Id, Double>();
		lastParkingArrivalTimeOfDay = new HashMap<Id, Double>();

		endTimeOfFirstActivityOfDay = new HashMap<Id, Double>();
		startTimeOfLastActivityOfDay = new HashMap<Id, Double>();

		walkDurationFromParking = new HashMap<Id, Double>();
		walkDurationToParking = new HashMap<Id, Double>();

		parkingArrivalTime = new HashMap<Id, Double>();
		parkingDepartureTime = new HashMap<Id, Double>();

		startTimeOfFirstActivityAfterParkingCar = new HashMap<Id, Double>();
		endTimeOfLastActivityBeforeLeavingWithCar = new HashMap<Id, Double>();

		firstParkingActivityPlanElemIndex = new HashMap<Id, Integer>();
		lastParkingActivityPlanElemIndex = new HashMap<Id, Integer>();
		
		parkingSearchStartTime=new HashMap<Id, Double>();
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		super.handleEvent(event);

		Id personId = event.getPersonId();
		correctCurrentPlanElementIndexIfWrongForLeg(personId,event.getLegMode());
		
		
		double departureTime = event.getTime();
		mostRecentDepartureTime.put(personId, departureTime);
		
		if (firstParkingActivityPlanElemIndex.get(personId)+1==currentPlanElementIndex){
			firstParkingDepartureTimeOfDay.put(personId, departureTime);
		}
		
		if (event.getLegMode().equals(TransportMode.car)){
			parkingDepartureTime.put(personId, departureTime);
		}
		
		
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		super.handleEvent(event);
		double arrivalTime = event.getTime();
		Id personId = event.getPersonId();
		
		correctCurrentPlanElementIndexIfWrongForLeg(personId,event.getLegMode());
		
		if (event.getLegMode().equals(TransportMode.walk)) {
			if (previousActivityIsParking(personId)){
				walkDurationFromParking.put(personId, arrivalTime-mostRecentDepartureTime.get(personId));
				
				if (lastParkingActivityPlanElemIndex.get(personId) < currentPlanElementIndex) {
					lastParkingWalkTimeOfDay.put(personId, arrivalTime-mostRecentDepartureTime.get(personId));
				}
			} else if (nextActivityIsParking(personId)){
				walkDurationToParking.put(personId, arrivalTime-mostRecentDepartureTime.get(personId));
				
				if (firstParkingActivityPlanElemIndex.get(personId) > currentPlanElementIndex) {
					firstParkingWalkTimeOfDay.put(personId, arrivalTime-mostRecentDepartureTime.get(personId));
				}
			}
		}
		
		if (lastParkingActivityPlanElemIndex.get(personId)-1==currentPlanElementIndex){
			lastParkingArrivalTimeOfDay.put(personId, event.getTime());
		}
		
		if (event.getLegMode().equals(TransportMode.car)){
			parkingArrivalTime.put(personId, arrivalTime);
		}
		
		
	}

	private void correctCurrentPlanElementIndexIfWrongForLeg(Id personId, String legMode) {
		currentPlanElementIndex = agents.get(personId).getCurrentPlanElementIndex();
		List<PlanElement> planElements = agents.get(personId).getSelectedPlan().getPlanElements();

		if (planElements.get(currentPlanElementIndex) instanceof ActivityImpl){
			LegImpl prevLeg = (LegImpl) planElements.get(currentPlanElementIndex-1);
			LegImpl nextLeg = (LegImpl) planElements.get(currentPlanElementIndex+1);
		
			if (legMode.equals(prevLeg.getMode())) {
				currentPlanElementIndex--;
			} else if (legMode.equals(nextLeg.getMode())){
				currentPlanElementIndex++;
			} else {
				DebugLib.stopSystemAndReportInconsistency();
			}
		}
	}

	private boolean previousActivityIsParking(Id personId) {
		ActivityImpl activityImpl = (ActivityImpl) agents.get(personId).getSelectedPlan().getPlanElements().get(currentPlanElementIndex-1);
		return activityImpl.getType().equalsIgnoreCase("parking");
	}

	private boolean nextActivityIsParking(Id personId) {
		ActivityImpl activityImpl = (ActivityImpl) agents.get(personId).getSelectedPlan().getPlanElements().get(currentPlanElementIndex+1);
		return activityImpl.getType().equalsIgnoreCase("parking");
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		super.handleEvent(event);
		Id personId = event.getPersonId();
		
		correctCurrentPlanElementIndexIfWrongForActivity(personId,event.getActType());
		
		double time = event.getTime();
		
		if (event.getActType().equalsIgnoreCase("parking")) {
			lastParkingFacilityIdOfDay.put(personId, event.getFacilityId());
		}
		
		if (lastParkingActivityPlanElemIndex.get(personId)+2==currentPlanElementIndex){
			startTimeOfLastActivityOfDay.put(personId, event.getTime());
		}
		
		if (firstParkingActivityPlanElemIndex.get(personId)<currentPlanElementIndex){
			if (((ActivityImpl) agents.get(personId).getSelectedPlan().getPlanElements().get(currentPlanElementIndex-2)).getType().equalsIgnoreCase("parking")){
				startTimeOfFirstActivityAfterParkingCar.put(personId, time);
			}
		}
		
		

	}
	
	
	private void correctCurrentPlanElementIndexIfWrongForActivity(Id personId, String actType) {
		currentPlanElementIndex = agents.get(personId).getCurrentPlanElementIndex();
		List<PlanElement> planElements = agents.get(personId).getSelectedPlan().getPlanElements();

		if (planElements.get(currentPlanElementIndex) instanceof LegImpl){
			ActivityImpl prevAct = (ActivityImpl) planElements.get(currentPlanElementIndex-1);
			ActivityImpl nextAct = (ActivityImpl) planElements.get(currentPlanElementIndex+1);
		
			if (actType.equals(prevAct.getType())) {
				currentPlanElementIndex--;
			} else if (actType.equals(nextAct.getType())){
				currentPlanElementIndex++;
			} else {
				DebugLib.stopSystemAndReportInconsistency();
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		super.handleEvent(event);
		double time = event.getTime();
		Id personId = event.getPersonId();
		
		correctCurrentPlanElementIndexIfWrongForActivity(personId,event.getActType());

		PlanBasedWithinDayAgent planBasedWithinDayAgent = agents.get(personId);
		if (firstParkingActivityPlanElemIndex.get(personId)-2==currentPlanElementIndex){
			endTimeOfFirstActivityOfDay.put(personId, event.getTime());
		}
		
		if (lastParkingActivityPlanElemIndex.get(personId)>currentPlanElementIndex){
			List<PlanElement> planElements = planBasedWithinDayAgent.getSelectedPlan().getPlanElements();
			
			
			
			if (event.getActType().equalsIgnoreCase("parking")){
				endTimeOfLastActivityBeforeLeavingWithCar.put(personId, time);
			}
		}
		
		
	}

	private void initializeFirstAndLastParkingActPlanElemIndex() {
		for (PlanBasedWithinDayAgent agent : this.agents.values()) {
			Plan executedPlan = agent.getSelectedPlan();

			for (int i = 0; i < executedPlan.getPlanElements().size(); i++) {
				Id personId = agent.getId();
				if (!firstParkingActivityPlanElemIndex.containsKey(personId)) {
					if (executedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
						Activity act = (Activity) executedPlan.getPlanElements().get(i);
						if (act.getType().equalsIgnoreCase("parking")) {
							firstParkingActivityPlanElemIndex.put(personId, i);
							break;
						}
					}
				}
			}

			for (int i = executedPlan.getPlanElements().size() - 1; i >= 0; i--) {
				Id personId = agent.getId();
				if (!lastParkingActivityPlanElemIndex.containsKey(personId)) {
					if (executedPlan.getPlanElements().get(i) instanceof ActivityImpl) {
						Activity act = (Activity) executedPlan.getPlanElements().get(i);
						if (act.getType().equalsIgnoreCase("parking")) {
							lastParkingActivityPlanElemIndex.put(personId, i);
							break;
						}
					}
				}
			}
		}
	}

	public HashMap<Id, Double> getParkingSearchStartTime() {
		return parkingSearchStartTime;
	}

}
