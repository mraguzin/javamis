package misgui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.JComponent;
import misgui.ProgramFrame.Akcija;
import misgui.ProgramFrame.TipObjave;
import parallelmis.Algoritam1v3;
import parallelmis.Graf;

/**
 *
 * @author mraguzin
 */
public class Površina extends JComponent {
    private static final int ŠIRINA = 600;
    private static final int VISINA = 400;
    private static final float RADIJUS = 10.0f; // default radijus čvorova
    private static final double EPSILON = 10.0; // duljina stranice kvadrata
    // za testiranje pripadnosti točke liniji
    
    private ArrayList<Krug> krugovi = new ArrayList<>();
    private ArrayList<Segment> segmenti = new ArrayList<>();
    private Krug trenutniKrug; // onaj nad kojim je miš (ako postoji)
    private Krug prviKrug; // početak brida
    private Krug zadnjiKrug; // kraj brida
    private Collection<Integer> obojeniKrugovi = List.of(); // oni iz rješenja, radi bržeg brisanja
    private Segment trenutniSegment;
    private final Rectangle2D testniRectangle2D = new Rectangle2D.Double();
    
    private Graf graf = new Graf();
    private Akcija akcija = Akcija.NEMA;
    private final ProgramFrame okvir;
    
    public Površina(ProgramFrame okvir) {
        setVisible(true);
        this.okvir = okvir;
                
        addMouseListener(new MouseAdapter() {
            private void resetirajGumbe() {
                akcija = Akcija.NEMA;
                okvir.objavi(TipObjave.RESETIRAJ_GUMBE, null);
            }

            private void dodajVrh(MouseEvent event) {
                if (trenutniKrug == null) {
                    dodajKrug(event.getPoint());
                }
                else if (event.getClickCount() >= 2) {
                    resetirajGumbe();
                }
            }

            private void dodajBrid(MouseEvent event) {
                if (trenutniKrug != null) {
                    trenutniKrug.aktiviraj();
                    repaint();
                    prviKrug = trenutniKrug;
                    zadnjiKrug = null;
                    akcija = Akcija.DODAJ_KRAJ; //TODO: dodati poruku u statusbar koja govori da treba kliknuti drugi kraj?
                }
                else if (event.getClickCount() >= 2) {
                    prviKrug = zadnjiKrug = null;
                    resetirajGumbe();
                }
            }

            private void dodajKraj() {
                if (trenutniKrug == null) {
                    prviKrug.resetiraj();
                    repaint();
                    prviKrug = zadnjiKrug = null;
                    resetirajGumbe();
                    return;
                }

                if (trenutniKrug == prviKrug) {
                    prviKrug.resetiraj();
                    repaint();
                    prviKrug = null;
                    akcija = Akcija.DODAJ_BRID;
                }
                else {
                    zadnjiKrug = trenutniKrug;
                    dodajSegment(prviKrug, zadnjiKrug);
                }
            }

            private void briši() {
                if (trenutniKrug != null)
                    ukloniKrug(trenutniKrug);
                else if (trenutniSegment != null)
                    ukloniSegment(trenutniSegment);
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                trenutniKrug = pronađiKrug(event.getPoint());
                trenutniSegment = pronađiSegment(event.getPoint());
                switch (akcija) {
                    case DODAJ_VRH -> dodajVrh(event);
                    case DODAJ_BRID -> dodajBrid(event);
                    case DODAJ_KRAJ -> dodajKraj();
                    case BRIŠI -> briši();
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent event) {
                if (akcija == Akcija.NEMA && trenutniKrug != null) {
                    int indexTrenutnogKruga = krugovi.indexOf(trenutniKrug);
                    for (int v : graf.dajListuSusjednostiKruga(indexTrenutnogKruga)) {
                        var tmp = new Segment(trenutniKrug, krugovi.get(v));
                        for (var seg : segmenti) {
                            if (seg.equals(tmp)) {
                                seg.pomakniKraj(trenutniKrug, event.getX(), event.getY());
                                break;
                            }
                        }
                    }
                    
                    trenutniKrug.pomakni(event.getX(), event.getY());
                    okvir.objavi(TipObjave.PROMJENA, null);
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                trenutniKrug = pronađiKrug(e.getPoint());
                trenutniSegment = pronađiSegment(e.getPoint());
                setCursor(Cursor.getDefaultCursor());
                switch (akcija) {
                    case DODAJ_VRH:
                        if (trenutniKrug == null)
                            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        break;
                    case DODAJ_BRID:
                    case DODAJ_KRAJ:
                        if (trenutniKrug != null) {
                            if (trenutniKrug == prviKrug && akcija == Akcija.DODAJ_KRAJ)
                                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            else
                                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        }
                        break;
                    case BRIŠI:
                        if (trenutniKrug == null && trenutniSegment == null)
                            setCursor(Cursor.getDefaultCursor());
                        else
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        break;
                    case NEMA:
                        if (trenutniKrug != null)
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        break;
                }
            }
        });
    }    
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ŠIRINA, VISINA);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        var g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, this.getWidth(), this.getHeight());
        
        for (var s : segmenti)
            g2.draw(s);
        
