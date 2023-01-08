package misgui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import parallelmis.Graf;

/**
 *
 * @author mraguzin
 * Glavni dio za grafički unos grafa, s gumbima itd.
 */
public class ProgramFrame extends JFrame {
    public static final String NASLOV = "Maksimalan nezavisni skup";
    private static final int ŠIRINA = 800;
    private static final int VISINA = 800;
    private static final Color GUMB_AKTIVAN = Color.RED;
    private final Color GUMB_NEAKTIVAN;
       
    public enum Tip {
        KRUG, SEGMENT;
    }
    
    public enum Akcija {
        DODAJ_VRH, DODAJ_BRID, DODAJ_KRAJ, BRIŠI, OČISTI, SEQ, PAR, NEMA;
        // DODAJ_KRAJ označava stanje u kojem čekamo klik za zadnji kraj brida
    }
    
    private Tip klik;
    private Akcija trenutnaAkcija;
    private Površina površina;
    private JPanel panel;
    private JButton aktivan;
    private List<JButton> gumbi;
    private List<JMenuItem> stavke;
    private JMenuBar menu;
    private JFileChooser chooser;
    
    
    private boolean postojiPromjena = false;
    
    public ProgramFrame() {
        površina = new Površina(this);
        setTitle(NASLOV);
        
        var gumbVrhovi = new JButton("Dodaj vrhove");
        var gumbBridovi= new JButton("Dodaj bridove");
        var gumbBriši = new JButton("Izbriši");
        var gumbOčisti= new JButton("Očisti sve");
        var gumbSeq = new JButton("Riješi sekvencijalno");
        var gumbPar = new JButton("Riješi paralelno");
        gumbi = List.of(gumbVrhovi, gumbBridovi,
            gumbBriši, gumbOčisti, gumbSeq, gumbPar);
        
        GUMB_NEAKTIVAN = gumbPar.getBackground();
        
        panel = new JPanel();
        gumbi.forEach((gumb) -> {
            panel.add(gumb);
        });
        
        panel.add(površina);
        
        gumbVrhovi.addActionListener(new GumbAkcija(Akcija.DODAJ_VRH, gumbVrhovi));
        gumbBridovi.addActionListener(new GumbAkcija(Akcija.DODAJ_BRID, gumbBridovi));
        gumbBriši.addActionListener(new GumbAkcija(Akcija.BRIŠI, gumbBriši));
        gumbOčisti.addActionListener((ActionEvent e) -> {
            površina.obavijesti(Akcija.OČISTI);
            resetirajGumbe();
        });
        gumbSeq.addActionListener((ActionEvent e) -> {
            resetirajGumbe();
            gumbi.forEach((gumb) -> {
                gumb.setEnabled(false); // ćemo tu mijenjati boju?
            });
            
            omogućiMenije(false);            
            površina.obavijesti(Akcija.SEQ);
        });
        gumbPar.addActionListener((ActionEvent e) -> {
            resetirajGumbe();
            gumbi.forEach((gumb) -> {
                gumb.setEnabled(false);
            });
            
            omogućiMenije(false);
            površina.obavijesti(Akcija.PAR);
        });
        gumbVrhovi.setToolTipText("Kliknite na prazno mjesto ploče za postaviti novi vrh");
        gumbBridovi.setToolTipText("Žuti vrh je prvi; kliknite na drugi vrh za spajanje");
        gumbBriši.setToolTipText("Kliknite na vrh ili brid za brisanje");
        gumbOčisti.setToolTipText("Očisti cijelu ploču (izgubit ćete graf!)");
        gumbSeq.setToolTipText("Riješi MIS problem sekvencijalno (sporo)");
        gumbPar.setToolTipText("Riješi MIS problem paralelno (brže za jako velike grafove");
        
        add(panel);
        menuSetup();
        
        pack();
    }
    
