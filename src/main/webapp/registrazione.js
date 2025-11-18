/**
 * registrazione.js - Logica per la pagina di registrazione di PoliMusic
 */

(function() {
    'use strict';

    // Riferimenti agli elementi DOM
    let registrationForm;
    let nomeInput;
    let cognomeInput;
    let usernameInput;
    let passwordInput;
    let submitBtn;
    let btnText;
    let btnLoader;

    /**
     * Inizializzazione quando il DOM è pronto
     */
    window.addEventListener('DOMContentLoaded', () => {
        console.log('Registrazione page inizializzata');
        initializeElements();
        initializeEventListeners();
    });

    /**
     * Inizializza i riferimenti agli elementi DOM
     */
    function initializeElements() {
        registrationForm = document.getElementById('registrationForm');
        nomeInput = document.getElementById('nome');
        cognomeInput = document.getElementById('cognome');
        usernameInput = document.getElementById('username');
        passwordInput = document.getElementById('password');
        submitBtn = document.getElementById('submitBtn');
        btnText = document.getElementById('btnText');
        btnLoader = document.getElementById('btnLoader');
    }

    /**
     * Inizializza gli event listeners
     */
    function initializeEventListeners() {
        if (registrationForm) {
            registrationForm.addEventListener('submit', handleRegistration);
        }

        // Validazione real-time per ogni campo
        const fields = [nomeInput, cognomeInput, usernameInput, passwordInput];
        fields.forEach(field => {
            if (field) {
                field.addEventListener('input', () => {
                    validateField(field);
                    hideError();
                    field.classList.remove('input-error');
                });
                field.addEventListener('blur', () => validateField(field));
            }
        });
    }

    /**
     * Valida un singolo campo
     * @param {HTMLInputElement} field - Campo da validare
     * @returns {boolean} True se valido
     */
    function validateField(field) {
        if (!field) return true;

        const value = field.value.trim();
        const fieldName = field.name;
        const errorElementId = `errore${fieldName.charAt(0).toUpperCase() + fieldName.slice(1)}`;
        const errorElement = document.getElementById(errorElementId);

        // Pattern per validazione
        const pattern = /^[a-zA-Z0-9_]{3,}$/;

        if (value === '') {
            field.classList.add('input-error');
            if (errorElement) {
                errorElement.textContent = 'Questo campo è obbligatorio';
                errorElement.style.display = 'block';
            }
            return false;
        } else if (!pattern.test(value)) {
            field.classList.add('input-error');
            if (errorElement) {
                errorElement.textContent = 'Deve contenere almeno 3 caratteri alfanumerici o underscore';
                errorElement.style.display = 'block';
            }
            return false;
        } else {
            field.classList.remove('input-error');
            if (errorElement) {
                errorElement.style.display = 'none';
            }
            return true;
        }
    }

    /**
     * Valida l'intero form
     * @returns {boolean} True se il form è valido
     */
    function validateRegistrationForm() {
        hideError();

        const nome = nomeInput.value.trim();
        const cognome = cognomeInput.value.trim();
        const username = usernameInput.value.trim();
        const password = passwordInput.value.trim();

        let isValid = true;
        let firstInvalidField = null;

        // Validazione nome
        if (!validateField(nomeInput)) {
            isValid = false;
            if (!firstInvalidField) firstInvalidField = nomeInput;
        }

        // Validazione cognome
        if (!validateField(cognomeInput)) {
            isValid = false;
            if (!firstInvalidField) firstInvalidField = cognomeInput;
        }

        // Validazione username
        if (!validateField(usernameInput)) {
            isValid = false;
            if (!firstInvalidField) firstInvalidField = usernameInput;
        }

        // Validazione password
        if (!validateField(passwordInput)) {
            isValid = false;
            if (!firstInvalidField) firstInvalidField = passwordInput;
        }

        // Focus sul primo campo invalido
        if (!isValid && firstInvalidField) {
            firstInvalidField.focus();
            showErrorMessage('Correggi i campi evidenziati in rosso');
        }

        return isValid;
    }

    /**
     * Gestisce il submit del form di registrazione
     * @param {Event} event - Evento submit
     */
    function handleRegistration(event) {
        event.preventDefault();

        console.log('Tentativo di registrazione...');

        // Validazione form
        if (!validateRegistrationForm()) {
            return;
        }

        // Disabilita il form durante il submit
        setFormLoading(true);

        // Prepara i dati del form
        const formData = new FormData(registrationForm);

        // Debug: verifica i dati
        console.log('Dati form registrazione:');
        for (let [key, value] of formData.entries()) {
            console.log(`${key}: ${value}`);
        }

        // Chiamata AJAX alla servlet
        makeCall(
            'POST',
            './registrazione',
            formData,
            onRegistrationSuccess,
            onRegistrationError,
            false // Non usare JSON, usa FormData
        );
    }

    /**
     * Callback in caso di registrazione riuscita
     * @param {Object} response - Risposta dal server
     */
    function onRegistrationSuccess(response) {
        console.log('Risposta registrazione:', response);

        setFormLoading(false);

        if (response.success) {
            // Registrazione riuscita
            showSuccess('Registrazione completata! Benvenuto in PoliMusic!');

            // Pulisci il form
            registrationForm.reset();

            // Redirect alla home dopo un breve delay
            setTimeout(() => {
                const redirectUrl = response.redirectUrl || './home.html';
                console.log('Redirect a:', redirectUrl);
                redirectTo(redirectUrl);
            }, 1500);

        } else {
            // Registrazione fallita
            showErrorMessage(response.error || 'Errore durante la registrazione');

            // Focus sul primo campo
            if (nomeInput) {
                nomeInput.focus();
            }
        }
    }

    /**
     * Callback in caso di errore nella chiamata
     * @param {string} error - Messaggio di errore
     */
    function onRegistrationError(error) {
        console.error('Errore registrazione:', error);

        setFormLoading(false);

        // Mostra messaggio di errore
        showErrorMessage(error || 'Errore durante la registrazione. Riprova.');

        // Focus sul primo campo
        if (nomeInput) {
            nomeInput.focus();
        }
    }

    /**
     * Mostra un messaggio di errore
     * @param {string} message - Messaggio da mostrare
     */
    function showErrorMessage(message) {
        const errorDiv = document.getElementById('errorMessage');
        const errorText = document.getElementById('errorText');

        if (errorDiv && errorText) {
            errorText.textContent = message;
            errorDiv.style.display = 'block';

            // Scroll to error message
            errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    /**
     * Nascondi il messaggio di errore
     */
    function hideError() {
        const errorDiv = document.getElementById('errorMessage');
        if (errorDiv) {
            errorDiv.style.display = 'none';
        }
    }

    /**
     * Abilita/disabilita lo stato di loading del form
     * @param {boolean} loading - True per abilitare loading
     */
    function setFormLoading(loading) {
        if (submitBtn) {
            submitBtn.disabled = loading;
        }

        if (btnText && btnLoader) {
            if (loading) {
                btnText.style.display = 'none';
                btnLoader.style.display = 'inline';
            } else {
                btnText.style.display = 'inline';
                btnLoader.style.display = 'none';
            }
        }

        // Disabilita/abilita input fields
        const inputs = registrationForm.querySelectorAll('input');
        inputs.forEach(input => {
            input.disabled = loading;
        });
    }

})();