        for (var k : krugovi) {
            g2.setColor(Color.BLACK);
            g2.draw(k);
            g2.setColor(k.dajBoju());
            g2.fill(k);
        }
    }
    
    public void učitajGraf(Graf g) {
        očisti();
        int i = 0;
        for (float y = 2* RADIJUS; y < VISINA - RADIJUS; y += 3* RADIJUS) {
            for (float x = 2* RADIJUS; x < ŠIRINA - RADIJUS && i < g.dajBrojVrhova(); x += 3* RADIJUS,++i) {
              dodajKrug(new Point2D.Float(x, y));
            }
        }
        
        for (i = 0; i < g.dajBrojVrhova(); ++i) {
            for (int j : g.dajListuSusjednostiKruga(i)) {
                dodajSegment(krugovi.get(i), krugovi.get(j));
            }
        }
    }


    private void akcijaNema() {
        zadnjiKrug = null;
        if (prviKrug != null) {
            prviKrug.resetiraj();
            prviKrug = null;
            repaint();
        }
    }

    private void akcijaOčisti(Akcija stara) {
        očisti();
        this.akcija = stara;
    }

    private void akcijaSeq() {
        if (graf.dajBrojVrhova() == 0)
            okvir.objavi(TipObjave.REZULTAT, null);
        else {
            var zadatak = CompletableFuture.supplyAsync(() -> graf.sequentialMIS());

            zadatak.thenAccept(this::procesirajRezultat);
        }
        this.akcija = Akcija.NEMA;
    }

    private void akcijaPar() {
        if (graf.dajBrojVrhova() == 0)
            okvir.objavi(TipObjave.REZULTAT, null);
        else {
            var zadatak = new Algoritam1v3(graf).dajZadatak();
            zadatak.thenAccept(this::procesirajRezultat);
        }
        this.akcija = Akcija.NEMA;
    }
    
    public void obavijesti(Akcija novaAkcija) {
        for (int i : obojeniKrugovi)
            krugovi.get(i).resetiraj();
        obojeniKrugovi = List.of();
        
        Akcija stara = akcija;
        if (novaAkcija != stara) {
            if (prviKrug != null) {
                prviKrug.resetiraj();
                prviKrug = null;
                repaint();
            }
        }
        
        this.akcija = novaAkcija;
        if (null != novaAkcija) switch (novaAkcija) {
            case NEMA -> akcijaNema();
            case OČISTI -> akcijaOčisti(stara);
            case SEQ -> akcijaSeq();
            case PAR -> akcijaPar();
        }
    }
    
    private void procesirajRezultat(Collection<Integer> rez) {
        for (int vrh : rez)
            krugovi.get(vrh).oboji();
        obojeniKrugovi = rez;
        
        repaint();
        okvir.objavi(TipObjave.REZULTAT, rez);
        okvir.objavi(TipObjave.PROMJENA, null);
    }
    
    private Krug pronađiKrug(Point2D točka) {
        for (var k : krugovi) {
            if (k.contains(točka))
                return k;
        }
        
        return null;
    }
    
    private Segment pronađiSegment(Point2D točka) {
        testniRectangle2D.setFrame(točka.getX()-EPSILON/2, točka.getY()-EPSILON/2, EPSILON, EPSILON);
        for (var s : segmenti) {
            if (s.intersects(testniRectangle2D))
                return s;
        }
        
        return null;
    }
    
    private void dodajKrug(Point2D point) {
        krugovi.add(new Krug((float)point.getX(), (float)point.getY(), RADIJUS));
        graf.dodajVrh();
        
        repaint();
        okvir.objavi(TipObjave.PROMJENA, null);
    }
    
    private void dodajSegment(Krug k1, Krug k2) {
        var novi = new Segment(k1, k2);
        if (!segmenti.contains(novi))
            segmenti.add(new Segment(k1, k2));
        graf.dodajBrid(krugovi.indexOf(k1), krugovi.indexOf(k2));
        
        repaint();
        zadnjiKrug = null;
        okvir.objavi(TipObjave.PROMJENA, null);
    }
    
    private void ukloniKrug(Krug k) {
        if (k == null)
            return;
        
        int i = krugovi.indexOf(k);
        var susjedi = graf.dajListuSusjednostiKruga(i);
        for (int v : susjedi) {
            Segment tmp = new Segment(krugovi.get(v), k);
            segmenti.remove(tmp);
        }
        
        graf.ukloniVrh(i);
        krugovi.remove(i);
                
        repaint();
        okvir.objavi(TipObjave.PROMJENA, null);
    }
    
    private void ukloniSegment(Segment s) {
        if (s == null)
            return;
        
        graf.ukloniBrid(krugovi.indexOf(s.dajKrug1()), krugovi.indexOf(s.dajKrug2()));
        segmenti.remove(s);
        
        repaint();
        okvir.objavi(TipObjave.PROMJENA, null);
    }
    
    public void očisti() {
        krugovi.clear();
        segmenti.clear();
        graf = new Graf();
        
        repaint();
        okvir.objavi(TipObjave.PROMJENA, null);
    }
}
