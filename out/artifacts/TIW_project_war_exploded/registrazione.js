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

    document.addEventListener('DOMContentLoaded', () => {
        console.log('Registrazione page inizializzata');
        initializeElements();
        initializeEventListeners();
    });

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

    function handleRegistration(event) {
        event.preventDefault();

        console.log('Tentativo di registrazione...');

        // Validazione form
        if (!validateRegistrationForm()) {
            return;
        }

        // Mostra il loading visivamente
        setButtonLoading(true);

        // Usa makeCall da utils.js
        makeCall(
            'POST',
            'registrazione',
            registrationForm,
            function(req) {
                if (req.readyState !== XMLHttpRequest.DONE) return;

                setButtonLoading(false);

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
                            window.location.href = "index.html";
                        }, 1500);
                        return;
                    }

                    if (response.success !== false) {
                        // Salva username in sessionStorage
                        sessionStorage.setItem('username', usernameInput.value);

                        showSuccess("✅ Registrazione completata! Benvenuto in PoliMusic!");

                        // Redirect dopo un breve delay
                        setTimeout(() => {
                            window.location.href = "index.html";
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
            false  // NON resettare il form
        );

        console.log('Richiesta di registrazione inviata');
    }

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

    function hideError() {
        const errorDiv = document.getElementById('errorMessage');
        if (errorDiv) {
            errorDiv.style.display = 'none';
        }
    }

    function setButtonLoading(loading) {
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
    }

})();