import org.unibl.etf.features.GenericLogger;

import javax.swing.*;
import java.io.*;

public class Application implements Serializable {
    private JTextField kidsField;
    private JTextField adultsField;
    private JTextField elderField;
    private JButton runSimulationButton;
    private JPanel mainPanel;
    private JTextField checkpointField;
    private JTextField ambulanceField;
    private JTextField houseField;

    public static void main(String[] args) {
        Application application = new Application();
        JFrame frame = new JFrame("Java") ;
        frame.setVisible(true);
        frame.setContentPane(application.mainPanel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        application.runSimulationButton.addActionListener(listener-> {
            try {
                Integer kidsCount = Integer.parseInt(application.kidsField.getText());
                Integer adultsCount = Integer.parseInt(application.adultsField.getText());
                Integer elderField = Integer.parseInt(application.elderField.getText());
                Integer checkpointField = Integer.parseInt(application.checkpointField.getText());
                Integer ambulanceField = Integer.parseInt(application.ambulanceField.getText());
                Integer housesField = Integer.parseInt(application.houseField.getText());
                new Simulation(kidsCount,adultsCount, elderField, housesField, checkpointField, ambulanceField);
            }
            catch (NumberFormatException numberFormatException){
                GenericLogger.log(Application.class, numberFormatException);
                JOptionPane.showMessageDialog(frame, "Neispravan unos", "Greška", JOptionPane.ERROR_MESSAGE);
            }

        });
    }
}
