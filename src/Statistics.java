import org.unibl.etf.features.GenericLogger;
import org.unibl.etf.features.Tuple;
import org.unibl.etf.residents.Adult;
import org.unibl.etf.residents.Elder;
import org.unibl.etf.residents.Kid;
import org.unibl.etf.residents.Resident;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Statistics extends JFrame implements Serializable {
    private JPanel mainPanel;
    private JTable statisticsTable;
    private JButton exportButton;
    private final String[] mandatoryFields = new String[]{"","Ukupno", "Djeca", "Odrasli", "Stari", "Muškarci", "Žene"};
    private String[][] tableContents;
    private List<Resident> residentList;
    private final int COLUMN_COUNT=3, ROW_COUNT=7;
    public Statistics(List<Resident> residentList){
        setContentPane(mainPanel);
        setVisible(true);
        pack();
        this.residentList = residentList;
        tableContents=  new String[ROW_COUNT][COLUMN_COUNT];
        fillInTable();
        exportButton.addActionListener(listener-> {
              JFileChooser jFileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV", "csv", "comma separated values");
              jFileChooser.setFileFilter(filter);
              if(jFileChooser.showSaveDialog(Statistics.this) == JFileChooser.APPROVE_OPTION) {
                  String fileName = jFileChooser.getSelectedFile().getAbsolutePath();
                  try {
                      TableModel model = statisticsTable.getModel();
                      FileWriter csv = new FileWriter(new File(fileName));

                      csv.write("\n");

                      for (int i = 0; i < model.getRowCount(); i++) {
                          for (int j = 0; j < model.getColumnCount(); j++) {
                              csv.write(model.getValueAt(i, j).toString() + ",");
                          }
                          csv.write("\n");
                      }
                      csv.close();
                      JOptionPane.showMessageDialog(this, "Uspješno spremljeno","Obavijest", JOptionPane.INFORMATION_MESSAGE);
                  } catch (IOException ioException) {
                      GenericLogger.log(Statistics.class, ioException);                  }
              }
        });
    }
    private Tuple<Integer, Integer> getInfectedAndRecoveredCount(List<Resident> list){
        var info = new Tuple<Integer, Integer>();
        int infectedCount= (int) list.stream().filter(Resident::getIsInfected).count();
        int recoveredCount = (int) list.stream().filter(Resident::getIsRecovered).count();
        info.setItems(infectedCount, recoveredCount);
        return info;
    }
    private void fillInTable(){
        for(int i=0; i<ROW_COUNT; i++)
            tableContents[i][0]=mandatoryFields[i];
        tableContents[0][1]="Broj zaraženih";
        tableContents[0][2]="Broj oporavljenih";
        var requiredInfo = getInfectedAndRecoveredCount(residentList);
        tableContents[1][1]=requiredInfo.getItem1().toString();
        tableContents[1][2]=requiredInfo.getItem2().toString();
        requiredInfo = getInfectedAndRecoveredCount(residentList.stream().filter(resident -> resident instanceof Kid).collect(Collectors.toList()));
        tableContents[2][1]=requiredInfo.getItem1().toString();
        tableContents[2][2]=requiredInfo.getItem2().toString();
        requiredInfo = getInfectedAndRecoveredCount(residentList.stream().filter(resident -> resident instanceof Adult).collect(Collectors.toList()));
        tableContents[3][1]=requiredInfo.getItem1().toString();
        tableContents[3][2]=requiredInfo.getItem2().toString();
        requiredInfo = getInfectedAndRecoveredCount(residentList.stream().filter(resident -> resident instanceof Elder).collect(Collectors.toList()));
        tableContents[4][1]=requiredInfo.getItem1().toString();
        tableContents[4][2]=requiredInfo.getItem2().toString();
        requiredInfo = getInfectedAndRecoveredCount(residentList.stream().filter(resident -> resident.getSex()=='m').collect(Collectors.toList()));
        tableContents[5][1]=requiredInfo.getItem1().toString();
        tableContents[5][2]=requiredInfo.getItem2().toString();
        requiredInfo = getInfectedAndRecoveredCount(residentList.stream().filter(resident -> resident.getSex()=='f').collect(Collectors.toList()));
        tableContents[6][1]=requiredInfo.getItem1().toString();
        tableContents[6][2]=requiredInfo.getItem2().toString();
        DefaultTableModel defaultTableModel = (DefaultTableModel)statisticsTable.getModel();
        defaultTableModel.setColumnCount(COLUMN_COUNT);
        Arrays.stream(tableContents).forEach(defaultTableModel::addRow);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        TableColumnModel tableColumnModel = statisticsTable.getColumnModel();
        statisticsTable.setRowHeight(24);
        for(int i=0; i<tableColumnModel.getColumnCount(); i++)
            tableColumnModel.getColumn(i).setCellRenderer(centerRenderer);

    }
}
