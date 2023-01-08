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
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import misgui.ProgramFrame.Akcija;
import parallelmis.Algoritam1v4;
import parallelmis.Graf;

/**
 *
 * @author mraguzin
 */
public class Površina extends JComponent {
    private static final int ŠIRINA = 600;
    private static final int VISINA = 400;
    private static final float R = 10.0f; // default radijus čvorova
    private static final double EPSILON = 10.0; // duljina stranice kvadrata
    // za testiranje pripadnosti točke liniji
    
    private ArrayList<Krug> krugovi = new ArrayList<>();
    private ArrayList<Segment> segmenti = new ArrayList<>();
    private Krug trenutniKrug; // onaj nad kojim je miš (ako postoji)
    private Krug prviKrug; // početak brida
    private Krug zadnjiKrug; // kraj brida
    private Collection<Integer> obojeniKrugovi = List.of(); // oni iz rješenja, radi bržeg brisanja
    private Segment trenutniSeg;
    private Rectangle2D testni = new Rectangle2D.Double();
    
    private Graf graf = new Graf();
    private Akcija akcija = Akcija.NEMA;
    private final ProgramFrame okvir;
    
    public Površina(ProgramFrame okvir) {
        setVisible(true);
        this.okvir = okvir;
                
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                trenutniKrug = pronađiKrug(event.getPoint());
                trenutniSeg = pronađiSeg(event.getPoint());
                switch (akcija) {
                    case DODAJ_VRH:
                        if (trenutniKrug == null) {
                            dodajKrug(event.getPoint());
                        }
                        else if (event.getClickCount() >= 2) {
                            akcija = Akcija.NEMA;
                            okvir.resetirajGumbe();
                        }
                        break;
                    case DODAJ_BRID:
                        if (trenutniKrug != null) {
                            trenutniKrug.aktiviraj();
                            repaint();
                            prviKrug = trenutniKrug;
                            zadnjiKrug = null;
                            akcija = Akcija.DODAJ_KRAJ; //TODO: dodati poruku u statusbar koja govori da treba kliknuti drugi kraj?
                        }
                        else if (event.getClickCount() >= 2) {
                            prviKrug = zadnjiKrug = null;
                            akcija = Akcija.NEMA;
                            okvir.resetirajGumbe();
                        }
                        break;
                    case DODAJ_KRAJ:
                        if (trenutniKrug != null) {
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
                        else {
                            prviKrug.resetiraj();
                            repaint();
                            prviKrug = zadnjiKrug = null;
                            akcija = Akcija.NEMA;
                            okvir.resetirajGumbe();
                        }
                        break;
                    case BRIŠI:
                        if (trenutniKrug != null)
                            ukloniKrug(trenutniKrug);
                        else if (trenutniSeg != null)
                            ukloniSegment(trenutniSeg);
                        break;
                    default:
                        break;
                }
            }
        });
        
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (akcija == Akcija.NEMA && trenutniKrug != null) {
                    int i = krugovi.indexOf(trenutniKrug);
                    for (int v : graf.dajListu().get(i)) {
                        var tmp = new Segment(trenutniKrug, krugovi.get(v));
                        for (var seg : segmenti) {
                            if (seg.equals(tmp)) {
                                seg.pomakniKraj(trenutniKrug, e.getX(), e.getY());
                                break;
                            }
                        }
                    }
                    
                    trenutniKrug.pomakni(e.getX(), e.getY());
                    okvir.objaviPromjenu();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                trenutniKrug = pronađiKrug(e.getPoint());
                trenutniSeg = pronađiSeg(e.getPoint());
                
                if (null == akcija) setCursor(Cursor.getDefaultCursor());
                else switch (akcija) {
                    case DODAJ_VRH:
                        if (trenutniKrug == null)
                            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        else
                            setCursor(Cursor.getDefaultCursor());
                        break;
                    case DODAJ_BRID:
                    case DODAJ_KRAJ:
                        if (trenutniKrug != null) {
                            if (trenutniKrug == prviKrug && akcija == Akcija.DODAJ_KRAJ)
                                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            else
                                setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        }
                        else
                            setCursor(Cursor.getDefaultCursor());
                        break;
                    case BRIŠI:
                        if (trenutniKrug == null && trenutniSeg == null)
                            setCursor(Cursor.getDefaultCursor());
                        else
                            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        break;
                    case NEMA:
                        if (trenutniKrug != null)
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        else
                            setCursor(Cursor.getDefaultCursor());
                        break;
                    default:
                        setCursor(Cursor.getDefaultCursor());
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
        for (float y = 2*R; y < VISINA - R; y += 3*R) {
            for (float x = 2*R; x < ŠIRINA - R && i < g.dajBrojVrhova(); x += 3*R,++i) {
              dodajKrug(new Point2D.Float(x, y));
            }
        }
        
        for (i = 0; i < g.dajBrojVrhova(); ++i) {
            for (int j : g.dajListu().get(i)) {
                dodajSegment(krugovi.get(i), krugovi.get(j));
            }
        }
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
            case NEMA:
                zadnjiKrug = null;
                if (prviKrug != null) {
                    prviKrug.resetiraj();
                    prviKrug = null;
                    repaint();
                }   break;
            case OČISTI:
                očisti();
                this.akcija = stara;
                break;
            case SEQ:
                if (graf.dajBrojVrhova() == 0)
                    okvir.objaviRezultat(null);
                else {
                    var zadatak = CompletableFuture.supplyAsync(() -> {
                        return graf.sequentialMIS();
                    });
                    
                    zadatak.thenAccept(this::procesirajRezultat);
                }   this.akcija = Akcija.NEMA;
                break;
            case PAR:
                if (graf.dajBrojVrhova() == 0)
                    okvir.objaviRezultat(null);
                else {
                    var zadatak = new Algoritam1v4(graf).dajZadatak(); //TODO: dodaj druge varijante, štopaj i usporedi vremena
                    zadatak.thenAccept(this::procesirajRezultat);
                }   this.akcija = Akcija.NEMA;
                break;
            default:
                break;
        }
    }
    
    private void procesirajRezultat(Collection<Integer> rez) {
        for (int vrh : rez)
            krugovi.get(vrh).oboji();
        obojeniKrugovi = rez;
        
        repaint();
        okvir.objaviRezultat(rez);
        okvir.objaviPromjenu();
    }
    
    private Krug pronađiKrug(Point2D točka) {
        for (var k : krugovi) {
            if (k.contains(točka))
                return k;
        }
        
        return null;
    }
    
    private Segment pronađiSeg(Point2D točka) {
        testni.setFrame(točka.getX()-EPSILON/2, točka.getY()-EPSILON/2,
                EPSILON, EPSILON);
        for (var s : segmenti) {
            if (s.intersects(testni))
                return s;
        }
        
        return null;
    }
    
    private void dodajKrug(Point2D c) {
        krugovi.add(new Krug((float)c.getX(), (float)c.getY(), R));
        graf.dodajVrh();
        
        repaint();
        okvir.objaviPromjenu();
    }
    
    private void dodajSegment(Krug k1, Krug k2) {
        var novi = new Segment(k1, k2);
        if (!segmenti.contains(novi))
            segmenti.add(new Segment(k1, k2));
        graf.dodajBrid(krugovi.indexOf(k1), krugovi.indexOf(k2));
        
        repaint();
        zadnjiKrug = null;
        okvir.objaviPromjenu();
    }
    
    private void ukloniKrug(Krug k) {
        if (k == null)
            return;
        
        int i = krugovi.indexOf(k);
        var susjedi = graf.dajListu().get(i);
        for (int v : susjedi) {
            Segment tmp = new Segment(krugovi.get(v), k);
            segmenti.remove(tmp);
        }
        
        graf.ukloniVrh(i);
        krugovi.remove(i);
                
        repaint();
        okvir.objaviPromjenu();
    }
    
    private void ukloniSegment(Segment s) {
        if (s == null)
            return;
        
        graf.ukloniBrid(krugovi.indexOf(s.dajKrug1()), krugovi.indexOf(s.dajKrug2()));
        segmenti.remove(s);
        
        repaint();
        okvir.objaviPromjenu();
    }
    
    public void očisti() {
        krugovi.clear();
        segmenti.clear();
        graf = new Graf();
        
        repaint();
        okvir.objaviPromjenu();
    }
}
