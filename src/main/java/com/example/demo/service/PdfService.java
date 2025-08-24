package com.example.demo.service;

import com.example.demo.model.*;

import jakarta.annotation.PostConstruct;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;

import static org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.*;

@Service
public class PdfService {
	private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

	private static final String AGENCY_LOGO_NAME = "agency-logo.png";
	private static final float MARGIN = 50;
	private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
	private static final float LOGO_HEIGHT = 60;
	private static final float LOGO_WIDTH = 150;

	// Define static fonts
	private static final PDFont FONT_BOLD = new PDType1Font(HELVETICA_BOLD);
	private static final PDFont FONT_NORMAL = new PDType1Font(HELVETICA);
	private static final PDFont FONT_ITALIC = new PDType1Font(HELVETICA_OBLIQUE);
	private static final PDFont FONT_SMALL = new PDType1Font(HELVETICA);

	@Value("${app.logos.path:classpath:static/logos/}")
	private String logosPath;

	@Value("${amadeus.api.key}")
	private String amadeusApiKey;

	@Value("${amadeus.api.secret}")
	private String amadeusApiSecret;

	@Value("${brandfetch.api.key}")
	private String brandfetchApiKey;

	private final RestTemplate restTemplate = new RestTemplate();

	public FlightDTO fetchFlightData(String flightNumber, String date) {
		logger.debug("Fetching flight data from Amadeus - Flight: {}, Date: {}", flightNumber, date);
		// Estrai codice vettore e numero volo
		if (flightNumber == null || flightNumber.length() < 2) {
			logger.error("Numero volo non valido: {}", flightNumber);
			return null;
		}

		String carrier = flightNumber.substring(0, 2);
		String number = flightNumber.substring(2);

		try {
			// Get Amadeus access token
			String accessToken = getAmadeusAccessToken();
			if (accessToken == null) {
				logger.error("Failed to get Amadeus access token");
				return null;
			}

			String url = "https://test.api.amadeus.com/v2/schedule/flights" + "?carrierCode=" + carrier
					+ "&flightNumber=" + number + "&scheduledDepartureDate=" + date;

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
	        Map<String, Object> responseBody = response.getBody();
	        
	        // Log della risposta completa
	        logger.debug("Amadeus API Response: {}", responseBody);
	        
	       	if (response.getStatusCode() != HttpStatus.OK || responseBody == null) {
				logger.error("Amadeus API error: {}", response.getStatusCode());
				return null;
			}

			if (responseBody.containsKey("data")) {
				List<Map<String, Object>> flights = (List<Map<String, Object>>) responseBody.get("data");
				if (flights != null && !flights.isEmpty()) {
					return parseAmadeusResponse(flights.getFirst());
				}
			}
		} catch (Exception e) {
			logger.error("Error fetching flight data from Amadeus", e);
		}
		return null;
	}

