package parallelmis.editmenuactions;

import misgui.Krug;
import misgui.Površina;
import misgui.Segment;

import java.awt.geom.Point2D;
import java.util.ArrayList;

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
        Krug krug1 = undoRedoAkcija.getVrhovi().get(0);
        if(undoRedoAkcija.getElement() == UndoRedoAkcija.TipElementa.VRH) {
            površina.dodajKrug(new Point2D.Float(krug1.dajX(), krug1.dajY()), Površina.IzvorAkcije.UNDOREDO);
            for(Segment segment : undoRedoAkcija.getSegmenti()) {
                površina.dodajSegment(krug1, segment.dajKrug2(), Površina.IzvorAkcije.UNDOREDO);
            }
        }
        else if(undoRedoAkcija.getElement() == UndoRedoAkcija.TipElementa.BRID) {
            površina.dodajSegment(krug1, undoRedoAkcija.getSegmenti().get(0).dajKrug2(), Površina.IzvorAkcije.UNDOREDO);
        }
        else if(undoRedoAkcija.getElement() == UndoRedoAkcija.TipElementa.VIŠE_ELEMENATA) {
            for(Krug krug : undoRedoAkcija.getVrhovi()) {
                površina.dodajKrug(new Point2D.Float(krug.dajX(), krug.dajY()), Površina.IzvorAkcije.UNDOREDO);
            }
            for(Segment segment : undoRedoAkcija.getSegmenti()) {
                površina.dodajSegment(segment.dajKrug1(), segment.dajKrug2(), Površina.IzvorAkcije.UNDOREDO);
            }
        }
    }

    public void izvršiAkcijuBriši(UndoRedoAkcija undoRedoAkcija) {
        if(undoRedoAkcija.getElement() == UndoRedoAkcija.TipElementa.VRH) {
            površina.ukloniKrug(undoRedoAkcija.getVrhovi().get(0), Površina.IzvorAkcije.UNDOREDO);
            for(Segment segment : undoRedoAkcija.getSegmenti()) {
                površina.ukloniSegment(segment, Površina.IzvorAkcije.UNDOREDO);
            }
        }
        else if(undoRedoAkcija.getElement() == UndoRedoAkcija.TipElementa.BRID) {
            površina.ukloniSegment(undoRedoAkcija.getSegmenti().get(0), Površina.IzvorAkcije.UNDOREDO);
        }
        else if(undoRedoAkcija.getElement() == UndoRedoAkcija.TipElementa.VIŠE_ELEMENATA) {
            for(Segment segment : undoRedoAkcija.getSegmenti()) {
                površina.ukloniSegment(segment, Površina.IzvorAkcije.UNDOREDO);
            }
            for(Krug krug : undoRedoAkcija.getVrhovi()) {
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
        if(undoRedoAkcija.getAkcija() == UndoRedoAkcija.TipAkcije.DODAJ) {
            izvršiAkcijuDodaj(undoRedoAkcija);
            undoRedoAkcija.setAkcija(UndoRedoAkcija.TipAkcije.BRIŠI);
        }
        else if(undoRedoAkcija.getAkcija() == UndoRedoAkcija.TipAkcije.BRIŠI) {
            izvršiAkcijuBriši(undoRedoAkcija);
            undoRedoAkcija.setAkcija(UndoRedoAkcija.TipAkcije.DODAJ);
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
        if(undoRedoAkcija.getAkcija() == UndoRedoAkcija.TipAkcije.DODAJ) {
            izvršiAkcijuDodaj(undoRedoAkcija);
            undoRedoAkcija.setAkcija(UndoRedoAkcija.TipAkcije.BRIŠI);
        }
        else if(undoRedoAkcija.getAkcija() == UndoRedoAkcija.TipAkcije.BRIŠI) {
            izvršiAkcijuBriši(undoRedoAkcija);
            undoRedoAkcija.setAkcija(UndoRedoAkcija.TipAkcije.DODAJ);
        }

        this.dodajUndoAkcijuIzRedo(undoRedoAkcija);
    }
}
