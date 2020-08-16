package org.unibl.etf.surveilance;

import org.unibl.etf.residents.Resident;

import java.io.Serializable;
import java.util.Stack;

public class AmbulanceVehicle implements Serializable {
    private static int ID=1;
    private int vehicleID;
    private Integer xCoordinate, yCoordinate;
    private Stack<Resident> infectedResident;

    public AmbulanceVehicle(){
        vehicleID=ID++;
    }
    public Integer getVehicleID(){
        return vehicleID;
    }
    public void setCoordinates(int xCoordinate, int yCoordinate){
        infectedResident=new Stack<>();
        this.xCoordinate=xCoordinate;
        this.yCoordinate=yCoordinate;
    }
    public Integer getXCoordinate(){
        return xCoordinate;
    }
    public Integer getYCoordinate(){
        return yCoordinate;
    }
    public void boardInfectedResident(Resident resident){
        infectedResident.push(resident);
    }
    public Resident dischargeInfectedResident(){
        return infectedResident.pop();
    }
    @Override
    public String toString() {
        return "V"+vehicleID;
    }
}
