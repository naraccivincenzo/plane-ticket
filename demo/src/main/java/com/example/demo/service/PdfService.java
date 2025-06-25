package com.example.demo.service;

import com.example.demo.model.Airline;
import com.example.demo.model.Passenger;
import com.example.demo.model.TicketDTO;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.*;

@Service
public class PdfService {

    public static final Map<String, Airline> AIRLINES = new HashMap<>();
    private static final String AGENCY_LOGO_NAME = "agency-logo.png";
    private static final float MARGIN = 50;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float RULES_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    // Define static fonts for consistent usage
    private static final PDFont FONT_BOLD = new PDType1Font(HELVETICA_BOLD);
    private static final PDFont FONT_NORMAL = new PDType1Font(HELVETICA);
    private static final PDFont FONT_ITALIC = new PDType1Font(HELVETICA_OBLIQUE);

    // Percorso configurabile per i loghi
    @Value("${app.logos.path:classpath:static/logos/}")
    private String logosPath;

    static {
        // Initialize airlines with rules and colors
        AIRLINES.put("AZ", new Airline("AZ", "Alitalia", "alitalia.png",
                "Bagaglio a mano incluso: 1 pezzo max 8kg\nBagaglio in stiva: 23kg a pagamento\n\nRichieste di cancellazione:\nI biglietti non sono rimborsabili tranne nei casi in cui la compagnia aerea annulli la prenotazione o sposti l'orario di partenza in modo significativo.\n\nCambi NOME:\nPermesso di cambio nome fino a 24 ore prima della partenza.\n\nDocumenti:\nAssicurati di avere documenti di identità validi per tutti i paesi che visiterai.",
                "#0066CC", "#FFFFFF"));

        AIRLINES.put("LH", new Airline("LH", "Lufthansa", "lufthansa.png",
                "Bagaglio a mano incluso: 1 pezzo + 1 personale\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione:\nRimborso completo fino a 48 ore prima del volo.\n\nCambi NOME:\nModifiche consentite con penale di 50€.\n\nDocumenti:\nPassaporto obbligatorio per voli extra-Schengen.",
                "#001E49", "#D80621"));

        AIRLINES.put("AF", new Airline("AF", "Air France", "airfrance.png",
                "Bagaglio a mano: 1 pezzo max 12kg\nBagaglio in stiva: 23kg incluso per voli intercontinentali\n\nRichieste di cancellazione:\nPenale del 20% per cancellazioni entro 7 giorni.\n\nCambi NOME:\nConsentito solo per errori di battitura entro 24 ore.\n\nDocumenti:\nVisto richiesto per alcune destinazioni, verificare prima della partenza.",
                "#002395", "#CE1126"));
    }

    public byte[] generateTicketPdf(TicketDTO ticket) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float finalYPosition = 0;
            float pageHeight = page.getMediaBox().getHeight();

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Header with logos
                float y = pageHeight - 70;

                // Airline Logo (top left)
                Airline airline = AIRLINES.get(ticket.getAirlineCode());
                if (airline != null) {
                    try {
                        PDImageXObject airlineImg = loadLogo(document, airline.getLogoPath());
                        float aspectRatio = (float) airlineImg.getWidth() / airlineImg.getHeight();
                        float imgHeight = 60;
                        float imgWidth = imgHeight * aspectRatio;
                        contentStream.drawImage(airlineImg, MARGIN, y - imgHeight, imgWidth, imgHeight);
                    } catch (Exception e) {
                        System.err.println("Error loading airline logo: " + e.getMessage());
                    }
                }

                // Agency Logo (top right)
                try {
                    PDImageXObject agencyImg = loadLogo(document, AGENCY_LOGO_NAME);
                    float aspectRatio = (float) agencyImg.getWidth() / agencyImg.getHeight();
                    float imgHeight = 50;
                    float imgWidth = imgHeight * aspectRatio;
                    float x = PAGE_WIDTH - MARGIN - imgWidth;
                    contentStream.drawImage(agencyImg, x, y - imgHeight, imgWidth, imgHeight);
                } catch (Exception e) {
                    System.err.println("Error loading agency logo: " + e.getMessage());
                }

                // Separator line
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(MARGIN, y - 80);
                contentStream.lineTo(PAGE_WIDTH - MARGIN, y - 80);
                contentStream.stroke();

                // Ticket details
                contentStream.setFont(FONT_BOLD, 14);
                y -= 100;

                // Title
                drawText(contentStream, "BIGLIETTO AEREO - " + (airline != null ? airline.getName().toUpperCase() : ""),
                        MARGIN, y);
                y -= 30;

                // PNR
                contentStream.setFont(FONT_BOLD, 12);
                drawText(contentStream, "PNR: " + ticket.getPnr(), MARGIN, y);
                y -= 30;

                // Flight info
                drawText(contentStream, "Tratta: " + ticket.getRoute(), MARGIN, y);
                y -= 25;

                drawText(contentStream, "Partenza: " + ticket.getDepartureTime(), MARGIN, y);
                y -= 25;

                drawText(contentStream, "Arrivo: " + ticket.getArrivalTime(), MARGIN, y);
                y -= 40;

                // Passengers header
                drawText(contentStream, "PASSEGGERI:", MARGIN, y);
                y -= 25;

                // Passenger table
                float tableStartY = y;
                float[] columnWidths = {60, 150, 80, 80, 80};
                float rowHeight = 20;

                // Table headers
                contentStream.setFont(FONT_BOLD, 10);
                drawTableRow(contentStream, MARGIN, tableStartY,
                        new String[]{"TIPO", "NOME E COGNOME", "BAG. A MANO", "KG", "BAG. STIVA"},
                        columnWidths);

                // Passenger data
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

                // Price
                contentStream.setFont(FONT_BOLD, 12);
                drawText(contentStream, "PREZZO TOTALE: €" + String.format("%.2f", ticket.getPrice()), MARGIN, y);
                y -= 30;

                // Save final Y position
                finalYPosition = y;
            }

            // Check if we need a new page for rules
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

                // Rules section
                Airline airline = AIRLINES.get(ticket.getAirlineCode());

                rulesContentStream.setFont(FONT_BOLD, 11);
                drawText(rulesContentStream, "REGOLAMENTO DI VIAGGIO", MARGIN, rulesY);
                rulesY -= 20;

                rulesContentStream.setFont(FONT_NORMAL, 9);
                String rules = airline != null ? airline.getRules() : ticket.getRules();
                rulesY = drawWrappedText(rulesContentStream, rules, MARGIN, rulesY, RULES_WIDTH, 12);

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
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
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