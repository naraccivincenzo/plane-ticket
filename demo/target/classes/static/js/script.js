// Funzione per inizializzare il form
function initForm() {
    // Precompila la data corrente
    const now = new Date();
    const timezoneOffset = now.getTimezoneOffset() * 60000;
    const localISOTime = new Date(now - timezoneOffset).toISOString().slice(0, 16);
    
    document.querySelector('[name="departureTime"]').value = localISOTime;
    document.querySelector('[name="arrivalTime"]').value = new Date(now - timezoneOffset + 3600000).toISOString().slice(0, 16);
    
    // Gestione cambio compagnia aerea
    const airlineSelect = document.getElementById('airlineCode');
    if (airlineSelect) {
        airlineSelect.addEventListener('change', function() {
            const selectedOption = this.options[this.selectedIndex];
            const logoPath = selectedOption.getAttribute('data-logo');
            const rules = selectedOption.getAttribute('data-rules');
            const primaryColor = selectedOption.getAttribute('data-primary');
            const secondaryColor = selectedOption.getAttribute('data-secondary');
            const previewDiv = document.getElementById('airline-logo-preview');
            const rulesTextarea = document.getElementById('rules');
            
            // Aggiorna logo e regole
            if (logoPath) {
                previewDiv.style.display = 'flex';
                previewDiv.querySelector('img').src = '/logos/' + logoPath.split('/').pop();
            } else {
                previewDiv.style.display = 'none';
            }
            
            if (rules) {
                rulesTextarea.value = rules;
            }
            
            // Applica i colori della compagnia
            if (primaryColor && secondaryColor) {
                document.documentElement.style.setProperty('--primary', primaryColor);
                document.documentElement.style.setProperty('--secondary', secondaryColor);
                
                // Aggiorna stili pulsanti e header
                document.querySelectorAll('.btn').forEach(btn => {
                    btn.style.background = `linear-gradient(135deg, ${primaryColor}, ${secondaryColor})`;
                });
                
                document.querySelector('.card-header').style.backgroundColor = primaryColor;
            }
        });
    }
    
    // Gestione aggiunta passeggeri
    let passengerCount = 1;
    document.getElementById('add-passenger').addEventListener('click', function() {
        const container = document.getElementById('passengers-container');
        const template = container.querySelector('.passenger-template').cloneNode(true);
        
        // Aggiorna ID e resetta valori
        template.id = `passenger-${passengerCount}`;
        template.querySelector('[name="passengerName"]').value = '';
        template.querySelector('[name="handLuggage"]').checked = false;
        template.querySelector('[name="handLuggageKg"]').value = '8';
        template.querySelector('[name="checkedLuggage"]').checked = false;
        template.querySelector('[name="checkedLuggageCount"]').value = '1';
        template.querySelector('[name="checkedLuggageKg"]').value = '23';
        
        // Mostra/nascondi dettagli bagagli
        toggleLuggageDetails(template);
        
        container.appendChild(template);
        passengerCount++;
    });
    
    // Gestione bagagli a mano
    document.addEventListener('change', function(e) {
        if (e.target.classList.contains('hand-luggage')) {
            const details = e.target.closest('.passenger-template').querySelector('.hand-luggage-details');
            details.style.display = e.target.checked ? 'block' : 'none';
        }
        
        if (e.target.classList.contains('checked-luggage')) {
            const details = e.target.closest('.passenger-template').querySelector('.checked-luggage-details');
            details.style.display = e.target.checked ? 'block' : 'none';
        }
    });
    
    // Validazione form
    const form = document.getElementById('ticketForm');
    form.addEventListener('submit', function(event) {
        if (!form.checkValidity()) {
            event.preventDefault();
            event.stopPropagation();
        }
        form.classList.add('was-validated');
    }, false);
    
    // Migliora visibilità input
    document.querySelectorAll('.form-control').forEach(input => {
        input.classList.add('form-control-lg');
    });
    
    // Imposta colori di default
    document.documentElement.style.setProperty('--primary', '#3498db');
    document.documentElement.style.setProperty('--secondary', '#2c3e50');
    
    // Inizializza gestione voli
    initFlights();
}

