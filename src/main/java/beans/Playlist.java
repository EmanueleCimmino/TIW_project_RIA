package beans;

import java.time.LocalDateTime;

public class Playlist {
    private int id_playlist;
    private int id_utente;
    private String titolo;
    private LocalDateTime data_creazione;

    public int getId() {
        return id_playlist;
    }
    public void setId(int id_playlist) {
        this.id_playlist = id_playlist;
    }
    public int getId_utente() {
        return id_utente;
    }
    public void setId_utente(int id_utente) {
        this.id_utente = id_utente;
    }
    public String getTitolo() {
        return titolo;
    }
    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }
    public LocalDateTime getData_creazione() {
        return data_creazione;
    }
    public void setData_creazione(LocalDateTime data_creazione) {
        this.data_creazione = data_creazione;
    }
}

