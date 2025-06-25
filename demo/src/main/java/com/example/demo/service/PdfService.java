package com.example.demo.service;

import com.example.demo.model.Airline;
import com.example.demo.model.Passenger;
import com.example.demo.model.TicketDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.*;

@Service
public class PdfService {
    
    private static final Map<String, Airline> AIRLINES = new HashMap<>();
    private static final String AGENCY_LOGO_PATH = "static/logos/agency-logo.png";
    
    static {
        // Inizializza le compagnie aeree con regole e colori
        AIRLINES.put("AZ", new Airline("AZ", "Alitalia", "static/logos/alitalia.png", 
            "Bagaglio a mano incluso: 1 pezzo max 8kg\nBagaglio in stiva: 23kg a pagamento\n\nRichieste di cancellazione:\nI biglietti non sono rimborsabili tranne nei casi in cui la compagnia aerea annulli la prenotazione o sposti l'orario di partenza in modo significativo.\n\nCambi NOME:\nPermesso di cambio nome fino a 24 ore prima della partenza.\n\nDocumenti:\nAssicurati di avere documenti di identità validi per tutti i paesi che visiterai.",
            "#0066CC", "#FFFFFF"));
        
        AIRLINES.put("LH", new Airline("LH", "Lufthansa", "static/logos/lufthansa.png", 
            "Bagaglio a mano incluso: 1 pezzo + 1 personale\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione:\nRimborso completo fino a 48 ore prima del volo.\n\nCambi NOME:\nModifiche consentite con penale di 50€.\n\nDocumenti:\nPassaporto obbligatorio per voli extra-Schengen.",
            "#001E49", "#D80621"));
        
        AIRLINES.put("AF", new Airline("AF", "Air France", "static/logos/airfrance.png", 
            "Bagaglio a mano: 1 pezzo max 12kg\nBagaglio in stiva: 23kg incluso per voli intercontinentali\n\nRichieste di cancellazione:\nPenale del 20% per cancellazioni entro 7 giorni.\n\nCambi NOME:\nConsentito solo per errori di battitura entro 24 ore.\n\nDocumenti:\nVisto richiesto per alcune destinazioni, verificare prima della partenza.",
            "#002395", "#CE1126"));
    }

    public byte[] generateTicketPdf(TicketDTO ticket) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Intestazione con loghi
                float y = page.getMediaBox().getHeight() - 70;
                float margin = 50;
                float pageWidth = page.getMediaBox().getWidth();
                
                // Logo Compagnia Aerea (alto a sinistra)
                Airline airline = AIRLINES.get(ticket.getAirlineCode());
                if (airline != null) {
                    try (InputStream is = new ClassPathResource(airline.getLogoPath()).getInputStream()) {
                        byte[] imageBytes = is.readAllBytes();
                        PDImageXObject airlineImg = PDImageXObject.createFromByteArray(
                            document, imageBytes, airline.getCode());
                        
                        float aspectRatio = (float) airlineImg.getWidth() / airlineImg.getHeight();
                        float imgHeight = 60;
                        float imgWidth = imgHeight * aspectRatio;
                        contentStream.drawImage(airlineImg, margin, y - imgHeight, imgWidth, imgHeight);
                    }
                }
                
                // Logo Agenzia Viaggi FISSO (alto a destra)
                try (InputStream is = new ClassPathResource(AGENCY_LOGO_PATH).getInputStream()) {
                    byte[] imageBytes = is.readAllBytes();
                    PDImageXObject agencyImg = PDImageXObject.createFromByteArray(
                        document, imageBytes, "agency");
                    
                    float aspectRatio = (float) agencyImg.getWidth() / agencyImg.getHeight();
                    float imgHeight = 50;
                    float imgWidth = imgHeight * aspectRatio;
                    float x = pageWidth - margin - imgWidth;
                    contentStream.drawImage(agencyImg, x, y - imgHeight, imgWidth, imgHeight);
                }
                
                // Linea separatore
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(margin, y - 80);
                contentStream.lineTo(pageWidth - margin, y - 80);
                contentStream.stroke();
                
