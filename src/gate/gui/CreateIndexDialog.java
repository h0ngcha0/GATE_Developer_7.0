/*
 *  CreateIndexDialog.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Rosen Marinov, 19/Apr/2002
 *
 */

package gate.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.*;

import gate.creole.ir.*;

public class CreateIndexDialog extends JDialog {

  private IndexedCorpus ic;

    protected JPanel panel1 = new JPanel();
    protected JLabel indexTypeLabel = new JLabel();
    protected JComboBox jComboIType = new JComboBox();
    protected JLabel locationLabel = new JLabel();
    protected JTextField locationTextField = new JTextField();
    protected JButton browse = new JButton();
    protected JLabel featureLable = new JLabel();
    protected JTextField featureTextField = new JTextField();
    protected JList jList1 = null;
    protected JScrollPane scrollPane = new JScrollPane();
    protected JButton addButton = new JButton();
    protected JCheckBox content = new JCheckBox();
    protected JButton createButton = new JButton();
    protected JButton cancelButton = new JButton();
    protected GridBagLayout gridBagLayout1 = new GridBagLayout();

    private Vector fields = new Vector();

  public CreateIndexDialog(Frame owner, IndexedCorpus ic){
    super(owner, true);
    this.ic = ic;
    init();
    pack();
  }

  public CreateIndexDialog(Dialog owner, IndexedCorpus ic){
    super(owner, true);
    this.ic = ic;
    init();
  }

  private void init(){
        panel1.setLayout(gridBagLayout1);
        indexTypeLabel.setText("Index Type");
        locationLabel.setText("Location");
        browse.setToolTipText("Browse location directory");
        browse.setText("Browse");
        featureLable.setText("Feature/Field  Name");
        addButton.setText("Add Field");
        content.setSelected(true);
        content.setText("Content");
        createButton.setText("Create");
        cancelButton.setText("Cancel");

        jComboIType.addItem("Lucene");
        jComboIType.setSelectedItem("Lucene");


        jList1 = new JList(fields);
        scrollPane.getViewport().setView(jList1);

        this.getContentPane().add(panel1, BorderLayout.NORTH);

        cancelButton.addActionListener( new ActionListener(){
          public void actionPerformed(ActionEvent e){
            cancelAction();
          }
        });

        createButton.addActionListener( new ActionListener(){
          public void actionPerformed(ActionEvent e){
            createAction();
          }
        });

        browse.addActionListener( new ActionListener(){
          public void actionPerformed(ActionEvent e){
            browseAction();
          }
        });

        addButton.addActionListener( new ActionListener(){
          public void actionPerformed(ActionEvent e){
            addAction();
          }
        });

        panel1.add(locationLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(indexTypeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(featureLable, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(locationTextField, new GridBagConstraints(1, 1, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(jComboIType, new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(browse, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(addButton, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(featureTextField, new GridBagConstraints(1, 2, 2, 1, 1.0, 0.0
            ,GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(jList1, new GridBagConstraints(1, 3, 2, 1, 1.0, 1.0
            ,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 185, 87));
        panel1.add(content, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(createButton, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));
        panel1.add(cancelButton, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0
            ,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 5, 5), 0, 0));

  }

  private void cancelAction(){
    this.dispose();
  }

  private void createAction(){
    DefaultIndexDefinition did = new DefaultIndexDefinition();
//    did.setIndexType(GateConstants.IR_LUCENE_INVFILE);

    String location = locationTextField.getText();
    did.setIndexLocation(location);

    if (content.isSelected()) {
      did.addIndexField(new IndexField("body", new DocumentContentReader(), false));
    }

    for (int i = 0; i<fields.size(); i++){
      did.addIndexField(new IndexField(fields.elementAt(i).toString(), null, false));
    }

    ic.setIndexDefinition(did);

    try {
      ic.getIndexManager().deleteIndex();
      ic.getIndexManager().createIndex();
    } catch (IndexException e){
      e.printStackTrace();
    }
    this.dispose();
  }

  private void browseAction(){
    JFileChooser fc =  MainFrame.getFileChooser();
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setDialogTitle("Select location directory");
    fc.setSelectedFiles(null);
    int res = fc.showDialog(this, "Select");
    if (res == JFileChooser.APPROVE_OPTION){
      File f = fc.getSelectedFile();
      locationTextField.setText(f.getAbsolutePath());
    }
  }

  private void addAction(){
    fields.add(featureTextField.getText());
  }

}