package com.example.demo.service;

import com.example.demo.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.*;

@Service
public class PdfService {

    private static final String AGENCY_LOGO_NAME = "agency-logo.png";
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float RULES_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float LOGO_HEIGHT = 60;
    private static final float LOGO_WIDTH = 150;

    // Define static fonts
    private static final PDFont FONT_BOLD = new PDType1Font(HELVETICA_BOLD);
    private static final PDFont FONT_NORMAL = new PDType1Font(HELVETICA);
    private static final PDFont FONT_ITALIC = new PDType1Font(HELVETICA_OBLIQUE);

    @Value("${app.logos.path:classpath:static/logos/}")
    private String logosPath;

    @Value("${aviationstack.api.key}")
    private String aviationApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public FlightDTO fetchFlightData(String flightNumber, String date) {
        String url = "https://api.aviationstack.com/v1/flights" +
                "?access_key=" + aviationApiKey +
                "&flight_iata=" + flightNumber +
                "&date=" + date;
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response != null && response.containsKey("data")) {
            List<Map<String, Object>> flights = (List<Map<String, Object>>) response.get("data");
            if (!flights.isEmpty()) {
                FlightDTO flight = getFlightDTO(flights);

                return flight;
            }
        }
        return null;
    }

    private static FlightDTO getFlightDTO(List<Map<String, Object>> flights) {
        Map<String, Object> flightData = flights.get(0);
        Map<String, String> departure = (Map<String, String>) flightData.get("departure");
        Map<String, String> arrival = (Map<String, String>) flightData.get("arrival");
        Map<String, String> airline = (Map<String, String>) flightData.get("airline");

        FlightDTO flight = new FlightDTO();
        flight.setRoute(departure.get("iata") + "-" + arrival.get("iata"));
        flight.setDepartureTime(departure.get("scheduled").replace("T", " "));
        flight.setArrivalTime(arrival.get("scheduled").replace("T", " "));
        flight.setAirlineCode(airline.get("iata"));
        flight.setAirlineName(airline.get("name"));
        flight.setLogoUrl("https://logo.clearbit.com/" + airline.get("name").toLowerCase().replace(" ", "") + ".com");
        return flight;
    }

    public byte[] generateTicketPdf(TicketDTO ticket) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float finalYPosition = 0;
            float pageHeight = page.getMediaBox().getHeight();

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Header con logo agenzia (dimensione fissa)
                float y = pageHeight - 70;
                try {
                    PDImageXObject agencyImg = loadLogo(document, AGENCY_LOGO_NAME);
                    contentStream.drawImage(agencyImg, PAGE_WIDTH - MARGIN - LOGO_WIDTH, y - LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
                } catch (Exception e) {
                    System.err.println("Error loading agency logo: " + e.getMessage());
                }

                // Separator line
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(MARGIN, y - 80);
                contentStream.lineTo(PAGE_WIDTH - MARGIN, y - 80);
                contentStream.stroke();

                // Dettagli passeggeri
                contentStream.setFont(FONT_BOLD, 14);
                y -= 100;
                drawText(contentStream, "PASSEGGERI", MARGIN, y);
                y -= 30;

                // Tabella passeggeri
                float tableStartY = y;
                float[] columnWidths = {60, 150, 80, 80, 80};
                float rowHeight = 20;

                contentStream.setFont(FONT_BOLD, 10);
                drawTableRow(contentStream, MARGIN, tableStartY,
                        new String[]{"TIPO", "NOME E COGNOME", "BAG. A MANO", "KG", "BAG. STIVA"},
                        columnWidths);

                contentStream.setFont(FONT_NORMAL, 10);
                for (Passenger passenger : ticket.getPassengers()) {
                    tableStartY -= rowHeight;
                    String handLuggage = passenger.hasHandLuggage() ? "SI" : "NO";
                    String handLuggageKg = passenger.hasHandLuggage() ? passenger.getHandLuggageKg() + "kg" : "-";
                    String checkedLuggage = passenger.hasCheckedLuggage() ?
                            passenger.getCheckedLuggageCount() + " bag (" + passenger.getCheckedLuggageKg() + "kg)" : "NO";

                    drawTableRow(contentStream, MARGIN, tableStartY,
                            new String[]{
                                    passenger.getType().toUpperCase(),
                                    passenger.getFullName(),
                                    handLuggage,
                                    handLuggageKg,
                                    checkedLuggage
                            },
                            columnWidths);
                }
                y = tableStartY - 40;

                // Voli
                contentStream.setFont(FONT_BOLD, 14);
                drawText(contentStream, "VOLI", MARGIN, y);
                y -= 30;

                for (FlightDTO flight : ticket.getFlights()) {
                    contentStream.setFont(FONT_BOLD, 12);
                    drawText(contentStream, "Volo: " + flight.getRoute(), MARGIN, y);
                    y -= 20;
                    
                    drawText(contentStream, "Partenza: " + flight.getDepartureTime(), MARGIN, y);
                    y -= 20;
                    
                    drawText(contentStream, "Arrivo: " + flight.getArrivalTime(), MARGIN, y);
                    y -= 20;
                    
                    drawText(contentStream, "Compagnia: " + flight.getAirlineName(), MARGIN, y);
                    y -= 20;
                    
                    drawText(contentStream, "PNR: " + flight.getPnr(), MARGIN, y);
                    y -= 20;
                    
                    drawText(contentStream, "Prezzo: €" + String.format("%.2f", flight.getPrice()), MARGIN, y);
                    y -= 30;
                    
                    // Logo compagnia (dimensione fissa)
                    try {
                        PDImageXObject airlineImg = loadImageFromUrl(document, flight.getLogoUrl());
                        if (airlineImg != null) {
                            contentStream.drawImage(airlineImg, MARGIN, y - LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading airline logo: " + e.getMessage());
                    }
                    y -= LOGO_HEIGHT + 20;
                }

                // Prezzo totale
                double totalPrice = ticket.getFlights().stream().mapToDouble(FlightDTO::getPrice).sum();
                contentStream.setFont(FONT_BOLD, 12);
                drawText(contentStream, "PREZZO TOTALE: €" + String.format("%.2f", totalPrice), MARGIN, y);
                y -= 30;

                // Save final Y position
                finalYPosition = y;
            }

            // Sezione regole
            PDPage rulesPage = page;
            boolean newPageCreated = false;

            if (finalYPosition < 150) {
                rulesPage = new PDPage(PDRectangle.A4);
                document.addPage(rulesPage);
                newPageCreated = true;
            }

            try (PDPageContentStream rulesContentStream = new PDPageContentStream(
                    document, rulesPage, PDPageContentStream.AppendMode.APPEND,
                    true)) {

                float rulesY = newPageCreated ? rulesPage.getMediaBox().getHeight() - 50 : finalYPosition;

                // Tabella regole per compagnia

                // Raggruppa regole per compagnia (senza duplicati)
                Map<String, String> distinctRules = ticket.getFlights().stream()
                        .collect(Collectors.toMap(
                                FlightDTO::getAirlineName,
                                f -> getAirlineRules(f.getAirlineName()),
                                (existing, replacement) -> existing));

                Map<String, String> airlineRules = new HashMap<>(distinctRules);

                // Intestazione tabella
                rulesContentStream.setFont(FONT_BOLD, 11);
                drawText(rulesContentStream, "REGOLAMENTO DI VIAGGIO", MARGIN, rulesY);
                rulesY -= 30;

                // Calcola dimensioni colonne
                int numAirlines = airlineRules.size();
                float colWidth = (PAGE_WIDTH - 2 * MARGIN) / numAirlines;
                float rowHeight = 20;
                float tableStartY = rulesY;

                // Intestazioni colonne
                rulesContentStream.setFont(FONT_BOLD, 9);
                float x = MARGIN;
                for (String airline : airlineRules.keySet()) {
                    drawText(rulesContentStream, "Regole compagnia " + airline, x, tableStartY);
                    x += colWidth;
                }
                tableStartY -= rowHeight;

                // Dettagli regole
                rulesContentStream.setFont(FONT_NORMAL, 8);
                x = MARGIN;
                for (Map.Entry<String, String> entry : airlineRules.entrySet()) {
                    rulesY = drawWrappedText(rulesContentStream, entry.getValue(), x, tableStartY, colWidth - 10, 10);
                    x += colWidth;
                }

                // Footer
                rulesContentStream.setFont(FONT_ITALIC, 8);
                drawText(rulesContentStream, "Biglietto generato automaticamente - Documento non trasferibile",
                        MARGIN, 50);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }

    private String getAirlineRules(String airlineName) {
        // Mappa statica per regole note
        Map<String, String> staticRules = new HashMap<>();
        staticRules.put("Alitalia", "Bagaglio a mano incluso: 1 pezzo max 8kg\nBagaglio in stiva: 23kg a pagamento\n\nRichieste di cancellazione: Non rimborsabile");
        staticRules.put("Lufthansa", "Bagaglio a mano: 1 pezzo + 1 personale\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione: Rimborso completo fino a 48 ore");
        staticRules.put("Air France", "Bagaglio a mano: 1 pezzo max 12kg\nBagaglio in stiva: 23kg incluso per voli intercontinentali");
        
        return staticRules.getOrDefault(airlineName, 
            "Bagaglio a mano: 1 pezzo max 8kg\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione: Rimborso completo fino a 48 ore prima del volo.");
    }

    private PDImageXObject loadImageFromUrl(PDDocument document, String url) throws IOException {
        try {
            byte[] imageBytes = restTemplate.getForObject(url, byte[].class);
            if (imageBytes != null && imageBytes.length > 0) {
                return PDImageXObject.createFromByteArray(document, imageBytes, url);
            }
        } catch (Exception e) {
            System.err.println("Error loading image from URL: " + url);
        }
        return null;
    }

    private PDImageXObject loadLogo(PDDocument document, String logoName) throws IOException {
        // Prima prova a caricare dal percorso esterno
        if (logosPath.startsWith("classpath:")) {
            // Caricamento da classpath (interno al JAR)
            String classpathPath = logosPath.substring(10) + logoName;
            try (InputStream is = new ClassPathResource(classpathPath).getInputStream()) {
                return PDImageXObject.createFromByteArray(document, is.readAllBytes(), logoName);
            }
        } else {
            // Caricamento da filesystem esterno
            File logoFile = new File(logosPath, logoName);
            if (logoFile.exists()) {
                return PDImageXObject.createFromFile(logoFile.getAbsolutePath(), document);
            } else {
                // Fallback al classpath
                try (InputStream is = new ClassPathResource("static/logos/" + logoName).getInputStream()) {
                    return PDImageXObject.createFromByteArray(document, is.readAllBytes(), logoName);
                }
            }
        }
    }

    private void drawText(PDPageContentStream contentStream, String text, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text != null ? text : "");
        contentStream.endText();
    }

    private void drawTableRow(PDPageContentStream contentStream, float startX, float y,
                              String[] texts, float[] columnWidths) throws IOException {
        float x = startX;
        for (int i = 0; i < texts.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(texts[i] != null ? texts[i] : "");
            contentStream.endText();
            x += columnWidths[i];
        }
    }

    private float drawWrappedText(PDPageContentStream contentStream, String text,
                                  float x, float y, float maxWidth, float leading) throws IOException {
        if (text == null || text.isEmpty()) return y;

        String[] paragraphs = text.split("\n");
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                y -= leading;
                continue;
            }

            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
                float testWidth = getStringWidth(testLine, FONT_NORMAL, 9);

                if (testWidth > maxWidth && currentLine.length() > 0) {
                    // Draw current line
                    drawText(contentStream, currentLine.toString(), x, y);
                    y -= leading;
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine = new StringBuilder(testLine);
                }

                // Draw last word
                if (i == words.length - 1) {
                    drawText(contentStream, currentLine.toString(), x, y);
                    y -= leading;
                }
            }

            // Add space between paragraphs
            y -= leading / 2;
        }
        return y;
    }

    private float getStringWidth(String text, PDFont font, float fontSize) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }
}
