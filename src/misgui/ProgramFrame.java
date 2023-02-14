package misgui;

import parallelmis.Graf;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.*;
import misgui.Površina.IzvorAkcije;

/**
 *
 * @author mraguzin
 * Glavni dio za grafički unos grafa, s gumbima itd.
 */
public class ProgramFrame extends JFrame {
    public static final String NASLOV = "Maksimalan nezavisni skup";
    private static final int ŠIRINA = 800;
    private static final int VISINA = 600;
    private static final Color GUMB_AKTIVAN = Color.RED;
    private final Color GUMB_NEAKTIVAN;
    private static final float R = 10.0f;
       
    public enum TipObjave {
        REZULTAT, RESETIRAJ_GUMBE, PROMJENA
    }
    
    public enum Akcija {
        DODAJ_VRH, DODAJ_BRID, DODAJ_KRAJ, BRIŠI, OČISTI, SEQ, PAR, GENERIRAJ_VRHOVE, GENERIRAJ_BRIDOVE, NEMA
        // DODAJ_KRAJ označava stanje u kojem čekamo klik za zadnji kraj brida
    }
    
    private Akcija trenutnaAkcija;
    private final Površina površina;
    private final JPanel panel;
    private JButton aktivan;
    private final JTextField textFieldZaGeneriranje;
    private int brojZaGeneriranje;
    private final List<JButton> gumbi;
    private List<JMenuItem> stavke;
    private JFileChooser chooser;
    
    private boolean postojiPromjena = false;
    private boolean stariGraf = false;
    private int idGrafa = 0;
    private String connectionString = "jdbc:postgresql://java-projekt.postgres.database.azure.com:5432/"
                                    + "postgres?user=francek&password=Password1&sslmode=require";

    public JTextField getTextFieldZaGeneriranje() {
        return textFieldZaGeneriranje;
    }
    public int getBrojZaGeneriranje() {
        return brojZaGeneriranje;
    }

    public void resetirajBrojZaGeneriranje() {
        brojZaGeneriranje = 0;
    }
    
