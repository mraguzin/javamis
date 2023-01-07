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
import java.util.ArrayList;
import java.util.Collection;
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
    
    private ArrayList<Krug> krugovi = new ArrayList<>();
    private ArrayList<Segment> segmenti = new ArrayList<>();
    private Krug trenutniKrug; // onaj nad kojim je miš (ako postoji)
    private Krug prviKrug; // početak brida
    private Krug zadnjiKrug; // kraj brida
    private Segment trenutniSeg;
    
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
                switch (akcija) { // TODO
                    case DODAJ_VRH:
                        if (trenutniKrug == null) {
                            dodajKrug(event.getPoint());
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
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                trenutniKrug = pronađiKrug(e.getPoint());
                trenutniSeg = pronađiSeg(e.getPoint());
                if (trenutniKrug == null && trenutniSeg == null)
                    setCursor(Cursor.getDefaultCursor());
                else
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
            g2.draw(k);
            g2.setColor(k.dajBoju());
            g2.fill(k);
        }
    }
    
    public void obavijesti(Akcija novaAkcija) {
        this.akcija = novaAkcija;
        if (novaAkcija == Akcija.NEMA) {
            zadnjiKrug = null;
            if (prviKrug != null) {
                prviKrug.resetiraj();
                prviKrug = null;
                repaint();
            }
        }
        else if (novaAkcija == Akcija.OČISTI) {
            očisti();
        }
        else if (novaAkcija == Akcija.SEQ) {
            if (graf.dajBrojVrhova() == 0)
                okvir.objaviRezultat(null);
            else {
                var zadatak = CompletableFuture.supplyAsync(() -> {
                    return graf.sequentialMIS();
                    });
                
                zadatak.thenAccept(this::procesirajRezultat);
            }
            
            this.akcija = Akcija.NEMA;
        }
        else if (novaAkcija == Akcija.PAR) {
            if (graf.dajBrojVrhova() == 0)
                okvir.objaviRezultat(null);
            else {
                var zadatak = new Algoritam1v4(graf).dajZadatak(); //TODO: dodaj druge varijante, štopaj i usporedi vremena
                zadatak.thenAccept(this::procesirajRezultat);
            }
            
            this.akcija = Akcija.NEMA;
        }
            
    }
    
    private void procesirajRezultat(Collection<Integer> rez) {
        for (int vrh : rez)
            krugovi.get(vrh).oboji();
        
        repaint();
        okvir.objaviRezultat(rez);
    }
    
    private Krug pronađiKrug(Point2D točka) {
        for (var k : krugovi) {
            if (k.contains(točka))
                return k;
        }
        
        return null;
    }
    
    private Segment pronađiSeg(Point2D točka) {
        for (var s : segmenti) {
            if (s.contains(točka))
                return s;
        }
        
        return null;
    }
    
    private void dodajKrug(Point2D c) {
        krugovi.add(new Krug((float)c.getX(), (float)c.getY(), R));
        graf.dodajVrh();
        
        repaint();
    }
    
    private void dodajSegment(Krug k1, Krug k2) {
        segmenti.add(new Segment(k1, k2));
        graf.dodajBrid(krugovi.indexOf(k1), krugovi.indexOf(k2));
        
        repaint();
        zadnjiKrug = null;
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
    }
    
    private void ukloniSegment(Segment s) {
        if (s == null)
            return;
        
        graf.ukloniBrid(krugovi.indexOf(s.dajKrug1()), krugovi.indexOf(s.dajKrug2()));
        segmenti.remove(s);
        
        repaint();
    }
    
    public void očisti() {
        krugovi.clear();
        segmenti.clear();
        graf = new Graf();
        obavijesti(Akcija.NEMA);
        
        repaint();
    }
}
