function makeCall(method, url, formElement, callback, reset = true) {
    const req = new XMLHttpRequest();
    req.onreadystatechange = () => callback(req);
    req.open(method, url);

    if (formElement == null) {
        req.send();
    } else {
        const formData = new FormData(formElement);
        const data = {};
        formData.forEach((value, key) => {
            if (data[key]) {
                if (Array.isArray(data[key])) {
                    data[key].push(value);
                } else {
                    data[key] = [data[key], value];
                }
            } else {
                data[key] = value;
            }
        });

        req.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
        req.send(JSON.stringify(data));
    }

    if (formElement != null && reset) formElement.reset();
}

function apriModale(modaleId) {
    const modale = document.getElementById(modaleId);
    if (!modale) {
        console.error('Modale non trovato:', modaleId);
        return;
    }
    modale.classList.remove('hidden');
    modale.style.display = 'flex';
}

function chiudiModale(modaleId) {
    const modale = document.getElementById(modaleId);
    if (!modale) return;
    modale.classList.add('hidden');
    modale.style.display = 'none';
}

function mostraMessaggio(messaggio, tipo = 'info', containerId = 'messageContainer', durata = 5000) {
    const contenitore = document.getElementById(containerId);
    if (!contenitore) {
        console.warn('Contenitore messaggi non trovato:', containerId);
        alert(messaggio);
        return;
    }

    contenitore.textContent = messaggio;
    contenitore.className = `message message-${tipo}`;
    contenitore.style.display = 'block';

    // Scroll appropriato in base al contenitore
    if (containerId === 'messageContainer') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
        // Per messaggi in modali
        const modalBody = contenitore.closest('.modal-body-scroll');
        if (modalBody) {
            modalBody.scrollTo({ top: 0, behavior: 'smooth' });
        }
    }

    setTimeout(() => {
        contenitore.style.display = 'none';
    }, durata);
}

function formattaData(stringaData) {
    if (!stringaData) return 'N/A';

    try {
        const data = new Date(stringaData);
        const giorno = String(data.getDate()).padStart(2, '0');
        const mese = String(data.getMonth() + 1).padStart(2, '0');
        const anno = data.getFullYear();
        return `${giorno}/${mese}/${anno}`;
    } catch (e) {
        return stringaData;
    }
}

function escapeHtml(testo) {
    if (!testo) return '';
    const div = document.createElement('div');
    div.textContent = testo;
    return div.innerHTML;
}

function validaLunghezzaTesto(testo, maxLength) {
    return testo && testo.trim().length > 0 && testo.length <= maxLength;
}

function validaFileAudio(file) {
    if (!file) return false;

    const nomeFile = file.name.toLowerCase();
    const estensioniValide = ['.mp3'];
    return estensioniValide.some(ext => nomeFile.endsWith(ext));
}