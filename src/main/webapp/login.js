/**
 * Login management
 */

(function() {

    document.getElementById("loginForm").addEventListener('submit', (e) => {
        e.preventDefault();

        const form = e.target;

        if (form.checkValidity()) {
            document.getElementById("errorMessage").style.display = "none";

            const submitBtn = document.getElementById("submitBtn");
            const btnText = document.getElementById("btnText");
            const btnLoader = document.getElementById("btnLoader");

            submitBtn.disabled = true;
            btnText.style.display = "none";
            btnLoader.style.display = "inline";

            makeCall(
                "POST",
                'login',
                form,
                function(req) {
                    if (req.readyState !== XMLHttpRequest.DONE) return;

                    submitBtn.disabled = false;
                    btnText.style.display = "inline";
                    btnLoader.style.display = "none";

                    const message = req.responseText;

                    console.log("Status:", req.status);
                    console.log("Response:", message);

                    if (req.status === 200) {
                        let response;
                        try {
                            response = JSON.parse(message);
                        } catch (e) {
                            const username = document.getElementById("username").value;
                            sessionStorage.setItem('username', username);
                            showSuccess("✅ Login effettuato con successo!");
                            setTimeout(() => {
                                window.location.href = "home.html";
                            }, 500);
                            return;
                        }

                        if (response.success !== false) {
                            sessionStorage.setItem('username', response.username || document.getElementById("username").value);
                            showSuccess("✅ Login effettuato con successo!");
                            setTimeout(() => {
                                window.location.href = response.redirectUrl || "home.html";
                            }, 500);
                        } else {
                            showErrorMessage(response.error || "Credenziali non valide");
                        }

                    } else if (req.status === 401) {
                        showErrorMessage("Username o password errati");
                    } else if (req.status === 400) {
                        showErrorMessage("Compila tutti i campi obbligatori");
                    } else {
                        showErrorMessage(message || "Errore durante il login. Riprova.");
                    }
                },
                false
            );

        } else {
            form.reportValidity();
        }
    });

    function showErrorMessage(message) {
        const errorDiv = document.getElementById("errorMessage");
        const errorText = document.getElementById("errorText");

        if (errorDiv && errorText) {
            errorText.textContent = message;
            errorDiv.style.display = "block";
            errorDiv.style.backgroundColor = "";
            errorDiv.style.borderColor = "";
            errorDiv.style.color = "";
            errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

    function showSuccess(message) {
        const errorDiv = document.getElementById("errorMessage");
        const errorText = document.getElementById("errorText");

        if (errorDiv && errorText) {
            errorText.textContent = message;
            errorDiv.style.display = "block";
            errorDiv.style.backgroundColor = "#d4edda";
            errorDiv.style.borderColor = "#c3e6cb";
            errorDiv.style.color = "#155724";
            errorDiv.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }

})();