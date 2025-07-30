package com.example.demo.controller;

import com.example.demo.model.FlightDTO;
import com.example.demo.model.Passenger;
import com.example.demo.model.TicketDTO;
import com.example.demo.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TicketController {

    private final PdfService pdfService;

    public TicketController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/")
    public String showForm() {
        return "form";
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam("passengerType[]") String[] passengerTypes,
            @RequestParam("passengerName[]") String[] passengerNames,
            @RequestParam(value = "handLuggage[]", required = false) String[] handLuggages,
            @RequestParam(value = "handLuggageKg[]", defaultValue = "0") int[] handLuggageKgs,
            @RequestParam(value = "checkedLuggage[]", required = false) String[] checkedLuggages,
            @RequestParam(value = "checkedLuggageCount[]", defaultValue = "0") int[] checkedLuggageCounts,
            @RequestParam(value = "checkedLuggageKg[]", defaultValue = "0") int[] checkedLuggageKgs,
            @RequestParam("route[]") String[] routes,
            @RequestParam("departureTime[]") String[] departureTimes,
            @RequestParam("arrivalTime[]") String[] arrivalTimes,
            @RequestParam("airlineCode[]") String[] airlineCodes,
            @RequestParam("airlineName[]") String[] airlineNames,
            @RequestParam("price[]") double[] prices,
            @RequestParam("rules") String rules,
            @RequestParam("pnr[]") String[] pnrs,
            @RequestParam(value = "samePnr", required = false) Boolean samePnr,
            @RequestParam("logoUrl[]") String[] logoUrls) throws Exception {

        List<Passenger> passengers = new ArrayList<>();
        for (int i = 0; i < passengerTypes.length; i++) {
            Passenger passenger = new Passenger();
            passenger.setType(passengerTypes[i]);
            passenger.setFullName(passengerNames[i]);

            boolean hasHandLuggage = handLuggages != null &&
                    i < handLuggages.length &&
                    "on".equals(handLuggages[i]);

            boolean hasCheckedLuggage = checkedLuggages != null &&
                    i < checkedLuggages.length &&
                    "on".equals(checkedLuggages[i]);

            passenger.setHandLuggage(hasHandLuggage);
            passenger.setHandLuggageKg(hasHandLuggage ? handLuggageKgs[i] : 0);

            passenger.setCheckedLuggage(hasCheckedLuggage);
            passenger.setCheckedLuggageCount(hasCheckedLuggage ? checkedLuggageCounts[i] : 0);
            passenger.setCheckedLuggageKg(hasCheckedLuggage ? checkedLuggageKgs[i] : 0);

            passengers.add(passenger);
        }

        List<FlightDTO> flights = new ArrayList<>();
        for (int i = 0; i < routes.length; i++) {
            FlightDTO flight = new FlightDTO();
            flight.setRoute(routes[i]);
            flight.setDepartureTime(departureTimes[i]);
            flight.setArrivalTime(arrivalTimes[i]);
            flight.setAirlineCode(airlineCodes[i]);
            flight.setAirlineName(airlineNames[i]);
            flight.setPrice(prices[i]);
            flight.setLogoUrl(logoUrls[i]);

            // Gestione PNR: se samePnr è true, usa il primo PNR per tutti i voli
            if (samePnr != null && samePnr) {
                flight.setPnr(pnrs.length > 0 ? pnrs[0] : "");
            } else {
                flight.setPnr(i < pnrs.length ? pnrs[i] : "");
            }

            flights.add(flight);
        }

        TicketDTO ticket = new TicketDTO();
        ticket.setPassengers(passengers);
        ticket.setFlights(flights);
        ticket.setRules(rules);

        byte[] pdfBytes = pdfService.generateTicketPdf(ticket);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/fetch-flight")
    @ResponseBody
    public FlightDTO fetchFlight(
            @RequestParam("flightNumber") String flightNumber,
            @RequestParam("date") String date) {
        return pdfService.fetchFlightData(flightNumber, date);
    }
}