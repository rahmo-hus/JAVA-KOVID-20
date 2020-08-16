package org.unibl.etf.features;


public class NoAmbulanceVehiclesException extends Exception {
    public NoAmbulanceVehiclesException(){
        super("Nema vozila na raspolaganju!");
    }
}
