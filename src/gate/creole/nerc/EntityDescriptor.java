/*
 *  EntityDescriptor.java
 *
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, July/2000
 *
 *  $Id: EntityDescriptor.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */

package gate.creole.nerc;

import java.io.Serializable;

import gate.Annotation;
import gate.Document;
import gate.util.InvalidOffsetException;

/** Represents a single named entity */
public class EntityDescriptor implements Serializable{

  /** Constructs a new entity descriptor */
  public EntityDescriptor(String string, String category, int start, int end) {
    this.string = normaliseString(string);
    this.category = category;
    offsets = new int[2];
    offsets[0] = start;
    offsets[1] = end;
  }

  /** Constructs a new entity descriptor starting from a Gate annotation */
  public EntityDescriptor(Document document, Annotation annotation) {
    offsets = new int[2];
    offsets[0] = annotation.getStartNode().getOffset().intValue();
    offsets[1] = annotation.getEndNode().getOffset().intValue();
    try{
      string = normaliseString(document.getContent().getContent(
                                    annotation.getStartNode().getOffset(),
                                    annotation.getEndNode().getOffset()).
                                    toString());
    } catch(InvalidOffsetException ioe){
      ioe.printStackTrace();
    }
    category = annotation.getType();
  }

  /** Returns a normalised string for the entity. This is the string from the
    * text document the entity was descovered in, with all whitespace sequences
    * replaced by a single space character
    */
  public String getString(){
    return string;
  }

  /** Returns the category of the entity*/
  public String getCategory(){
    return category;
  }

  /** Returns a pair of integers specifying the character offsets in the
    * original file where the entity occured
    */
  public int[] getOffsets(){
    return offsets;
  }

  /** Returns a string giving the category, offsets and normalised string for
    * the entity, with no newlines.
    */
  public String toString(){
    return category + " " + offsets[0] + " " + offsets[1] + " " + string;
  }

  String string;
  String category;
  int[] offsets;

  /** Normalises a string. That is removes all the leading and trailing
    * whitespace characters and replaces all inner whitespace sequences with a
    * single space character
    */
  protected String normaliseString(String text){
///    String res = "";
    StringBuffer res = new StringBuffer(gate.Gate.STRINGBUFFER_SIZE);
    if(text == null) return null;
    int charIdx = 0;
    boolean lastWasSpace = false;
    //skip the leading spaces
    while(charIdx < text.length() &&
          Character.isWhitespace(text.charAt(charIdx))) charIdx++;
    //parse the rest of the text
    while(charIdx < text.length()){
      if(Character.isWhitespace(text.charAt(charIdx))){
        //reading spaces
        lastWasSpace = true;
      }else{
        //reading non-spaces
        if(lastWasSpace) ///res += " ";
                res.append(" ");
///        res += text.charAt(charIdx);
        res.append(text.charAt(charIdx));
        lastWasSpace = false;
      }
      charIdx++;
    }//while(charIdx < text.length())
    return res.toString();
  }

}
