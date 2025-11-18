// Chiamata helper: io la uso per tutte le richieste asincrone verso le servlet.
// Parametri:
//  - method: 'GET', 'POST', ecc. (lo passo così com'è a XMLHttpRequest.open)
//  - url: la servlet/endpoint (senza modifiche sul context path qui)
//  - formElement: se passo un <form>, lo trasformo in JSON e lo invio; altrimenti invio vuoto
//  - callback: la funzione che voglio chiamare ogni volta che cambia readyState (riceve l'oggetto XHR)
//  - reset: se true (default) resetto il form dopo l'invio (comodo per i modali)
function makeCall(method, url, formElement, callback, reset = true) {
    // Creo l'oggetto XHR e delego la gestione degli eventi al caller (callback).
    const req = new XMLHttpRequest();
    req.onreadystatechange = () => callback(req);

    // Apro la richiesta: non aggiungo nulla al path, passo esattamente l'url ricevuto.
    req.open(method, url);

    // Se mi è stato fornito un form lo invio come JSON, altrimenti invio senza body.
    if (formElement == null) {
        req.send();
    } else {
        // ✅ Converti il form in oggetto JavaScript
        const formData = new FormData(formElement);
        const data = {};
        formData.forEach((value, key) => {
            // Gestisci checkbox multipli con lo stesso nome
            if (data[key]) {
                // Se esiste già, trasformalo in array
                if (Array.isArray(data[key])) {
                    data[key].push(value);
                } else {
                    data[key] = [data[key], value];
                }
            } else {
                data[key] = value;
            }
        });

        // ✅ Imposta Content-Type per JSON
        req.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');

        // ✅ Invia l'oggetto come stringa JSON
        req.send(JSON.stringify(data));
    }

    // Se ho inviato un form e ho voluto il reset, svuoto i campi subito dopo l'invio.
    if (formElement != null && reset) formElement.reset();
}

// Rimuovo tutto il contenuto dentro #main. Lo chiamo quando voglio ricostruire la vista da zero.
function cleanMain() {
    const main = document.getElementById("main");
    if (main) main.innerHTML = "";
}

// Svuoto il contenitore delle modali dinamiche: utile prima di ricreare nuove modali
function clearModals() {
    const m = document.getElementById("modals");
    if (m) m.innerHTML = "";
}

// Costruisco una modale standardizzata che riutilizzo in tutta l'app.
// Struttura DOM che creo:
// <div id="..." class="modal-window hidden">
//   <div class="modal-content">
//     <div class="nav-bar">[title][spacer][close]</div>
//     <form class="stack"> ... <div class="nav-bar">[button]</div></form>
//   </div>
// </div>
function createModal(id, titleText, buttonId, buttonText) {
    const modal = document.createElement("div");
    modal.id = id;
    modal.className = "modal-window hidden";

    const container = document.createElement("div");
    container.className = "modal-content";
    const topNavbar = document.createElement("div");
    topNavbar.className = "nav-bar";

    const title = document.createElement("div");
    title.className = "modal-title";
    title.textContent = titleText;

    const spacer = document.createElement("div");
    spacer.className = "spacer";

    const close = document.createElement("button");
    close.type = "button";
    close.className = "modal-close";
    close.setAttribute("aria-label", "Chiudi modale");
    close.innerHTML = '<svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" aria-hidden="true"><path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/></svg>';
    close.addEventListener("click", () => closeModal(modal));

    const form = document.createElement("form");
    form.className = "stack";

    topNavbar.appendChild(title);
    topNavbar.appendChild(spacer);
    topNavbar.appendChild(close);
    container.appendChild(topNavbar);
    container.appendChild(form);
    modal.appendChild(container);

    const bottomNavbar = document.createElement("div");
    bottomNavbar.className = "nav-bar";
    const button = document.createElement("button");
    button.id = buttonId;
    button.type = "button";
    button.className = "button";
    button.textContent = buttonText;
    bottomNavbar.appendChild(button);
    form.appendChild(bottomNavbar);

    return modal;
}

// Mostro la modale: tolgo hidden e abilito pointer events.
function showModal(modal) {
    modal.classList.remove("hidden");
    modal.style.visibility = "visible";
    modal.style.pointerEvents = "auto";
}

// Nascondo la modale senza rimuoverla dal DOM (così posso riaprirla rapidamente).
function closeModal(modal) {
    modal.classList.add("hidden");
    modal.style.visibility = "hidden";
    modal.style.pointerEvents = "none";
}

// Rimuovo la bottom navbar legacy se presente (la uso raramente adesso).
function clearBottomNavbar() {
    const navbar = document.getElementById("bottom-nav-bar");
    if (navbar) navbar.remove();
}

// Recupero la lista delle tracce dell'utente o quelle non incluse in una playlist.
// Il parametro trackSelector può essere un <select> legacy oppure un container div dove creo checkbox rows.
// Se passo playlist, chiedo solo le tracce non presenti in quella playlist (usato per "Aggiungi brani").
function loadUserTracks(trackSelector, playlist = null) {
    trackSelector.innerHTML = "";
    const url = playlist == null ? "GetUserTracks" : "GetTracksNotInPlaylist?playlistTitle=" + encodeURIComponent(playlist.title);

    makeCall("GET", url, null, (req) => {
        if (req.readyState !== XMLHttpRequest.DONE) return;
        const message = req.responseText;
        if (req.status === 200) {
            let tracks = [];
            try { tracks = JSON.parse(message) || []; } catch (_) { tracks = []; }

            if (tracks.length === 0) {
                if (trackSelector.tagName && trackSelector.tagName.toLowerCase() === 'select') {
                    const option = document.createElement("option");
                    option.value = "";
                    option.textContent = "Nessuna traccia disponibile da aggiungere";
                    trackSelector.appendChild(option);
                } else {
                    const msg = document.createElement('div');
                    msg.className = 'no-tracks-msg';
                    msg.textContent = 'Nessuna traccia disponibile da aggiungere';
                    trackSelector.appendChild(msg);
                }
                return;
            }

            if (trackSelector.tagName && trackSelector.tagName.toLowerCase() === 'select') {
                tracks.forEach(track => {
                    const option = document.createElement("option");
                    option.value = track.id.toString();
                    option.textContent = track.artist + " - " + track.title + " (" + track.year + ")";
                    trackSelector.appendChild(option);
                });
            } else {
                const inputName = (trackSelector.id && trackSelector.id.indexOf('create') >= 0) ? 'selectedTrackIds' : 'selectedTracksIds';
                tracks.forEach(track => {
                    const row = document.createElement('div');
                    row.className = 'track-checkbox-row';

                    const input = document.createElement('input');
                    input.type = 'checkbox';
                    input.name = inputName;
                    input.value = track.id.toString();
                    input.id = (trackSelector.id || 'tracklist') + '-chk-' + track.id;
                    input.className = 'track-checkbox-input';

                    const label = document.createElement('label');
                    label.htmlFor = input.id;
                    label.className = 'track-checkbox-label';
                    label.textContent = track.artist + ' - ' + track.title + ' (' + track.year + ')';

                    row.appendChild(input);
                    row.appendChild(label);
                    trackSelector.appendChild(row);
                });
            }
        } else if (req.status === 401) {
            alert("Sessione scaduta. Effettua di nuovo il login.");
            window.location.href = "login.html";
        } else {
            alert("Impossibile recuperare i dati.");
        }
    }, false);
}