	private String getAmadeusAccessToken() {
		logger.debug("Requesting Amadeus access token");
		String url = "https://test.api.amadeus.com/v1/security/oauth2/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		String body = "grant_type=client_credentials" + "&client_id=" + amadeusApiKey + "&client_secret="
				+ amadeusApiSecret;

		HttpEntity<String> request = new HttpEntity<>(body, headers);

		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
			if (response.getStatusCode() == HttpStatus.OK) {
				Map<String, Object> responseBody = response.getBody();
				if (responseBody != null && responseBody.containsKey("access_token")) {
					return (String) responseBody.get("access_token");
				}
			}
			logger.error("Failed to get Amadeus token: {}", response.getBody());
		} catch (Exception e) {
			logger.error("Error getting Amadeus token", e);
		}
		return null;
	}

	private FlightDTO parseAmadeusResponse(Map<String, Object> flightData) {
		FlightDTO flight = new FlightDTO();

		// Extract route
		Map<String, Object> departure = (Map<String, Object>) flightData.get("departure");
		Map<String, Object> arrival = (Map<String, Object>) flightData.get("arrival");
		String departureAirport = (String) departure.get("iataCode");
		String arrivalAirport = (String) arrival.get("iataCode");
		flight.setRoute(departureAirport + "-" + arrivalAirport);

		// Extract times
		flight.setDepartureTime((String) departure.get("scheduledTime"));
		flight.setArrivalTime((String) arrival.get("scheduledTime"));

		// Extract airline info
		Map<String, Object> marketingCarrier = (Map<String, Object>) flightData.get("marketingCarrier");
		String airlineCode = (String) marketingCarrier.get("iataCode");
		String airlineName = (String) marketingCarrier.get("businessName");
		flight.setAirlineCode(airlineCode);
		flight.setAirlineName(airlineName);

		// Get logo
		String logoUrl = getBrandfetchLogoUrl(airlineName, airlineCode);
		flight.setLogoUrl(logoUrl != null ? logoUrl : getFallbackLogoUrl(airlineCode));

		return flight;
	}

	private String formatDateTimeForDisplay(String dateTime) {
		if (dateTime == null)
			return "";
		try {
			// Converti da formato input HTML a formato visualizzazione
			LocalDateTime ldt = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'alle' HH.mm");
			return ldt.format(formatter);
		} catch (Exception e) {
			logger.error("Error formatting date for display: {}", dateTime, e);
			// Formato fallback
			return dateTime.replace("T", " ").substring(0, 16);
		}
	}

	private String getFallbackLogoUrl(String airlineCode) {
		if (airlineCode == null || airlineCode.isEmpty())
			return "";
		return "https://daisycon.io/images/airline/?width=150&height=60&iata=" + airlineCode;
	}

	private String getBrandfetchLogoUrl(String airlineName, String airlineCode) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", "Bearer " + brandfetchApiKey);
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<String> entity = new HttpEntity<>(headers);
			String encodedName = airlineName.replace(" ", "%20");
			String apiUrl = "https://api.brandfetch.io/v2/brands/search?query=" + encodedName;

			ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				Map<String, Object> responseBody = response.getBody();
				if (responseBody != null && responseBody.containsKey("data")) {
					List<Map<String, Object>> brands = (List<Map<String, Object>>) responseBody.get("data");
					if (brands != null && !brands.isEmpty()) {
						Map<String, Object> brand = brands.get(0);
						if (brand.containsKey("logos")) {
							List<Map<String, Object>> logos = (List<Map<String, Object>>) brand.get("logos");

							if (logos != null && !logos.isEmpty()) {
								for (Map<String, Object> logo : logos) {
									String type = (String) logo.get("type");
									if ("main".equals(type) || "symbol".equals(type)) {
										List<Map<String, Object>> formats = (List<Map<String, Object>>) logo
												.get("formats");
										if (formats != null && !formats.isEmpty()) {
											return (String) formats.get(0).get("src");
										}
									}
								}

								// Fallback to first available logo
								Map<String, Object> firstLogo = logos.get(0);
								List<Map<String, Object>> formats = (List<Map<String, Object>>) firstLogo
										.get("formats");
								if (formats != null && !formats.isEmpty()) {
									return (String) formats.get(0).get("src");
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Brandfetch error for airline: {}", airlineName, e);
		}
		return getFallbackLogoUrl(airlineCode);
	}

	public byte[] generateTicketPdf(TicketDTO ticket) throws IOException {
		if (ticket == null) {
			throw new IllegalArgumentException("TicketDTO cannot be null");
		}

		// Initialize null collections to prevent NullPointerException
		if (ticket.getPassengers() == null) {
			ticket.setPassengers(Collections.emptyList());
		}
		if (ticket.getFlights() == null) {
			ticket.setFlights(Collections.emptyList());
		}

		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);

			float finalYPosition = 0;
			float pageHeight = page.getMediaBox().getHeight();

			try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
				// Header with agency logo (top right)
				float y = pageHeight - 70;
				try {
					PDImageXObject agencyImg = loadLogo(document, AGENCY_LOGO_NAME);
					contentStream.drawImage(agencyImg, PAGE_WIDTH - MARGIN - LOGO_WIDTH, y - LOGO_HEIGHT, LOGO_WIDTH,
							LOGO_HEIGHT);
				} catch (Exception e) {
					logger.error("Error loading agency logo", e);
				}

				// Airline logo (top left) - only if all flights have same airline
				boolean sameAirline = !ticket.getFlights().isEmpty()
						&& ticket.getFlights().stream().map(FlightDTO::getAirlineName).distinct().count() == 1;

				if (sameAirline) {
					try {
						String logoUrl = ticket.getFlights().get(0).getLogoUrl();
						if (logoUrl != null && !logoUrl.isEmpty()) {
							PDImageXObject airlineImg = loadImageFromUrl(document, logoUrl);
							if (airlineImg != null) {
								contentStream.drawImage(airlineImg, MARGIN, y - LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
							}
						}
					} catch (Exception e) {
						logger.error("Error loading airline logo", e);
					}
				}

				// Separator line
				contentStream.setLineWidth(1.5f);
				contentStream.moveTo(MARGIN, y - 80);
				contentStream.lineTo(PAGE_WIDTH - MARGIN, y - 80);
				contentStream.stroke();

				// Passengers section
				contentStream.setFont(FONT_BOLD, 14);
				y -= 100;
				drawText(contentStream, "PASSEGGERI", MARGIN, y);
				y -= 30;

				// Passengers table
				if (!ticket.getPassengers().isEmpty()) {
					float tableStartY = y;
					float[] columnWidths = { 60, 150, 80, 80, 100, 100 };
					float rowHeight = 20;

					contentStream.setFont(FONT_BOLD, 10);
					drawTableRow(contentStream, MARGIN, tableStartY, new String[] { "TIPO", "NOME E COGNOME",
							"BAG. A MANO", "KG", "BAG. STIVA", "KG PER BAGAGLIO" }, columnWidths);

					contentStream.setFont(FONT_NORMAL, 10);
					for (Passenger passenger : ticket.getPassengers()) {
						tableStartY -= rowHeight;
						String handLuggage = passenger.hasHandLuggage() ? "SI" : "NO";
						String handLuggageKg = passenger.hasHandLuggage() ? passenger.getHandLuggageKg() + "kg" : "-";
						String checkedLuggage = passenger.hasCheckedLuggage()
								? passenger.getCheckedLuggageCount() + " bagagli"
								: "NO";
						String checkedLuggageKg = passenger.hasCheckedLuggage() ? passenger.getCheckedLuggageKg() + "kg"
								: "-";

						drawTableRow(contentStream, MARGIN, tableStartY,
								new String[] { passenger.getType().toUpperCase(), passenger.getFullName(), handLuggage,
										handLuggageKg, checkedLuggage, checkedLuggageKg },
								columnWidths);
					}
					y = tableStartY - 40;
				} else {
					y -= 20;
				}

				// Flights section
				contentStream.setFont(FONT_BOLD, 14);
				drawText(contentStream, "VOLI", MARGIN, y);
				y -= 30;

				// Show all flights regardless of airline
				for (int i = 0; i < ticket.getFlights().size(); i++) {
					FlightDTO flight = ticket.getFlights().get(i);

					// Add space before flight details
					y -= 20;

					// Flight header
					contentStream.setFont(FONT_BOLD, 12);
					drawText(contentStream, "Volo: " + flight.getRoute(), MARGIN, y);
					y -= 20;

					// Formatta la data per la visualizzazione
					String departureDisplay = formatDateTimeForDisplay(flight.getDepartureTime());
					String arrivalDisplay = formatDateTimeForDisplay(flight.getArrivalTime());

					drawText(contentStream, "Partenza: " + departureDisplay, MARGIN, y);
					y -= 20;

					drawText(contentStream, "Arrivo: " + arrivalDisplay, MARGIN, y);
					y -= 20;

					drawText(contentStream, "Compagnia: " + flight.getAirlineName(), MARGIN, y);
					y -= 20;

					// Show PNR only if available
					if (flight.getPnr() != null && !flight.getPnr().isEmpty()) {
						drawText(contentStream, "PNR: " + flight.getPnr(), MARGIN, y);
						y -= 20;
					}

					// Logo compagnia (dimensione fissa) - only if airlines are different
					if (!sameAirline) {
						try {
							PDImageXObject airlineImg = loadImageFromUrl(document, flight.getLogoUrl());
							if (airlineImg != null) {
								contentStream.drawImage(airlineImg, MARGIN, y - LOGO_HEIGHT, LOGO_WIDTH, LOGO_HEIGHT);
							}
						} catch (Exception e) {
							logger.error("Error loading airline logo: {}", flight.getAirlineName(), e);
						}
						y -= LOGO_HEIGHT + 20;
					} else {
						y -= 20;
					}

					// Add separator line between flights (except after last flight)
					if (i < ticket.getFlights().size() - 1) {
						// Space before separator
						y -= 20;

						contentStream.setLineWidth(0.5f);
						contentStream.moveTo(MARGIN, y - 10);
						contentStream.lineTo(PAGE_WIDTH - MARGIN, y - 10);
						contentStream.stroke();

						// Space after separator
						y -= 40;
					}
				}

				// Add more space before total price
				y -= 60;

				// Prezzo totale (solo alla fine)
				double totalPrice = ticket.getFlights().stream().mapToDouble(FlightDTO::getPrice).sum();
				contentStream.setFont(FONT_BOLD, 14);
				drawText(contentStream, "PREZZO TOTALE: €" + String.format("%.2f", totalPrice), MARGIN, y);
				y -= 40;

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

			try (PDPageContentStream rulesContentStream = new PDPageContentStream(document, rulesPage,
					PDPageContentStream.AppendMode.APPEND, true)) {

				float rulesY = newPageCreated ? rulesPage.getMediaBox().getHeight() - 50 : finalYPosition;

				// Tabella regole per compagnia
				rulesContentStream.setFont(FONT_BOLD, 11);
				drawText(rulesContentStream, "REGOLAMENTO DI VIAGGIO", MARGIN, rulesY);
				rulesY -= 20;

				// Get distinct airlines
				Map<String, String> airlineRules = ticket.getFlights().stream()
						.collect(Collectors.toMap(FlightDTO::getAirlineName, f -> getAirlineRules(f.getAirlineName()),
								(existing, replacement) -> existing));

				// List rules vertically
				for (Map.Entry<String, String> entry : airlineRules.entrySet()) {
					String airline = entry.getKey();
					String rules = entry.getValue();

					// Airline name
					rulesContentStream.setFont(FONT_BOLD, 9);
					drawText(rulesContentStream, airline + ":", MARGIN, rulesY);
					rulesY -= 15;

					// Rules content
					rulesContentStream.setFont(FONT_NORMAL, 8);
					rulesY = drawWrappedText(rulesContentStream, rules, MARGIN + 10, rulesY,
							PAGE_WIDTH - 2 * MARGIN - 10, 10);
					rulesY -= 20; // Extra space between airlines
				}

				// Footer
				rulesContentStream.setFont(FONT_ITALIC, 8);
				drawText(rulesContentStream, "Biglietto generato automaticamente - Documento non trasferibile", MARGIN,
						50);

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
				drawText(rulesContentStream, "Biglietto generato automaticamente - Documento non trasferibile", MARGIN,
						50);
			}

			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
			document.save(byteArrayOutputStream);
			return byteArrayOutputStream.toByteArray();
		}
	}

	private String getAirlineRules(String airlineName) {
		// Mappa statica per regole note
		Map<String, String> staticRules = new HashMap<>();
		staticRules.put("Alitalia",
				"Bagaglio a mano incluso: 1 pezzo max 8kg\nBagaglio in stiva: 23kg a pagamento\n\nRichieste di cancellazione: Non rimborsabile");
		staticRules.put("Lufthansa",
				"Bagaglio a mano: 1 pezzo + 1 personale\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione: Rimborso completo fino a 48 ore");
		staticRules.put("Air France",
				"Bagaglio a mano: 1 pezzo max 12kg\nBagaglio in stiva: 23kg incluso per voli intercontinentali");
		staticRules.put("ITA Airways",
				"Bagaglio a mano: 1 pezzo max 8kg\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione: Rimborso completo fino a 48 ore prima del volo.");
		staticRules.put("Ryanair",
				"Bagaglio a mano: 1 pezzo max 10kg (dimensioni 40x20x25cm)\nBagaglio in stiva: 20kg a pagamento\n\nRichieste di cancellazione: Non rimborsabile");

		return staticRules.getOrDefault(airlineName,
				"Bagaglio a mano: 1 pezzo max 8kg\nBagaglio in stiva: 23kg incluso\n\nRichieste di cancellazione: Rimborso completo fino a 48 ore prima del volo.");
	}

	private PDImageXObject loadImageFromUrl(PDDocument document, String url) throws IOException {
		try {
			// Create headers with authorization
			HttpHeaders headers = new HttpHeaders();
			headers.set("Authorization", "Bearer " + brandfetchApiKey);
			headers.setAccept(Collections.singletonList(MediaType.IMAGE_PNG));

			HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				return PDImageXObject.createFromByteArray(document, response.getBody(), url);
			}
		} catch (Exception e) {
			logger.error("Error loading image from URL: {}", url, e);
		}
		return null;
	}

	private PDImageXObject loadLogo(PDDocument document, String logoName) throws IOException {
		if (logosPath.startsWith("classpath:")) {
			String classpathPath = logosPath.substring(10) + logoName;
			try (InputStream is = new ClassPathResource(classpathPath).getInputStream()) {
				return PDImageXObject.createFromByteArray(document, is.readAllBytes(), logoName);
			}
		} else {
			java.io.File logoFile = new java.io.File(logosPath, logoName);
			if (logoFile.exists()) {
				return PDImageXObject.createFromFile(logoFile.getAbsolutePath(), document);
			} else {
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

	private void drawTableRow(PDPageContentStream contentStream, float startX, float y, String[] texts,
			float[] columnWidths) throws IOException {
		float x = startX;
		for (int i = 0; i < texts.length; i++) {
			contentStream.beginText();
			contentStream.newLineAtOffset(x, y);
			contentStream.showText(texts[i] != null ? texts[i] : "");
			contentStream.endText();
			x += columnWidths[i];
		}
	}

	private float drawWrappedText(PDPageContentStream contentStream, String text, float x, float y, float maxWidth,
			float leading) throws IOException {
		if (text == null || text.isEmpty())
			return y;

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
					drawText(contentStream, currentLine.toString(), x, y);
					y -= leading;
					currentLine = new StringBuilder(word);
				} else {
					currentLine = new StringBuilder(testLine);
				}

				if (i == words.length - 1) {
					drawText(contentStream, currentLine.toString(), x, y);
					y -= leading;
				}
			}

			y -= leading / 2;
		}
		return y;
	}

	private float getStringWidth(String text, PDFont font, float fontSize) throws IOException {
		return font.getStringWidth(text) / 1000 * fontSize;
	}

	@PostConstruct
	public void init() {
		restTemplate.setInterceptors(Collections.singletonList(new RestTemplateInterceptor()));
	}

	private static class RestTemplateInterceptor implements ClientHttpRequestInterceptor {
		private final Logger logger = LoggerFactory.getLogger(RestTemplateInterceptor.class);

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
				throws IOException {
			logger.debug("Request: {} {}", request.getMethod(), request.getURI());
			logger.debug("Headers: {}", request.getHeaders());
			return execution.execute(request, body);
		}
	}
}
