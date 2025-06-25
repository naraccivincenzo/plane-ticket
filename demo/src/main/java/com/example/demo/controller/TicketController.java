package com.example.demo.controller;

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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Controller
public class TicketController {

    private final PdfService pdfService;

    public TicketController(PdfService pdfService) {
        this.pdfService = pdfService;
    }

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("ticket", new TicketDTO());
        return "form";
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestParam String passengers,
            @RequestParam String route,
            @RequestParam String departureTime,
            @RequestParam String arrivalTime,
            @RequestParam String airline,
            @RequestParam double price,
            @RequestParam String rules,
            @RequestParam MultipartFile airlineLogo,
            @RequestParam MultipartFile agencyLogo) throws IOException {
        
        TicketDTO ticket = new TicketDTO();
        List<String> passengerList = Arrays.asList(passengers.split("\\s*,\\s*"));
        
        ticket.setPassengers(passengerList);
        ticket.setRoute(route);
        ticket.setDepartureTime(departureTime);
        ticket.setArrivalTime(arrivalTime);
        ticket.setAirline(airline);
        ticket.setPrice(price);
        ticket.setRules(rules);
        ticket.setAirlineLogo(airlineLogo);
        ticket.setAgencyLogo(agencyLogo);
        
        byte[] pdfBytes = pdfService.generateTicketPdf(ticket);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ticket.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}