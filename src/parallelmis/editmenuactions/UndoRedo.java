package parallelmis.editmenuactions;

import misgui.Krug;
import misgui.Površina;
import misgui.Segment;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;

public class UndoRedo {
    private final int MAX_ACTIONS = 10;
    private static Površina površina;
    private static ArrayList<UndoRedoAkcija> undoLista;
    private static ArrayList<UndoRedoAkcija> redoLista;
    private static int undoKraj;
    private static int redoKraj;

    public UndoRedo(Površina površina) {
        undoLista = new ArrayList<>(10);
        redoLista = new ArrayList<>(10);
        UndoRedo.površina = površina;
        UndoRedo.undoKraj = 0;
        UndoRedo.redoKraj = 0;
    }

    public void dodajUndoAkciju(UndoRedoAkcija undoRedoAkcija) {
        if(undoLista.size() >= MAX_ACTIONS) {
            undoLista.remove(undoLista.get(0));
            --undoKraj;
        }
        površina.undo.setEnabled(true);
        površina.redo.setEnabled(false);
        undoLista.add(undoKraj, undoRedoAkcija);
        redoLista.clear();
        redoKraj = 0;
        ++undoKraj;
    }

    private void dodajUndoAkcijuIzRedo(UndoRedoAkcija undoRedoAkcija) {
        if(undoLista.size() >= MAX_ACTIONS) {
            undoLista.remove(undoLista.get(0));
            --undoKraj;
        }
        površina.undo.setEnabled(true);
        undoLista.add(undoKraj, undoRedoAkcija);
        ++undoKraj;
    }

    private void dodajRedoAkciju(UndoRedoAkcija undoRedoAkcija) {
        if(redoLista.size() >= MAX_ACTIONS) {
            redoLista.remove(redoLista.get(0));
            --redoKraj;
        }
        redoLista.add(redoKraj, undoRedoAkcija);
        površina.redo.setEnabled(true);
        ++redoKraj;
    }

    public void izvršiAkcijuDodaj(UndoRedoAkcija undoRedoAkcija) {
        if(undoRedoAkcija.dajElement() == UndoRedoAkcija.TipElementa.VRH) {
            Krug krug1 = undoRedoAkcija.dajVrhovi().get(0);
            površina.dodajKrug(new Point2D.Float(krug1.dajX(), krug1.dajY()), Površina.IzvorAkcije.UNDOREDO);
            for(Segment segment : undoRedoAkcija.dajSegmenti()) {
                površina.dodajSegment(segment.dajKrug1(), segment.dajKrug2(), Površina.IzvorAkcije.UNDOREDO);
            }
        }
        else if(undoRedoAkcija.dajElement() == UndoRedoAkcija.TipElementa.BRID) {
            Krug krug1 = undoRedoAkcija.dajVrhovi().get(0);
            površina.dodajSegment(krug1, undoRedoAkcija.dajSegmenti().get(0).dajKrug2(), Površina.IzvorAkcije.UNDOREDO);
        }
        else if(undoRedoAkcija.dajElement() == UndoRedoAkcija.TipElementa.VIŠE_ELEMENATA) {
            for(Krug krug : undoRedoAkcija.dajVrhovi()) {
                površina.dodajKrug(new Point2D.Float(krug.dajX(), krug.dajY()), Površina.IzvorAkcije.UNDOREDO);
            }
            for(Segment segment : undoRedoAkcija.dajSegmenti()) {
                površina.dodajSegment(segment.dajKrug1(), segment.dajKrug2(), Površina.IzvorAkcije.UNDOREDO);
            }
        }
    }

    public void izvršiAkcijuBriši(UndoRedoAkcija undoRedoAkcija) {
        if(undoRedoAkcija.dajElement() == UndoRedoAkcija.TipElementa.VRH) {
            površina.ukloniKrug(undoRedoAkcija.dajVrhovi().get(0), Površina.IzvorAkcije.UNDOREDO);
            for(Segment segment : undoRedoAkcija.dajSegmenti()) {
                površina.ukloniSegment(segment, Površina.IzvorAkcije.UNDOREDO);
            }
        }
        else if(undoRedoAkcija.dajElement() == UndoRedoAkcija.TipElementa.BRID) {
            površina.ukloniSegment(undoRedoAkcija.dajSegmenti().get(0), Površina.IzvorAkcije.UNDOREDO);
        }
        else if(undoRedoAkcija.dajElement() == UndoRedoAkcija.TipElementa.VIŠE_ELEMENATA) {
            for(Segment segment : undoRedoAkcija.dajSegmenti()) {
                površina.ukloniSegment(segment, Površina.IzvorAkcije.UNDOREDO);
            }
//            Collections.reverse(undoRedoAkcija.dajVrhovi());
            for(Krug krug : undoRedoAkcija.dajVrhovi()) {
                površina.ukloniKrug(krug, Površina.IzvorAkcije.UNDOREDO);
            }
        }
    }

    public void izvrsiUndoAkciju() {
        if(undoLista.isEmpty()) {
            return;
        }

        if(--undoKraj == 0) {
            površina.undo.setEnabled(false);
        }
        UndoRedoAkcija undoRedoAkcija = undoLista.get(undoKraj);
        undoLista.remove(undoKraj);
        if(undoRedoAkcija.dajAkcija() == UndoRedoAkcija.TipAkcije.DODAJ) {
            izvršiAkcijuDodaj(undoRedoAkcija);
            undoRedoAkcija.postaviAkcija(UndoRedoAkcija.TipAkcije.BRIŠI);
        }
        else if(undoRedoAkcija.dajAkcija() == UndoRedoAkcija.TipAkcije.BRIŠI) {
            izvršiAkcijuBriši(undoRedoAkcija);
            undoRedoAkcija.postaviAkcija(UndoRedoAkcija.TipAkcije.DODAJ);
        }

        this.dodajRedoAkciju(undoRedoAkcija);
    }

    public void izvrsiRedoAkciju() {
        if(redoLista.isEmpty()) {
            return;
        }

        if(--redoKraj == 0) {
            površina.redo.setEnabled(false);
        }
        UndoRedoAkcija undoRedoAkcija = redoLista.get(redoKraj);
        redoLista.remove(redoKraj);
        if(undoRedoAkcija.dajAkcija() == UndoRedoAkcija.TipAkcije.DODAJ) {
            izvršiAkcijuDodaj(undoRedoAkcija);
            undoRedoAkcija.postaviAkcija(UndoRedoAkcija.TipAkcije.BRIŠI);
        }
        else if(undoRedoAkcija.dajAkcija() == UndoRedoAkcija.TipAkcije.BRIŠI) {
            izvršiAkcijuBriši(undoRedoAkcija);
            undoRedoAkcija.postaviAkcija(UndoRedoAkcija.TipAkcije.DODAJ);
        }

        this.dodajUndoAkcijuIzRedo(undoRedoAkcija);
    }
}
