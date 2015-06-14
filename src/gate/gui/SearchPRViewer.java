/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan 15 May 2002
 *
 *  $Id: SearchPRViewer.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */
package gate.gui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import gate.*;
import gate.creole.AbstractVisualResource;
import gate.creole.ir.*;
import gate.event.ProgressListener;
import gate.swing.XJTable;


/**
 * Shows the results of a IR query. This VR is associated to
 * {@link gate.creole.ir.SearchPR}.
 */
public class SearchPRViewer extends AbstractVisualResource
                            implements ProgressListener{

  public Resource init(){
    initLocalData();
    initGuiComponents();
    initListeners();
    return this;
  }

  protected void initLocalData(){
    results = new ArrayList<QueryResult>();
  }

  protected void initGuiComponents(){
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    resultsTableModel = new ResultsTableModel();
    resultsTable = new XJTable(resultsTableModel);
    resultsTable.getColumnModel().getColumn(1).
                 setCellRenderer(new FloatRenderer());
    add(new JScrollPane(resultsTable));
//    add(Box.createHorizontalGlue());
  }

  protected void initListeners(){
    resultsTable.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if(e.getClickCount() == 2){
          //where inside the table
          selectDocument((String)resultsTable.getValueAt(
                            resultsTable.rowAtPoint(e.getPoint()), 0));
        }
      }
    });
  }

  /**
   * Tries to load (if necessary) and select (i.e. bring to front) a document
   * based on its name.
   */
  protected void selectDocument(String documentName){
    Corpus corpus = target.getCorpus();
    int i = 0;
    for(;
        i < corpus.size() &&
        (!corpus.getDocumentName(i).equals(documentName));
        i++);
    if(corpus.getDocumentName(i).equals(documentName)){
      //trigger document loading if needed
      Document doc = (Document)corpus.get(i);
      //try to select the document
      Component root = SwingUtilities.getRoot(this);
      if(root instanceof MainFrame){
        MainFrame mainFrame = (MainFrame)root;
        mainFrame.select(doc);
      }
    }
  }

  /**
   * Called by the GUI when this viewer/editor has to initialise itself for a
   * specific object.
   * @param target the object (be it a {@link gate.Resource},
   * {@link gate.DataStore} or whatever) this viewer has to display
   */
  public void setTarget(Object target){
    if(!(target instanceof SearchPR)){
      throw new IllegalArgumentException(
        "The GATE IR results viewer can only be used with a GATE search PR!\n" +
        target.getClass().toString() + " is not a GATE search PR!");
    }
    this.target = (SearchPR)target;
    this.target.addProgressListener(this);
  }

  /**
   * Does nothing.
   * @param i
   */
  public void progressChanged(int i){}

  /**
   * Called when the process is finished, fires a refresh for this VR.
   */
  public void processFinished(){
    updateDisplay();
  }

  protected void updateDisplay(){
    results.clear();
    if(target != null){
      QueryResultList resultsList = target.getResult();
      Iterator<QueryResult> resIter = resultsList.getQueryResults();
      while(resIter.hasNext()){
        results.add(resIter.next());
      }
      SwingUtilities.invokeLater(new Runnable(){
        public void run(){
          resultsTableModel.fireTableDataChanged();
        }
      });
    }
  }

  protected class ResultsTableModel extends AbstractTableModel{
    public int getRowCount(){
      return results.size();
    }

    public int getColumnCount(){
      return 2;
    }

    public String getColumnName(int columnIndex){
      switch(columnIndex){
        case DOC_NAME_COLUMN: return "Document";
        case DOC_SCORE_COLUMN: return "Score";
        default: return "?";
      }
    }

    public Class<?> getColumnClass(int columnIndex){
      switch(columnIndex){
        case DOC_NAME_COLUMN: return String.class;
        case DOC_SCORE_COLUMN: return Float.class;
        default: return Object.class;
      }
    }

    public boolean isCellEditable(int rowIndex, int columnIndex){
      return false;
    }

    public Object getValueAt(int rowIndex, int columnIndex){
      QueryResult aResult = (QueryResult)results.get(rowIndex);
      switch(columnIndex){
        case DOC_NAME_COLUMN: return guessDocName(aResult.getDocumentID()).
                                     toString();
        case DOC_SCORE_COLUMN: return new Float(aResult.getScore());
        default: return null;
      }
    }

    /**
     * Attempts to guess the document name from the ID returned by the searcher
     */
    protected Object guessDocName(Object docID){
      //the doc ID is most likely the persistence ID for the doc
      Corpus corpus = target.getCorpus();
      DataStore ds = corpus.getDataStore();
      if(ds != null){
        try{
          return ds.getLrName(docID);
        }catch(Exception e){}
      }
      //we couldn't guess anything
      return docID;
    }

    static private final int DOC_NAME_COLUMN = 0;
    static private final int DOC_SCORE_COLUMN = 1;
  }

  protected class FloatRenderer extends JProgressBar
                                implements TableCellRenderer{
    public FloatRenderer(){
      setStringPainted(true);
      setForeground(new Color(150, 75, 150));
      setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      setMinimum(0);
      //we'll use 3 decimal digits
      setMaximum(1000);
      numberFormat = NumberFormat.getInstance(Locale.getDefault());
      numberFormat.setMaximumFractionDigits(3);
    }


    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column){

      float fValue = ((Float)value).floatValue();
      setValue((int)(fValue * 1000));
      setBackground(table.getBackground());

      setString(numberFormat.format(value));
      return this;
    }

    /*
     * The following methods are overridden as a performance measure to
     * to prune code-paths are often called in the case of renders
     * but which we know are unnecessary.
     */

    /**
     * Overridden for performance reasons.
     */
    public boolean isOpaque() {
      Color back = getBackground();
      Component p = getParent();
      if (p != null) {
        p = p.getParent();
      }
      // p should now be the JTable.
      boolean colorMatch = (back != null) && (p != null) &&
      back.equals(p.getBackground()) &&
      p.isOpaque();
      return !colorMatch && super.isOpaque();
    }

    /**
     * Overridden for performance reasons.
     */
    public void validate() {}

    /**
     * Overridden for performance reasons.
     */
    public void revalidate() {}

    /**
     * Overridden for performance reasons.
     */
    public void repaint(long tm, int x, int y, int width, int height) {}

    /**
     * Overridden for performance reasons.
     */
    public void repaint(Rectangle r) { }

    /**
     * Overridden for performance reasons.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {
      // Strings get interned...
      if ("text".equals(propertyName)) {
        super.firePropertyChange(propertyName, oldValue, newValue);
      }
    }

    /**
     * Overridden for performance reasons.
     */
    public void firePropertyChange(String propertyName, boolean oldValue,
                                   boolean newValue) { }

    NumberFormat numberFormat;
  }

  /**
   * The search PR this VR is associated to.
   */
  SearchPR target;

  /**
   * The table displaying the results
   */
  XJTable resultsTable;

  /**
   * The model for the results table.
   */
  ResultsTableModel resultsTableModel;

  /**
   * Contains the {@link gate.creole.ir.QueryResult} objects returned by the
   * search.
   */
  List<QueryResult> results;

}