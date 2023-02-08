package parallelmis;

import misgui.ProgramFrame;

import javax.swing.*;
import java.awt.*;

/**
 *
 * @author mraguzin
 */
public class ParallelMIS {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            var frame = new ProgramFrame();
            frame.setLocationByPlatform(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            //frame.setIconImage(new ImageIcon("ikona.gif").getImage()); //TODO: napraviti ikonu za ovo
            frame.setVisible(true);
        });
    }
    
}
