/*  ApperanceDialog.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 12/04/2001
 *
 *  $Id: AppearanceDialog.java 15333 2012-02-07 13:18:33Z ian_roberts $
 *
 */
package gate.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;

import gate.Gate;
import gate.GateConstants;
import gate.swing.JFontChooser;

/**
 * No more used and replaced by {@link OptionsDialog}.
 * @deprecated
 */
public class AppearanceDialog extends JDialog {

  public AppearanceDialog(Frame owner, String title, boolean modal,
                         Component[] targets) {
    super(owner, title, modal);
    this.targets = targets;
    init();
  }// ApperanceDialog

  public AppearanceDialog(Dialog owner, String title, boolean modal,
                         Component[] targets) {
    super(owner, title, modal);
    this.targets = targets;
    init();
  }//ApperanceDialog

  protected void init() {
    initLocalData();
    initGuiComponents();
    initListeners();
    bGroup.setSelected(menusRBtn.getModel(), true);
    cancelBtn.getAction().actionPerformed(null);
  }

  protected void initLocalData() {
    Font font = Gate.getUserConfig().getFont(GateConstants.MENUS_FONT);
    oldMenusFont = menusFont = font == null ?
                               UIManager.getFont("Menu.font") :
                               font;

    font = Gate.getUserConfig().getFont(GateConstants.OTHER_COMPONENTS_FONT);
    oldComponentsFont = componentsFont = font == null ?
                                         UIManager.getFont("Button.font"):
                                         font;

    font = Gate.getUserConfig().getFont(GateConstants.TEXT_COMPONENTS_FONT);
    oldTextComponentsFont = textComponentsFont =
          font == null ? UIManager.getFont("TextPane.font") : font;
  }// initLocalData()

  protected void initGuiComponents() {
    getContentPane().setLayout(new BoxLayout(getContentPane(),
                                             BoxLayout.Y_AXIS));
    //add the radio buttons
    Box box = Box.createHorizontalBox();
    Box tempBox = Box.createVerticalBox();
    bGroup = new ButtonGroup();
    menusRBtn = new JRadioButton("Menus", false);
    menusRBtn.setActionCommand("menus");
    bGroup.add(menusRBtn);
    tempBox.add(menusRBtn);
    componentsRBtn = new JRadioButton("Components", false);
    componentsRBtn.setActionCommand("components");
    bGroup.add(componentsRBtn);
    tempBox.add(componentsRBtn);
    textComponentsRBtn = new JRadioButton("Text components", false);
    textComponentsRBtn.setActionCommand("text components");
    bGroup.add(textComponentsRBtn);
    tempBox.add(textComponentsRBtn);
    box.add(tempBox);
    box.add(Box.createHorizontalGlue());
    getContentPane().add(box);

    //add the font chooser
    fontChooser = new JFontChooser();
    getContentPane().add(fontChooser);

    //add the buttons
    box = Box.createHorizontalBox();
    okBtn = new JButton(new OKAction());
    box.add(okBtn);
    cancelBtn = new JButton(new CancelAction());
    box.add(cancelBtn);
    applyBtn = new JButton(new ApplyAction());
    box.add(applyBtn);
    getContentPane().add(box);

    setResizable(false);

  }// initGuiComponents()

