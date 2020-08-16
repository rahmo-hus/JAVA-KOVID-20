package org.unibl.etf.surveilance;

public class Alarm {
    public Integer xCoordinate, yCoordinate, houseID;
    public Alarm(int xCoordinate, int yCoordinate, int houseID){
        this.xCoordinate=xCoordinate;
        this.yCoordinate=yCoordinate;
        this.houseID=houseID;
    }
}
