package beans;

public class Brano {
    private int id_brano;
    private String titolo;
    private String file_musicale;
    private int id_utente;
    private int id_album;

    public int getId() {
        return id_brano;
    }
    public void setId(int id_brano) {
        this.id_brano = id_brano;
    }
    public String getTitolo() {
        return titolo;
    }
    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }
    public String getFile_musicale() {
        return file_musicale;
    }
    public void setFile_musicale(String file_musicale) {
        this.file_musicale = file_musicale;
    }
    public int getId_utente() {
        return id_utente;
    }
    public void setId_utente(int id_utente) {
        this.id_utente = id_utente;
    }
    public int getId_album() {
        return id_album;
    }
    public void setId_album(int id_album) {
        this.id_album = id_album;
    }

}
