/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 16/07/2001
 *
 *  $Id: OkCancelDialog.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */

package gate.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * A simple modal dialog that displays a component provided by the user along
 * with two buttons ("OK" and "Cancel").
 */
public class OkCancelDialog extends JDialog {

  protected OkCancelDialog(Frame owner, String title, Component contents){
    super(owner, title);
    init(contents);
  }

  protected OkCancelDialog(Dialog owner, String title, Component contents){
    super(owner, title);
    init(contents);
  }

  protected OkCancelDialog(String title, Component contents){
    super();
    setTitle(title);
    init(contents);
  }

  protected void init(Component contents){
    MainFrame.getGuiRoots().add(this);

    getContentPane().setLayout(new BorderLayout());

//    //fill in the contents
//    JPanel vBox = new JPanel();
//    vBox.setLayout(new BoxLayout(vBox, BoxLayout.Y_AXIS));
//
//    JPanel contentsPanel = new JPanel();
//    contentsPanel.add(contents);
//    contentsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
//
//    vBox.add(contentsPanel);

    getContentPane().add(contents, BorderLayout.CENTER);

    JPanel buttonsBox = new JPanel();
    buttonsBox.setLayout(new BoxLayout(buttonsBox, BoxLayout.X_AXIS));
    buttonsBox.setAlignmentX(Component.CENTER_ALIGNMENT);
    okButton = new JButton("OK");
    cancelButton = new JButton("Cancel");
    buttonsBox.add(Box.createHorizontalGlue());
    buttonsBox.add(okButton);
    buttonsBox.add(Box.createHorizontalStrut(20));
    buttonsBox.add(cancelButton);
    buttonsBox.add(Box.createHorizontalGlue());

    Box vBox = Box.createVerticalBox();
    vBox.add(Box.createVerticalStrut(10));
    vBox.add(buttonsBox);
    vBox.add(Box.createVerticalStrut(10));

    getContentPane().add(vBox, BorderLayout.SOUTH);

    Action applyAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        userHasPressedOK = true;
        setVisible(false);
      }
    };

    Action cancelAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        userHasPressedCancel = true;
        setVisible(false);
      }
    };

    // define keystrokes action bindings at the level of the main window
    InputMap inputMap = ((JComponent)this.getContentPane()).
      getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap =
      ((JComponent)this.getContentPane()).getActionMap();
    inputMap.put(KeyStroke.getKeyStroke("ENTER"), "Apply");
    actionMap.put("Apply", applyAction);
    inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "Cancel");
    actionMap.put("Cancel", cancelAction);

    okButton.addActionListener(applyAction);
    getRootPane().setDefaultButton(okButton);
    cancelButton.addActionListener(cancelAction);
  }

  public void dispose(){
    MainFrame.getGuiRoots().remove(this);
    super.dispose();
  }

  /**
   * @return true if the user has selected the "OK" button.
   */
  public static boolean showDialog(Component parentComponent,
                                   Component contents,
                                   String title){
    //construct the dialog
    Window parent = null;
    if(parentComponent != null){
      parent = SwingUtilities.getWindowAncestor(parentComponent);
    }
    OkCancelDialog dialog;
    if(parent == null) dialog = new OkCancelDialog(title, contents);
    else if(parent instanceof Frame){
      dialog = new OkCancelDialog((Frame)parent, title, contents);
    } else{
      dialog = new OkCancelDialog((Dialog)parent, title, contents);
    }

    //position the dialog
    dialog.pack();
    dialog.setLocationRelativeTo(parentComponent);

    //kalina: make it fit the screen
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension dialogSize = dialog.getSize();
    if (dialogSize.height > screenSize.height)
      dialogSize.height = screenSize.height;
    if (dialogSize.width > screenSize.width)
      dialogSize.width = screenSize.width;
    dialog.setSize(dialogSize);
    //end kalina

    //show the dialog
    dialog.setModal(true);
    dialog.userHasPressedOK = false;
    dialog.userHasPressedCancel = false;
    dialog.setVisible(true);
//    
//    dialog.show();
    return dialog.userHasPressedOK;
  }

  protected JButton okButton;
  protected JButton cancelButton;
  protected boolean userHasPressedOK;
  protected static boolean userHasPressedCancel;
}