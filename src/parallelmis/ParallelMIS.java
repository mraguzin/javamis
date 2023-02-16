package parallelmis;

import misgui.ProgramFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Glavna klasa programa; postavlja red događaja za Swing GUI i otvara
 * glavni prozor.
 * 
 * @author mraguzin
 */
public class ParallelMIS {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            var frame = new ProgramFrame();
            frame.setLocationByPlatform(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);
            frame.setVisible(true);
        });
    }
    
}
