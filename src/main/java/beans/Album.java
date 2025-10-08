package beans;

public class Album {
    private int id_album;
    private String titolo_album;
    private String interprete;
    private String genere;
    private int anno_pubblicazione;
    private String immagine_album;

    public int getId_album(){
        return id_album;
    }
    public void setId_album(int id_album){
        this.id_album = id_album;
    }
    public String getTitolo_album(){
        return titolo_album;
    }
    public void setTitolo_album(String titolo_album){
        this.titolo_album = titolo_album;
    }
    public String getInterprete(){
        return interprete;
    }
    public void setInterprete(String interprete){
        this.interprete = interprete;
    }
    public String getGenere(){
        return genere;
    }
    public void setGenere(String genere){
        this.genere = genere;
    }
    public int getAnno_pubblicazione(){
        return anno_pubblicazione;
    }
    public void setAnno_pubblicazione(int anno_pubblicazione){
        this.anno_pubblicazione = anno_pubblicazione;
    }
    public String getImmagine_album(){
        return immagine_album;
    }
    public void setImmagine_album(String immagine_album){
        this.immagine_album = immagine_album;
    }
}

