/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.population;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.internal.MatsimToplevelContainer;

/**
 * Stores links between individual plans,
 * as defined by joint plans.
 * <br>
 * Using the {@link JointPlanFactory} to create instances of individual
 * plans will allow to cache the JointPlan in the Plan instance,
 * avoiding expensive Map lookups.
 * Howerver, <b>this makes the behavior when using several JointPlan containers
 * ill-defined!</b>
 *
 * @author thibautd
 */
public class JointPlans implements MatsimToplevelContainer {
	public static final String ELEMENT_NAME = "jointPlans";

	private final Map<Plan, JointPlan> planToJointPlan = new ConcurrentHashMap<Plan, JointPlan>();

	private final JointPlanFactory factory = new JointPlanFactory();
	
	public JointPlan getJointPlan(final Plan indivPlan) {
		if ( indivPlan instanceof PlanWithCachedJointPlan ) {
			return ((PlanWithCachedJointPlan) indivPlan).getJointPlan();
		}
		return planToJointPlan.get( indivPlan );
	}

	public void removeJointPlan(final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				if ( indivPlan instanceof PlanWithCachedJointPlan ) {
					final PlanWithCachedJointPlan withCache = (PlanWithCachedJointPlan) indivPlan;

					if (withCache.getJointPlan() != jointPlan) throw new PlanLinkException( withCache.getJointPlan()+" differs from "+indivPlan );

					withCache.resetJointPlan();
				}
				else {
					final Object removed = planToJointPlan.remove( indivPlan );
					if (removed != jointPlan) throw new PlanLinkException( removed+" differs from "+indivPlan );
				}
			}
		}
	}

	public void addJointPlan(final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				if ( indivPlan instanceof PlanWithCachedJointPlan ) {
					final PlanWithCachedJointPlan withCache = (PlanWithCachedJointPlan) indivPlan;

					if ( withCache.getJointPlan() != null && withCache.getJointPlan() != jointPlan) {
						throw new PlanLinkException( withCache.getJointPlan()+" was associated to "+indivPlan+
								" while trying to associate "+jointPlan );
					}

					withCache.setJointPlan( jointPlan );
				}
				else {
					final Object removed = planToJointPlan.put( indivPlan , jointPlan );
					if (removed != null && removed != jointPlan) {
						throw new PlanLinkException( removed+" was associated to "+indivPlan+
								" while trying to associate "+jointPlan );
					}
				}
			}
		}
	}
	
	public boolean contains( final JointPlan jointPlan) {
		synchronized (jointPlan) {
			for (Plan indivPlan : jointPlan.getIndividualPlans().values()) {
				if ( indivPlan instanceof PlanWithCachedJointPlan ) {
					if ( ((PlanWithCachedJointPlan) indivPlan).getJointPlan() == jointPlan ) {
						return true;
					}
				}
				else {
					final Object removed = planToJointPlan.get( indivPlan );
					if (removed != null && removed == jointPlan) {
						return true;
					}
				}
			}
			return false;
		}
	}

	public void addJointPlans(final Iterable<JointPlan> jointPlans) {
		for (JointPlan jp : jointPlans) addJointPlan( jp );
	}

	@Override
	public JointPlanFactory getFactory() {
		return factory;
	}

	public static class PlanLinkException extends RuntimeException {
		private static final long serialVersionUID = -6189128092802956514L;

		private PlanLinkException( final String msg ) {
			super( msg );
		}
	}


}