    private void menuSetup() {
        menu = new JMenuBar();
        setJMenuBar(menu);
        var fileMenu = new JMenu("File");
        var editMenu = new JMenu("Edit");
        menu.add(fileMenu);
        menu.add(editMenu);
        stavke = new ArrayList<>();
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                izlaz();
            }
        });
        
        // File
        var spremiAkcija = new AbstractAction("Spremi", new ImageIcon("disketa.gif")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                 spremi();
            }
        };
        
        var spremiItem = new JMenuItem(spremiAkcija);
        spremiItem.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
        
        var spremiKaoAkcija = new AbstractAction("Spremi kao") {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO...
            }
        };
        
        var učitajAkcija = new AbstractAction("Učitaj", new ImageIcon("load.gif")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: dijalog za učitavanje iz baze
            }
        };
        
        var učitajItem = new JMenuItem(učitajAkcija);
        učitajItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        
        var izlazAkcija = new AbstractAction("Izlaz") {
            @Override
            public void actionPerformed(ActionEvent e) {
                izlaz();
            }
        };
        
        stavke.add(fileMenu.add(spremiItem));
        stavke.add(fileMenu.add(spremiKaoAkcija));
        fileMenu.addSeparator();
        stavke.add(fileMenu.add(učitajItem));
        fileMenu.addSeparator();
        fileMenu.add(izlazAkcija);
        
        // Edit
        // TODO: treba li tu Undo?
        var uveziItem = new JMenuItem("Uvezi graf");
        uveziItem.setToolTipText("Graf mora biti u Challenge 9 tekstualnom formatu");
        uveziItem.addActionListener((ActionEvent e) -> {
            var c = getChooser();
            c.setCurrentDirectory(new File("."));
            int rez = c.showDialog(this, "Uvezi");
            if (rez == JFileChooser.APPROVE_OPTION) {
                try {
                    Graf g = Graf.stvoriIzDatoteke(c.getSelectedFile());
                    površina.učitajGraf(g);
                } 
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Greška pri čitanju.\n" 
                            + ex.getMessage(), "Krivi format datoteke",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        stavke.add(editMenu.add(uveziItem));
    }
    
    private void izlaz() {
        if (postojiPromjena) {
                    int odabir = JOptionPane.showConfirmDialog(null,
   "Postoje izmjene koje nisu spremljene. Želite li ih spremiti prije izlaza?",
   "Prije izlaska", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    switch (odabir) {
                        case JOptionPane.YES_OPTION:
                            spremi();
                            break;
                        case JOptionPane.NO_OPTION:
                            break;
                        default:
                            return;
                    }
                }
                
                System.exit(0);
    }
    
    private JFileChooser getChooser() {
        if (chooser == null)
            chooser = new JFileChooser();
        
        return chooser;
    }
    
    private void spremi()
    {
        // TODO
        
    }
    
    private void omogućiMenije(boolean b) {
        for (var i : stavke)
            i.setEnabled(b);
    }
    
    public void objaviSnimanje() {
        postojiPromjena = false;
        setTitle(ProgramFrame.NASLOV);
    }
    
    public void objaviPromjenu() {
        postojiPromjena = true;
        setTitle("* " + ProgramFrame.NASLOV);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ŠIRINA, VISINA);
    }
    
    public void objaviRezultat(Collection<Integer> rezultat) {
        gumbi.forEach((gumb) -> {
            gumb.setEnabled(true);
        });
        
        omogućiMenije(true);
        
        if (rezultat == null) {
            JOptionPane.showMessageDialog(null, "Niste nacrtali graf!",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
        
        // TODO: štopanje, neke dodatne poruke...?
    }
    
    public void resetirajGumbe() {
        aktivan = null;
        gumbi.forEach((gumb) -> {
            gumb.setBackground(GUMB_NEAKTIVAN);
        });
    }
    
    private class GumbAkcija implements ActionListener {
        private Color boja;
        private Akcija akcija;
        private JButton gumb;
        
        public GumbAkcija(Akcija akcija, JButton gumb) {
            this.akcija = akcija;
            this.gumb = gumb;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (aktivan == gumb) {
                resetirajGumbe();
                aktivan = null;
                površina.obavijesti(Akcija.NEMA);
            }
            else {
                površina.obavijesti(akcija);
                resetirajGumbe();
                gumb.setBackground(GUMB_AKTIVAN);
                aktivan = gumb;
            }
        }
    }
}
