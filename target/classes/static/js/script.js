document.addEventListener('DOMContentLoaded', function() {
    // Inizializza il form
    initForm();
});

function initForm() {
    // Precompila la data corrente per il primo volo
    initFirstFlightDates();
    
    // Gestione PNR
    document.getElementById('samePnr')?.addEventListener('change', handlePnrChange);
    
    // Aggiungi passeggero
    document.getElementById('add-passenger')?.addEventListener('click', addPassenger);
    
    // Aggiungi volo
    document.getElementById('add-flight')?.addEventListener('click', addFlight);
    
    // Gestione bagagli
    document.addEventListener('change', handleLuggageChanges);
    
    // Validazione form
    const form = document.getElementById('ticketForm');
    form?.addEventListener('submit', validateForm);
    
    // Nascondi il pulsante rimuovi per il primo volo
    document.querySelector('.remove-flight')?.style.display = 'none';
}

function initFirstFlightDates() {
    const now = new Date();
    const timezoneOffset = now.getTimezoneOffset() * 60000;
    const localISOTime = new Date(now - timezoneOffset).toISOString().slice(0, 16);
    const oneHourLater = new Date(now - timezoneOffset + 3600000).toISOString().slice(0, 16);

    const departureInputs = document.querySelectorAll('input[name="departureTime[]"]');
    const arrivalInputs = document.querySelectorAll('input[name="arrivalTime[]"]');
    
    if (departureInputs.length > 0) departureInputs[0].value = localISOTime;
    if (arrivalInputs.length > 0) arrivalInputs[0].value = oneHourLater;
}

function handlePnrChange() {
    const isChecked = this.checked;
    const pnrInputs = document.querySelectorAll('input[name="pnr[]"]');
    const firstPnr = pnrInputs[0]?.value;
    
    pnrInputs.forEach((input, index) => {
        if (index === 0) return;
        if (isChecked) {
            input.value = firstPnr;
            input.readOnly = true;
        } else {
            input.value = '';
            input.readOnly = false;
        }
    });
}

function addPassenger() {
    const container = document.getElementById('passengers-container');
    const firstPassenger = container.querySelector('.passenger-template');
    
    if (!firstPassenger) return;
    
    const newPassenger = firstPassenger.cloneNode(true);
    const passengerCount = container.children.length;
    
    // Aggiorna ID
    newPassenger.id = `passenger-${passengerCount}`;
    
    // Resetta valori
    const inputs = newPassenger.querySelectorAll('input');
    inputs.forEach(input => {
        if (input.type === 'checkbox') {
            input.checked = false;
        } else {
            input.value = '';
        }
    });
    
    // Resetta select
    const select = newPassenger.querySelector('select');
    if (select) select.selectedIndex = 0;
    
    // Nascondi dettagli bagagli
    newPassenger.querySelectorAll('.hand-luggage-details, .checked-luggage-details').forEach(el => {
        el.style.display = 'none';
    });
    
    container.appendChild(newPassenger);
}

function addFlight() {
    const container = document.getElementById('flights-container');
    const firstFlight = container.querySelector('.flight-section');
    
    if (!firstFlight) return;
    
    const newFlight = firstFlight.cloneNode(true);
    const flightCount = container.children.length;
    
    // Aggiorna ID e titolo
    newFlight.id = `flight-${flightCount}`;
    const header = newFlight.querySelector('.flight-header h4');
    if (header) header.textContent = `Volo #${flightCount + 1}`;
    
    // Resetta valori
    const inputs = newFlight.querySelectorAll('input');
    inputs.forEach(input => {
        input.value = '';
        if (input.type === 'checkbox') {
            input.checked = false;
        }
    });
    
    // Nascondi anteprima logo
    const logoPreview = newFlight.querySelector('.logo-preview-container');
    if (logoPreview) logoPreview.style.display = 'none';
    
    // Mostra pulsante rimuovi
    const removeBtn = newFlight.querySelector('.remove-flight');
    if (removeBtn) removeBtn.style.display = 'block';
    
    // Aggiungi gestore eventi per il pulsante di ricerca
    const fetchBtn = newFlight.querySelector('.fetch-flight');
    if (fetchBtn) {
        fetchBtn.addEventListener('click', fetchFlightData);
    }
    
    container.appendChild(newFlight);
    
    // Aggiorna numerazione
    updateFlightNumbers();
}

