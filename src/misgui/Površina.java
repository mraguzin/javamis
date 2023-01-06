package misgui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import javax.swing.JComponent;
import misgui.Ploča.Akcija;
import parallelmis.Graf;

/**
 *
 * @author mraguzin
 */
public class Površina extends JComponent {
    private static final int ŠIRINA = 600;
    private static final int VISINA = 400;
    private static final float R = 10.0f; // default radijus čvorova
    private int id = 0;
    
    private ArrayList<Krug> krugovi = new ArrayList<>();
    private ArrayList<Segment> segmenti = new ArrayList<>();
    private Krug trenutniKrug; // onaj nad kojim je miš (ako postoji)
    private Segment trenutniSeg;
    
    private Graf graf = new Graf();
    private Akcija akcija;
    
    public Površina() {
                
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
                    case DODAJ_BRID:
                        break;
                    case BRIŠI:
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
                Krug k = pronađiKrug(e.getPoint());
                Segment s = pronađiSeg(e.getPoint());
                if (k == null && s == null)
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
        g2.setBackground(Color.WHITE);
        
        for (var k : krugovi)
            g2.draw(k);
        for (var s : segmenti)
            g2.draw(s);
    }
    
    public void obavijesti(Akcija novaAkcija) {
        this.akcija = novaAkcija;
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
        krugovi.add(new Krug(id++, (float)c.getX(), (float)c.getY(), R));
        graf.dodajVrh();
        
        repaint();
    }
    
    public void očisti() {
        id = 0;
        krugovi.clear();
        segmenti.clear();
        graf = new Graf();
        
        repaint();
    }
}