    public ProgramFrame() {
        površina = new Površina(this);
        setTitle(NASLOV);
        
        var gumbVrhovi = new JButton("Dodaj vrhove");
        var gumbBridovi= new JButton("Dodaj bridove");
        var gumbBriši = new JButton("Izbriši");
        var gumbOčisti= new JButton("Očisti sve");
        var gumbSeq = new JButton("Riješi sekvencijalno");
        var gumbPar = new JButton("Riješi paralelno");
        var gumbGenerirajVrhove = new JButton("Generiraj N vrhova");
        var gumbGenerirajBridove = new JButton("Generiraj M bridova");
        gumbi = List.of(gumbVrhovi, gumbBridovi, gumbBriši,
                gumbOčisti, gumbGenerirajVrhove, gumbGenerirajBridove,
                gumbSeq, gumbPar);
        
        GUMB_NEAKTIVAN = gumbPar.getBackground();
        
        panel = new JPanel();
        gumbi.forEach(panel::add);

        panel.add(površina);
        
        gumbVrhovi.addActionListener(new GumbAkcija(Akcija.DODAJ_VRH, gumbVrhovi));
        gumbBridovi.addActionListener(new GumbAkcija(Akcija.DODAJ_BRID, gumbBridovi));
        gumbBriši.addActionListener(new GumbAkcija(Akcija.BRIŠI, gumbBriši));
        gumbOčisti.addActionListener((ActionEvent e) -> {
            površina.obavijesti(Akcija.OČISTI);
            stariGraf = false;
            resetirajGumbe();
        });
        gumbSeq.addActionListener((ActionEvent e) -> {
            resetirajGumbe();
            omogućiGumbe(false);
            omogućiMenije(false);            
            površina.obavijesti(Akcija.SEQ);
        });
        gumbPar.addActionListener((ActionEvent e) -> {
            resetirajGumbe();
            omogućiGumbe(false);            
            omogućiMenije(false);
            površina.obavijesti(Akcija.PAR);
        });
        gumbGenerirajVrhove.addActionListener(new GumbAkcija(Akcija.GENERIRAJ_VRHOVE, gumbGenerirajVrhove));
        gumbGenerirajBridove.addActionListener(new GumbAkcija(Akcija.GENERIRAJ_BRIDOVE, gumbGenerirajBridove));
        gumbVrhovi.setToolTipText("Kliknite na prazno mjesto ploče za postaviti novi vrh");
        gumbBridovi.setToolTipText("Žuti vrh je prvi; kliknite na drugi vrh za spajanje");
        gumbBriši.setToolTipText("Kliknite na vrh ili brid za brisanje");
        gumbOčisti.setToolTipText("Očisti cijelu ploču (izgubit ćete graf!)");
        gumbSeq.setToolTipText("Riješi MIS problem sekvencijalno (sporo)");
        gumbPar.setToolTipText("Riješi MIS problem paralelno (brže za jako velike grafove");
        gumbGenerirajVrhove.setToolTipText("Generiraj N nasumično razmještenih vrhova");
        gumbGenerirajBridove.setToolTipText("Generiraj M bridova između nasumičnih vrhova");

        textFieldZaGeneriranje = new JTextField();
        textFieldZaGeneriranje.setSize(120, 50);
        textFieldZaGeneriranje.setLocation(540, 40);
        textFieldZaGeneriranje.setFont(new Font("Calibri", Font.BOLD, 25));
        textFieldZaGeneriranje.setBackground(new Color(0, 102, 204));
        textFieldZaGeneriranje.setForeground(Color.WHITE);
        textFieldZaGeneriranje.setBorder(new CompoundBorder(
                new LineBorder(Color.BLUE, 2),
                new EmptyBorder(5, 5, 5, 5)));
        textFieldZaGeneriranje.setVisible(false);

        textFieldZaGeneriranje.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char keyChar = e.getKeyChar();
                if (!((keyChar >= '0') && (keyChar <= '9') ||
                        (keyChar == KeyEvent.VK_BACK_SPACE) ||
                        (keyChar == KeyEvent.VK_DELETE) ||
                        (keyChar == KeyEvent.VK_ENTER))) {
                    getToolkit().beep();
                    e.consume();
                }
                if(keyChar == KeyEvent.VK_ENTER) {
                    String textZaGeneriraj = textFieldZaGeneriranje.getText();
                    brojZaGeneriranje = Integer.parseInt(textZaGeneriraj);
                    if(brojZaGeneriranje > 0 && brojZaGeneriranje < 100) {
                        textFieldZaGeneriranje.setVisible(false);
                        textFieldZaGeneriranje.setText("");
                    }
                    else {
                        JOptionPane.showMessageDialog(null, "Broj treba biti manji od 100!");
                    }
                }
            }
        });

        add(textFieldZaGeneriranje);
        
        add(panel);
        menuSetup();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                izlaz();
            }
        });
        
        pack();
    }
    
    private void menuSetup() {
        JMenuBar menu;
        menu = new JMenuBar();
        setJMenuBar(menu);
        var fileMenu = new JMenu("File");
        var editMenu = new JMenu("Edit");
        menu.add(fileMenu);
        menu.add(editMenu);
        stavke = new ArrayList<>();
        
        // File
        var spremiAkcija = new AbstractAction("Spremi u bazu", new ImageIcon("disketa.gif")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                 spremi();
            }
        };
        
        var spremiItem = new JMenuItem(spremiAkcija);
        spremiItem.setAccelerator(KeyStroke.getKeyStroke("ctrl S"));
        
        var učitajAkcija = new AbstractAction("Učitaj iz baze", new ImageIcon("load.gif")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ucitaj();
            }
        };
        
        var učitajGraf = new JMenuItem(učitajAkcija);
        učitajGraf.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        
        var izlazAkcija = new AbstractAction("Izlaz") {
            @Override
            public void actionPerformed(ActionEvent e) {
                izlaz();
            }
        };
        
        stavke.add(fileMenu.add(spremiItem));
        fileMenu.addSeparator();
        stavke.add(fileMenu.add(učitajGraf));
        fileMenu.addSeparator();
        fileMenu.add(izlazAkcija);
        
        // Edit
        var uveziGraf = new JMenuItem("Uvezi graf");
        uveziGraf.setToolTipText("Graf mora biti u Challenge 9 tekstualnom formatu");
        uveziGraf.addActionListener((ActionEvent e) -> {
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
        stavke.add(editMenu.add(uveziGraf));

        površina.undo.setEnabled(false);
        stavke.add(editMenu.add(površina.undo));
        površina.redo.setEnabled(false);
        stavke.add(editMenu.add(površina.redo));
        površina.undo.addActionListener((ActionEvent e) -> površina.undoRedo.izvrsiUndoAkciju());
        površina.redo.addActionListener((ActionEvent e) -> površina.undoRedo.izvrsiRedoAkciju());
    }
    
    private void izlaz() {
        if (postojiPromjena) {
                    int odabir = JOptionPane.showConfirmDialog(null,
   "Postoje izmjene koje nisu spremljene. Želite li ih spremiti prije izlaza?",
   "Prije izlaska", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
                    switch (odabir) {
                        case JOptionPane.YES_OPTION -> spremi();
                        case JOptionPane.NO_OPTION -> {}
                        case JOptionPane.CANCEL_OPTION -> { return;}
                    }
                }
                
                System.exit(0);
    }

    private JFileChooser getChooser() {
        if (chooser == null) {
            chooser = new JFileChooser();
        }
        
        return chooser;
    }
    
    private void ucitaj()
    {
        String url = "jdbc:postgresql://localhost:5432/parallelMSI";
             
        String imeGrafa = JOptionPane.showInputDialog("Kako se zove graf koji želite učitati?");
        if(imeGrafa == null || "".equals(imeGrafa))
            return;
        
        ArrayList<ArrayList<Integer>> listaSusjednosti = new ArrayList<>();
        ArrayList<ArrayList<Float>> koordinateSvih = new ArrayList<>();
        
        if(postojiPromjena) // ako je postojao graf na povrsini prije ucitavanja novog
        {
            int odabir =JOptionPane.showConfirmDialog(null, "Želite li sačuvati postojeći graf prije učitavanja novog?",
                    "Upozorenje!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if(odabir == 0)
                spremi();
        }
        
        površina.očisti(IzvorAkcije.KORISNIK);
        
        try 
        {
            Connection conn = DriverManager.getConnection(connectionString, "francek", "Password1");
            String sqlQuery = "SELECT * FROM grafovi "
                            + "WHERE ime_grafa = '" + imeGrafa + "'";
            
            Statement stm = conn.createStatement();
            ResultSet rs = stm.executeQuery(sqlQuery); // pretrazujemo samo podatke s tocnim imenom grafa
            
            int brojVrhova = 1, id_vrha = 0, brojac = 0;
            
            ArrayList<Integer> listaSusjedaZaJedanVrh = new ArrayList<>();
            ArrayList<Float> koordinateVrha = new ArrayList<>();
            float x_koord, y_koord;
            
            while(rs.next())
            {
                idGrafa = rs.getInt("id_grafa");
                if(rs.getInt("id_vrha") == id_vrha) // naisli smo na redak u bazi za vrh za koji vec znamo id i koordinate
                {                                         // trazimo samo njegovog sljedeceg susjeda
                    if(brojac == 0)                       // brojac sluzi da samo u prvom prolazu uzmemo koordinate nultog vrha
                    {                                       
                        x_koord = rs.getFloat("x_koordinata");
                        y_koord = rs.getFloat("y_koordinata");
                        koordinateVrha.add(x_koord);
                        koordinateVrha.add(y_koord);
                        ArrayList<Float> kopijaKoordinata = new ArrayList<>();
                        koordinateVrha.forEach(element -> kopijaKoordinata.add(element));
                        koordinateSvih.add(kopijaKoordinata);
                        ++brojac;
                    }
                    listaSusjedaZaJedanVrh.add(rs.getInt("id_susjeda"));
                }
                else  // dosli smo do novog vrha                          
                {
                    ArrayList<Integer> kopijaSusjeda = new ArrayList<>();
                    listaSusjedaZaJedanVrh.forEach(element -> kopijaSusjeda.add(element)); // treba napraviti deep copy jer se
                    listaSusjednosti.add(kopijaSusjeda);                                   // objekti kopiraju po referenci
                    
                    listaSusjedaZaJedanVrh.clear(); koordinateVrha.clear();                    
                    ++brojVrhova; ++id_vrha;
                    listaSusjedaZaJedanVrh.add(rs.getInt("id_susjeda"));
                    
                    x_koord = rs.getFloat("x_koordinata");
                    y_koord = rs.getFloat("y_koordinata");
                    koordinateVrha.add(x_koord);
                    koordinateVrha.add(y_koord);
                    ArrayList<Float> kopijaKoordinata = new ArrayList<>();
                    koordinateVrha.forEach(element -> kopijaKoordinata.add(element));      // treba napraviti deep copy
                    koordinateSvih.add(kopijaKoordinata);                    
                }              
            }
            listaSusjednosti.add(listaSusjedaZaJedanVrh);
        }
        catch(SQLException e)
        {
            JOptionPane.showConfirmDialog(null, "Neuspjelo spajanje na bazu", "Neuspjelo!", 
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            System.out.println(e.getMessage());
        }
        // lista_susjednosti mi sada sadrzi broj vrhova i sve bridove
        // koordinate_svih sadrze koordinate tocaka potrebnih za crtanje grafa

        ArrayList<Krug> krugovi = new ArrayList<>();

        for(int i = 0; i < koordinateSvih.size(); ++i)
        {
            float x_koord = koordinateSvih.get(i).get(0), y_koord = koordinateSvih.get(i).get(1);
            Krug krug = new Krug(x_koord, y_koord, R);
            krugovi.add(krug);
            Point2D c = new Point2D.Float(x_koord, y_koord);
            površina.dodajKrug(c, IzvorAkcije.UČITAVANJE);
        }    
        
        površina.namjestiKrugove(krugovi);

        for(int i = 0; i < listaSusjednosti.size(); ++i)
        {
            for(int j = 0; j < listaSusjednosti.get(i).size(); ++j)
            {
                int susjed_od_i = listaSusjednosti.get(i).get(j);
                if(susjed_od_i != -1)
                    površina.dodajSegment(krugovi.get(i), krugovi.get(susjed_od_i), IzvorAkcije.UČITAVANJE); 
            }       
        }
        postojiPromjena = false;
        stariGraf = true;
    }
    
    private void spremi()
    {
        Graf graf = površina.dajGraf();
        if(!postojiPromjena || graf.dajBrojVrhova() == 0)
            return;
        
        String imeGrafa = "", sqlQuery = "", sqlQuery1;
        String url = "jdbc:postgresql://localhost:5432/parallelMSI";
        try(Connection conn = DriverManager.getConnection(connectionString, "francek", "Password1"))
        {
            if(stariGraf == false) // ako spremamo graf prvi puta, dajemo novi id, za jedan veci od prijasnjeg najveceg
            {
                sqlQuery = "SELECT id_grafa FROM grafovi "
                        + "ORDER BY id_grafa DESC "
                        + "LIMIT 1";
                Statement stm = conn.createStatement();
                ResultSet rs = stm.executeQuery(sqlQuery);
                if(rs.next())
                    idGrafa = rs.getInt("id_grafa") + 1;
                
                imeGrafa = JOptionPane.showInputDialog("Unesite ime grafa"); // dajemo ime novom grafu
                if(imeGrafa == null || "".equals(imeGrafa))
                    return;
            }
            else 
            {
                // moramo zapamtiti id i ime grafa ucitanog iz baze                
                sqlQuery = "SELECT ime_grafa FROM grafovi "
                         + "WHERE id_grafa = " + idGrafa 
                         + " LIMIT 1";
                Statement stm = conn.createStatement();
                ResultSet rs = stm.executeQuery(sqlQuery);
                if(rs.next())
                    imeGrafa = rs.getString("ime_grafa");
                // TODO: treba izbrisati sve prijašnje zapise o tom grafu iz baze jer ćemo ih kasnije ponovno popuniti
                
                sqlQuery1 = "DELETE FROM grafovi "
                         + "WHERE id_grafa = " + idGrafa;
                PreparedStatement ps = conn.prepareStatement(sqlQuery1);
                ps.executeUpdate();
            }
            
            PreparedStatement st = conn.prepareStatement("insert into grafovi"
                            + "(id_grafa, ime_grafa, id_vrha, id_susjeda, x_koordinata, y_koordinata)"
                            + "VALUES(?,?,?,?,?,?)");
                     
            for(int i = 0; i < graf.dajBrojVrhova(); ++i)
            {               
                if(graf.dajListuSusjednosti().get(i).isEmpty())
                {
                    st.setInt(1, idGrafa);
                    st.setString(2,imeGrafa);
                    st.setInt(3, i);
                    st.setInt(4, -1); // svi su nam vrhovi id-a 0,1,... pa id susjeda = -1 oznacava da vrh nema susjeda
                    st.setDouble(5, površina.dajKrugove().get(i).dajX());
                    st.setDouble(6, površina.dajKrugove().get(i).dajY());
                    st.executeUpdate();
                }
                for(int j = 0; j < graf.dajListuSusjednosti().get(i).size(); ++j)
                {                   
                    st.setInt(1, idGrafa);
                    st.setString(2, imeGrafa);
                    st.setInt(3, i);
                    st.setInt(4, graf.dajListuSusjednosti().get(i).get(j));
                    st.setDouble(5, površina.dajKrugove().get(i).dajX());
                    st.setDouble(6, površina.dajKrugove().get(i).dajY());
                    st.executeUpdate();
                }
            }
            JOptionPane.showConfirmDialog(null, "Graf uspješno spremljen u bazu",
                                        "USPJEH", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            postojiPromjena = false;
        }
        catch(SQLException e) {
            JOptionPane.showConfirmDialog(null, "Neuspjelo spajanje na bazu", "Neuspjelo!", 
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            System.out.println(e.getMessage());
        }
    }
    
    private void omogućiMenije(boolean b) {
        for (var stavka : stavke)
            stavka.setEnabled(b);
    }
    
    private void omogućiGumbe(boolean b) {
        for (var gumb : gumbi)
            gumb.setEnabled(b);
    }
    
    private void objaviSnimanje() {
        postojiPromjena = false;
        setTitle(ProgramFrame.NASLOV);
    }
    
    public void objavi(TipObjave objava, Collection<Integer> rezultat) {
        switch (objava) {
            case RESETIRAJ_GUMBE -> resetirajGumbe();
            case REZULTAT -> objaviRezultat(rezultat);
            case PROMJENA -> objaviPromjenu();
        }
    }
    
    private void objaviPromjenu() {
        postojiPromjena = true;
        setTitle("* " + ProgramFrame.NASLOV);
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(ŠIRINA, VISINA);
    }
    
    private void objaviRezultat(Collection<Integer> rezultat) {
        omogućiGumbe(true);
        omogućiMenije(true);
        
        if (rezultat == null) {
            JOptionPane.showMessageDialog(null, "Niste nacrtali graf!",
                    "Greška", JOptionPane.ERROR_MESSAGE);
        }
        
        // TODO: štopanje, neke dodatne poruke...?
    }
    
    private void resetirajGumbe() {
        aktivan = null;
        gumbi.forEach((gumb) -> gumb.setBackground(GUMB_NEAKTIVAN));
        textFieldZaGeneriranje.setVisible(false);
    }

    private class GumbAkcija implements ActionListener {
        private final Akcija akcija;
        private final JButton gumb;
        
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
