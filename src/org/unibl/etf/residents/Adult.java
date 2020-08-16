package org.unibl.etf.residents;

import java.util.Calendar;
import java.util.Random;

public class Adult extends Resident {
    public static final int UPPER_BOUND = 65;
    public static final int LOWER_BOUND = 18;
    public static final double RADIUS_PERCENTAGE=0.25;
    public Adult(int cityDimension){
        super(Calendar.getInstance().get(Calendar.YEAR) + LOWER_BOUND- random.nextInt(UPPER_BOUND - LOWER_BOUND), cityDimension);
        setMaxDistance((int)Math.round((double)cityDimension*RADIUS_PERCENTAGE));
    }
    @Override
    public String toString() {
        return this.getClass().getSimpleName().charAt(0)+super.toString();
    }
}
