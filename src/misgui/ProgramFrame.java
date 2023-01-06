package misgui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author mraguzin
 */
public class ProgramFrame extends JFrame {
    private static final int ŠIRINA = 1000;
    private static final int VISINA = 1000;
    private JPanel gumbi;
    
    public ProgramFrame() {
        setSize(ŠIRINA, VISINA);
        
        add(new Ploča());
        pack();
    }
}
