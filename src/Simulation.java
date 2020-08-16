import org.unibl.etf.features.GenericLogger;
import org.unibl.etf.features.NoAmbulanceVehiclesException;
import org.unibl.etf.features.TheresNoRoomException;
import org.unibl.etf.institutions.Ambulance;
import org.unibl.etf.institutions.Checkpoint;
import org.unibl.etf.features.Tuple;
import org.unibl.etf.institutions.House;
import org.unibl.etf.residents.*;
import org.unibl.etf.surveilance.Alarm;
import org.unibl.etf.surveilance.AmbulanceVehicle;
import org.unibl.etf.surveilance.SurveillanceSystem;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import static java.nio.file.StandardWatchEventKinds.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Simulation extends JFrame implements Serializable {
    public JPanel mainPanel;
    private JLabel infectedCountField;
    private JLabel recoveredCountField;
    private JTable matrix;
    private JButton enableMovementButton;
    private JButton checkAmbulanceSituationButton;
    private JButton sendAmbulanceVehicleButton;
    private JButton viewStatisticsButton;
    private JButton stopSimulationButton;
    private JPanel infoPanel;
    private JLabel residentInfoLabel;
    private JButton rerunSimulationButton;
    private JButton endSimulationButton;
    private JScrollPane infoScrollPane;
    private Integer kidsCount, adultCount, elderCount, houseCount, checkpointCount, ambulanceVehicleCount, ambulanceCount;
    private long startTime;
    private Object[][] city;
    private static House[] houses;
    private AmbulanceVehicle[] ambulanceVehicles;
    private static Random random = new Random();
    private Resident[] kids, adults, elders;
    private static List<Resident> allResidents;
    public static Integer cityDimension=15+random.nextInt(16);
    private Checkpoint[] checkpoints;
    private static List<Ambulance> ambulances;
    private static final int MINIMUM_DISTANCE = 3, AMBULANCE_COUNT=4;
    private final int WAIT_TIME=1000, REFRESH_RATE=500;
    private Stack<Resident> infectedResidents = new Stack<>();
    public boolean execute = true, pause=false, initiatedMovement=false;
    private static List<Resident> recoveredResidents = new LinkedList<>();
    private static Hashtable<Resident, String> residentMovement = new Hashtable<>();
    private static Hashtable<Resident, Tuple<Integer, Integer>> residentCoordinates=new Hashtable<>();
    private static Hashtable<Resident, Thread> movementManipulator = new Hashtable<>();
    private static Hashtable<House,Tuple<Integer,Integer>> houseCoordinates = new Hashtable<>();
    private static Hashtable<AmbulanceVehicle, Tuple<Integer,Integer>> vehicleCoordinates = new Hashtable<>();
    private static Integer infectedCount=0, recoveredCount=0;
    public Simulation(Integer kidsCount, Integer adultCount,Integer elderCount,Integer houseCount,
                      Integer checkpointCount,Integer ambulanceVehicleCount){
        super("Simulacija");
        setContentPane(mainPanel);
        setVisible(true);
        pack();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        startTime = System.currentTimeMillis();
        this.kidsCount = kidsCount;
        this.adultCount = adultCount;
        this.elderCount = elderCount;
        this.houseCount = houseCount;
        this.checkpointCount = checkpointCount;
        this.ambulanceVehicleCount = ambulanceVehicleCount;
        this.ambulanceCount=AMBULANCE_COUNT;
        infectedCountField.setText("Zaraženi: 0");
        recoveredCountField.setText("Oporavljeni: 0");
        createHousesAndDistributeResidents();
        displayMatrix();
        createUIComponents();
        new Thread(()->startWatching()).start();
    }
    private void startWatching(){
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path dir = Paths.get(".");
            dir.register(watchService, ENTRY_MODIFY);
            while(true){
                WatchKey key = watchService.take();
                key.pollEvents().forEach(event->{
                    var kind = event.kind();
                    if(kind!=OVERFLOW){
                        WatchEvent<Path> ev = (WatchEvent<Path>)event;
                        Path fileName = ev.context();
                        try {
                            if (fileName.getFileName().toString().trim().equals("podaci.txt")) {
                                Integer temp = Integer.parseInt(Files.readString(fileName));
                                if (temp < infectedCount) {
                                    recoveredCount+=(infectedCount-temp);
                                    recoveredCountField.setText("Oporavljeni: " + recoveredCount);
                                    try {
                                        Resident recoveredResident = allResidents.stream().filter(r -> r.getIsRecovered() && !recoveredResidents.contains(r)).findAny().get();
                                        recoveredResidents.add(recoveredResident);
                                        var localTemp = new Tuple<Integer, Integer>();
                                        localTemp.setItems(recoveredResident.getXCoordinate(), recoveredResident.getYCoordinate());
                                        residentCoordinates.put(recoveredResident, localTemp);
                                        recoveredResident.setIsRecovered();
                                        JOptionPane.showMessageDialog(this, "Stanovnik "+recoveredResident.toString()+" se oporavio i vraca se kuci", "Obavijest", JOptionPane.INFORMATION_MESSAGE);
                                        sendHome(recoveredResident, false);
                                    }
                                    catch (NoSuchElementException noSuchElementException){
                                        Logger.getLogger(Simulation.class.getName()).log(Level.OFF, noSuchElementException.fillInStackTrace().toString());
                                    }
                                }
                                infectedCountField.setText("Zaraženi: " + temp);
                                infectedCount=temp;
                            }
                        }
                        catch (IOException | NumberFormatException e){
                            GenericLogger.log(Simulation.class, e);
                        }
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        return;
                    }
                });
            }
        }
        catch (IOException | InterruptedException ioException){
            GenericLogger.log(Simulation.class, ioException);
        }

    }
    public void startMovement(){
        new Thread(() -> {
            while(execute) {
                try {
                    while (pause) {
                        Thread.sleep(WAIT_TIME);
                    }
                    updateCityMatrix();
                    Thread.sleep(REFRESH_RATE);
                }
                catch (InterruptedException e){
                    GenericLogger.log(Simulation.class, e);                }
            }
        }).start();
        for (House house : houses) {
            var residents = house.getResidents();
            while(residents.size() != 0) {
                var resident = residents.pop();
                movementManipulator.put(resident, new Thread(() -> moveResident(resident, house.getXCoordinate(), house.getYCoordinate())));
                movementManipulator.get(resident).start();
            }
        }
        for(Checkpoint checkpoint: checkpoints){
            new Thread(()->{
                while(execute) {
                    while(pause){
                        try {
                            Thread.sleep(WAIT_TIME);
                        } catch (InterruptedException e) {
                            GenericLogger.log(Simulation.class, e);                        }
                    }
                    var infectedOrNull=checkpoint.watch(Collections.list(residentCoordinates.keys()));
                    if(infectedOrNull!=null){
                        if(!infectedResidents.contains(infectedOrNull)) {
                            JOptionPane.showMessageDialog(this, infectedOrNull.toString()+"("+infectedOrNull.getXCoordinate()+","+infectedOrNull.getYCoordinate()+")", "Pošalji ambulantu", JOptionPane.WARNING_MESSAGE);
                            SurveillanceSystem.addAlarm(infectedOrNull.getXCoordinate(),infectedOrNull.getYCoordinate(), infectedOrNull.getHouseID());
                            infectedResidents.push(infectedOrNull);
                            infectedOrNull.setInfected(true);
                            sendHome(infectedOrNull, true);
                        }
                    }
                    try {
                        Thread.sleep(REFRESH_RATE);
                    } catch (InterruptedException e) {
                        GenericLogger.log(Simulation.class, e);                    }
                }
            }).start();
        }

    }
    private void sendHome(Resident infectedResident, boolean everyone){ //sends home all residents except infected one
        House house = getHouseByID(infectedResident.getHouseID());
        List<Resident> residentsToReturnHome = everyone ? allResidents.stream().filter(r-> r.getHouseID().equals(house.getId()) && !r.equals(infectedResident)).collect(Collectors.toList()) : Arrays.asList(infectedResident);
        residentsToReturnHome.forEach(resident -> {
            resident.setStopMoving();
            new Thread(()->{
                try {
                    Thread.sleep(WAIT_TIME+REFRESH_RATE);
                } catch (InterruptedException e) {
                    GenericLogger.log(Simulation.class, e);                }
                resident.setCanMove();
                var movement = new Tuple<Integer,Integer>();
                while(!(Math.abs(resident.getXCoordinate() - house.getXCoordinate())<=1 && Math.abs(resident.getYCoordinate()-house.getYCoordinate())<=1) && execute){
                    while(pause) {
                        try {
                            Thread.sleep(WAIT_TIME);
                        } catch (InterruptedException e) {
                            GenericLogger.log(Simulation.class, e);                        }
                    }
                    movement.setItems(Integer.compare(0, resident.getXCoordinate() - house.getXCoordinate()), Integer.compare(0, resident.getYCoordinate() - house.getYCoordinate()));
                    houseCoordinates.forEach((localHouse, coordinate)->{
                        if((resident.getXCoordinate()+movement.getItem1())==coordinate.getItem1() && (resident.getYCoordinate()+movement.getItem2())==coordinate.getItem2() && !resident.getHouseID().equals(localHouse.getId())){
                            if(movement.getItem1()!=0 && movement.getItem2()!=0)
                                movement.setItem2(0);
                            else if(movement.getItem1()!=0 && movement.getItem2()==0)
                                movement.setItem2(1);
                            else
                                movement.setItem1(1);
                        }
                    });
                    resident.setCoordinates(resident.getXCoordinate()+movement.getItem1(), resident.getYCoordinate()+movement.getItem2());
                    var tempCoo = new Tuple<Integer, Integer>();
                    tempCoo.setItems(resident.getXCoordinate(), resident.getYCoordinate());
                    residentCoordinates.put(resident, tempCoo);
                    residentMovement.put(resident,"kuci");
                    try {
                        Thread.sleep(WAIT_TIME);
                    } catch (InterruptedException e) {
                        GenericLogger.log(Simulation.class, e);                    }

                }
                house.addResidents(resident);
                residentCoordinates.remove(resident);
            }).start();
        });


    }
    private House getHouseByID(Integer id){
        return Arrays.stream(houses).filter(house-> house.getId().equals(id)).findFirst().orElse(null);
    }
    private static boolean check(Resident resident, int x, int y){
        var otherResidents = allResidents.stream().filter(r->!r.equals(resident)).collect(Collectors.toList());
        return otherResidents.stream().anyMatch(r-> !r.getHouseID().equals(resident.getHouseID()) || r instanceof Kid ?
                Math.abs(resident.getXCoordinate()+x-r.getXCoordinate())<MINIMUM_DISTANCE &&
                        Math.abs(resident.getYCoordinate()+y-r.getYCoordinate())< MINIMUM_DISTANCE :
                Math.abs(resident.getXCoordinate()+x-r.getXCoordinate())<MINIMUM_DISTANCE -1 &&
                        Math.abs(resident.getYCoordinate()+y-r.getYCoordinate())< MINIMUM_DISTANCE-1 );
    }
    private static boolean isViolated(Resident resident, Tuple<Integer, Integer> movement){
        var otherResidents = allResidents.stream().filter(r->!r.equals(resident)).collect(Collectors.toList());
        for(Resident r: otherResidents)
            for(House house: houses){
                if(r.getXCoordinate().equals(house.getXCoordinate()) && r.getYCoordinate().equals(house.getYCoordinate()))
                    return false;
            }

        boolean violated = check(resident, movement.getItem1(), movement.getItem2());
        int count=0;
        if(violated){
            for(int i=-1; i<2; i++)
                for (int j=-1; j<2; j++)
                    if(check(resident, i, j))
                        count++;
        }
        return count != 9 && violated;
    }
    private void moveResident(Resident resident, Integer houseXCoordinate, Integer houseYCoordinate){
        var movement = new Tuple<Integer, Integer>();
        movement.setItems(1 - random.nextInt(3), 1 - random.nextInt(3));
        while(execute) {
                while (pause) {
                    try {
                        Thread.sleep(WAIT_TIME);
                    } catch (InterruptedException e) {
                        GenericLogger.log(Simulation.class, e);
                    }
                }
                if(!resident.getCanMove()){
                    break;
                }
                while (((movement.getItem1() == 0 && movement.getItem2() == 0) ||//you can't stay in place
                        !(movement.getItem1() + resident.getXCoordinate() < cityDimension && movement.getItem1() + resident.getXCoordinate() >= 0 &&
                                movement.getItem2() + resident.getYCoordinate() < cityDimension && movement.getItem2() + resident.getYCoordinate() >= 0) ||
                                city[movement.getItem1() + resident.getXCoordinate()][movement.getItem2() + resident.getYCoordinate()] != null
                        || // now we are watching movement limits
                        (movement.getItem2() + resident.getYCoordinate() - houseYCoordinate > resident.rightLimit ||
                                houseYCoordinate - movement.getItem2() - resident.getYCoordinate() > resident.leftLimit ||
                                houseXCoordinate - movement.getItem1() - resident.getXCoordinate() > resident.upLimit ||
                                movement.getItem1() + resident.getXCoordinate() - houseXCoordinate > resident.downLimit))
                     || isViolated(resident, movement)){
                    movement.setItems(1 - random.nextInt(3), 1 - random.nextInt(3));
                }
            resident.setCoordinates(resident.getXCoordinate()+movement.getItem1(),
                    resident.getYCoordinate()+movement.getItem2());
            Tuple<Integer, Integer> temp = new Tuple<>();
            temp.setItems(resident.getXCoordinate(), resident.getYCoordinate());
            residentCoordinates.put(resident, temp);
            var movementInfo = new Tuple<String, Tuple<Integer,Integer>>();
            movementInfo.setItems(resident.toString(), movement);
            residentMovement.put(resident, getDirection(movement)); //hashtable of movements
            try {
                Thread.sleep(WAIT_TIME);
            } catch (InterruptedException e) {
                GenericLogger.log(Simulation.class, e);            }
        }

    }
    private void createHousesAndDistributeResidents(){
        houses = new House[houseCount];
        kids = new Kid[kidsCount];
        elders = new Elder[elderCount];
        adults = new Adult[adultCount];
        ambulances = new LinkedList<Ambulance>();
        checkpoints = new Checkpoint[checkpointCount];
        ambulanceVehicles=new AmbulanceVehicle[ambulanceVehicleCount];
        for(int i=0; i<kidsCount; ++i)
            kids[i] = new Kid(cityDimension);
        for(int i=0; i<adultCount; ++i)
            adults[i] = new Adult(cityDimension);
        for(int i=0; i<elderCount; ++i)
            elders[i] = new Elder(cityDimension);
        for(int i=0; i<houseCount; ++i)
            houses[i]=new House(i+1);
        for(int i =0; i<checkpointCount; ++i)
            checkpoints[i] = new Checkpoint();
        for(int i = 0; i< ambulanceCount; ++i)
            ambulances.add(new Ambulance(kidsCount+elderCount+adultCount));
        for(int i=0; i<ambulanceVehicleCount; ++i)
            ambulanceVehicles[i]= new AmbulanceVehicle();
        ambulances.get(0).setCoordinates(0,0);
        ambulances.get(1).setCoordinates(0,cityDimension-1);
        ambulances.get(2).setCoordinates(cityDimension-1, 0);
        ambulances.get(3).setCoordinates(cityDimension-1,cityDimension-1);
        distributeAmbulanceVehicles();
        Stack<Resident> elderAndAdults = new Stack<>();
        elderAndAdults.addAll(Arrays.asList(adults));
        elderAndAdults.addAll(Arrays.asList(elders));
        List<Resident> allResidentsLocal = Stream.of(Stream.of(kids), Stream.of (elders), Stream.of(adults)).
                flatMap(object->object)
                .collect(Collectors.toList());
        allResidents = allResidentsLocal;
        allResidents.forEach(resident -> residentMovement.put(resident, ""));
        if(elderCount+adultCount > houseCount){
            int minimumOlderResidentsCount = (elderCount+adultCount)/houseCount;
            int remainder = (elderCount+adultCount)%houseCount;
            int count=-1;
            for(int i=0, j=0; j<houseCount && minimumOlderResidentsCount !=0 && elderAndAdults.size()!=0; i++){ //TODO: check logic
                houses[j].addResidents((Resident) elderAndAdults.pop());
                if(((i+1)%minimumOlderResidentsCount)==0)
                    j++;
            }

            for(int i=0; i<remainder; i++)
                houses[i].addResidents((Resident)elderAndAdults.pop());
            int minimumKidsPerHouseCount = kidsCount/houseCount;
            remainder = kidsCount%houseCount;
            count=0;
            if(houseCount==1){
                while(count<allResidentsLocal.size())
                    houses[0].addResidents(allResidentsLocal.get(count++));
            }
            else {
                for (int i = 0, j = 0; j < houseCount && minimumKidsPerHouseCount != 0 && count<kidsCount; ) {
                    houses[j].addResidents(kids[count++]);
                    if ((i + 1) % minimumKidsPerHouseCount == 0)
                        j++;
                }
                for (int i = 0; i < remainder; i++)
                    houses[i].addResidents(kids[count++]);
            }
        }
        else{
            int elderAndAdultsSize= elderAndAdults.size();
            for(int i=0; elderAndAdults.size()!=0;i++)
                houses[i].addResidents((Resident) elderAndAdults.pop());
            int minimumKidsPerHouse = kidsCount/elderAndAdultsSize;
            int remainder = kidsCount%elderAndAdultsSize;
            if(minimumKidsPerHouse>0)
                for(int i=0, j=0; j<elderAndAdultsSize;) {
                    houses[j].addResidents(kids[i++]);
                    if((i+1)%minimumKidsPerHouse==0)
                        j++;
                }
            for(int i=0; i<remainder; i++)
                houses[i].addResidents(kids[i+minimumKidsPerHouse]);
        }
    }
    private void displayMatrix(){
        city = new Object[cityDimension][cityDimension];
        Arrays.asList(houses).
                forEach(house -> {
            int row = 1 + random.nextInt(cityDimension-2);
            int column = 1 + random.nextInt(cityDimension-2);
            for(int i=-1; i<2; ++i)
                for(int j=-1; j<2; j++){
                    if(city[row+i][column+j] != null){
                        row = 1 + random.nextInt(cityDimension-2);
                        column = 1 + random.nextInt(cityDimension-2);
                        i=j=-1;
                    }
                }
            house.setCoordinates(row, column);
            city[row][column] = house;
            var temp = new Tuple<Integer,Integer>();
            temp.setItems(row, column);
            houseCoordinates.put(house,temp);
        });
        Arrays.stream(checkpoints).forEach(checkpoint ->{
            int row = 1 + random.nextInt(cityDimension-2);
            int column = 1 + random.nextInt(cityDimension-2);
            for(int i=-1; i<2; ++i)
                for(int j=-1; j<2; j++){
                    if(city[row+i][column+j] != null){
                        row = 1 + random.nextInt(cityDimension-2);
                        column = 1 + random.nextInt(cityDimension-2);
                        i=j=-1;
                    }
                }
            checkpoint.setCoordinates(row, column);
            city[row][column] = checkpoint;
        });
        createTable(cityDimension);
        updateCityMatrix();
    }
    private static String getDirection(Tuple<Integer, Integer> movement){
        return movement.getItem1()==1 && movement.getItem2()==1 ? "jugoistok" :
                movement.getItem1()==0 && movement.getItem2()==1 ? "istok" :
                        movement.getItem1()==0 && movement.getItem2()==-1 ? "zapad" :
                                movement.getItem1()==-1 && movement.getItem2()==0 ? "sjever" :
                                movement.getItem1()==-1 && movement.getItem2()==-1 ? "sjeverozapad" :
                                        movement.getItem1()==-1 && movement.getItem2()==1 ? "sjeveroistok":
                                                movement.getItem1()==1 && movement.getItem2()==0 ? "jug": "jugozapad";

    }
    private void distributeAmbulanceVehicles(){
        int count=0, index=0;
        while(count<ambulanceVehicleCount){
            ambulances.get(index).addVehicle(ambulanceVehicles[count]);
            index = index==3?0:index+1;
            count++;
       }
    }
    private void updateCityMatrix(){
        SwingUtilities.invokeLater(() -> {
            Arrays.stream(city).forEach(row->Arrays.fill(row, null));
            residentCoordinates.forEach((resident, coordinate)->city[coordinate.getItem1()][coordinate.getItem2()]=resident);
            Arrays.stream(houses).forEach(house->city[house.getXCoordinate()][house.getYCoordinate()]=house);
            vehicleCoordinates.forEach((vehicle, coordinate)->city[coordinate.getItem1()][coordinate.getItem2()]=vehicle);
            Arrays.stream(checkpoints).forEach(checkpoint -> city[checkpoint.getXCoordinate()][checkpoint.getYCoordinate()]=checkpoint);
            ambulances.stream().forEach(ambulance -> city[ambulance.getXCoordinate()][ambulance.getYCoordinate()] = ambulance);
            DefaultTableModel model = (DefaultTableModel) matrix.getModel();
            while (model.getRowCount() != 0)
                model.removeRow(0);
            Stream.of(city).forEach(row -> model.addRow(row));
            String houseInfo = "<html>";
            for(House house: houses) {
                houseInfo+=house.houseInfo()+"<br/>";
            }
           houseInfo+="<br/>";
            StringBuilder movements= new StringBuilder();
            for (Resident resident:allResidents){
                movements.append(resident.toString()).append(" (").append(resident.getXCoordinate()).append(",").append(resident.getYCoordinate()).append(") ").append(residentMovement.get(resident)).append("<br/>");
            }
            movements.append("</html>");
            residentInfoLabel.setText(houseInfo+movements);
        });
    }
    private void createTable(int dimensions){
        DefaultTableModel tableModel = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tableModel.setColumnCount(dimensions);
        matrix.setRowHeight(30);
        matrix.setModel(tableModel);
        TableColumnModel tableColumnModel = matrix.getColumnModel();
        for(int i=0; i<tableColumnModel.getColumnCount(); i++)
            tableColumnModel.getColumn(i).setCellRenderer(centerRenderer);
    }
    private void sendAmbulanceVehicle(Integer xCoordinate, Integer yCoordinate) throws TheresNoRoomException, NoAmbulanceVehiclesException {
        Ambulance closestAmbulance = ambulances.get(xCoordinate<=cityDimension/2 && yCoordinate<=cityDimension/2 ? 0 :
                xCoordinate<=cityDimension/2 && yCoordinate>cityDimension/2 ? 1 :
                        xCoordinate>cityDimension/2 && yCoordinate>cityDimension/2 ? 3 : 2);
        AmbulanceVehicle requiredVehicle;
        if(closestAmbulance.getNumberOfVehicles()==0 || closestAmbulance.getRemainingCapacity()==0){
            Optional<Ambulance> ambulance = ambulances.stream().filter(amb->amb.getNumberOfVehicles()!=0).findAny();
                if (ambulance.isPresent()) {
                    if (ambulance.get().getRemainingCapacity() != 0) {
                        closestAmbulance = ambulance.get();
                        requiredVehicle = closestAmbulance.getVehicle();
                        requiredVehicle.setCoordinates(closestAmbulance.getXCoordinate(), closestAmbulance.getYCoordinate());
                    } else {
                        Optional<Ambulance> anyOtherFreeAmbulance = ambulances.stream().filter(a -> a.getNumberOfVehicles() != 0 && a.getRemainingCapacity() != 0).findAny();
                        if (anyOtherFreeAmbulance.isPresent()) {
                            closestAmbulance = anyOtherFreeAmbulance.get();
                            requiredVehicle = closestAmbulance.getVehicle();
                            requiredVehicle.setCoordinates(closestAmbulance.getXCoordinate(), closestAmbulance.getYCoordinate());
                        } else {
                            throw new TheresNoRoomException("Kapaciteti svih ambulanti koja imaju vozilo su puni!");
                        }
                    }
                }
                else throw new NoAmbulanceVehiclesException();
        }
        else {
            requiredVehicle=closestAmbulance.getVehicle();
            requiredVehicle.setCoordinates(closestAmbulance.getXCoordinate(),closestAmbulance.getYCoordinate());
        }
        AmbulanceVehicle finalRequiredVehicle = requiredVehicle;
        Ambulance finalClosestAmbulance = closestAmbulance;
        new Thread(()->{
           Stack<Tuple<Integer,Integer>> route = new Stack<>();
            var temp = new Tuple<Integer, Integer>();
            while(!(Math.abs(finalRequiredVehicle.getXCoordinate()-xCoordinate)<=1 && Math.abs(finalRequiredVehicle.getYCoordinate()-yCoordinate)<=1)){
                while(pause) {
                    try {
                        Thread.sleep(WAIT_TIME);
                    } catch (InterruptedException e) {
                        GenericLogger.log(Simulation.class, e);                    }
                }
                Tuple<Integer,Integer> movement = new Tuple<>();
                movement.setItems(Integer.compare(0, finalRequiredVehicle.getXCoordinate() - xCoordinate),
                        Integer.compare(0, finalRequiredVehicle.getYCoordinate() - yCoordinate));
                for (Map.Entry<House, Tuple<Integer, Integer>> entry : houseCoordinates.entrySet()) {
                    Tuple<Integer, Integer> coordinate = entry.getValue(), localTemp = new Tuple<>();
                    localTemp.setItems(finalRequiredVehicle.getXCoordinate() + movement.getItem1(),finalRequiredVehicle.getYCoordinate() + movement.getItem2());
                    if (localTemp.getItem1().equals(coordinate.getItem1()) && localTemp.getItem2().equals(coordinate.getItem2())) {
                        if(movement.getItem1() > 0 && movement.getItem2()>0 || movement.getItem1()<0 && movement.getItem2()>0)
                            movement.setItem1(0);
                       else if((movement.getItem1() == 0)&&movement.getItem2()!=0){
                           movement.setItem1(1);
                        }
                       else if(movement.getItem1()!=0 && movement.getItem2()==0)
                           movement.setItem2(1);
                       else if(residentCoordinates.containsValue(localTemp))
                           movement.setItems(0,0);
                       else movement.setItem2(0);
                    }
                }
                finalRequiredVehicle.setCoordinates(finalRequiredVehicle.getXCoordinate()+movement.getItem1(), finalRequiredVehicle.getYCoordinate()+movement.getItem2());
                route.push(movement);
                temp.setItems(finalRequiredVehicle.getXCoordinate(), finalRequiredVehicle.getYCoordinate());
                vehicleCoordinates.put(finalRequiredVehicle, temp);
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {
                    GenericLogger.log(Simulation.class, e);
                }
            }
            temp.setItems(xCoordinate, yCoordinate);
            vehicleCoordinates.put(finalRequiredVehicle, temp);
            Optional<Resident> infected= allResidents.stream().filter(r->r.getXCoordinate()==xCoordinate&& r.getYCoordinate()==yCoordinate).findAny();
            residentCoordinates.remove(infected.get());
            finalRequiredVehicle.boardInfectedResident(infected.get());
            infectedResidents.remove(infected.get());
            residentMovement.put(infected.get(), "Ambulantno vozilo");
            while(!route.isEmpty()){
                while(pause){
                    try {
                        Thread.sleep(WAIT_TIME);
                    } catch (InterruptedException e) {
                        GenericLogger.log(Simulation.class, e);
                    }
                }
                var localMovement=route.pop();
                finalRequiredVehicle.setCoordinates(finalRequiredVehicle.getXCoordinate()-localMovement.getItem1(), finalRequiredVehicle.getYCoordinate()-localMovement.getItem2());
                var localTemp = new Tuple<Integer, Integer>();
                localTemp.setItems(finalRequiredVehicle.getXCoordinate(), finalRequiredVehicle.getYCoordinate());
                vehicleCoordinates.put(finalRequiredVehicle, localTemp);
                infected.get().setCoordinates(localTemp.getItem1(), localTemp.getItem2());
                try {
                    Thread.sleep(WAIT_TIME);
                } catch (InterruptedException e) {
                    GenericLogger.log(Simulation.class, e);
                }
            }
            vehicleCoordinates.remove(finalRequiredVehicle);
            finalRequiredVehicle.setCoordinates(finalClosestAmbulance.getXCoordinate(), finalClosestAmbulance.getYCoordinate());
            infected.get().setCoordinates(finalClosestAmbulance.getXCoordinate(), finalClosestAmbulance.getYCoordinate());
            try {
                finalClosestAmbulance.addInfectedResident(infected.get());
                residentMovement.put(infected.get(), "Smjesten u "+finalClosestAmbulance.toString());
                finalClosestAmbulance.addVehicle(finalRequiredVehicle);
            } catch (TheresNoRoomException e) {
                GenericLogger.log(Simulation.class, e);
            }

        }).start();

    }
    public static Ambulance createNewAmbulance(){
        boolean edge = random.nextBoolean(), okay=false;
        Integer newXCoordinate=0, newYCoordinate=0;
        while(!okay) {
            if (edge) {
                newXCoordinate = random.nextBoolean() ? 0 : cityDimension - 1;
                newYCoordinate = 1 + random.nextInt(cityDimension - 2);
            }
            else {
                newYCoordinate=random.nextBoolean()?0:cityDimension-1;
                newXCoordinate = 1+random.nextInt(cityDimension-2);
            }
            Integer finalNewXCoordinate = newXCoordinate;
            Integer finalNewYCoordinate = newYCoordinate;
            if (ambulances.stream().noneMatch(ambulance -> ambulance.getXCoordinate() == finalNewXCoordinate && ambulance.getYCoordinate() == finalNewYCoordinate))
                okay = true;
        }
        Ambulance tempAmbulance = new Ambulance(allResidents.size());
        tempAmbulance.setCoordinates(newXCoordinate, newYCoordinate);
        tempAmbulance.addVehicle(new AmbulanceVehicle());
        ambulances.add(tempAmbulance);
        return tempAmbulance;
    }
    private void createUIComponents() {
        enableMovementButton.addActionListener(listener->{
            if(!initiatedMovement) {
                startMovement();
                initiatedMovement=true;
            }
            else
                JOptionPane.showMessageDialog(this, "Simulacija je vec pokrenuta", "Upozorenje", JOptionPane.WARNING_MESSAGE);
        });
        sendAmbulanceVehicleButton.addActionListener(listener->{
            if(!pause) {
                Alarm alarm = SurveillanceSystem.getAlarm();
                if (alarm != null) {
                    try {
                        sendAmbulanceVehicle(alarm.xCoordinate, alarm.yCoordinate);
                    }
                    catch (TheresNoRoomException | NoAmbulanceVehiclesException exception){
                        JOptionPane.showMessageDialog(this, exception.getMessage(), "Upozorenje", JOptionPane.WARNING_MESSAGE);
                        GenericLogger.log(Simulation.class, exception);
                        SurveillanceSystem.addAlarm(alarm.xCoordinate, alarm.yCoordinate, alarm.houseID);
                    }
                }
                else
                    JOptionPane.showMessageDialog(this, "Nema alarma na steku", "Obavijest", JOptionPane.INFORMATION_MESSAGE);
            }
            else
                JOptionPane.showMessageDialog(this, "Simulacija je pauzirana. Ponovo pokreni simulaciju za slanje ambulantnog vozila", "Poruka", JOptionPane.WARNING_MESSAGE);

        });
        checkAmbulanceSituationButton.addActionListener(listener-> new AmbulanceWindow(ambulances));
        viewStatisticsButton.addActionListener(listener->new Statistics(allResidents));
        stopSimulationButton.addActionListener(listener->{
            try {
                FileOutputStream fileOutputStream = new FileOutputStream("matrix.ser");
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
                objectOutputStream.writeObject(city);
                Ambulance.pause=true;
                this.pause = true;
                objectOutputStream.close();
            }
            catch (IOException ioException){
                GenericLogger.log(Simulation.class, ioException);
            }
        });
        rerunSimulationButton.addActionListener(listener->{
            try {
                Ambulance.pause=false;
                FileInputStream fileInputStream = new FileInputStream("matrix.ser");
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                city = (Object[][]) objectInputStream.readObject();
                objectInputStream.close();
                this.pause = false;
            }
            catch (IOException | ClassNotFoundException ioException){
                GenericLogger.log(Simulation.class, ioException);
            }
        });
        endSimulationButton.addActionListener(listener->{
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("-hh_mm_ss_dd.MM.yyyy");
            float duration =(float)(System.currentTimeMillis()-startTime)/1000;
            String info = "Vrijeme trajanja simulacije: "+duration+" sekundi\n"+
                    "KREIRANO JE:\n" +
                    "1. "+houseCount+" kuca\n"+
                    "2. "+allResidents.size()+ " stanovnika od cega je "+kidsCount+" djece, "+elderCount+" starih i "+adultCount+" odraslih.\n"
                    +"3. "+ambulanceCount+" ambulante, te "+ambulanceVehicleCount+" ambulantnih vozila.\n" +
                    "\nSTATISTIKA BROJA ZARAZENIH / OPORAVLJENIH\n"+
                    "Ukupno: "+allResidents.stream().filter(r->r.getIsInfected()).count()+"/"+allResidents.stream().filter(r->r.getIsRecovered()).count()+"\n"+
                    "Djeca: "+allResidents.stream().filter(r->r instanceof Kid && r.getIsInfected()).count()+"/"+allResidents.stream().filter(r->r instanceof Kid && r.getIsRecovered()).count()+"\n"+
                    "Stariji: "+allResidents.stream().filter(r->r instanceof Elder && r.getIsInfected()).count()+"/"+allResidents.stream().filter(r->r instanceof Elder && r.getIsRecovered()).count()+"\n"+
                    "Odrasli: "+allResidents.stream().filter(r->r instanceof Adult && r.getIsInfected()).count()+"/"+allResidents.stream().filter(r->r instanceof Adult && r.getIsRecovered()).count()+"\n"+
                    "Muskarci: "+allResidents.stream().filter(r->r.getSex() == 'm' && r.getIsInfected()).count()+"/"+allResidents.stream().filter(r->r.getSex()=='m'&&r.getIsRecovered()).count()+"\n"+
                    "Zene: "+allResidents.stream().filter(r->r.getSex()=='f' && r.getIsInfected()).count()+"/"+allResidents.stream().filter(r->r.getSex()=='f'&&r.getIsRecovered()).count();
            try {
                Files.writeString(Paths.get("."+File.separator+"SIM_Java-Kov" + simpleDateFormat.format(new Date()) + ".txt"), info);
            } catch (IOException ioException) {
                GenericLogger.log(Simulation.class, ioException);
            }
            processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });
    }
}
