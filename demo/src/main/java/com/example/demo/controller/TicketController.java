package com.example.demo.controller;

import com.example.demo.model.Passenger;
import com.example.demo.model.TicketDTO;
import com.example.demo.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class TicketController {

    private final PdfService pdfService;
    public final Map<String, com.example.demo.model.Airline> airlines = PdfService.AIRLINES;

    public TicketController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("airlines", airlines.values());
        return "form";
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam("passengerType") String[] passengerTypes,
            @RequestParam("passengerName") String[] passengerNames,
            @RequestParam(value = "handLuggage", required = false) String[] handLuggages,
            @RequestParam(value = "handLuggageKg", defaultValue = "0") int[] handLuggageKgs,
            @RequestParam(value = "checkedLuggage", required = false) String[] checkedLuggages,
            @RequestParam(value = "checkedLuggageCount", defaultValue = "0") int[] checkedLuggageCounts,
            @RequestParam(value = "checkedLuggageKg", defaultValue = "0") int[] checkedLuggageKgs,
            @RequestParam("route") String route,
            @RequestParam("departureTime") String departureTime,
            @RequestParam("arrivalTime") String arrivalTime,
            @RequestParam("airlineCode") String airlineCode,
            @RequestParam("price") double price,
            @RequestParam("rules") String rules,
            @RequestParam("pnr") String pnr) throws Exception {
        
        List<Passenger> passengers = new ArrayList<>();
        for (int i = 0; i < passengerTypes.length; i++) {
            Passenger passenger = new Passenger();
            passenger.setType(passengerTypes[i]);
            passenger.setFullName(passengerNames[i]);
            
            passenger.setHandLuggage(handLuggages != null && i < handLuggages.length && "on".equals(handLuggages[i]));
            passenger.setHandLuggageKg(handLuggageKgs[i]);
            
            passenger.setCheckedLuggage(checkedLuggages != null && i < checkedLuggages.length && "on".equals(checkedLuggages[i]));
            passenger.setCheckedLuggageCount(checkedLuggageCounts[i]);
            passenger.setCheckedLuggageKg(checkedLuggageKgs[i]);
            
            passengers.add(passenger);
        }
        
        TicketDTO ticket = new TicketDTO();
        ticket.setPassengers(passengers);
        ticket.setRoute(route);
        ticket.setDepartureTime(departureTime);
        ticket.setArrivalTime(arrivalTime);
        ticket.setAirlineCode(airlineCode);
        ticket.setPrice(price);
        ticket.setRules(rules);
        ticket.setPnr(pnr);
        
        byte[] pdfBytes = pdfService.generateTicketPdf(ticket);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}