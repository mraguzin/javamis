package misgui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import javax.swing.JPanel;
import parallelmis.Graf;

/**
 *
 * @author mraguzin
 * Glavni dio za grafički unos grafa, s gumbima itd.
 */
public class ProgramFrame extends JFrame {
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
    
    public ProgramFrame() {
        površina = new Površina(this);
        
        var gumbVrhovi = new JButton("Dodaj vrhove");
        var gumbBridovi= new JButton("Dodaj bridove");
        var gumbBriši = new JButton("Izbriši");
        var gumbOčisti= new JButton("Očisti sve");
        var gumbSeq = new JButton("Riješi sekvencijalno");
        var gumbPar = new JButton("Riješi paralelno");
        gumbi = List.of(gumbVrhovi, gumbBridovi,
            gumbBriši, gumbOčisti, gumbSeq, gumbPar);
        
        GUMB_NEAKTIVAN = gumbPar.getBackground();
        
         //TODO: dodaj tooltipove preko AbstractAction impl
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
        });
        gumbSeq.addActionListener((ActionEvent e) -> {
            resetirajGumbe();
            gumbi.forEach((gumb) -> {
                gumb.setEnabled(false); // ćemo tu mijenjati boju?
            });
            
            površina.obavijesti(Akcija.SEQ);
        });
        gumbPar.addActionListener((ActionEvent e) -> {
            resetirajGumbe();
            gumbi.forEach((gumb) -> {
                gumb.setEnabled(false);
            });
            
            površina.obavijesti(Akcija.PAR);
        });
        
        add(panel);
        pack();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ŠIRINA, VISINA);
    }
    
    public void objaviRezultat(Collection<Integer> rezultat) {
        gumbi.forEach((gumb) -> {
            gumb.setEnabled(true);
        });
        
        if (rezultat == null) {
            JOptionPane.showMessageDialog(null, "Niste nacrtali graf!",
                    "Greška", ERROR_MESSAGE);
        }
        
        // TODO: štopanje, neke dodatne poruke...?
    }
    
    private void resetirajGumbe() {
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
                aktivan = null;
                površina.obavijesti(Akcija.NEMA);
                resetirajGumbe();
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
