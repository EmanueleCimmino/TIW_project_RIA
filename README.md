# 🎵 Playlist Musicale - Versione RIA (Rich Internet Application)

> **Gestione personale di playlist e brani musicali in una single-page application**
> Progetto d'esame - Tecnologie Informatiche per il Web - AA 2024-25

---

## 📋 Indice

1. [Panoramica del Progetto](#-panoramica-del-progetto)
2. [Funzionalità](#-funzionalità)
3. [Architettura](#️-architettura)
4. [Database](#️-database)
5. [Struttura del Progetto](#-struttura-del-progetto)
6. [Interfaccia dell'Applicazione](#-interfaccia-dellapplicazione)
7. [API del Server](#-api-del-server)
8. [Installazione](#️-installazione)
9. [Aspetti di Sicurezza](#-aspetti-di-sicurezza)

---

## 🎯 Panoramica del Progetto

Applicazione web **single-page** per la gestione di una collezione personale di brani musicali organizzati in playlist. Dopo il login l'intera applicazione vive in **un'unica pagina**: ogni interazione dell'utente produce una **chiamata asincrona (AJAX)** al server e aggiorna solo la porzione di interfaccia interessata, senza ricaricare la pagina.

Rispetto alla versione [Pure HTML](https://github.com/EmanueleCimmino/TIW_project_html), la logica di presentazione e di navigazione si sposta lato client in JavaScript, mentre il server espone servlet che restituiscono dati in formato **JSON**.

### Caratteristiche Principali

- ✅ **Single-page application**: nessun reload dopo il login
- ✅ **Comunicazione asincrona** client-server (AJAX) con risposte JSON
- ✅ **Autenticazione utente** con sessioni server-side
- ✅ **Caricamento brani** con metadati completi e file audio/immagine
- ✅ **Creazione playlist** con selezione multipla di brani
- ✅ **Navigazione paginata lato client** (5 brani per pagina)
- ✅ **Player audio in finestra modale** (HTML5)
- ✅ **Ordinamento personalizzato con drag and drop** dei brani di una playlist
- ✅ **Privacy completa**: brani e playlist personali e non condivisi

### Tecnologie Utilizzate

- **Backend**: Java Servlets + JDBC
- **Frontend**: HTML5 + CSS + **JavaScript (vanilla)**, AJAX
- **Formato di scambio dati**: JSON
- **Database**: MariaDB
- **Server**: Apache Tomcat 10.1.49
- **Architettura**: Client-server con logica di vista lato client (RIA)

<!-- VERIFICA: confermare libreria JSON usata (Gson?), uso di fetch o XMLHttpRequest, eventuale uso di Thymeleaf per la sola pagina di login -->

---

## 🎯 Funzionalità

### Gestione Utenti

- Login e logout con sessioni server-side
- Registrazione di un nuovo utente con controlli lato client e lato server
- Ogni utente ha la propria collezione privata

### Gestione Brani

- Caricamento brani con metadati completi (titolo, album, interprete, anno, genere)
- Upload file audio e immagine copertina (multipart)
- Validazione input lato client e server

### Gestione Playlist

- Creazione playlist con selezione multipla di brani
- Visualizzazione playlist ordinate per data di creazione
- Aggiunta brani a playlist esistenti
- Navigazione a gruppi di 5 brani gestita interamente lato client
- Ordinamento di default per interprete e anno di pubblicazione
- **Ordinamento personalizzato** tramite drag and drop, salvato in modo persistente

### Riproduzione

- Player audio HTML5 in **finestra modale**
- Visualizzazione metadati completi del brano
- Copertina album

### Gestione degli Errori

- Gli errori restituiti dal server (o di rete) vengono mostrati come messaggio all'interno della pagina, senza interrompere la sessione di lavoro

---

## 🏗️ Architettura

L'applicazione adotta un'architettura **client-server** in cui il client è una vera e propria applicazione JavaScript.

### Livelli dell'Applicazione

1. **Presentation Layer (client)**: pagina HTML + moduli JavaScript che costruiscono e aggiornano dinamicamente il DOM
2. **Controller Layer (server)**: Servlet Java che ricevono richieste asincrone e rispondono in JSON
3. **Model Layer (server)**: Bean (POJO) e DAO per la logica di business
4. **Data Layer**: Database MariaDB

### Flusso di una richiesta

```
Evento utente → JavaScript (client) → richiesta AJAX → Servlet (Controller)
                      ↑                                       ↓
              aggiornamento DOM  ←  risposta JSON  ←  DAO (Model) → Database
```

### Differenze rispetto alla versione Pure HTML

| Aspetto | Pure HTML | RIA |
|---|---|---|
| Pagine | Una pagina per funzione | Una sola pagina dopo il login |
| Rendering | Server-side (TemplateHandler) | Client-side (DOM via JavaScript) |
| Risposte del server | HTML | JSON |
| Paginazione brani | Richiesta al server a ogni cambio pagina | Gestita lato client |
| Player | Pagina dedicata | Finestra modale |
| Ordinamento playlist | Solo automatico | Automatico + personalizzato (drag and drop) |

---

## 🗄️ Database

### Schema Relazionale

Il database è composto da 5 tabelle principali:

#### **UTENTE**

- `id` (PK)
- `username` (UNIQUE)
- `nome`
- `cognome`
- `password`

#### **ALBUM**

- `id` (PK)
- `titolo`
- `interprete`
- `genere` (ENUM)
- `anno_pubblicazione`
- `immagine` (nome file)

#### **BRANO**

- `id` (PK)
- `titolo`
- `file_musicale` (nome file)
- `id_utente` (FK → UTENTE)
- `id_album` (FK → ALBUM)

#### **PLAYLIST**

- `id` (PK)
- `titolo`
- `data_creazione`
- `id_utente` (FK → UTENTE)

#### **PLAYLIST_BRANO** (Tabella di Join)

- `id_brano` (FK → BRANO)
- `id_playlist` (FK → PLAYLIST)
- `id_utente` (FK → UTENTE)
- `posizione` (ordinamento personalizzato)
- PK: (`id_brano`, `id_playlist`)

<!-- VERIFICA: nome e tipo esatto della colonna usata per l'ordinamento personalizzato (es. posizione / ordine) e se si trova in PLAYLIST_BRANO -->

### Relazioni

- **UTENTE → BRANO**: 1:N (un utente crea molti brani)
- **UTENTE → PLAYLIST**: 1:N (un utente crea molte playlist)
- **ALBUM → BRANO**: 1:N (un album contiene molti brani)
- **BRANO ↔ PLAYLIST**: N:M (gestita tramite PLAYLIST_BRANO)

### Regole di Business

- Ogni brano e playlist appartiene a un solo utente (privacy)
- Un brano può appartenere a un solo album (no compilation)
- Uno stesso brano può essere in più playlist
- Ordinamento di default: interprete (alfabetico) → anno pubblicazione (crescente)
- Se esiste un ordinamento personalizzato, questo ha la precedenza su quello di default; i brani non ancora ordinati manualmente seguono l'ordinamento di default

---

## 📁 Struttura del Progetto

```
TIW_project_RIA/
│
├── src/main/
│   ├── java/
│   │   │
│   │   ├── beans/
│   │   │   ├── Utente.java
│   │   │   ├── Brano.java
│   │   │   ├── Album.java
│   │   │   ├── Playlist.java
│   │   │   └── PlaylistBrano.java
│   │   │
│   │   ├── daos/
│   │   │   ├── UtenteDAO.java
│   │   │   ├── BranoDAO.java
│   │   │   ├── PlaylistDAO.java
│   │   │   └── AlbumDAO.java
│   │   │
│   │   ├── controller/
│   │   │   ├── LoginServlet.java
│   │   │   ├── LogoutServlet.java
│   │   │   ├── RegistrazioneServlet.java
│   │   │   ├── HomeServlet.java
│   │   │   ├── CaricaBranoServlet.java
│   │   │   ├── CreaPlaylistServlet.java
│   │   │   ├── PlaylistServlet.java
│   │   │   ├── AggiungiBranoAPlaylistServlet.java
│   │   │   ├── SalvaOrdinamentoServlet.java
│   │   │   ├── GetAlbumServlet.java
│   │   │   └── GetAudioServlet.java
│   │   │
│   │   └── utils/
│   │       └── ConnectionHandler.java
│   │
│   └── webapp/
│       ├── index.html
│       ├── home.html
│       │
│       ├── css/
│       │   └── style.css
│       │
│       ├── js/
│       │   └── ... (moduli JavaScript)
│       │
│       └── WEB-INF/
│           └── web.xml
```

<!-- VERIFICA: allineare questo albero ai file reali (nomi delle servlet, cartella js/, file HTML presenti, presenza o meno di filtri per il controllo di sessione) -->

---

## 🧩 Componenti Principali

### Beans (Model)

- **Utente**: Rappresenta un utente registrato
- **Album**: Informazioni album musicale
- **Brano**: Singolo brano musicale
- **Playlist**: Collezione di brani
- **PlaylistBrano**: Relazione N:M tra Playlist e Brano

### DAO (Data Access Object)

- **UtenteDAO**: Gestione utenti e autenticazione
- **BranoDAO**: Operazioni CRUD sui brani
- **PlaylistDAO**: Gestione playlist, relazioni con brani e ordinamento personalizzato
- **AlbumDAO**: Gestione album musicali

### Controllers (Servlets)

#### Autenticazione

- **LoginServlet**: Verifica credenziali e crea sessione
- **LogoutServlet**: Termina la sessione
- **RegistrazioneServlet**: Creazione nuovo utente

#### Gestione Contenuti

- **CaricaBranoServlet**: Upload brano con file multipart (salvataggio in directory server)
- **CreaPlaylistServlet**: Creazione nuova playlist
- **AggiungiBranoAPlaylistServlet**: Aggiunta brani a playlist esistente
- **SalvaOrdinamentoServlet**: Salvataggio persistente dell'ordinamento personalizzato

#### Navigazione e Dati

- **HomeServlet**: Restituisce in JSON playlist e brani dell'utente
- **PlaylistServlet**: Restituisce in JSON i brani di una playlist nell'ordine corretto

#### Gestione File Multimediali

- **GetAlbumServlet**: Serve le immagini delle copertine album da directory server
  * Verifica ownership del brano
  * Gestisce MIME type appropriato
  * Protezione contro path traversal
- **GetAudioServlet**: Serve i file audio con supporto HTTP Range Requests
  * Consente streaming progressivo
  * Supporto per seek nel player HTML5
  * Verifica ownership del brano

### Utilities

- **ConnectionHandler**: Gestione connessioni al database

### Client (JavaScript)

Il codice client è organizzato in componenti con responsabilità distinte:

- **Gestione pagina / orchestrazione**: inizializza l'applicazione, mostra e nasconde le sezioni (home, playlist) e coordina i componenti
- **Componenti di vista**: elenco playlist, form di caricamento brano, form di creazione playlist, griglia dei brani con paginazione, finestra modale del player, finestra modale di ordinamento
- **Layer di comunicazione**: funzioni che incapsulano le chiamate AJAX, la gestione degli errori e il parsing del JSON

<!-- VERIFICA: elencare i veri file/oggetti JS (es. PageOrchestrator, PlaylistList, ...) con una riga ciascuno -->

### Gestione File Multimediali

I file (immagini e audio) vengono salvati in **directory prestabilite sul server** e non nella cartella `webapp`:

- **Immagini album**: Directory configurata in `GetAlbumServlet`
- **File audio**: Directory configurata in `GetAudioServlet`

Le servlet dedicate servono i file verificando:

1. Autenticazione utente
2. Ownership del brano
3. Esistenza del file
4. Protezione contro path traversal

Il database memorizza solo i **nomi dei file**, non i path completi.

---

## 📱 Interfaccia dell'Applicazione

L'interfaccia è composta da una pagina di accesso e da **un'unica pagina applicativa** i cui contenuti cambiano dinamicamente.

### Start Page (`index.html`)

- Form di **login** e form di **registrazione**
- Validazione lato client (campi obbligatori, conferma password, formato)
- Messaggi di errore mostrati in pagina
- Dopo il login riuscito: accesso alla pagina applicativa

---

### 🏠 Sezione Home

**Requisiti**: Sessione attiva

#### 1. Lista Playlist
- Visualizzazione di tutte le playlist dell'utente
- Ordinamento per data di creazione decrescente
- Click sul titolo → mostra la sezione playlist **senza ricaricare la pagina**

#### 2. Form Caricamento Brano
Campi richiesti:
- Titolo brano
- Titolo album
- Interprete/Gruppo
- Anno di pubblicazione
- Genere musicale (select con opzioni predefinite)
- Immagine copertina (file upload)
- File audio (file upload)

**Servlet**: `CaricaBranoServlet`

Al termine, l'elenco dei brani viene aggiornato senza reload.

#### 3. Form Creazione Playlist
- Input per titolo playlist
- Lista brani utente con checkbox multipla
- Ordinamento brani: interprete (alfabetico) → anno (crescente)
- La data di creazione viene assegnata automaticamente

**Servlet**: `CreaPlaylistServlet`

La nuova playlist compare subito nell'elenco, senza reload.

---

### 🎧 Sezione Playlist

**Requisiti**: Sessione attiva + Ownership playlist

#### 1. Visualizzazione Brani
- Una riga con **5 brani** alla volta: ogni cella mostra copertina + titolo
- Click sul titolo di un brano → apre il player in finestra modale
- Ordine: personalizzato se presente, altrimenti interprete (alfabetico) → anno (crescente)

#### 2. Navigazione Paginata (lato client)
- Tutti i brani della playlist vengono scaricati una sola volta; il cambio di gruppo avviene **solo lato client**
- Bottone "← PRECEDENTI" (se non si è nel primo gruppo)
- Bottone "SUCCESSIVI →" (se non si è nell'ultimo gruppo)

#### 3. Form Aggiunta Brani
- Lista brani NON presenti nella playlist
- Checkbox per selezione multipla
- Dopo l'aggiunta, la vista viene aggiornata dinamicamente

**Servlet**: `AggiungiBranoAPlaylistServlet`

#### 4. Ordinamento Personalizzato (drag and drop)
- Un link **"Ordinamento"** apre una **finestra modale** con l'elenco completo dei brani della playlist, nell'ordine corrente
- L'utente trascina il titolo di un brano e lo rilascia nella nuova posizione
- Il pulsante **"Salva ordinamento"** invia al server il nuovo ordine, che viene memorizzato in modo persistente
- Alla successiva apertura della playlist (anche in un'altra sessione) i brani appaiono nell'ordine salvato

**Servlet**: `SalvaOrdinamentoServlet`

---

### 🎵 Player (finestra modale)

**Requisiti**: Sessione attiva + Ownership brano

**Contenuti**:
- **Metadati completi**: titolo, album, interprete, anno, genere
- **Immagine copertina** recuperata tramite `GetAlbumServlet`
- **Player audio HTML5** con file servito da `GetAudioServlet`
- Pulsante di chiusura per tornare alla playlist

**Caratteristiche del Player**:
- Supporto **HTTP Range Requests** per streaming progressivo
- Possibilità di **seek** (spostamento temporale) nel brano
- Nessun cambio di pagina: la modale si sovrappone alla sezione playlist

Esempio di utilizzo dei file multimediali:

- **Immagini**: `<img src="GetAlbum?brano_id=123">`
- **Audio**: `<audio src="GetAudio?brano_id=123">`

---

## 🔌 API del Server

Tutte le servlet (eccetto login e registrazione) richiedono una **sessione attiva** e rispondono con codici HTTP significativi; i messaggi d'errore vengono mostrati dal client nella pagina.

| Endpoint | Metodo | Descrizione | Risposta |
|---|---|---|---|
| `/Login` | POST | Autenticazione | JSON / codice HTTP |
| `/Registrazione` | POST | Creazione nuovo utente | JSON / codice HTTP |
| `/Logout` | GET | Termina la sessione | redirect / codice HTTP |
| `/Home` | GET | Playlist e brani dell'utente | JSON |
| `/CaricaBrano` | POST (multipart) | Upload di un brano | JSON / codice HTTP |
| `/CreaPlaylist` | POST | Creazione di una playlist | JSON / codice HTTP |
| `/Playlist?id=<id>` | GET | Brani di una playlist (ordinati) | JSON |
| `/AggiungiBranoAPlaylist` | POST | Aggiunta di brani a una playlist | JSON / codice HTTP |
| `/SalvaOrdinamento` | POST | Salvataggio ordinamento personalizzato | codice HTTP |
| `/GetAlbum?brano_id=<id>` | GET | Immagine copertina | `image/*` |
| `/GetAudio?brano_id=<id>` | GET | File audio (Range supportato) | `audio/*` |

<!-- VERIFICA: sostituire con gli URL pattern reali di web.xml / @WebServlet, i metodi HTTP e i formati di risposta effettivi -->

---

## ⚙️ Installazione

### Prerequisiti

- **Java JDK** 11 o superiore
- **Apache Tomcat** 10.1.49
- **MariaDB** 10.x o superiore
- **IDE** (consigliato): Eclipse EE o IntelliJ IDEA
- Un browser moderno con JavaScript abilitato

### Setup

1. **Database**

```bash
mysql -u root -p
CREATE DATABASE playlist_musicale;
USE playlist_musicale;
SOURCE schema.sql;
```

2. **Directory File Multimediali**

    Crea le directory per i file caricati e configura i path nelle servlet:

  - **GetAlbumServlet**: Modifica `IMAGE_PATH` con il percorso desiderato
  - **GetAudioServlet**: Modifica `AUDIO_PATH` con il percorso desiderato
  - **CaricaBranoServlet**: Configura i path di salvataggio per immagini e audio

    Esempio:

```java
private static final String IMAGE_PATH = "C:\\Users\\username\\Immagini\\";
private static final String AUDIO_PATH = "C:\\Users\\username\\Audio\\";
```

    Assicurati che le directory esistano e abbiano i permessi di scrittura corretti.

3. **Configurazione Database**

  - Modifica i parametri di connessione in `ConnectionHandler.java` o `web.xml`
  - Aggiungi MySQL Connector JAR (e la libreria JSON utilizzata) alle dipendenze

4. **Deploy**

  - Compila il progetto Maven
  - Deploy su Tomcat tramite IDE o manualmente
  - Oppure: esporta come WAR e copia in `tomcat/webapps/`

5. **Accesso**

```
http://localhost:8080/TIW_project_RIA/
```

### Dipendenze

- **mysql-connector-j** (JDBC Driver per MariaDB/MySQL)
- **Jakarta Servlet API** (fornita da Tomcat 10)
- **Libreria JSON per Java** (es. Gson) per la serializzazione delle risposte

<!-- VERIFICA: confermare le dipendenze reali in pom.xml -->

---

## 🔒 Aspetti di Sicurezza

- **Autenticazione**: Gestione sessioni server-side
- **Autorizzazione**: Verifica ownership su playlist e brani, controllata sempre **lato server** (il client non è mai considerato affidabile)
- **Validazione**: Input validati lato client (HTML5/JavaScript) e lato server
- **SQL Injection**: Utilizzo di PreparedStatement
- **XSS Prevention**: I contenuti dinamici vengono inseriti nel DOM come testo (non come HTML grezzo)
- **File Upload**:
  * Validazione MIME type
  * Sanitizzazione nomi file
  * Salvataggio in directory esterne alla webapp
- **Path Traversal Protection**: Verifica che i file richiesti siano all'interno delle directory consentite
- **Ownership Verification**: Solo il proprietario di un brano può accedere ai suoi file audio/immagini
- **Ordinamento personalizzato**: il server verifica che tutti i brani inviati appartengano alla playlist dell'utente prima di salvare l'ordine

---

## 📚 Riferimenti

- [Jakarta Servlet Specification](https://jakarta.ee/specifications/servlet/)
- [MariaDB Documentation](https://mariadb.com/kb/en/)
- [Apache Tomcat 10](https://tomcat.apache.org/tomcat-10.1-doc/)
- [MDN - Drag and Drop API](https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API)
- [Versione Pure HTML del progetto](https://github.com/EmanueleCimmino/TIW_project_html)

---

## 👨‍💻 Autore: Emanuele Cimmino

Progetto realizzato per il corso di **Tecnologie Informatiche per il Web**
Politecnico di Milano - AA 2024-2025

---

**Made with ❤️**
