package org.unibl.etf.surveilance;

import java.util.concurrent.ConcurrentLinkedDeque;

public class SurveillanceSystem{

    private static ConcurrentLinkedDeque<Alarm> alarms=new ConcurrentLinkedDeque<>();

    public static void addAlarm(int xCoordinate, int yCoordinate, int houseID){
        alarms.push(new Alarm(xCoordinate, yCoordinate, houseID));
    }
    public static Alarm getAlarm(){
        return alarms.poll();
    }
}