  protected void initListeners() {
    fontChooser.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        pack();
     }
    });

    menusRBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(menusRBtn.isSelected()) fontChooser.setFontValue(menusFont);
      }// public void actionPerformed(ActionEvent e)
    });

    componentsRBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(componentsRBtn.isSelected())
          fontChooser.setFontValue(componentsFont);
      }// public void actionPerformed(ActionEvent e)
    });

    textComponentsRBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(textComponentsRBtn.isSelected())
          fontChooser.setFontValue(textComponentsFont);
      }// public void actionPerformed(ActionEvent e)
    });
  }// initListeners()

  public void show(Component[] targets) {
    this.targets = targets;
    oldMenusFont = menusFont = UIManager.getFont("Menu.font");
    oldComponentsFont = componentsFont = UIManager.getFont("Button.font");
    oldTextComponentsFont = textComponentsFont =
                            UIManager.getFont("TextPane.font");
    super.setVisible(true);
  }// show(Component[] targets)


  protected static void setUIDefaults(Object[] keys, Object value) {
    for(int i = 0; i < keys.length; i++){
      UIManager.put(keys[i], value);
    }
  }// setUIDefaults(Object[] keys, Object value)

  /**
   * Updates the Swing defaults table with the provided font to be used for the
   * text components
   */
  public static void setTextComponentsFont(Font textComponentsFont){
    setUIDefaults(textComponentsKeys, new FontUIResource(textComponentsFont));
    Gate.getUserConfig().put(GateConstants.TEXT_COMPONENTS_FONT,
                             textComponentsFont);
  }

  /**
   * Updates the Swing defaults table with the provided font to be used for the
   * menu components
   */
  public static void setMenuComponentsFont(Font menuComponentsFont){
    setUIDefaults(menuKeys, new FontUIResource(menuComponentsFont));
    Gate.getUserConfig().put(GateConstants.MENUS_FONT,
                             menuComponentsFont);
  }

  /**
   * Updates the Swing defaults table with the provided font to be used for
   * various compoents that neither text or menu components
   */
  public static void setComponentsFont(Font componentsFont){
    setUIDefaults(componentsKeys, new FontUIResource(componentsFont));
    Gate.getUserConfig().put(GateConstants.OTHER_COMPONENTS_FONT,
                             componentsFont);
  }

  /**
   * Test code
   */
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch(Exception e){
      e.printStackTrace();
    }

    JFrame frame = new JFrame("Foo frame");
    final AppearanceDialog apperanceDialog1 = new AppearanceDialog(frame,
                                                           "Font appearance",
                                                           true,
                                                           new Component[]{frame});
    apperanceDialog1.pack();

    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    JButton btn = new JButton("Show dialog");
    btn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        apperanceDialog1.setVisible(true);
      }
    });

    frame.getContentPane().add(btn);
    frame.setSize(new Dimension(300, 300));
    frame.setVisible(true);
  }// public static void main(String[] args)

  JRadioButton menusRBtn;
  JRadioButton componentsRBtn;
  JRadioButton textComponentsRBtn;
  JFontChooser fontChooser;

  JButton okBtn;
  JButton applyBtn;
  JButton cancelBtn;
  ButtonGroup bGroup;

  Font menusFont;
  Font componentsFont;
  Font textComponentsFont;

  Font oldMenusFont;
  Font oldComponentsFont;
  Font oldTextComponentsFont;

  /**
   * Which font is being edited now. Possible vlues: "menu", "text",
   * "components".
   */
  String currentFont;
  Component[] targets;

  public static String[] menuKeys = new String[]{"CheckBoxMenuItem.acceleratorFont",
                                          "CheckBoxMenuItem.font",
                                          "Menu.acceleratorFont",
                                          "Menu.font",
                                          "MenuBar.font",
                                          "MenuItem.acceleratorFont",
                                          "MenuItem.font",
                                          "RadioButtonMenuItem.acceleratorFont",
                                          "RadioButtonMenuItem.font"};

  public static String[] componentsKeys =
                             new String[]{"Button.font",
                                          "CheckBox.font",
                                          "ColorChooser.font",
                                          "ComboBox.font",
                                          "InternalFrame.titleFont",
                                          "Label.font",
                                          "List.font",
                                          "OptionPane.font",
                                          "Panel.font",
                                          "PasswordField.font",
                                          "PopupMenu.font",
                                          "ProgressBar.font",
                                          "RadioButton.font",
                                          "ScrollPane.font",
                                          "TabbedPane.font",
                                          "Table.font",
                                          "TableHeader.font",
                                          "TitledBorder.font",
                                          "ToggleButton.font",
                                          "ToolBar.font",
                                          "ToolTip.font",
                                          "Tree.font",
                                          "Viewport.font"};

  public static String[] textComponentsKeys =
                             new String[]{"EditorPane.font",
                                          "TextArea.font",
                                          "TextField.font",
                                          "TextPane.font"};

  class ApplyAction extends AbstractAction{
    ApplyAction(){
      super("Apply");
    }

    public void actionPerformed(ActionEvent evt) {
      setMenuComponentsFont(menusFont);
      setComponentsFont(componentsFont);
      setTextComponentsFont(textComponentsFont);
      SwingUtilities.updateComponentTreeUI(AppearanceDialog.this);
      for(int i = 0; i< targets.length; i++){
        if(targets[i] instanceof Window) {
          SwingUtilities.updateComponentTreeUI(targets[i]);
        } else {
          SwingUtilities.updateComponentTreeUI(
            SwingUtilities.getRoot(targets[i])
          );
        }
      }
    }// void actionPerformed(ActionEvent evt)
  }

  class OKAction extends AbstractAction {
    OKAction(){
      super("OK");
    }

    public void actionPerformed(ActionEvent evt){
      applyBtn.getAction().actionPerformed(evt);
      setVisible(false);
    }
  }// class OKAction extends AbstractAction

  class CancelAction extends AbstractAction {
    CancelAction(){
      super("Cancel");
    }

    public void actionPerformed(ActionEvent evt){
      setUIDefaults(menuKeys, new FontUIResource(oldMenusFont));
      setUIDefaults(componentsKeys, new FontUIResource(oldComponentsFont));
      setUIDefaults(textComponentsKeys, new FontUIResource(oldTextComponentsFont));
      SwingUtilities.updateComponentTreeUI(
                                  SwingUtilities.getRoot(AppearanceDialog.this));
      for(int i = 0; i< targets.length; i++){
        if(targets[i] instanceof Window){
          SwingUtilities.updateComponentTreeUI(targets[i]);
        } else {
          SwingUtilities.updateComponentTreeUI(
            SwingUtilities.getRoot(targets[i])
          );
        }
      }
      setVisible(false);
    }// void actionPerformed(ActionEvent evt)
  }// class CancelAction extends AbstractAction

}// class ApperanceDialog