package org.unibl.etf.institutions;

import org.unibl.etf.residents.Resident;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Checkpoint implements Serializable {
    private static int count=1;
    private int ID;
    private static ConcurrentLinkedDeque<Integer> spottedIDs = new ConcurrentLinkedDeque<>();
    private Integer xCoordinate, yCoordinate;
    public Checkpoint(){
        ID=count++;
    }
    public void setCoordinates(Integer xCoordinate, Integer yCoordinate){
        this.xCoordinate=xCoordinate;
        this.yCoordinate=yCoordinate;
    }
    public Integer getXCoordinate(){
        return xCoordinate;
    }
    public Integer getYCoordinate(){
        return yCoordinate;
    }
    @Override
    public String toString() {
        return "CP"+ID;
    }

    public Resident watch(List<Resident> residents){
        for (Resident resident : residents) {
            if (Math.abs(resident.getXCoordinate() - xCoordinate) < 2 && Math.abs(resident.getYCoordinate() - yCoordinate) < 2)
                if (isInfected(resident) && !spottedIDs.contains(resident.getHouseID())) {
                    resident.setStopMoving();
                    spottedIDs.add(resident.getHouseID());
                    return resident;
                }
        }
        return null;
    }

    private boolean isInfected(Resident resident){
        return resident.getBodyTemperature()>37.00;
    }

}
