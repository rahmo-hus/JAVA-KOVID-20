package org.unibl.etf.residents;

import java.util.Calendar;

public class Elder extends Resident{

    public static final int UPPER_BOUND = 100;
    public static final int LOWER_BOUND = 65;
    public static final int MAX_DISTANCE=3;

    public Elder(int cityDimension) {
        super(Calendar.getInstance().get(Calendar.YEAR) + LOWER_BOUND - random.nextInt(UPPER_BOUND -LOWER_BOUND), cityDimension);
        setMaxDistance(MAX_DISTANCE);
    }
    @Override
    public String toString() {
        return this.getClass().getSimpleName().charAt(0)+super.toString();
    }
}