                // Dettagli Biglietto
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 14);
                y -= 100;
                
                // Titolo
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("BIGLIETTO AEREO - " + (airline != null ? airline.getName().toUpperCase() : ""));
                contentStream.endText();
                y -= 30;
                
                // PNR
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("PNR: " + ticket.getPnr());
                contentStream.endText();
                y -= 30;
                
                // Informazioni volo
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Tratta: " + ticket.getRoute());
                contentStream.endText();
                y -= 25;
                
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Partenza: " + ticket.getDepartureTime());
                contentStream.endText();
                y -= 25;
                
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Arrivo: " + ticket.getArrivalTime());
                contentStream.endText();
                y -= 40;
                
                // Intestazione passeggeri
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("PASSEGGERI:");
                contentStream.endText();
                y -= 25;
                
                // Tabella passeggeri
                float tableY = y;
                float[] columnWidths = {60, 150, 80, 80, 80};
                float tableHeight = 20 + (ticket.getPassengers().size() * 20);
                
                // Intestazioni colonne
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 10);
                drawTableRow(contentStream, margin, tableY, new String[]{"TIPO", "NOME E COGNOME", "BAG. A MANO", "KG", "BAG. STIVA"}, columnWidths);
                
                // Dettagli passeggeri
                contentStream.setFont(new PDType1Font(HELVETICA), 10);
                for (Passenger passenger : ticket.getPassengers()) {
                    tableY -= 20;
                    String handLuggage = passenger.hasHandLuggage() ? "SI" : "NO";
                    String handLuggageKg = passenger.hasHandLuggage() ? passenger.getHandLuggageKg() + "kg" : "-";
                    String checkedLuggage = passenger.hasCheckedLuggage() ? 
                        passenger.getCheckedLuggageCount() + " bag (" + passenger.getCheckedLuggageKg() + "kg)" : "NO";
                    
                    drawTableRow(contentStream, margin, tableY, 
                        new String[]{
                            passenger.getType().toUpperCase(),
                            passenger.getFullName(),
                            handLuggage,
                            handLuggageKg,
                            checkedLuggage
                        }, 
                        columnWidths);
                }
                y = tableY - 40;
                
                // Prezzo
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("PREZZO TOTALE: €" + String.format("%.2f", ticket.getPrice()));
                contentStream.endText();
                y -= 30;
                
                // Regole
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 11);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("REGOLAMENTO DI VIAGGIO");
                contentStream.endText();
                y -= 20;
                
                contentStream.setFont(new PDType1Font(HELVETICA), 9);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                
                String rules = ticket.getRules();
                String[] rulesLines = rules.split("\n");
                float leading = 12;
                
                for (String line : rulesLines) {
                    if (line.trim().isEmpty()) {
                        contentStream.newLineAtOffset(0, -leading);
                        continue;
                    }
                    
                    // Gestione testo multilinea
                    while (line.length() > 100) {
                        int splitIndex = line.substring(0, 100).lastIndexOf(' ');
                        if (splitIndex <= 0) splitIndex = 100;
                        contentStream.showText(line.substring(0, splitIndex));
                        contentStream.newLineAtOffset(0, -leading);
                        line = line.substring(splitIndex).trim();
                    }
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -leading);
                }
                contentStream.endText();
                
                // Footer
                contentStream.setFont(new PDType1Font(HELVETICA_OBLIQUE), 8);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, 50);
                contentStream.showText("Biglietto generato automaticamente - Documento non trasferibile");
                contentStream.endText();
            }
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }
    
    private void drawTableRow(PDPageContentStream contentStream, float x, float y, String[] texts, float[] columnWidths) throws IOException {
        contentStream.beginText();
        float currentX = x;
        for (int i = 0; i < texts.length; i++) {
            contentStream.newLineAtOffset(currentX - contentStream.getCurrentPosition().getX(), 0);
            contentStream.showText(texts[i]);
            currentX += columnWidths[i];
        }
        contentStream.endText();
    }
}