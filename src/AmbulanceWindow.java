import org.unibl.etf.institutions.Ambulance;

import javax.swing.*;
import java.util.List;

public class AmbulanceWindow extends JFrame {
    private JPanel mainPanel;
    private JTextArea ambulanceInfoLabel;
    private JButton createNewAmbulanceButton;
    private List<Ambulance> ambulances;

    public AmbulanceWindow(List<Ambulance> ambulances){
        setContentPane(mainPanel);
        setVisible(true);
        pack();
        ambulanceInfoLabel.setEditable(false);
        this.ambulances= ambulances;
        printAmbulanceInfo();
        setListeners();
    }
    private void setListeners(){
        createNewAmbulanceButton.addActionListener(listener->{
            if(ambulances.stream().anyMatch(a-> a.getRemainingCapacity()==0)) {
                Ambulance a = Simulation.createNewAmbulance();
                JOptionPane.showMessageDialog(this,"Ambulanta uspješno kreirana na koordinatama (" + a.getXCoordinate() + "," + a.getYCoordinate() + ")",
                        "Obavijest",JOptionPane.INFORMATION_MESSAGE);
            }
            else
                JOptionPane.showMessageDialog(this, "Nije moguće kreirati ambulantu jer su sve ambulante otvorenog kapaciteta", "Obavijest", JOptionPane.WARNING_MESSAGE);
        });
    }
    private void printAmbulanceInfo(){
        String info="";
        for(Ambulance ambulance:ambulances){
            info+=ambulance.toString()+" "+ambulance.getAmbulanceInfo()+"\n";
        }
        ambulanceInfoLabel.setText(info);
    }
}
