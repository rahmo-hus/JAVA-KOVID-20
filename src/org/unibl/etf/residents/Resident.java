package org.unibl.etf.residents;

import org.unibl.etf.features.GenericLogger;
import org.unibl.etf.institutions.Ambulance;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

public class Resident implements Serializable {
    private static Integer idCounter = 0;
    private Integer yearOfBirth, houseID, uniqueID, xCoordinate, yCoordinate;
    private String firstName, lastName;
    private char sex;
    private Double bodyTemperature;
    public Integer upLimit, downLimit, leftLimit, rightLimit, maxDistance, cityDimension;
    protected static Random random = new Random();
    private boolean canMove, isInfected, isRecovered;

    public static final int PERIOD_SECONDS=30000;

    public Resident(Integer yearOfBirth, int cityDimension) {
        uniqueID = idCounter++;
        this.yearOfBirth = yearOfBirth;
        this.firstName = generateRandomString();
        this.lastName = generateRandomString();
        this.sex = generateSex();
        this.cityDimension = cityDimension;
        canMove = true;
        isInfected = isRecovered = false;
        new Thread(() -> {
            while (true) {
                bodyTemperature = generateBodyTemperature();
                try {
                    Thread.sleep(PERIOD_SECONDS);
                } catch (InterruptedException e) {
                    GenericLogger.log(Resident.class, e);
                }
            }
        }).start();
    }

    public char getSex() {
        return sex;
    }

    public void setStopMoving() {
        canMove = false;
    }

    public void setCanMove() {
        canMove = true;
    }
    public boolean getCanMove() {
        return canMove;
    }

    public Double getBodyTemperature() {
        return bodyTemperature;
    }

    public void setInfected(boolean isInfected) {
        this.isInfected = isInfected;
    }

    public boolean getIsInfected() {
        return isInfected;
    }

    public void setIsRecovered() {
        isRecovered = true;
    }

    public boolean getIsRecovered() {
        return isRecovered;
    }

    public void setMaxDistance(int maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void setLimits() {
        if (maxDistance != -1) {
            leftLimit = yCoordinate < maxDistance ? yCoordinate : cityDimension - 1 - yCoordinate < maxDistance ?
                    2 * maxDistance - cityDimension + 1 + yCoordinate : maxDistance;
            rightLimit = cityDimension - 1 - yCoordinate < maxDistance ? cityDimension - 1 - yCoordinate : yCoordinate < maxDistance ?
                    2 * maxDistance - yCoordinate : maxDistance;
            upLimit = xCoordinate < maxDistance ? xCoordinate : cityDimension - 1 - xCoordinate < maxDistance ?
                    2 * maxDistance - cityDimension + 1 + xCoordinate : maxDistance;
            downLimit = cityDimension - 1 - xCoordinate < maxDistance ? cityDimension - 1 - xCoordinate : xCoordinate < maxDistance ?
                    2 * maxDistance - xCoordinate : maxDistance;
        } else //odnosi se na djecu
        {
            leftLimit = yCoordinate;
            rightLimit = cityDimension - yCoordinate - 1;
            upLimit = xCoordinate;
            downLimit = cityDimension - xCoordinate - 1;
        }
    }

    public void setCoordinates(int x, int y) {
        xCoordinate = x;
        yCoordinate = y;
    }

    public Integer getXCoordinate() {
        return xCoordinate;
    }

    public Integer getYCoordinate() {
        return yCoordinate;
    }

    private char generateSex() {
        Random random = new Random();
        return random.nextInt(2) == 1 ? 'm' : 'f';
    }

    public void setHouseID(Integer houseID) {
        this.houseID = houseID;
    }

    public Integer getHouseID() {
        return houseID;
    }

    private static String generateRandomString() {
        Random random = new Random();
        return random.ints(97, 123)
                .limit(5)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private Double generateBodyTemperature() {
        Random random = new Random();
        int integerPart = 35 + random.nextInt(4);
        double decimalPart = random.nextDouble();
        return (double) integerPart + decimalPart;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Resident) && obj != null)
            return false;
        if (((Resident) obj).uniqueID == this.uniqueID)
            return true;
        return false;
    }

    @Override
    public String toString() {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        return uniqueID + " " + decimalFormat.format(bodyTemperature) + " (H" + houseID + ")";//+"("+xCoordinate+","+yCoordinate+") leftL:"+leftLimit+" rightLimit:"+
        //rightLimit+" upLimit:"+upLimit+" downLimit:"+downLimit;
    }
}