// Mostra/nascondi dettagli bagagli per ogni passeggero
function toggleLuggageDetails(passengerElement) {
    const handLuggage = passengerElement.querySelector('.hand-luggage');
    const handDetails = passengerElement.querySelector('.hand-luggage-details');
    const checkedLuggage = passengerElement.querySelector('.checked-luggage');
    const checkedDetails = passengerElement.querySelector('.checked-luggage-details');
    
    handDetails.style.display = handLuggage.checked ? 'block' : 'none';
    checkedDetails.style.display = checkedLuggage.checked ? 'block' : 'none';
}

// Inizializza gestione voli
function initFlights() {
    let flightCount = 1;
    
    // Aggiungi nuovo volo
    document.getElementById('add-flight').addEventListener('click', function() {
        const container = document.getElementById('flights-container');
        const template = container.querySelector('.flight-section').cloneNode(true);
        
        flightCount++;
        template.id = `flight-${flightCount}`;
        
        template.querySelector('.flight-header h4').textContent = `Volo #${flightCount}`;
        template.querySelector('.remove-flight').style.display = 'block';
        
        const inputs = template.querySelectorAll('input');
        inputs.forEach(input => {
            if (input.type !== 'hidden') input.value = '';
        });
        
        const logoPreview = template.querySelector('[id^="airline-logo-preview"]');
        logoPreview.id = `airline-logo-preview-${flightCount}`;
        logoPreview.style.display = 'none';
        
        template.querySelector('.remove-flight').addEventListener('click', function() {
            if (flightCount > 1) {
                template.remove();
                flightCount--;
            }
        });
        
        template.querySelector('.fetch-flight').addEventListener('click', fetchFlightData);
        
        container.appendChild(template);
    });
    
    // Gestione stesso PNR
    document.getElementById('samePnr').addEventListener('change', function() {
        const pnrInputs = document.querySelectorAll('input[name="pnr"]');
        if (this.checked) {
            const firstPnr = pnrInputs[0].value;
            pnrInputs.forEach((input, index) => {
                if (index > 0) {
                    input.value = firstPnr;
                    input.disabled = true;
                }
            });
        } else {
            pnrInputs.forEach(input => {
                input.disabled = false;
            });
        }
    });
    
    // Aggiungi gestore evento per ricerca volo al volo iniziale
    document.querySelector('.fetch-flight').addEventListener('click', fetchFlightData);
}

// Funzione per recuperare dati volo
function fetchFlightData() {
    const flightSection = this.closest('.flight-section');
    const flightNumber = flightSection.querySelector('.flight-number').value;
    const date = flightSection.querySelector('.flight-date').value;
    
    if (!flightNumber || !date) {
        alert('Inserisci numero volo e data');
        return;
    }
    
    const fetchButton = flightSection.querySelector('.fetch-flight');
    const originalHtml = fetchButton.innerHTML;
    fetchButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Caricamento...';
    fetchButton.disabled = true;
    
    fetch(`/fetch-flight?flightNumber=${flightNumber}&date=${date}`)
        .then(response => response.json())
        .then(data => {
            fetchButton.innerHTML = originalHtml;
            fetchButton.disabled = false;
            
            if (data) {
                flightSection.querySelector('input[name="route"]').value = data.route;
                flightSection.querySelector('input[name="departureTime"]').value = formatDateTimeLocal(data.departureTime);
                flightSection.querySelector('input[name="arrivalTime"]').value = formatDateTimeLocal(data.arrivalTime);
                flightSection.querySelector('input[name="airlineName"]').value = data.airlineName;
                flightSection.querySelector('input[name="airlineCode"]').value = data.airlineCode;
                flightSection.querySelector('input[name="logoUrl"]').value = data.logoUrl;
                
                const logoPreview = flightSection.querySelector('[id^="airline-logo-preview"]');
                const img = logoPreview.querySelector('img');
                img.src = data.logoUrl;
                logoPreview.style.display = 'flex';
            } else {
                alert('Volo non trovato');
            }
        })
        .catch(error => {
            fetchButton.innerHTML = originalHtml;
            fetchButton.disabled = false;
            console.error('Error:', error);
            alert('Errore nel recupero dati volo');
        });
}

// Formatta data per input datetime-local
function formatDateTimeLocal(datetime) {
    const dt = new Date(datetime);
    const pad = num => num.toString().padStart(2, '0');
    return `${dt.getFullYear()}-${pad(dt.getMonth()+1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
}

// Inizializza quando il documento è pronto
document.addEventListener('DOMContentLoaded', initForm);