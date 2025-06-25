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
    
    // Imposta colori di default
    document.documentElement.style.setProperty('--primary', '#3498db');
    document.documentElement.style.setProperty('--secondary', '#2c3e50');
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

// Inizializza quando il documento è pronto
document.addEventListener('DOMContentLoaded', initForm);