package parallelmis.editmenuactions;

import misgui.Krug;
import misgui.Segment;

import java.util.ArrayList;

public class UndoRedoAkcija {

    public enum TipElementa {
        VRH, BRID, VIŠE_ELEMENATA
    }

    public enum TipAkcije {
        DODAJ, BRIŠI
    }
    private final TipElementa element;
    private TipAkcije akcija;
    private final ArrayList<Krug> vrhovi;
    private final ArrayList<Segment> segmenti;

    public UndoRedoAkcija(TipElementa element, TipAkcije akcija, Segment segment) {
        this.element = element;
        this.akcija = akcija;
        this.vrhovi = new ArrayList<>();
        this.vrhovi.add(segment.dajKrug1());
        this.segmenti = new ArrayList<>();
        this.segmenti.add(segment);
    }

    public UndoRedoAkcija(TipElementa element, TipAkcije akcija, Krug krug) {
        this.element = element;
        this.akcija = akcija;
        this.vrhovi = new ArrayList<>();
        this.vrhovi.add(krug);
        this.segmenti = new ArrayList<>();
    }

    public UndoRedoAkcija(TipElementa element, TipAkcije akcija) {
        this.element = element;
        this.akcija = akcija;
        this.vrhovi = new ArrayList<>();
        this.segmenti = new ArrayList<>();
    }

    /**
     * <p><b>undo</b>
     * Kad obrišemo vrh (a onda i njegove bridove), pomoću jednog undo možemo vratiti vrh i sve njegove obrisane bridove</p>
     * <p><b>redo</b>
     * Kad napravimo undo i vratimo vrh (a onda i njegove bridove), pomoću jednog redo možemo opet obrisati vrh i sve njegove bridove</p>
     */
    public void dodajDodatniSegment(Segment segment) {
        this.segmenti.add(segment);
    }
    public void dodajDodatniVrh(Krug krug) {
        this.vrhovi.add(krug);
    }

    public TipAkcije dajAkcija() {
        return akcija;
    }
    public TipElementa dajElement() {
        return element;
    }
    public ArrayList<Segment> dajSegmenti() {
        return segmenti;
    }
    public ArrayList<Krug> dajVrhovi() {
        return vrhovi;
    }
    public void postaviAkcija(TipAkcije tipAkcije) {
        this.akcija = tipAkcije;
    }
}

