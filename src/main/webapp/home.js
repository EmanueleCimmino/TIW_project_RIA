/**
 * home.js - Gestione HOME PAGE RIA PoliMusic
 * APPROCCIO PROFESSORE: Separazione load/show/update
 * MODIFICA: Gestione ordinamento personalizzato
 */

(function() {
    'use strict';

    // ========================================
    // DATI APPLICAZIONE
    // ========================================
    let datiUtente = null;
    let tuttiBrani = [];
    let tuttiAlbum = [];
    let tuttePlaylist = [];

    // Dati playlist corrente (modale)
    let playlistCorrente = null;
    let braniPlaylistCorrente = [];
    let braniDisponibili = [];
    let albumsMap = {};
    let offsetCorrente = 0;
    const BRANI_PER_PAGINA = 5;

    // ========================================
    // INIZIALIZZAZIONE
    // ========================================
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

    // ========================================
    // EVENT LISTENERS
    // ========================================
    function impostaEventListener() {
        // Logout
        document.getElementById('logoutBtn')?.addEventListener('click', gestisciLogout);

        // Form creazione playlist
        document.getElementById('createPlaylistForm')?.addEventListener('submit', gestisciCreazionePlaylist);

        // Form upload brano
        document.getElementById('uploadSongForm')?.addEventListener('submit', gestisciCaricamentoBrano);

        // Chiusura modale playlist
        document.getElementById('btnChiudiModalPlaylist')?.addEventListener('click', chiudiModalePlaylist);

        // Chiusura modale cliccando sull'overlay
        const modalPlaylist = document.getElementById('modalPlaylist');
        if (modalPlaylist) {
            modalPlaylist.querySelector('.modal-overlay')?.addEventListener('click', chiudiModalePlaylist);
        }

        // Paginazione playlist (nel modale)
        document.getElementById('btnPrecedenti')?.addEventListener('click', mostraBraniPrecedenti);
        document.getElementById('btnSuccessivi')?.addEventListener('click', mostraBraniSuccessivi);

        // Form aggiungi brani a playlist
        document.getElementById('addSongsForm')?.addEventListener('submit', gestisciAggiuntaBrani);

        // Bottone riordina brani
        const btnRiordina = document.getElementById('btnRiordinaBrani');
        if (btnRiordina) {
            btnRiordina.addEventListener('click', apriModaleRiordino);
        }

        // Chiusura modale riordino
        document.getElementById('btnChiudiModalRiordino')?.addEventListener('click', chiudiModaleRiordino);
        document.getElementById('btnAnnullaRiordino')?.addEventListener('click', chiudiModaleRiordino);

        // Chiusura modale riordino cliccando sull'overlay
        const modalRiordino = document.getElementById('modalRiordino');
        if (modalRiordino) {
            modalRiordino.querySelector('.modal-overlay')?.addEventListener('click', chiudiModaleRiordino);
        }

        // Salva ordinamento
        document.getElementById('btnSalvaOrdinamento')?.addEventListener('click', salvaOrdinamento);

        // Chiusura modale player
        document.getElementById('btnChiudiModalPlayer')?.addEventListener('click', chiudiModalePlayer);

        // Chiusura modale player cliccando sull'overlay
        const modalPlayer = document.getElementById('modalPlayer');
        if (modalPlayer) {
            modalPlayer.querySelector('.modal-overlay')?.addEventListener('click', chiudiModalePlayer);
        }
    }

    // ========================================
    // CARICAMENTO DATI HOME
    // ========================================
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

    // ========================================
    // SHOW: MOSTRA HOME PAGE
    // ========================================
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

    // ========================================
    // SHOW: LISTA PLAYLIST
    // ========================================
    function mostraListaPlaylist(playlist) {
        const contenitore = document.getElementById('playlistsContainer');
        if (!contenitore) return;

        contenitore.innerHTML = '';

        if (!playlist || playlist.length === 0) {
            contenitore.innerHTML = '<p class="empty-message">📭 Non hai ancora creato playlist. Crea la tua prima playlist!</p>';
            return;
        }

        console.log(`Mostra ${playlist.length} playlist`);

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

    // ========================================
    // SHOW: SELECT ALBUM
    // ========================================
    function mostraSelectAlbum(album) {
        const select = document.getElementById('albumSelect');
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

    // ========================================
    // SHOW: BRANI PER CREAZIONE PLAYLIST
    // ========================================
    function mostraBraniPerPlaylist(brani) {
        const contenitore = document.getElementById('braniSelectionContainer');
        const btnSubmit = document.getElementById('createPlaylistBtn');

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

    // ========================================
    // GESTIONE CREAZIONE PLAYLIST
    // ========================================
    function gestisciCreazionePlaylist(e) {
        e.preventDefault();

        const form = e.target;
        const titolo = document.getElementById('playlistTitle').value.trim();
        const checkboxes = form.querySelectorAll('input[name="braniSelezionati"]:checked');

        if (!titolo) {
            mostraMessaggio('⚠️ Inserisci un titolo per la playlist', 'error');
            return;
        }

        if (titolo.length > 50) {
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
            true // Reset form
        );
    }

    // ========================================
    // GESTIONE UPLOAD BRANO
    // ========================================
    function gestisciCaricamentoBrano(e) {
        e.preventDefault();

        const form = e.target;
        const titolo = document.getElementById('songTitle').value.trim();
        const idAlbum = document.getElementById('albumSelect').value;
        const inputFile = document.getElementById('songFile');

        if (!titolo || !idAlbum || !inputFile.files[0]) {
            mostraMessaggio('⚠️ Compila tutti i campi obbligatori', 'error');
            return;
        }

        const file = inputFile.files[0];
        const tipiConsentiti = ['audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/ogg'];

        if (!tipiConsentiti.some(tipo => file.type.includes(tipo.split('/')[1]))) {
            mostraMessaggio('⚠️ Formato non valido. Usa MP3, WAV o OGG', 'error');
            return;
        }

        console.log('Upload brano:', titolo);

        const formData = new FormData(form);
        const xhr = new XMLHttpRequest();

        xhr.onreadystatechange = function() {
            if (xhr.readyState !== XMLHttpRequest.DONE) return;

            if (xhr.status === 200) {
                mostraMessaggio('✅ Brano "' + titolo + '" caricato con successo!', 'success');
                form.reset();
                setTimeout(() => caricaDatiHome(), 1000);
            } else {
                mostraMessaggio('❌ Errore upload (HTTP ' + xhr.status + ')', 'error');
            }
        };

        xhr.open('POST', 'caricaBrano');
        xhr.send(formData);
    }

    // ========================================
    // MODALE PLAYLIST: APRI
    // ========================================
    function apriModalePlaylist(idPlaylist) {
        console.log('Apri modale playlist:', idPlaylist);

        const modale = document.getElementById('modalPlaylist');
        if (!modale) return;

        // Mostra modale
        modale.classList.remove('hidden');
        modale.style.display = 'flex';

        // Reset stato
        offsetCorrente = 0;
        playlistCorrente = null;
        braniPlaylistCorrente = [];
        braniDisponibili = [];
        albumsMap = {};

        // Carica dati playlist
        caricaDatiPlaylist(idPlaylist);
    }

    // ========================================
    // MODALE PLAYLIST: CHIUDI
    // ========================================
    function chiudiModalePlaylist() {
        console.log('Chiudi modale playlist');

        const modale = document.getElementById('modalPlaylist');
        if (!modale) return;

        modale.classList.add('hidden');
        modale.style.display = 'none';

        // Reset stato
        offsetCorrente = 0;
        playlistCorrente = null;
        braniPlaylistCorrente = [];
        braniDisponibili = [];
        albumsMap = {};
    }

    // ========================================
    // LOAD: DATI PLAYLIST
    // ========================================
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
                        mostraMessaggioPlaylist('Errore nel formato dei dati', 'error');
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

                        // *** MODIFICA: Controlla se esiste ordinamento personalizzato ***
                        applicaOrdinamentoSeNecessario();

                        offsetCorrente = 0;

                        console.log(`Playlist caricata: ${braniPlaylistCorrente.length} brani`);

                        // SHOW: Mostra playlist
                        mostraPlaylist();

                    } else {
                        mostraMessaggioPlaylist(risposta.error || 'Errore caricamento', 'error');
                    }
                } else if (req.status === 401) {
                    mostraMessaggioPlaylist('Sessione scaduta', 'error');
                    setTimeout(() => window.location.href = 'index.html', 2000);
                } else {
                    mostraMessaggioPlaylist('Errore (HTTP ' + req.status + ')', 'error');
                }
            },
            false
        );
    }

    // ========================================
    // NUOVO: APPLICA ORDINAMENTO PERSONALIZZATO
    // ========================================
    function applicaOrdinamentoSeNecessario() {
        // DEBUG: Mostra i dati ricevuti
        console.log('=== DEBUG ORDINAMENTO ===');
        console.log('Numero brani:', braniPlaylistCorrente.length);

        if (braniPlaylistCorrente.length > 0) {
            console.log('Esempio primo brano:', braniPlaylistCorrente[0]);
            console.log('Campi disponibili:', Object.keys(braniPlaylistCorrente[0]));
        }

        // Mostra tutti i valori di ordinamento
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
            // Nessun ordinamento personalizzato, lascia l'ordine originale
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

    // ========================================
    // SHOW: MOSTRA PLAYLIST NEL MODALE
    // ========================================
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

    // ========================================
    // SHOW: TABELLA BRANI (5 per riga)
    // ========================================
    function mostraTabellaPlaylist() {
        const tbody = document.getElementById('songsTableBody');
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
                         class="song-image"
                         onerror="this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 width=%22150%22 height=%22150%22%3E%3Crect fill=%22%23ddd%22 width=%22150%22 height=%22150%22/%3E%3Ctext fill=%22%23999%22 x=%2250%25%22 y=%2250%25%22 dominant-baseline=%22middle%22 text-anchor=%22middle%22 font-size=%2214%22%3ENo Image%3C/text%3E%3C/svg%3E'">
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

    // ========================================
    // UPDATE: CONTROLLI PAGINAZIONE
    // Gestione CLIENT-SIDE (nessuna chiamata server)
    // ========================================
    function aggiornaControlliPaginazione() {
        const btnPrecedenti = document.getElementById('btnPrecedenti');
        const btnSuccessivi = document.getElementById('btnSuccessivi');
        const infoEl = document.getElementById('paginationInfo');

        const totaleBrani = braniPlaylistCorrente.length;
        const paginaCorrente = Math.floor(offsetCorrente / BRANI_PER_PAGINA) + 1;
        const totalePagine = Math.ceil(totaleBrani / BRANI_PER_PAGINA);

        const ciSonoPrecedenti = offsetCorrente > 0;
        const ciSonoSuccessivi = (offsetCorrente + BRANI_PER_PAGINA) < totaleBrani;

        console.log(`Paginazione: pagina ${paginaCorrente}/${totalePagine}, precedenti=${ciSonoPrecedenti}, successivi=${ciSonoSuccessivi}`);

        // Mostra/nascondi bottoni secondo le specifiche del progetto
        if (ciSonoPrecedenti) {
            btnPrecedenti.classList.remove('hidden');
        } else {
            btnPrecedenti.classList.add('hidden');
        }

        if (ciSonoSuccessivi) {
            btnSuccessivi.classList.remove('hidden');
        } else {
            btnSuccessivi.classList.add('hidden');
        }

        // Info paginazione
        if (totaleBrani > 0 && totalePagine > 1) {
            infoEl.textContent = `Pagina ${paginaCorrente} di ${totalePagine} (${totaleBrani} brani totali)`;
        } else if (totaleBrani > 0) {
            infoEl.textContent = `${totaleBrani} brani`;
        } else {
            infoEl.textContent = '';
        }
    }

    // ========================================
    // PAGINAZIONE: PRECEDENTI (CLIENT-SIDE)
    // ========================================
    function mostraBraniPrecedenti() {
        offsetCorrente = Math.max(0, offsetCorrente - BRANI_PER_PAGINA);
        console.log('Vai a brani precedenti - nuovo offset:', offsetCorrente);
        // UPDATE: Ricarica solo la tabella, senza fetch
        mostraTabellaPlaylist();
    }

    // ========================================
    // PAGINAZIONE: SUCCESSIVI (CLIENT-SIDE)
    // ========================================
    function mostraBraniSuccessivi() {
        const nuovoOffset = offsetCorrente + BRANI_PER_PAGINA;
        if (nuovoOffset < braniPlaylistCorrente.length) {
            offsetCorrente = nuovoOffset;
            console.log('Vai a brani successivi - nuovo offset:', offsetCorrente);
            // UPDATE: Ricarica solo la tabella, senza fetch
            mostraTabellaPlaylist();
        }
    }

    // ========================================
    // SHOW: BRANI DISPONIBILI DA AGGIUNGERE
    // ========================================
    function mostraBraniDisponibili() {
        const contenitore = document.getElementById('braniDisponibiliContainer');
        const btnSubmit = document.getElementById('addSongsBtn');

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
        hiddenInput.name = 'playlist_id';
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
            contenitore.querySelectorAll('input[name="selectedSongs"]').forEach(cb => {
                cb.checked = e.target.checked;
            });
        });

        // Renderizza brani
        braniDisponibili.forEach(brano => {
            const divBrano = document.createElement('div');
            divBrano.className = 'brano-item';

            const album = albumsMap[brano.id_album];
            const nomeAlbum = album ? album.titolo_album : 'Album sconosciuto';
            const interprete = album ? album.interprete : '';
            const anno = album ? album.anno_pubblicazione : '';

            divBrano.innerHTML = `
                <label>
                    <input type="checkbox" name="selectedSongs" value="${brano.id_brano}">
                    <span class="brano-info">
                        <strong>${escapeHtml(brano.titolo)}</strong>
                        <span class="brano-meta">
                            ${escapeHtml(nomeAlbum)}
                            ${interprete ? ' - ' + escapeHtml(interprete) : ''}
                            ${anno ? ' (' + anno + ')' : ''}
                        </span>
                    </span>
                </label>
            `;

            contenitore.appendChild(divBrano);
        });
    }

    // ========================================
    // GESTIONE AGGIUNTA BRANI
    // ========================================
    function gestisciAggiuntaBrani(e) {
        e.preventDefault();

        const form = e.target;
        const checkboxes = form.querySelectorAll('input[name="selectedSongs"]:checked');

        if (checkboxes.length === 0) {
            mostraMessaggioPlaylist('⚠️ Seleziona almeno un brano', 'error');
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
                    mostraMessaggioPlaylist('✅ Brani aggiunti con successo!', 'success');
                    // Ricarica playlist e torna al primo blocco
                    setTimeout(() => {
                        offsetCorrente = 0;
                        caricaDatiPlaylist(playlistCorrente.id_playlist);
                    }, 1000);
                } else {
                    mostraMessaggioPlaylist('❌ Errore aggiunta brani', 'error');
                }
            },
            false
        );
    }

    // ========================================
    // GESTIONE LOGOUT
    // ========================================
    function gestisciLogout() {
        if (!confirm('Sei sicuro di voler uscire?')) return;

        makeCall(
            'GET',
            'Logout',
            null,
            (req) => {
                if (req.readyState !== XMLHttpRequest.DONE) return;
                // In ogni caso, redirect al login
                window.location.href = 'index.html';
            },
            false
        );
    }

    // ========================================
    // UTILITY: MESSAGGI
    // ========================================
    function mostraMessaggio(messaggio, tipo = 'info') {
        const contenitore = document.getElementById('messageContainer');
        if (!contenitore) {
            console.warn('Contenitore messaggi non trovato');
            alert(messaggio);
            return;
        }

        contenitore.textContent = messaggio;
        contenitore.className = `message message-${tipo}`;
        contenitore.style.display = 'block';

        window.scrollTo({ top: 0, behavior: 'smooth' });

        setTimeout(() => {
            contenitore.style.display = 'none';
        }, 5000);
    }

    function mostraMessaggioPlaylist(messaggio, tipo = 'info') {
        const contenitore = document.getElementById('playlistMessageContainer');
        if (!contenitore) {
            console.warn('Contenitore messaggi playlist non trovato');
            alert(messaggio);
            return;
        }

        contenitore.textContent = messaggio;
        contenitore.className = `message message-${tipo}`;
        contenitore.style.display = 'block';

        // Scroll al top del modale
        const modalBody = document.querySelector('.modal-body-scroll');
        if (modalBody) {
            modalBody.scrollTo({ top: 0, behavior: 'smooth' });
        }

        setTimeout(() => {
            contenitore.style.display = 'none';
        }, 5000);
    }

    // ========================================
    // UTILITY: FORMATTAZIONE
    // ========================================
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

    // ========================================
    // MODALE PLAYER
    // ========================================

    /**
     * Apre il modale player per un brano
     */
    function apriModalePlayer(idBrano) {
        console.log('Apri modale player per brano:', idBrano);

        // Trova il brano nei dati correnti
        const brano = braniPlaylistCorrente.find(b => b.id_brano == idBrano);
        if (!brano) {
            console.error('Brano non trovato');
            return;
        }

        const album = albumsMap[brano.id_album];

        // Apri modale
        const modale = document.getElementById('modalPlayer');
        if (!modale) return;

        modale.classList.remove('hidden');
        modale.style.display = 'flex';

        // Popola dati
        document.getElementById('playerBranoTitolo').textContent = '🎵 ' + brano.titolo;
        document.getElementById('playerInfoTitolo').textContent = brano.titolo;
        document.getElementById('playerInfoAlbum').textContent = album ? album.titolo_album : 'N/A';
        document.getElementById('playerInfoInterprete').textContent = album ? album.interprete : 'N/A';
        document.getElementById('playerInfoAnno').textContent = album ? album.anno_pubblicazione : 'N/A';
        document.getElementById('playerInfoGenere').textContent = album ? album.genere : 'N/A';

        // Immagine album
        const img = document.getElementById('playerAlbumCover');
        if (img) {
            img.src = `GetAlbum?brano_id=${brano.id_brano}`;
            img.alt = album ? album.titolo_album : 'Copertina album';
        }

        // Audio player
        const audioPlayer = document.getElementById('audioPlayer');
        if (audioPlayer) {
            audioPlayer.src = `GetAudio?brano_id=${brano.id_brano}`;
            audioPlayer.load();
        }
    }

    /**
     * Chiude il modale player
     */
    function chiudiModalePlayer() {
        const modale = document.getElementById('modalPlayer');
        if (!modale) return;

        // Ferma l'audio
        const audioPlayer = document.getElementById('audioPlayer');
        if (audioPlayer) {
            audioPlayer.pause();
            audioPlayer.src = '';
        }

        modale.classList.add('hidden');
        modale.style.display = 'none';
    }

    // ========================================
    // MODALE RIORDINO BRANI
    // ========================================

    let braniOrdinati = []; // Array temporaneo per il drag & drop
    let elementoDragato = null;

    /**
     * Apre il modale riordino brani
     */
    function apriModaleRiordino() {
        console.log('Apri modale riordino');

        if (!playlistCorrente || braniPlaylistCorrente.length === 0) {
            mostraMessaggioPlaylist('⚠️ Nessun brano nella playlist', 'error');
            return;
        }

        // Copia array brani (per non modificare l'originale durante il drag)
        braniOrdinati = [...braniPlaylistCorrente];

        // Apri modale
        const modale = document.getElementById('modalRiordino');
        if (!modale) return;

        modale.classList.remove('hidden');
        modale.style.display = 'flex';

        // Popola titolo
        document.getElementById('riordinoPlaylistTitolo').textContent = '🔄 Riordina: ' + playlistCorrente.titolo;

        // Renderizza lista brani
        renderizzaListaRiordino();
    }

    /**
     * Renderizza la lista di brani per il riordino
     */
    function renderizzaListaRiordino() {
        const lista = document.getElementById('listaBraniRiordino');
        if (!lista) return;

        lista.innerHTML = '';

        braniOrdinati.forEach((brano, index) => {
            const li = document.createElement('li');
            li.className = 'brano-reorder-item';
            li.draggable = true;
            li.dataset.branoId = brano.id_brano;
            li.dataset.index = index;

            const album = albumsMap[brano.id_album];
            const nomeAlbum = album ? album.titolo_album : 'Album sconosciuto';
            const interprete = album ? album.interprete : '';
            const anno = album ? album.anno_pubblicazione : '';

            li.innerHTML = `
                <div class="brano-reorder-info">
                    <div class="brano-reorder-title">${escapeHtml(brano.titolo)}</div>
                    <div class="brano-reorder-meta">
                        ${escapeHtml(nomeAlbum)}${interprete ? ' - ' + escapeHtml(interprete) : ''}${anno ? ' (' + anno + ')' : ''}
                    </div>
                </div>
                <span class="drag-handle">⋮⋮</span>
            `;

            // Event listeners per drag & drop
            li.addEventListener('dragstart', handleDragStart);
            li.addEventListener('dragend', handleDragEnd);
            li.addEventListener('dragover', handleDragOver);
            li.addEventListener('drop', handleDrop);
            li.addEventListener('dragenter', handleDragEnter);
            li.addEventListener('dragleave', handleDragLeave);

            lista.appendChild(li);
        });
    }

    /**
     * Handlers per drag & drop
     */
    function handleDragStart(e) {
        elementoDragato = this;
        this.classList.add('dragging');
        e.dataTransfer.effectAllowed = 'move';
        e.dataTransfer.setData('text/html', this.innerHTML);
    }

    function handleDragEnd(e) {
        this.classList.remove('dragging');

        // Rimuovi evidenziazione da tutti gli elementi
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
            // Ottieni indici
            const daIndex = parseInt(elementoDragato.dataset.index);
            const aIndex = parseInt(this.dataset.index);

            // Riordina array
            const [elementoSpostato] = braniOrdinati.splice(daIndex, 1);
            braniOrdinati.splice(aIndex, 0, elementoSpostato);

            // Ri-renderizza lista
            renderizzaListaRiordino();
        }

        return false;
    }

    /**
     * Salva il nuovo ordinamento sul server
     */
    function salvaOrdinamento() {
        console.log('Salva ordinamento');

        if (!playlistCorrente || braniOrdinati.length === 0) {
            mostraMessaggioRiordino('⚠️ Nessun brano da riordinare', 'error');
            return;
        }

        // Crea array di ID nell'ordine corrente
        const idBraniOrdinati = braniOrdinati.map(b => b.id_brano);

        const dati = {
            playlist_id: playlistCorrente.id_playlist,
            brani_ordinati: idBraniOrdinati
        };

        console.log('Invio ordinamento:', dati);

        // Invia al server
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
                    mostraMessaggioRiordino('✅ Ordinamento salvato!', 'success');

                    // Chiudi modale e ricarica playlist
                    setTimeout(() => {
                        chiudiModaleRiordino();
                        caricaDatiPlaylist(playlistCorrente.id_playlist);
                    }, 1000);
                    return;
                }

                if (risposta.success) {
                    mostraMessaggioRiordino('✅ Ordinamento salvato con successo!', 'success');

                    // Chiudi modale e ricarica playlist
                    setTimeout(() => {
                        chiudiModaleRiordino();
                        caricaDatiPlaylist(playlistCorrente.id_playlist);
                    }, 1000);
                } else {
                    mostraMessaggioRiordino('❌ ' + (risposta.error || 'Errore nel salvataggio'), 'error');
                }
            } else {
                mostraMessaggioRiordino('❌ Errore nel salvataggio (HTTP ' + xhr.status + ')', 'error');
            }
        };

        xhr.open('POST', 'SalvaOrdinamento');
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.send(JSON.stringify(dati));
    }

    /**
     * Chiude il modale riordino
     */
    function chiudiModaleRiordino() {
        const modale = document.getElementById('modalRiordino');
        if (!modale) return;

        modale.classList.add('hidden');
        modale.style.display = 'none';

        // Reset dati
        braniOrdinati = [];
        elementoDragato = null;
    }

    /**
     * Mostra un messaggio nel modale riordino
     */
    function mostraMessaggioRiordino(messaggio, tipo = 'info') {
        const contenitore = document.getElementById('riordinoMessageContainer');
        if (!contenitore) {
            console.warn('Contenitore messaggi riordino non trovato');
            alert(messaggio);
            return;
        }

        contenitore.textContent = messaggio;
        contenitore.className = `message message-${tipo}`;
        contenitore.style.display = 'block';

        // Scroll al top del modale
        const modalBody = document.querySelector('#modalRiordino .modal-body-scroll');
        if (modalBody) {
            modalBody.scrollTo({ top: 0, behavior: 'smooth' });
        }

        setTimeout(() => {
            contenitore.style.display = 'none';
        }, 5000);
    }

})();