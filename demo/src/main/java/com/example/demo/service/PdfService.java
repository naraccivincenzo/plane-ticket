package com.example.demo.service;

import com.example.demo.model.TicketDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.*;

@Service
public class PdfService {

    /**
     * @param ticket
     * @return
     * @throws IOException
     */
    public byte[] generateTicketPdf(TicketDTO ticket) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Intestazione con loghi
                float y = page.getMediaBox().getHeight() - 70;
                float margin = 50;
                
                // Logo Compagnia Aerea (sinistra)
                if (ticket.getAirlineLogo() != null && !ticket.getAirlineLogo().isEmpty()) {
                    try (InputStream is = ticket.getAirlineLogo().getInputStream()) {
                        byte[] imageBytes = StreamUtils.copyToByteArray(is);
                        PDImageXObject airlineImg = PDImageXObject.createFromByteArray(document, imageBytes, "airline");
                        float aspectRatio = (float) airlineImg.getWidth() / airlineImg.getHeight();
                        float imgHeight = 60;
                        float imgWidth = imgHeight * aspectRatio;
                        contentStream.drawImage(airlineImg, margin, y - imgHeight, imgWidth, imgHeight);
                    }
                }
                
                // Logo Agenzia Viaggi (destra)
                if (ticket.getAgencyLogo() != null && !ticket.getAgencyLogo().isEmpty()) {
                    try (InputStream is = ticket.getAgencyLogo().getInputStream()) {
                        byte[] imageBytes = StreamUtils.copyToByteArray(is);
                        PDImageXObject agencyImg = PDImageXObject.createFromByteArray(document, imageBytes, "agency");
                        float aspectRatio = (float) agencyImg.getWidth() / agencyImg.getHeight();
                        float imgHeight = 50;
                        float imgWidth = imgHeight * aspectRatio;
                        float x = page.getMediaBox().getWidth() - margin - imgWidth;
                        contentStream.drawImage(agencyImg, x, y - imgHeight, imgWidth, imgHeight);
                    }
                }
                
                // Linea separatore
                contentStream.moveTo(margin, y - 80);
                contentStream.lineTo(page.getMediaBox().getWidth() - margin, y - 80);
                contentStream.stroke();
                
                // Dettagli Biglietto
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 14);
                y -= 100;
                
                // Titolo
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("BIGLIETTO AEREO - " + ticket.getAirline().toUpperCase());
                contentStream.endText();
                y -= 30;
                
                // Informazioni passeggero
                contentStream.setFont(new PDType1Font(HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Passeggero: " + String.join(", ", ticket.getPassengers()));
                contentStream.endText();
                y -= 25;
                
                // Tratta
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Tratta: " + ticket.getRoute());
                contentStream.endText();
                y -= 25;
                
                // Orari
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Partenza: " + ticket.getDepartureTime());
                contentStream.endText();
                y -= 25;
                
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Arrivo: " + ticket.getArrivalTime());
                contentStream.endText();
                y -= 25;
                
                // Prezzo
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                contentStream.showText("Prezzo: €" + "%.2f".formatted(ticket.getPrice()));
                contentStream.endText();
                y -= 40;
                
                // Regole
                contentStream.setFont(new PDType1Font(HELVETICA), 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, y);
                String rules = "Regole: " + ticket.getRules();
                // Gestione testo multilinea
                for (String line : rules.split("(?<=\\G.{80})")) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);
                }
                contentStream.endText();
                
                // Footer
                contentStream.setFont(new PDType1Font(HELVETICA_OBLIQUE), 8);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, 50);
                contentStream.showText("Biglietto generato automaticamente - Valid solo per la persona indicata");
                contentStream.endText();
            }
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            document.save(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        }
    }
}