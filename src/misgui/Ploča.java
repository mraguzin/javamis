package misgui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import parallelmis.Graf;

/**
 *
 * @author mraguzin
 * Glavni dio za grafički unos grafa, s gumbima itd.
 */
public class Ploča extends JComponent {
    private static final int ŠIRINA = 600;
    private static final int VISINA = 600;
       
    public enum Tip {
        KRUG, SEGMENT;
    }
    
    public enum Akcija {
        DODAJ_VRH, DODAJ_BRID, BRIŠI;
    }
    
    private Tip klik;
    private Akcija trenutnaAkcija;
    
    public Ploča() {
                
        var gumbVrhovi = new JButton("Dodaj vrhove");
        var gumbBridovi= new JButton("Dodaj bridove");
        var gumbBriši = new JButton("Izbriši");
        var gumbOčisti= new JButton("Očisti sve");
        var gumbSeq = new JButton("Riješi sekvencijalno");
        var gumbPar = new JButton("Riješi paralelno");
        
        var gumbi = new JPanel(); //TODO: dodaj tooltipove preko AbstractAction impl
        gumbi.add(gumbVrhovi);
        gumbi.add(gumbBridovi);
        gumbi.add(gumbBriši);
        gumbi.add(gumbOčisti);
        gumbi.add(gumbSeq);
        gumbi.add(gumbPar);
        
        add(gumbi);
        
        var površina = new Površina();
        
        
    }
    
    @Override
    public void paintComponent(Graphics g) {
        //setSize(ŠIRINA, VISINA);
        var g2 = (Graphics2D) g;
        g2.setBackground(Color.RED);
        var dims = getPreferredSize();
        setVisible(true);
        //g.clearRect(0, 0, dims.width, dims.height);
        //g.fillRect(20, 20, dims.width, dims.height);
        //g2.clearRect(0, 0, dims.width, dims.height);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ŠIRINA, VISINA);
    }
}
