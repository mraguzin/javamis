package parallelmis;

/**
 *
 * @author mraguzin
 */
public class Algoritam1v1 extends Algoritam1 { // TODO: ovo po mjerenjima (vidi Primjer3)
    // ispada prosječno najbrža varijanta, pa bi se isplatilo napraviti neke
    // dodatne optimizacije. Nakon nje, najbrža je uglavnom v3.
    // Ipak, sve paralelne impl. uvijek ispadnu sporije od sekvencijalne,
    // pa bi trebali koristiti neke još veće grafove da vidimo kada se zaista
    // paralelnost isplati i koja implementacija konzistentno pobjeđuje.
    
    // Moguće optimizacije: 
    // * paralelizirati particioniranje i računanje vjerojatnosti
    // pomoću dodatne, nulte barijere koja se samo jedanput prođe za sve dretve.
    // * paralelizirati b2kraj() tako da sve dretve paralelno ubacuju u I
    
    public Algoritam1v1(Graf graf, int brojDretvi) {
        super(graf, brojDretvi);
    }
    
    public Algoritam1v1(Graf graf) {
        super(graf);
    }
    
}
