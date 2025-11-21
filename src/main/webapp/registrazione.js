/**
 * registrazione.js - Logica RIA per la registrazione di PoliMusic
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
    document.addEventListener('DOMContentLoaded', () => {
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

        // Chiamata AJAX alla servlet con JSON
        makeCall(
            'POST',
            'registrazione',
            registrationForm,
            function(req) {
                if (req.readyState !== XMLHttpRequest.DONE) return;

                setFormLoading(false);

                const message = req.responseText;

                console.log("Status:", req.status);
                console.log("Response:", message);

                if (req.status === 200) {
                    let response;
                    try {
                        response = JSON.parse(message);
                    } catch (e) {
                        // Fallback se non è JSON
                        showSuccess("✅ Registrazione completata! Benvenuto in PoliMusic!");
                        setTimeout(() => {
                            window.location.href = "home.html";
                        }, 1500);
                        return;
                    }

                    if (response.success !== false) {
                        // Salva username in sessionStorage
                        sessionStorage.setItem('username', usernameInput.value);

                        showSuccess("✅ Registrazione completata! Benvenuto in PoliMusic!");

                        // Pulisci il form
                        registrationForm.reset();

                        // Redirect dopo un breve delay
                        setTimeout(() => {
                            window.location.href = response.redirectUrl || "home.html";
                        }, 1500);

                    } else {
                        showErrorMessage(response.error || "Errore durante la registrazione");
                        if (nomeInput) nomeInput.focus();
                    }

                } else if (req.status === 409) {
                    // Username duplicato
                    showErrorMessage("Username già esistente. Scegline un altro.");
                    usernameInput.focus();
                    usernameInput.classList.add('input-error');
                } else if (req.status === 400) {
                    // Errore di validazione
                    try {
                        const response = JSON.parse(message);
                        showErrorMessage(response.error || "Compila correttamente tutti i campi");
                    } catch (e) {
                        showErrorMessage("Compila correttamente tutti i campi");
                    }
                } else {
                    showErrorMessage(message || "Errore durante la registrazione. Riprova.");
                }
            },
            false // Usa JSON (gestito da makeCall con utils.js)
        );
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
            errorDiv.style.backgroundColor = '';
            errorDiv.style.borderColor = '';
            errorDiv.style.color = '';
            errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    /**
     * Mostra un messaggio di successo
     * @param {string} message - Messaggio da mostrare
     */
    function showSuccess(message) {
        const errorDiv = document.getElementById('errorMessage');
        const errorText = document.getElementById('errorText');

        if (errorDiv && errorText) {
            errorText.textContent = message;
            errorDiv.style.display = 'block';
            errorDiv.style.backgroundColor = '#d4edda';
            errorDiv.style.borderColor = '#c3e6cb';
            errorDiv.style.color = '#155724';
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