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

/**
 * 
 */
package playground.ikaddoura.noise2;

import java.util.List;

/**
 * 
 * Contains general equations that are relevant to compute noise emission and immission levels, based on the German EWS approach 'Lange gerade Straßen'.
 * 
 * @author lkroeger, ikaddoura
 *
 */

public class NoiseEquations {
	
	public static double calculateMittelungspegelLm(int n, double p) {

		//	Der Beurteilungspegel L_r ist bei Straßenverkehrsgeraeuschen gleich dem Mittelungspegel L_m.
		//  L_r = L_m = 10 * lg(( 1 / T_r ) * (Integral)_T_r(10^(0,1*1(t))dt))
		//	L_m,e ist der Mittelungspegel im Abstand von 25m von der Achse der Schallausbreitung
				
		// 	M ... traffic volume
		// 	p ... share of hdv in %
				
		double pInPercentagePoints = p * 100.;	
		double mittelungspegel = 37.3 + 10* Math.log10(n * (1 + (0.082 * pInPercentagePoints)));
		
		return mittelungspegel;
	}
	
	public static double calculateGeschwindigkeitskorrekturDv (double vCar , double vHdv , double p) {
		
		// 	basically the speed is 100 km/h
		// 	p ... share of hdv, in percentage points
		
		double pInPercentagePoints = p * 100.;	

		double lCar = calculateLCar(vCar);
		double lHdv = calculateLHdv(vHdv);

		double d = lHdv - lCar; 
		
		double geschwindigkeitskorrekturDv = lCar - 37.3 + 10* Math.log10((100.0 + (Math.pow(10.0, (0.1 * d)) - 1) * pInPercentagePoints ) / (100 + 8.23 * pInPercentagePoints));
		
		return geschwindigkeitskorrekturDv;
	}
			
	public double calculateResultingNoiseImmission (List<Double> noiseImmissions){
		
		double resultingNoiseImmission = 0.;
		
		if (noiseImmissions.size() > 0) {
			double sumTmp = 0.;
			for(double noiseImmission : noiseImmissions){
				sumTmp = sumTmp + (Math.pow(10, (0.1 * noiseImmission)));
			}
			resultingNoiseImmission = 10 * Math.log10(sumTmp);
			if(resultingNoiseImmission < 0) {
				resultingNoiseImmission = 0.;
			}
		}
		return resultingNoiseImmission;
	}
	
	public double calculateShareOfResultingNoiseImmission (double noiseImmission , double resultingNoiseImmission){
		
		double shareOfResultingNoiseImmission = Math.pow(((Math.pow(10, (0.05 * noiseImmission))) / (Math.pow(10, (0.05 * resultingNoiseImmission)))), 2);
		return shareOfResultingNoiseImmission;	
	}

	public static double calculateLCar(double vCar) {
		
		double lCar = 27.7 + (10 * Math.log10(1.0 + Math.pow(0.02 * vCar, 3.0)));
		return lCar;
	}

	public static double calculateLHdv(double vHdv) {
		
		double lHdv = 23.1 + (12.5 * Math.log10(vHdv));
		return lHdv;
	}

	public static double calculateShare(int nVehicleType1, double lVehicleType1, int nVehicleType2, double lVehicleType2) {
		
		double share = ((nVehicleType1 * Math.pow(10, 0.1 * lVehicleType1)) / ((nVehicleType1 * Math.pow(10, 0.1 * lVehicleType1)) + (nVehicleType2 * Math.pow(10, 0.1 * lVehicleType2))));
		return share;
	}	
	
}
