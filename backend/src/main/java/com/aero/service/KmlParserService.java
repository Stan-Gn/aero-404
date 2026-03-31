package com.aero.service;

import com.aero.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class KmlParserService {

    private static final Logger log = LoggerFactory.getLogger(KmlParserService.class);

    private static final int MAX_POINTS = 5000;
    private static final double MIN_LAT = 49.0;
    private static final double MAX_LAT = 55.0;
    private static final double MIN_LON = 14.0;
    private static final double MAX_LON = 24.0;

    public List<double[]> parse(String kmlContent) {
        if (kmlContent == null || kmlContent.isBlank()) {
            throw new ValidationException("KML content cannot be empty");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(kmlContent.getBytes(StandardCharsets.UTF_8)));

            NodeList coordinatesNodes = doc.getElementsByTagName("coordinates");
            if (coordinatesNodes.getLength() == 0) {
                throw new ValidationException("KML file does not contain any coordinates");
            }

            List<double[]> allPoints = new ArrayList<>();

            for (int i = 0; i < coordinatesNodes.getLength(); i++) {
                String rawCoordinates = coordinatesNodes.item(i).getTextContent().trim();
                String[] tuples = rawCoordinates.split("\\s+");

                for (String tuple : tuples) {
                    if (tuple.isBlank()) continue;

                    String[] parts = tuple.split(",");
                    if (parts.length < 2) {
                        throw new ValidationException("Invalid coordinate format: " + tuple);
                    }

                    double lon = Double.parseDouble(parts[0].trim());
                    double lat = Double.parseDouble(parts[1].trim());

                    if (lat < MIN_LAT || lat > MAX_LAT || lon < MIN_LON || lon > MAX_LON) {
                        throw new ValidationException(
                                String.format("Coordinate (%.6f, %.6f) is outside Poland bounding box", lat, lon));
                    }

                    allPoints.add(new double[]{lat, lon});
                }
            }

            if (allPoints.isEmpty()) {
                throw new ValidationException("KML file does not contain any valid coordinates");
            }

            if (allPoints.size() > MAX_POINTS) {
                throw new ValidationException(
                        String.format("KML file contains %d points, maximum allowed is %d", allPoints.size(), MAX_POINTS));
            }

            log.info("Parsed {} points from KML", allPoints.size());
            return allPoints;

        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to parse KML file: " + e.getMessage());
        }
    }
}
