// Funzione per inizializzare il form
function initForm() {
    // Precompila la data corrente per il primo volo
    const now = new Date();
    const timezoneOffset = now.getTimezoneOffset() * 60000;
    const localISOTime = new Date(now - timezoneOffset).toISOString().slice(0, 16);
    const oneHourLater = new Date(now - timezoneOffset + 3600000).toISOString().slice(0, 16);

    $('input[name="departureTime[]"]').first().val(localISOTime);
    $('input[name="arrivalTime[]"]').first().val(oneHourLater);

    // Inizializza gestione PNR
    $('#samePnr').change(function() {
        const isChecked = $(this).is(':checked');
        const firstPnr = $('input[name="pnr[]"]').first().val();

        $('input[name="pnr[]"]').each(function(index) {
            if (index === 0) return; // Salta il primo

            if (isChecked) {
                $(this).val(firstPnr).prop('readonly', true);
            } else {
                $(this).val('').prop('readonly', false);
            }
        });
    });

    // Aggiungi passeggero
    $('#add-passenger').click(function() {
        const container = $('#passengers-container');
        const passengerCount = container.children().length;
        const newPassenger = container.children().first().clone();

        newPassenger.attr('id', 'passenger-' + passengerCount);
        newPassenger.find('input, select').val('');
        container.append(newPassenger);
    });

    // Aggiungi volo
    $('#add-flight').click(function() {
        const container = $('#flights-container');
        const flightCount = container.children().length;
        const newFlight = container.children().first().clone();

        newFlight.attr('id', 'flight-' + flightCount);
        newPassenger.find('input, select').val('');
        newFlight.find('.preview-logo').attr('src', '');
        newFlight.find('[id^="airline-logo-preview"]').hide();
        newFlight.find('.remove-flight').show();
        newFlight.find('.flight-header h4').text('Volo #' + (flightCount + 1));

        container.append(newFlight);
    });

    // Rimuovi volo
    $(document).on('click', '.remove-flight', function() {
        if ($('#flights-container').children().length > 1) {
            $(this).closest('.flight-section').remove();
        }
    });

    // Ricerca volo
    $(document).on('click', '.fetch-flight', function() {
        const flightSection = $(this).closest('.flight-section');
        const flightNumber = flightSection.find('.flight-number').val();
        const flightDate = flightSection.find('.flight-date').val();

        if (!flightNumber || !flightDate) {
            alert('Inserisci numero volo e data');
            return;
        }

        const fetchButton = flightSection.find('.fetch-flight');
        const originalHtml = fetchButton.html();
        fetchButton.html('<i class="fas fa-spinner fa-spin"></i> Caricamento...');
        fetchButton.prop('disabled', true);

        $.ajax({
            url: '/search-flight',
            type: 'GET',
            data: {
                flightNumber: flightNumber,
                date: flightDate
            },
            success: function(data) {
                fetchButton.html(originalHtml);
                fetchButton.prop('disabled', false);

                if (data) {
                    flightSection.find('input[name="route[]"]').val(data.route);
                    flightSection.find('input[name="departureTime[]"]').val(data.departureTime);
                    flightSection.find('input[name="arrivalTime[]"]').val(data.arrivalTime);
                    flightSection.find('input[name="airlineName[]"]').val(data.airlineName);
                    flightSection.find('input[name="airlineCode[]"]').val(data.airlineCode);
                    flightSection.find('input[name="logoUrl[]"]').val(data.logoUrl);

                    const preview = flightSection.find('.preview-logo');
                    preview.attr('src', data.logoUrl);
                    preview.closest('div').show();
                } else {
                    alert('Volo non trovato');
                }
            },
            error: function() {
                fetchButton.html(originalHtml);
                fetchButton.prop('disabled', false);
                alert('Errore nella ricerca del volo');
            }
        });
    });

    // Gestione bagagli
    $(document).on('change', '.hand-luggage', function() {
        const details = $(this).closest('.col-md-4').find('.hand-luggage-details');
        details.toggle(this.checked);
    });

    $(document).on('change', '.checked-luggage', function() {
        const details = $(this).closest('.col-md-4').find('.checked-luggage-details');
        details.toggle(this.checked);
    });

    // Validazione form
    const form = $('#ticketForm');
    form.on('submit', function(event) {
        if (form[0].checkValidity() === false) {
            event.preventDefault();
            event.stopPropagation();
        }
        form.addClass('was-validated');
    });
}

// Inizializza quando il documento è pronto
$(document).ready(function() {
    initForm();
});