package org.unibl.etf.institutions;

import org.unibl.etf.residents.Resident;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedDeque;

public class House implements Serializable {
    private Integer id;
    private ConcurrentLinkedDeque<Resident> residents;
    private Integer xCoordinate, yCoordinate;

    public void setCoordinates(Integer x, Integer y){
        xCoordinate=x;
        yCoordinate=y;
        residents.forEach(resident ->
        {
            resident.setCoordinates(x,y);
            resident.setLimits();
        });
    }
    public ConcurrentLinkedDeque<Resident> getResidents(){ return residents;}
    public Integer getXCoordinate(){
        return xCoordinate;
    }
    public Integer getYCoordinate(){
        return yCoordinate;
    }
    public House(Integer id) {
        this.id=id;
        residents = new ConcurrentLinkedDeque<>();
    }

    public Integer getId(){
        return id;
    }
    public void addResidents(Resident resident){
        resident.setHouseID(id);
        residents.push(resident);
    }

    public void setId(Integer id){
        this.id=id;
    }
    public String houseInfo(){
        String info= "Kuća "+id;
        String members=" ";
        for(Resident resident:residents) {
            members += resident + ",";
        }
        return info+ (members.length() != 0 ? members.substring(0, members.length()-1) : "");

    }

    @Override
    public String toString() {
        return "H"+id;
    }
}
