(function() {
    'use strict';

    let datiUtente = null;
    let tuttiBrani = [];
    let tuttiAlbum = [];
    let tuttePlaylist = [];

    let playlistCorrente = null;
    let braniPlaylistCorrente = [];
    let braniDisponibili = [];
    let albumsMap = {};
    let offsetCorrente = 0;
    const BRANI_PER_PAGINA = 5;

    // INIZIALIZZAZIONE

    window.addEventListener('DOMContentLoaded', () => {
        console.log('Home page RIA caricata');

        if (typeof makeCall !== 'function') {
            console.error('ERRORE: makeCall non definita!');
            alert('Errore: funzioni di utilità non caricate');
            return;
        }

        inizializzaApplicazione();
    });

    function inizializzaApplicazione() {
        impostaEventListener();
        caricaDatiHome();
    }

    // EVENT LISTENERS

    function impostaEventListener() {
        // Logout
        document.getElementById('LogoutBtn')?.addEventListener('click', gestisciLogout);

        // Form creazione playlist
        document.getElementById('creaPlaylistForm')?.addEventListener('submit', gestisciCreazionePlaylist);

        // Form upload brano
        document.getElementById('CaricaBranoForm')?.addEventListener('submit', gestisciCaricamentoBrano);

        // Chiusura modale playlist
        document.getElementById('ChiudiModalPlaylistBtn')?.addEventListener('click', () => {
            chiudiModale('modalPlaylist');
        });

        // Chiusura modale cliccando sull'overlay
        const modalPlaylist = document.getElementById('modalPlaylist');
        if (modalPlaylist) {
            modalPlaylist.querySelector('.modal-overlay')?.addEventListener('click', () => {
                chiudiModale('modalPlaylist');
            });
        }

        // Paginazione playlist (nel modale)
        document.getElementById('PrecedentiBtn')?.addEventListener('click', mostraBraniPrecedenti);
        document.getElementById('SuccessiviBtn')?.addEventListener('click', mostraBraniSuccessivi);

        // Form aggiungi brani a playlist
        document.getElementById('aggiungiBraniAllaPlaylistForm')?.addEventListener('submit', gestisciAggiuntaBrani);

        // Bottone riordina brani
        const RiordinaBtn = document.getElementById('RiordinaBraniBtn');
        if (RiordinaBtn) {
            RiordinaBtn.addEventListener('click', apriModaleRiordino);
        }

        // Chiusura modale riordino
        document.getElementById('ChiudiModalRiordinoBtn')?.addEventListener('click', () => {
            chiudiModale('modalRiordino');
        });
        document.getElementById('AnnullaRiordinoBtn')?.addEventListener('click', () => {
            chiudiModale('modalRiordino');
        });

        // Chiusura modale riordino cliccando sull'overlay
        const modalRiordino = document.getElementById('modalRiordino');
        if (modalRiordino) {
            modalRiordino.querySelector('.modal-overlay')?.addEventListener('click', () => {
                chiudiModale('modalRiordino');
            });
        }

        // Salva ordinamento
        document.getElementById('SalvaOrdinamentoBtn')?.addEventListener('click', salvaOrdinamento);

        // Chiusura modale player
        document.getElementById('ChiudiModalPlayerBtn')?.addEventListener('click', chiudiModalePlayer);

        // Chiusura modale player cliccando sull'overlay
        const modalPlayer = document.getElementById('modalPlayer');
        if (modalPlayer) {
            modalPlayer.querySelector('.modal-overlay')?.addEventListener('click', chiudiModalePlayer);
        }
    }

    // CARICAMENTO DATI HOME

    function caricaDatiHome() {
        console.log('Caricamento dati home...');

        makeCall(
            'GET',
            'home',
            null,
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;

                console.log('Risposta home - Status:', req.status);

                if (req.status === 200) {
                    let risposta;
                    try {
                        console.log(req.responseText);
                        risposta = JSON.parse(req.responseText);
                        console.log('Dati home ricevuti:', risposta);
                    } catch (e) {
                        console.error('Errore parsing JSON:', e);
                        mostraMessaggio('Errore nel formato dei dati ricevuti', 'error');
                        return;
                    }

                    if (risposta.success === false) {
                        console.error('Errore dal server:', risposta.error);
                        mostraMessaggio(risposta.error || 'Errore nel caricamento', 'error');

                        if (risposta.error && risposta.error.includes('non autenticato')) {
                            setTimeout(() => {
                                window.location.href = risposta.redirectUrl || 'index.html';
                            }, 2000);
                        }
                        return;
                    }

                    // Salva i dati
                    datiUtente = risposta;
                    tuttiBrani = risposta.brani || [];
                    tuttiAlbum = risposta.albums || [];
                    tuttePlaylist = risposta.playlists || [];

                    console.log(`Dati caricati: ${tuttePlaylist.length} playlist, ${tuttiBrani.length} brani`);

                    // SHOW: Renderizza la pagina
                    mostraHome(risposta);

                } else if (req.status === 401) {
                    mostraMessaggio('Sessione scaduta. Reindirizzamento...', 'error');
                    setTimeout(() => window.location.href = 'index.html', 2000);
                } else {
                    mostraMessaggio('Errore nel caricamento (HTTP ' + req.status + ')', 'error');
                }
            },
            false
        );
    }


    // SHOW: MOSTRA HOME PAGE

    function mostraHome(dati) {
        console.log('Mostra home page');

        // Username
        const spanUsername = document.getElementById('username');
        if (spanUsername && dati.username) {
            spanUsername.textContent = dati.username;
        }

        // Renderizza componenti
        mostraListaPlaylist(dati.playlists || []);
        mostraSelectAlbum(dati.albums || []);
        mostraBraniPerPlaylist(dati.brani || []);
    }


    // SHOW: LISTA PLAYLIST

    function mostraListaPlaylist(playlist) {
        const contenitore = document.getElementById('contenitorePlaylist');
        if (!contenitore) return;

        contenitore.innerHTML = '';

        if (!playlist || playlist.length === 0) {
            contenitore.innerHTML = '<p class="empty-message">📭 Non hai ancora creato playlist. Crea la tua prima playlist!</p>';
            return;
        }

        console.log(`Mostra ${playlist.length} brani`);

        playlist.forEach(pl => {
            if (!pl) return;

            const card = document.createElement('div');
            card.className = 'playlist-card';

            const titolo = pl.titolo || pl.title || 'Playlist senza titolo';
            const dataCreazione = formattaData(pl.data_creazione || pl.dataCreazione || pl.date);
            const idPlaylist = pl.id_playlist || pl.idPlaylist || pl.id;

            card.innerHTML = `
                <h3>${escapeHtml(titolo)}</h3>
                <p class="playlist-date">📅 ${dataCreazione}</p>
                <button class="btn-view" data-playlist-id="${idPlaylist}">
                    Visualizza Playlist →
                </button>
            `;

            contenitore.appendChild(card);
        });

        // Event listener per aprire playlist
        contenitore.querySelectorAll('.btn-view').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const idPlaylist = e.target.getAttribute('data-playlist-id');
                apriModalePlaylist(idPlaylist);
            });
        });
    }

    // SHOW: SELECT ALBUM

    function mostraSelectAlbum(album) {
        const select = document.getElementById('SelezionaAlbum');
        if (!select) return;

        select.innerHTML = '<option value="">-- Seleziona un album --</option>';

        if (!album || album.length === 0) {
            select.innerHTML += '<option value="" disabled>⚠️ Nessun album disponibile</option>';
            select.disabled = true;
            return;
        }

        select.disabled = false;

        album.forEach(alb => {
            const opzione = document.createElement('option');
            opzione.value = alb.id_album;
            opzione.textContent = `${alb.titolo_album} - ${alb.interprete} (${alb.anno_pubblicazione})`;
            select.appendChild(opzione);
        });
    }

    // SHOW: BRANI PER CREAZIONE PLAYLIST

    function mostraBraniPerPlaylist(brani) {
        const contenitore = document.getElementById('braniSelectionContainer');
        const btnSubmit = document.getElementById('creaPlaylistBtn');

        if (!contenitore) return;

        contenitore.innerHTML = '';

        if (!brani || brani.length === 0) {
            contenitore.innerHTML = '<p class="empty-message">📭 Nessun brano disponibile. Carica prima alcuni brani.</p>';
            if (btnSubmit) btnSubmit.disabled = true;
            return;
        }

        if (btnSubmit) btnSubmit.disabled = false;

        // Checkbox "Seleziona tutti"
        const divSelezionaTutti = document.createElement('div');
        divSelezionaTutti.className = 'select-all-container';
        divSelezionaTutti.innerHTML = `
            <label>
                <input type="checkbox" id="selectAllBrani">
                <strong>✓ Seleziona tutti i brani</strong>
            </label>
        `;
        contenitore.appendChild(divSelezionaTutti);

        document.getElementById('selectAllBrani')?.addEventListener('change', (e) => {
            contenitore.querySelectorAll('input[name="braniSelezionati"]').forEach(cb => {
                cb.checked = e.target.checked;
            });
        });

        // Mappa album
        const mappaAlbum = {};
        tuttiAlbum.forEach(album => {
            mappaAlbum[album.id_album] = album;
        });

        // Renderizza brani
        brani.forEach(brano => {
            const divBrano = document.createElement('div');
            divBrano.className = 'brano-item';

            const album = mappaAlbum[brano.id_album];
            const nomeAlbum = album ? album.titolo_album : 'Album sconosciuto';
            const interprete = album ? album.interprete : '';
            const anno = album ? album.anno_pubblicazione : '';

            divBrano.innerHTML = `
                <label>
                    <input type="checkbox" name="braniSelezionati" value="${brano.id_brano}">
                    <span class="brano-info">
                        <strong>${escapeHtml(brano.titolo)}</strong>
                        <span class="brano-meta">${escapeHtml(nomeAlbum)}${interprete ? ' - ' + escapeHtml(interprete) : ''}${anno ? ' (' + anno + ')' : ''}</span>
                    </span>
                </label>
            `;

            contenitore.appendChild(divBrano);
        });
    }

    // GESTIONE CREAZIONE PLAYLIST

    function gestisciCreazionePlaylist(e) {
        e.preventDefault();

        const form = e.target;
        const titolo = document.getElementById('playlistTitle').value.trim();
        const checkboxes = form.querySelectorAll('input[name="braniSelezionati"]:checked');

        if (!titolo) {
            mostraMessaggio('⚠️ Inserisci un titolo per la playlist', 'error');
            return;
        }

        if (!validaLunghezzaTesto(titolo, 50)) {
            mostraMessaggio('⚠️ Il titolo non può superare i 50 caratteri', 'error');
            return;
        }

        if (checkboxes.length === 0) {
            mostraMessaggio('⚠️ Seleziona almeno un brano', 'error');
            return;
        }

        console.log('Creazione playlist:', titolo, 'con', checkboxes.length, 'brani');

        makeCall(
            'POST',
            'CreaPlaylist',
            form,
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;

                if (req.status === 200) {
                    mostraMessaggio('✅ Playlist "' + titolo + '" creata con successo!', 'success');
                    form.reset();
                    // Ricarica i dati
                    setTimeout(() => caricaDatiHome(), 1000);
                } else {
                    mostraMessaggio('❌ Errore nella creazione (HTTP ' + req.status + ')', 'error');
                }
            },
            true
        );
    }

    // GESTIONE UPLOAD BRANO

    function gestisciCaricamentoBrano(e) {
        e.preventDefault();

        const form = e.target;
        const titolo = document.getElementById('titolo_brano').value.trim();
        const idAlbum = document.getElementById('SelezionaAlbum').value;
        const inputFile = document.getElementById('fileBrano');

        if (!titolo) {
            mostraMessaggio('⚠️ Il titolo del brano è obbligatorio', 'error');
            return;
        }

        if (!idAlbum) {
            mostraMessaggio('⚠️ Devi selezionare un album', 'error');
            return;
        }

        if (!inputFile.files || !inputFile.files[0]) {
            mostraMessaggio('⚠️ Devi caricare un file audio', 'error');
            return;
        }

        const file = inputFile.files[0];

        // Validazione estensione file usando utils.js
        if (!validaFileAudio(file)) {
            mostraMessaggio('⚠️ Formato non valido. Usa MP3', 'error');
            return;
        }

        const maxSize = 50 * 1024 * 1024; // 50MB in bytes
        if (file.size > maxSize) {
            mostraMessaggio('⚠️ Il file è troppo grande. Massimo 50MB', 'error');
            return;
        }

        console.log('Upload brano:', titolo);

        const formData = new FormData(form);

        for (let [key, value] of formData.entries()) {
            if (value instanceof File) {
                console.log(`  ${key}: File(${value.name}, ${value.size} bytes)`);
            } else {
                console.log(`  ${key}: ${value}`);
            }
        }

        const xhr = new XMLHttpRequest();

        xhr.onreadystatechange = function() {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;

            if (xhr.status === 200) {
                let risposta;
                try {
                    risposta = JSON.parse(xhr.responseText);
                    console.log('✅ Risposta parsata:', risposta);

                    if (risposta.success) {
                        mostraMessaggio('✅ ' + risposta.message, 'success');
                        form.reset();
                        // Ricarica i dati dopo 1 secondo
                        setTimeout(() => {
                            console.log('🔄 Ricarico dati home...');
                            caricaDatiHome();
                        }, 1000);
                    } else {
                        mostraMessaggio('❌ ' + (risposta.error || 'Errore durante il caricamento'), 'error');
                    }
                } catch (e) {
                    console.error('⚠️ Errore parsing JSON:', e);
                    console.log('⚠️ Response non è JSON valido, ma status è 200 - considero successo');
                    mostraMessaggio('✅ Brano "' + titolo + '" caricato con successo!', 'success');
                    form.reset();
                    setTimeout(() => caricaDatiHome(), 1000);
                }
            } else if (xhr.status === 400) {
                // Errore validazione
                try {
                    const risposta = JSON.parse(xhr.responseText);
                    mostraMessaggio((risposta.error || 'Dati non validi'), 'error');
                } catch (e) {
                    mostraMessaggio('Errore: dati non validi (HTTP 400)', 'error');
                }
            } else if (xhr.status === 401) {
                mostraMessaggio('Sessione scaduta. Reindirizzamento...', 'error');
                setTimeout(() => window.location.href = 'index.html', 2000);
            } else if (xhr.status === 500) {
                try {
                    const risposta = JSON.parse(xhr.responseText);
                    mostraMessaggio((risposta.error || 'Errore del server'), 'error');
                } catch (e) {
                    mostraMessaggio('Errore del server (HTTP 500)', 'error');
                }
            } else {
                mostraMessaggio('Errore upload (HTTP ' + xhr.status + ')', 'error');
            }
        };

        // ⚠️ NON impostare Content-Type! Il browser lo imposta automaticamente con il boundary corretto
        xhr.open('POST', 'caricaBrano');

        console.log('Invio richiesta upload...');
        xhr.send(formData);
    }

    // MODALE PLAYLIST: APRI

    function apriModalePlaylist(idPlaylist) {
        console.log('Apri modale playlist:', idPlaylist);

        // Reset stato
        offsetCorrente = 0;
        playlistCorrente = null;
        braniPlaylistCorrente = [];
        braniDisponibili = [];
        albumsMap = {};

        // Apri modale
        apriModale('modalPlaylist');

        // Carica dati playlist
        caricaDatiPlaylist(idPlaylist);
    }

    // LOAD: DATI PLAYLIST

    function caricaDatiPlaylist(idPlaylist) {
        console.log('Caricamento dati playlist:', idPlaylist);

        makeCall(
            'GET',
            'GetPlaylist?id_playlist=' + idPlaylist,
            null,
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;

                if (req.status === 200) {
                    let risposta;
                    try {
                        risposta = JSON.parse(req.responseText);
                        console.log('Dati playlist ricevuti:', risposta);
                    } catch (e) {
                        console.error('Errore parsing JSON:', e);
                        mostraMessaggio('Errore nel formato dei dati', 'error', 'playlistMessageContainer');
                        return;
                    }

                    if (risposta.success) {
                        // Salva dati
                        playlistCorrente = risposta.playlist;
                        braniPlaylistCorrente = risposta.brani || [];
                        braniDisponibili = risposta.braniDisponibili || [];

                        // Crea mappa albums
                        albumsMap = {};
                        (risposta.albums || []).forEach(album => {
                            albumsMap[album.id_album] = album;
                        });

                        // Controlla se esiste ordinamento personalizzato
                        applicaOrdinamentoSeNecessario();

                        offsetCorrente = 0;

                        console.log(`Playlist caricata: ${braniPlaylistCorrente.length} brani`);

                        // SHOW: Mostra playlist
                        mostraPlaylist();

                    } else {
                        mostraMessaggio(risposta.error || 'Errore caricamento', 'error', 'playlistMessageContainer');
                    }
                } else if (req.status === 401) {
                    mostraMessaggio('Sessione scaduta', 'error', 'playlistMessageContainer');
                    setTimeout(() => window.location.href = 'index.html', 2000);
                } else {
                    mostraMessaggio('Errore (HTTP ' + req.status + ')', 'error', 'playlistMessageContainer');
                }
            },
            false
        );
    }

    // APPLICA ORDINAMENTO PERSONALIZZATO

    function applicaOrdinamentoSeNecessario() {
        console.log('=== DEBUG ORDINAMENTO ===');
        console.log('Numero brani:', braniPlaylistCorrente.length);

        if (braniPlaylistCorrente.length > 0) {
            console.log('Esempio primo brano:', braniPlaylistCorrente[0]);
            console.log('Campi disponibili:', Object.keys(braniPlaylistCorrente[0]));
        }

        braniPlaylistCorrente.forEach((brano, idx) => {
            console.log(`Brano ${idx}: "${brano.titolo}" - ordinamento: ${brano.ordinamento}`);
        });

        // Controlla se tutti i brani hanno ordinamento = 0
        const tuttiZero = braniPlaylistCorrente.every(brano => {
            const ord = brano.ordinamento || 0;
            return ord === 0;
        });

        console.log('Tutti ordinamento = 0?', tuttiZero);

        if (tuttiZero) {
            console.log('Nessun ordinamento personalizzato rilevato. Ordine originale mantenuto.');
            return;
        }

        // Esiste un ordinamento personalizzato: riordina i brani
        console.log('Ordinamento personalizzato rilevato. Riordino brani...');

        braniPlaylistCorrente.sort((a, b) => {
            const ordA = a.ordinamento || 0;
            const ordB = b.ordinamento || 0;
            return ordA - ordB;
        });

        console.log('Brani riordinati secondo ordinamento personalizzato');
        console.log('Nuovo ordine:');
        braniPlaylistCorrente.forEach((brano, idx) => {
            console.log(`  ${idx + 1}. "${brano.titolo}" (ordinamento: ${brano.ordinamento})`);
        });
        console.log('=== FINE DEBUG ===');
    }

    // SHOW: MOSTRA PLAYLIST NEL MODALE

    function mostraPlaylist() {
        console.log('Mostra playlist nel modale');

        // Titolo
        const titoloEl = document.getElementById('playlistTitleView');
        if (titoloEl) {
            titoloEl.textContent = '🎧 ' + playlistCorrente.titolo;
        }

        // Mostra tabella brani
        mostraTabellaPlaylist();

        // Mostra brani disponibili
        mostraBraniDisponibili();
    }

    // SHOW: TABELLA BRANI (5 per riga)

    function mostraTabellaPlaylist() {
        const tbody = document.getElementById('tabellaBraniBody');
        if (!tbody) return;

        tbody.innerHTML = '';

        if (braniPlaylistCorrente.length === 0) {
            const tr = document.createElement('tr');
            tr.innerHTML = '<td colspan="5" class="empty-message">📭 Questa playlist non contiene ancora brani</td>';
            tbody.appendChild(tr);
            aggiornaControlliPaginazione();
            return;
        }

        // Calcola brani da visualizzare
        const inizio = offsetCorrente;
        const fine = Math.min(inizio + BRANI_PER_PAGINA, braniPlaylistCorrente.length);
        const braniDaVisualizzare = braniPlaylistCorrente.slice(inizio, fine);

        console.log(`Visualizzo brani da ${inizio} a ${fine} (offset: ${offsetCorrente})`);

        // Crea riga
        const tr = document.createElement('tr');

        braniDaVisualizzare.forEach(brano => {
            const td = document.createElement('td');
            td.className = 'song-cell';

            const album = albumsMap[brano.id_album];
            const imgSrc = album ? `GetAlbum?brano_id=${brano.id_brano}` : '';

            td.innerHTML = `
                <div class="song-card" data-brano-id="${brano.id_brano}">
                    <img src="${imgSrc}" 
                         alt="${escapeHtml(album ? album.titolo_album : 'Album')}"
                         class="song-image">
                    <div class="song-title">${escapeHtml(brano.titolo)}</div>
                </div>
            `;

            tr.appendChild(td);
        });

        // Celle vuote se meno di 5
        const celleVuote = BRANI_PER_PAGINA - braniDaVisualizzare.length;
        for (let i = 0; i < celleVuote; i++) {
            const tdVuota = document.createElement('td');
            tdVuota.className = 'song-cell empty';
            tr.appendChild(tdVuota);
        }

        tbody.appendChild(tr);

        // Event listener click sui brani
        tbody.querySelectorAll('.song-card').forEach(card => {
            card.addEventListener('click', () => {
                const idBrano = card.getAttribute('data-brano-id');
                apriModalePlayer(idBrano);
            });
        });

        // UPDATE: Aggiorna controlli paginazione
        aggiornaControlliPaginazione();
    }

    // UPDATE: CONTROLLI PAGINAZIONE

    function aggiornaControlliPaginazione() {
        const PrecedentiBtn = document.getElementById('PrecedentiBtn');
        const SuccessiviBtn = document.getElementById('SuccessiviBtn');
        const infoEl = document.getElementById('paginationInfo');

        const totaleBrani = braniPlaylistCorrente.length;
        const paginaCorrente = Math.floor(offsetCorrente / BRANI_PER_PAGINA) + 1;
        const totalePagine = Math.ceil(totaleBrani / BRANI_PER_PAGINA);

        const ciSonoPrecedenti = offsetCorrente > 0;
        const ciSonoSuccessivi = (offsetCorrente + BRANI_PER_PAGINA) < totaleBrani;

        console.log(`Paginazione: pagina ${paginaCorrente}/${totalePagine}, precedenti=${ciSonoPrecedenti}, successivi=${ciSonoSuccessivi}`);

        if (ciSonoPrecedenti) {
            PrecedentiBtn.classList.remove('hidden');
        } else {
            PrecedentiBtn.classList.add('hidden');
        }

        if (ciSonoSuccessivi) {
            SuccessiviBtn.classList.remove('hidden');
        } else {
            SuccessiviBtn.classList.add('hidden');
        }

        if (totaleBrani > 0 && totalePagine > 1) {
            infoEl.textContent = `Pagina ${paginaCorrente} di ${totalePagine} (${totaleBrani} brani totali)`;
        } else if (totaleBrani > 0) {
            infoEl.textContent = `${totaleBrani} brani`;
        } else {
            infoEl.textContent = '';
        }
    }

    // PAGINAZIONE: PRECEDENTI

    function mostraBraniPrecedenti() {
        offsetCorrente = Math.max(0, offsetCorrente - BRANI_PER_PAGINA);
        console.log('Vai a brani precedenti - nuovo offset:', offsetCorrente);
        mostraTabellaPlaylist();
    }

    // PAGINAZIONE: SUCCESSIVI

    function mostraBraniSuccessivi() {
        const nuovoOffset = offsetCorrente + BRANI_PER_PAGINA;
        if (nuovoOffset < braniPlaylistCorrente.length) {
            offsetCorrente = nuovoOffset;
            console.log('Vai a brani successivi - nuovo offset:', offsetCorrente);
            mostraTabellaPlaylist();
        }
    }

    // SHOW: BRANI DISPONIBILI DA AGGIUNGERE

    function mostraBraniDisponibili() {
        const contenitore = document.getElementById('braniDisponibiliContainer');
        const btnSubmit = document.getElementById('aggiungiBraniAllaPlaylistBtn');

        if (!contenitore) return;

        contenitore.innerHTML = '';

        if (!braniDisponibili || braniDisponibili.length === 0) {
            contenitore.innerHTML = '<p class="empty-message">📭 Nessun brano disponibile da aggiungere.</p>';
            if (btnSubmit) btnSubmit.disabled = true;
            return;
        }

        if (btnSubmit) btnSubmit.disabled = false;

        // Hidden input con ID playlist
        const hiddenInput = document.createElement('input');
        hiddenInput.type = 'hidden';
        hiddenInput.name = 'id_playlist';
        hiddenInput.value = playlistCorrente.id_playlist;
        contenitore.appendChild(hiddenInput);

        // Checkbox "Seleziona tutti"
        const divSelezionaTutti = document.createElement('div');
        divSelezionaTutti.className = 'select-all-container';
        divSelezionaTutti.innerHTML = `
            <label>
                <input type="checkbox" id="selectAllBraniDisponibili">
                <strong>✓ Seleziona tutti i brani disponibili</strong>
            </label>
        `;
        contenitore.appendChild(divSelezionaTutti);

        document.getElementById('selectAllBraniDisponibili')?.addEventListener('change', (e) => {
            contenitore.querySelectorAll('input[name="braniSelezionati_id"]').forEach(cb => {
                cb.checked = e.target.checked;
            });
        });

        // Renderizza brani
        braniDisponibili.forEach(brano => {
            const divBrano = document.createElement('div');
            divBrano.className = 'brano-item';

            const album = albumsMap[brano.id_album];
            const titolo_album = album ? album.titolo_album : 'Album sconosciuto';
            const interprete = album ? album.interprete : '';
            const anno_pubblicazione = album ? album.anno_pubblicazione : '';

            divBrano.innerHTML = `
                <label>
                    <input type="checkbox" name="braniSelezionati_id" value="${brano.id_brano}">
                    <span class="brano-info">
                        <strong>${escapeHtml(brano.titolo)}</strong>
                        <span class="brano-meta">
                            ${escapeHtml(titolo_album)}
                            ${interprete ? ' - ' + escapeHtml(interprete) : ''}
                            ${anno_pubblicazione ? ' (' + anno_pubblicazione + ')' : ''}
                        </span>
                    </span>
                </label>
            `;

            contenitore.appendChild(divBrano);
        });
    }

    // GESTIONE AGGIUNTA BRANI

    function gestisciAggiuntaBrani(e) {
        e.preventDefault();

        const form = e.target;
        const checkboxes = form.querySelectorAll('input[name="braniSelezionati_id"]:checked');

        if (checkboxes.length === 0) {
            mostraMessaggio('⚠️ Seleziona almeno un brano', 'error', 'playlistMessageContainer');
            return;
        }

        console.log('Aggiunta di', checkboxes.length, 'brani');

        makeCall(
            'POST',
            'AggiungiBranoAPlaylist',
            form,
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;

                if (req.status === 200) {
                    mostraMessaggio('✅ Brani aggiunti con successo!', 'success', 'playlistMessageContainer');
                    setTimeout(() => {
                        offsetCorrente = 0;
                        caricaDatiPlaylist(playlistCorrente.id_playlist);
                    }, 1000);
                } else {
                    mostraMessaggio('❌ Errore aggiunta brani', 'error', 'playlistMessageContainer');
                }
            },
            false
        );
    }

    // GESTIONE LOGOUT

    function gestisciLogout() {
        if (!confirm('Sei sicuro di voler uscire?')) return;

        makeCall(
            'GET',
            'Logout',
            null,
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;
                window.location.href = 'index.html';
            },
            false
        );
    }

    // MODALE PLAYER

    function apriModalePlayer(idBrano) {
        console.log('Apri modale player per brano:', idBrano, 'tipo:', typeof idBrano);

        const idBranoNum = parseInt(idBrano);
        console.log('ID convertito:', idBranoNum);

        console.log('IDs disponibili nella playlist:', braniPlaylistCorrente.map(b => b.id_brano));

        const brano = braniPlaylistCorrente.find(b => {
            console.log(`Confronto: ${b.id_brano} (${typeof b.id_brano}) == ${idBranoNum} (${typeof idBranoNum}) = ${b.id_brano == idBranoNum}`);
            return b.id_brano == idBranoNum;
        });

        if (!brano) {
            console.error('Brano non trovato - ID cercato:', idBranoNum);
            console.log('Brani disponibili:', braniPlaylistCorrente.map(b => ({id: b.id_brano, titolo: b.titolo})));
            alert('Errore: Brano non trovato nella playlist corrente');
            return;
        }

        const album = albumsMap[brano.id_album];

        apriModale('modalPlayer');

        document.getElementById('playerBranoTitolo').textContent = '🎵 ' + brano.titolo;
        document.getElementById('playerInfoTitolo').textContent = brano.titolo;
        document.getElementById('playerInfoAlbum').textContent = album ? album.titolo_album : 'N/A';
        document.getElementById('playerInfoInterprete').textContent = album ? album.interprete : 'N/A';
        document.getElementById('playerInfoAnno').textContent = album ? album.anno_pubblicazione : 'N/A';
        document.getElementById('playerInfoGenere').textContent = album ? album.genere : 'N/A';

        const img = document.getElementById('playerAlbumCover');
        if (img) {
            img.src = `GetAlbum?brano_id=${brano.id_brano}`;
            img.alt = album ? album.titolo_album : 'Copertina album';
        }

        const audioPlayer = document.getElementById('audioPlayer');
        if (audioPlayer) {
            audioPlayer.src = `GetAudio?brano_id=${brano.id_brano}`;
            audioPlayer.load();
        }

        console.log('✅ Modale player aperto per:', brano.titolo);
    }

    function chiudiModalePlayer() {
        const audioPlayer = document.getElementById('audioPlayer');
        if (audioPlayer) {
            audioPlayer.pause();
            audioPlayer.src = '';
        }

        chiudiModale('modalPlayer');
    }

    // MODALE RIORDINO BRANI

    let braniOrdinati = [];
    let elementoDragato = null;

    function apriModaleRiordino() {
        console.log('Apri modale riordino');

        if (!playlistCorrente || braniPlaylistCorrente.length === 0) {
            mostraMessaggio('⚠️ Nessun brano nella playlist', 'error', 'playlistMessageContainer');
            return;
        }

        braniOrdinati = [...braniPlaylistCorrente];

        apriModale('modalRiordino');

        document.getElementById('riordinoPlaylistTitolo').textContent = '🔄 Riordina: ' + playlistCorrente.titolo;

        renderizzaListaRiordino();
    }

    function renderizzaListaRiordino() {
        const lista = document.getElementById('listaBraniRiordino');
        if (!lista) return;

        lista.innerHTML = '';

        braniOrdinati.forEach((brano, index) => {
            const li = document.createElement('li');
            li.className = 'brano-reorder-item';
            li.draggable = true;
            li.dataset.id_brano = brano.id_brano;
            li.dataset.index = index;

            const album = albumsMap[brano.id_album];
            const titolo_album = album ? album.titolo_album : 'Album sconosciuto';
            const interprete = album ? album.interprete : '';
            const anno_pubblicazione = album ? album.anno_pubblicazione : '';

            li.innerHTML = `
                <div class="brano-reorder-info">
                    <div class="brano-reorder-title">${escapeHtml(brano.titolo)}</div>
                    <div class="brano-reorder-meta">
                        ${escapeHtml(titolo_album)}${interprete ? ' - ' + escapeHtml(interprete) : ''}${anno_pubblicazione ? ' (' + anno_pubblicazione + ')' : ''}
                    </div>
                </div>
                <span class="drag-handle">⋮⋮</span>
            `;

            li.addEventListener('dragstart', handleDragStart);
            li.addEventListener('dragend', handleDragEnd);
            li.addEventListener('dragover', handleDragOver);
            li.addEventListener('drop', handleDrop);
            li.addEventListener('dragenter', handleDragEnter);
            li.addEventListener('dragleave', handleDragLeave);

            lista.appendChild(li);
        });
    }

    function handleDragStart(e) {
        elementoDragato = this;
        this.classList.add('dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/html', this.innerHTML);
    }

    function handleDragEnd(e) {
        this.classList.remove('dragging');

        document.querySelectorAll('.brano-reorder-item').forEach(item => {
            item.classList.remove('drag-over');
        });
    }

    function handleDragOver(e) {
        if (e.preventDefault) {
            e.preventDefault();
        }
        e.dataTransfer.dropEffect = 'move';
        return false;
    }

    function handleDragEnter(e) {
        if (this !== elementoDragato) {
            this.classList.add('drag-over');
        }
    }

    function handleDragLeave(e) {
        this.classList.remove('drag-over');
    }

    function handleDrop(e) {
        if (e.stopPropagation) {
            e.stopPropagation();
        }

        if (elementoDragato !== this) {
            const daIndex = parseInt(elementoDragato.dataset.index);
            const aIndex = parseInt(this.dataset.index);

            const [elementoSpostato] = braniOrdinati.splice(daIndex, 1);
            braniOrdinati.splice(aIndex, 0, elementoSpostato);

            renderizzaListaRiordino();
        }

        return false;
    }

    function salvaOrdinamento() {
        console.log('Salva ordinamento');

        if (!playlistCorrente || braniOrdinati.length === 0) {
            mostraMessaggio('Nessun brano da riordinare', 'error', 'riordinoMessageContainer');
            return;
        }

        const idBraniOrdinati = braniOrdinati.map(b => b.id_brano);

        const dati = {
            id_playlist: playlistCorrente.id_playlist,
            brani_ordinati: idBraniOrdinati
        };

        console.log('Invio ordinamento:', dati);

        const xhr = new XMLHttpRequest();
        xhr.onreadystatechange = function() {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;

            console.log('Risposta salvataggio ordinamento - Status:', xhr.status);

            if (xhr.status === 200) {
                let risposta;
                try {
                    risposta = JSON.parse(xhr.responseText);
                } catch (e) {
                    console.error('Errore parsing risposta:', e);
                    mostraMessaggio('✅ Ordinamento salvato!', 'success', 'riordinoMessageContainer');

                    setTimeout(() => {
                        chiudiModale('modalRiordino');
                        caricaDatiPlaylist(playlistCorrente.id_playlist);
                    }, 1000);
                    return;
                }

                if (risposta.success) {
                    mostraMessaggio('Ordinamento salvato con successo!', 'success', 'riordinoMessageContainer');

                    setTimeout(() => {
                        chiudiModale('modalRiordino');
                        caricaDatiPlaylist(playlistCorrente.id_playlist);
                    }, 1000);
                } else {
                    mostraMessaggio((risposta.error || 'Errore nel salvataggio'), 'error', 'riordinoMessageContainer');
                }
            } else {
                mostraMessaggio('Errore nel salvataggio (HTTP ' + xhr.status + ')', 'error', 'riordinoMessageContainer');
            }
        };

        xhr.open('POST', 'SalvaOrdinamento');
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JSON.stringify(dati));
    }

})();