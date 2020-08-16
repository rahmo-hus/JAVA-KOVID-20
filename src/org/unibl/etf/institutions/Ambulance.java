package org.unibl.etf.institutions;

import org.unibl.etf.features.GenericLogger;
import org.unibl.etf.features.NoAmbulanceVehiclesException;
import org.unibl.etf.features.TheresNoRoomException;
import org.unibl.etf.residents.Resident;
import org.unibl.etf.surveilance.AmbulanceVehicle;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Ambulance implements Serializable {
    public static final int RECOVERY_PERIOD = 30000;
    private static int ID=1, globalCapacity=0;
    private int ambulanceID;
    private ConcurrentLinkedDeque<AmbulanceVehicle> vehicles;
    private ConcurrentLinkedDeque<Resident> infectedResidents;
    private Integer xCoordinate, yCoordinate;
    private int capacity;
    public static boolean pause=false;
    private static Random random = new Random();

    static
    {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("podaci.txt"));
            bufferedWriter.write("0");
            bufferedWriter.close();
        }
        catch (IOException ioException){
            GenericLogger.log(Ambulance.class, ioException);
        }
    }

    public Ambulance(int allResidentsCount){
        ambulanceID=ID++;
        vehicles=new ConcurrentLinkedDeque<>();
        capacity= Math.round((float)allResidentsCount*((float)(10+random.nextInt(6))/100));
        globalCapacity+=capacity;
        infectedResidents=new ConcurrentLinkedDeque<>();
    }
    public void setCoordinates(int xCoordinate, int yCoordinate){
        this.xCoordinate=xCoordinate;
        this.yCoordinate=yCoordinate;
    }
    public Integer getXCoordinate(){
        return xCoordinate;
    }
    public Integer getYCoordinate(){
        return yCoordinate;
    }
    public Integer getRemainingCapacity(){
        return capacity-infectedResidents.size();
    }
    public Integer getRemainingGlobalCapacity(){
        return globalCapacity;
    }
    public Integer getNumberOfVehicles(){
        return vehicles.size();
    }

    public Integer getAmbulanceID(){
        return ambulanceID;
    }
    public void addVehicle(AmbulanceVehicle vehicle){
        vehicles.push(vehicle);
    }

    public AmbulanceVehicle getVehicle() throws NoAmbulanceVehiclesException {
        if(!vehicles.isEmpty())
            return vehicles.pop();
        else
            throw new NoAmbulanceVehiclesException();

    }

    public void addInfectedResident(Resident resident) throws TheresNoRoomException{
        if(infectedResidents.size()<capacity) {
            infectedResidents.push(resident);
            globalCapacity--;
            try {
                String value = Files.readString(Paths.get("./podaci.txt"));
                Integer infectedValue = Integer.parseInt(value);
                Files.writeString(Paths.get("./podaci.txt"), (++infectedValue).toString());
            }
            catch (IOException ioException){
                GenericLogger.log(Ambulance.class, ioException);
            }
            new Thread(()->startRecoveryProcess(resident)).start();
        }
        else
            throw new TheresNoRoomException("Nema mjesta");
    }
    private void startRecoveryProcess(Resident resident){
        Queue<Double> temperatures = new LinkedList<>();
        boolean recovered=false;
        while(!recovered){
            while(pause) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    GenericLogger.log(Ambulance.class, e);
                }
            }
            temperatures.add(resident.getBodyTemperature().doubleValue());
            if(temperatures.size()==3){
                DoubleSummaryStatistics summaryStatistics = temperatures
                        .stream()
                        .mapToDouble(x->x)
                        .summaryStatistics();
                if(summaryStatistics.getAverage() < 37.00){
                    resident.setIsRecovered();
                    resident.setInfected(false);
                    recovered=true;
                    infectedResidents.remove(resident);
                    try {
                        String value = Files.readString(Paths.get("./podaci.txt"));
                        Integer infectedValue = Integer.parseInt(value);
                        Files.writeString(Paths.get("./podaci.txt"), (--infectedValue).toString());
                    }
                    catch (IOException ioException){
                        GenericLogger.log(Ambulance.class, ioException);
                    }
                }
                else
                    temperatures.poll();
            }
            try {
                Thread.sleep(RECOVERY_PERIOD);
            } catch (InterruptedException e) {
                GenericLogger.log(Ambulance.class, e);
            }
        }

    }
    public void printVehicles(){
        vehicles.forEach(vehicle -> System.out.println("    "+vehicle));
    }
    public String getAmbulanceInfo(){
        String info="";
        if(!vehicles.isEmpty()) {
            info += "Vozila: ";
            for(var vehicle: vehicles)
                info+=vehicle.toString()+",";
            info=info.substring(0, info.length()-1);
        }
        else
            info="Nema vozila ";
        info+=" Kapacitet: "+capacity+" Popunjeno: "+(infectedResidents.size());
        if(!infectedResidents.isEmpty()){
            info+=" Pacijenti: ";
            for(var patient: infectedResidents)
                info+=patient.toString()+" ";
        }
        return info;


    }
    @Override
    public String toString() {
        return "A"+ambulanceID;
    }
}
