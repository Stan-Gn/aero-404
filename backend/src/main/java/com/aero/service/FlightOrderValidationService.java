package com.aero.service;

import com.aero.domain.CrewMember;
import com.aero.domain.FlightOrder;
import com.aero.domain.Helicopter;
import com.aero.exception.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;

@Service
public class FlightOrderValidationService {

    public void validate(FlightOrder order) {
        LocalDate flightDate = order.getPlannedDeparture().toLocalDate();
        Helicopter helicopter = order.getHelicopter();
        CrewMember pilot = order.getPilot();

        validateHelicopterReview(helicopter, flightDate);
        validatePilotLicense(pilot, flightDate);
        validateCrewTraining(pilot, order.getCrewMembers(), flightDate);
        validateCrewWeight(order.getCrewWeight(), helicopter.getMaxPayload());
        validateRouteRange(order.getEstimatedRouteKm(), helicopter.getRangeKm());
    }

    private void validateHelicopterReview(Helicopter helicopter, LocalDate flightDate) {
        if (helicopter.getReviewDate() == null || helicopter.getReviewDate().isBefore(flightDate)) {
            throw new HelicopterReviewExpiredException(
                    "Helicopter " + helicopter.getRegNumber() + " does not have a valid review for flight date " + flightDate);
        }
    }

    private void validatePilotLicense(CrewMember pilot, LocalDate flightDate) {
        if (pilot.getLicenseExpiry() == null || pilot.getLicenseExpiry().isBefore(flightDate)) {
            throw new PilotLicenseExpiredException(
                    "Pilot " + pilot.getFirstName() + " " + pilot.getLastName() + " does not have a valid license for flight date " + flightDate);
        }
    }

    private void validateCrewTraining(CrewMember pilot, Set<CrewMember> crewMembers, LocalDate flightDate) {
        checkTraining(pilot, flightDate);
        if (crewMembers != null) {
            for (CrewMember member : crewMembers) {
                checkTraining(member, flightDate);
            }
        }
    }

    private void checkTraining(CrewMember member, LocalDate flightDate) {
        if (member.getTrainingExpiry() == null || member.getTrainingExpiry().isBefore(flightDate)) {
            throw new CrewMemberTrainingExpiredException(
                    "Crew member " + member.getFirstName() + " " + member.getLastName() + " does not have valid training for flight date " + flightDate);
        }
    }

    private void validateCrewWeight(Integer crewWeight, Integer maxPayload) {
        if (crewWeight != null && crewWeight > maxPayload) {
            throw new CrewWeightExceededException(
                    "Crew weight " + crewWeight + " kg exceeds helicopter max payload " + maxPayload + " kg");
        }
    }

    private void validateRouteRange(Integer estimatedRouteKm, Integer rangeKm) {
        if (estimatedRouteKm != null && estimatedRouteKm > rangeKm) {
            throw new RouteExceedsRangeException(
                    "Estimated route " + estimatedRouteKm + " km exceeds helicopter range " + rangeKm + " km");
        }
    }
}
