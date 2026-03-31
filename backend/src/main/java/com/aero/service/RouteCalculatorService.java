package com.aero.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteCalculatorService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public int calculateRouteKm(List<double[]> points) {
        if (points == null || points.size() < 2) {
            return 0;
        }

        double totalKm = 0.0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalKm += haversine(
                    points.get(i)[0], points.get(i)[1],
                    points.get(i + 1)[0], points.get(i + 1)[1]
            );
        }

        return (int) Math.round(totalKm);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public String pointsToJson(List<double[]> points) {
        if (points == null || points.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("[%.6f,%.6f]", points.get(i)[0], points.get(i)[1]));
        }
        sb.append("]");
        return sb.toString();
    }
}
