/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Niraj Aswani, 2007
 *
 *  $Id$
 *
 */

package gate.gui.ontology;

import gate.gui.MainFrame;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.*;
import javax.swing.text.JTextComponent;

public class ValuesSelectionAction {
  public ValuesSelectionAction() {
    list = null;
    domainBox = new JComboBox();
    domainBox.setEditable(true);
    list = new JList(new DefaultComboBoxModel());
    list.setVisibleRowCount(7);
    add = new JButton("Add");
    remove = new JButton("Remove");
    panel = new JPanel();
    BoxLayout boxlayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
    panel.setLayout(boxlayout);
    panel.add(domainBox);
    domainBox.setEditable(true);
    domainBox.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent keyevent) {
        String s = ((JTextComponent)domainBox.getEditor().getEditorComponent())
                .getText();
        if(s != null) {
          if(keyevent.getKeyCode() != KeyEvent.VK_ENTER) {
            ArrayList<String> arraylist = new ArrayList<String>();
            for(int i = 0; i < ontologyClasses.length; i++) {
              String s1 = ontologyClasses[i];
              if(s1.toLowerCase().startsWith(s.toLowerCase())) {
                arraylist.add(s1);
              }
            }
            Collections.sort(arraylist);
            DefaultComboBoxModel model =
              new DefaultComboBoxModel(arraylist.toArray());
            domainBox.setModel(model);
            if(!arraylist.isEmpty()) domainBox.showPopup();
          }
          ((JTextComponent)domainBox.getEditor().getEditorComponent())
                  .setText(s);
        }
      }
    });
    JPanel jpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    jpanel.add(add);
    jpanel.add(remove);
    panel.add(jpanel);
    panel.add(new JScrollPane(list));
    add.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent actionevent) {
        String s = (String)domainBox.getSelectedItem();
        if(!allowValueOutsideDropDownList) {
          if(!Arrays.asList(ontologyClasses).contains(s)) {
            JOptionPane.showMessageDialog(MainFrame.getInstance(),
                    "The value \"" + s + "\" is not in the drop down list!");
            return;
          }
        }
        if(((DefaultComboBoxModel)list.getModel()).getIndexOf(s) != -1) {
          JOptionPane.showMessageDialog(MainFrame.getInstance(),
            "Already added!");
        }
        else {
          ((DefaultComboBoxModel)list.getModel()).addElement(s);
        }
      }
    });
    remove.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent actionevent) {
        Object aobj[] = list.getSelectedValues();
        if(aobj != null && aobj.length > 0) {
          for(int i = 0; i < aobj.length; i++)
            ((DefaultComboBoxModel)list.getModel()).removeElement(aobj[i]);
        }
      }
    });
  }

  /**
   * Dialogue that list possible choices to choose from.
   * @param windowTitle title of the window
   * @param inDropDownList list of choices
   * @param alreadySelected initial selection
   * @param allowValueOutsideDropDownList true if allowed
   * @param icon message dialogue icon
   * @return {@link JOptionPane#CLOSED_OPTION},
   *  {@link JOptionPane#UNINITIALIZED_VALUE}, {@link JOptionPane#OK_OPTION},
   *  {@link JOptionPane#CANCEL_OPTION}.
   */
  public int showGUI(String windowTitle, String[] inDropDownList,
          String[] alreadySelected, boolean allowValueOutsideDropDownList,
          Icon icon) {
    this.ontologyClasses = inDropDownList;
    this.allowValueOutsideDropDownList = allowValueOutsideDropDownList;
    domainBox.setModel(new DefaultComboBoxModel(inDropDownList));
    list.setModel(new DefaultComboBoxModel(alreadySelected));
    JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE,
     JOptionPane.OK_CANCEL_OPTION, icon) {
      public void selectInitialValue() {
        domainBox.requestFocusInWindow();
        domainBox.getEditor().selectAll();
      }
    };
    pane.createDialog(MainFrame.getInstance(), windowTitle).setVisible(true);
    return pane.getValue() == null ?
      JOptionPane.CLOSED_OPTION : (Integer) pane.getValue();
  }

  public String[] getSelectedValues() {
    DefaultComboBoxModel model = (DefaultComboBoxModel) list.getModel();
    String as[] = new String[model.getSize()];
    for(int i = 0; i < as.length; i++)
      as[i] = (String) model.getElementAt(i);
    return as;
  }

  protected JComboBox domainBox;
  protected JList list;
  protected JButton add;
  protected JButton remove;
  protected JPanel panel;
  protected String[] ontologyClasses;
  protected boolean allowValueOutsideDropDownList = true;
}