function updateFlightNumbers() {
    const flights = document.querySelectorAll('#flights-container .flight-section');
    flights.forEach((flight, index) => {
        const header = flight.querySelector('.flight-header h4');
        if (header) header.textContent = `Volo #${index + 1}`;
        
        // Nascondi rimuovi solo per il primo volo
        const removeBtn = flight.querySelector('.remove-flight');
        if (removeBtn) {
            removeBtn.style.display = index === 0 ? 'none' : 'block';
        }
    });
}

function handleLuggageChanges(event) {
    if (event.target.classList.contains('hand-luggage')) {
        const details = event.target.closest('.row').querySelector('.hand-luggage-details');
        if (details) details.style.display = event.target.checked ? 'block' : 'none';
    }
    
    if (event.target.classList.contains('checked-luggage')) {
        const details = event.target.closest('.row').querySelector('.checked-luggage-details');
        if (details) details.style.display = event.target.checked ? 'block' : 'none';
    }
}

function validateForm(event) {
    const form = event.target;
    if (!form.checkValidity()) {
        event.preventDefault();
        event.stopPropagation();
    }
    form.classList.add('was-validated');
}

// Gestione rimozione voli
document.addEventListener('click', function(event) {
    if (event.target.classList.contains('remove-flight') || 
        event.target.closest('.remove-flight')) {
        removeFlight(event);
    }
    
    if (event.target.classList.contains('fetch-flight') || 
        event.target.closest('.fetch-flight')) {
        fetchFlightData(event);
    }
});

function removeFlight(event) {
    const flightSection = event.target.closest('.flight-section');
    if (!flightSection) return;
    
    const flightsContainer = document.getElementById('flights-container');
    if (flightsContainer.children.length > 1) {
        flightSection.remove();
        updateFlightNumbers();
    }
}

function fetchFlightData(event) {
    const button = event.target.closest('.fetch-flight');
    if (!button) return;
    
    const flightSection = button.closest('.flight-section');
    if (!flightSection) return;
    
    const flightNumber = flightSection.querySelector('.flight-number')?.value;
    const flightDate = flightSection.querySelector('.flight-date')?.value;
    
    if (!flightNumber || !flightDate) {
        alert('Inserisci numero volo e data');
        return;
    }
    
    // Salva HTML originale e disabilita pulsante
    const originalHtml = button.innerHTML;
    button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Caricamento...';
    button.disabled = true;
    
    // Esegui richiesta AJAX
    fetch(`/fetch-flight?flightNumber=${flightNumber}&date=${flightDate}`)
        .then(response => response.json())
        .then(data => {
            if (data) {
                flightSection.querySelector('input[name="route[]"]').value = data.route || '';
                flightSection.querySelector('input[name="departureTime[]"]').value = data.departureTime || '';
                flightSection.querySelector('input[name="arrivalTime[]"]').value = data.arrivalTime || '';
                flightSection.querySelector('.airline-name').value = data.airlineName || '';
                flightSection.querySelector('input[name="airlineCode[]"]').value = data.airlineCode || '';
                flightSection.querySelector('input[name="logoUrl[]"]').value = data.logoUrl || '';
                
                const preview = flightSection.querySelector('.preview-logo');
                if (preview) {
                    preview.src = data.logoUrl || '';
                    preview.closest('.logo-preview-container').style.display = 'block';
                }
            } else {
                alert('Volo non trovato');
            }
        })
        .catch(error => {
            console.error('Errore nella ricerca del volo:', error);
            alert('Errore nella ricerca del volo');
        })
        .finally(() => {
            // Ripristina pulsante
            button.innerHTML = originalHtml;
            button.disabled = false;
        });
}