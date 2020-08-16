package org.unibl.etf.residents;

import java.util.Calendar;

public class Kid extends Resident{

    public static final int UPPER_BOUND = 18;
    public static final int MAX_DISTANCE = -1;

    public Kid(int cityDimension) {
        super(Calendar.getInstance().get(Calendar.YEAR) - random.nextInt(UPPER_BOUND), cityDimension);
        setMaxDistance(MAX_DISTANCE);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName().charAt(0)+super.toString();
    }
}
