/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Kalina Bontcheva 19/11/2001
 *
 *  $Id: AccessRightsDialog.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */

package gate.gui;

import java.awt.Component;

import javax.swing.*;

import gate.security.SecurityInfo;

public class AccessRightsDialog {
  private static JRadioButton gr_gw = new JRadioButton();
  private static JRadioButton gr_ow = new JRadioButton();
  private static JRadioButton or_ow = new JRadioButton();
  private static JRadioButton wr_gw = new JRadioButton();
  private static ButtonGroup group;

  public static boolean showDialog(Component parentComponent){
    gr_gw.setText("Group read/group write");
    gr_ow.setText("Group read/owner write");
    or_ow.setText("Owner read/owner write");
    wr_gw.setText("All read/group write");

    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1,BoxLayout.Y_AXIS));

    group = new ButtonGroup();
    group.add(gr_gw);
    group.add(gr_ow);
    group.add(or_ow);
    group.add(wr_gw);
    gr_gw.setSelected(true);

    panel1.add(wr_gw);
    panel1.add(Box.createHorizontalStrut(30));
    panel1.add(gr_gw);
    panel1.add(Box.createHorizontalStrut(30));
    panel1.add(gr_ow);
    panel1.add(Box.createHorizontalStrut(30));
    panel1.add(or_ow);
    panel1.add(Box.createHorizontalStrut(30));

    return
      OkCancelDialog.showDialog(parentComponent,
                                panel1,
                                "Choose access mode");

  }

  public static int getSelectedMode() {
    if(gr_gw.isSelected())
      return SecurityInfo.ACCESS_GR_GW;
    else if(gr_ow.isSelected())
      return SecurityInfo.ACCESS_GR_OW;
    else if(or_ow.isSelected())
      return SecurityInfo.ACCESS_OR_OW;
    else if(wr_gw.isSelected())
      return SecurityInfo.ACCESS_WR_GW;

    return -1;
  }

}