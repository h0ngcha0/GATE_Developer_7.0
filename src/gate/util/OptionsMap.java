/*
 *  Copyright (c) 1995-2012, The University of Sheffield. See the file
 *  COPYRIGHT.txt in the software or at http://gate.ac.uk/gate/COPYRIGHT.txt
 *
 *  This file is part of GATE (see http://gate.ac.uk/), and is free
 *  software, licenced under the GNU Library General Public License,
 *  Version 2, June 1991 (in the distribution as file licence.html,
 *  and also available at http://gate.ac.uk/gate/licence.html).
 *
 *  Valentin Tablan, 09/11/2001
 *
 *  $Id: OptionsMap.java 15333 2012-02-07 13:18:33Z ian_roberts $
 */
package gate.util;

import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.*;
import java.io.Serializable;

/**
 * A map that stores values as strings and provides support for converting some
 * frequently used types to and from string.<br>
 * Not very efficient as there is a lot of conversions from/to String.
 * The conversion could happen only when loading/saving from/to a file.
 */
public class OptionsMap extends TreeMap<Object, Object> {

  /**
   * Converts the value to string using {@link Strings#toString(Object)}
   * method and then stores it.
   * There is get methods for values that are a String, an Integer, a Boolean,
   * a Font, a List of String and a Map of String*String.
   */
  public Object put(Object key, Object value) {
    if(value instanceof Font){
      Font font = (Font)value;
      String family = font.getFamily();
      int size = font.getSize();
      boolean italic = font.isItalic();
      boolean bold = font.isBold();
      value = family + "#" + size + "#" + italic + "#" + bold;
    }
    return super.put(key, Strings.toString(value));
  }

  public Object put(Object key, LinkedHashSet<String> value) {
    return super.put(key, Strings.toString(value));
  }

  public Object put(Object key, Map<String, String> value) {
    return super.put(key, Strings.toString(value));
  }

  /**
   * If the object stored under key is an Integer then returns its value
   * otherwise returns null.
   * @param key key associated to the value to retrieve
   * @return the associated integer
   */
  public Integer getInt(Object key) {
    try {
      return Integer.decode((String) get(key));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * If the object stored under key is an Double then returns its value
   * otherwise returns null.
   * @param key key associated to the value to retrieve
   * @return the associated Double
   */
  public Double getDouble(Object key) {
    try {
      return Double.valueOf((String) get(key));
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * If the object stored under key is a Boolean then returns its value
   * otherwise returns false.
   * @param key key associated to the value to retrieve
   * @return the associated boolean
   */
  public Boolean getBoolean(Object key) {
    try {
      return Boolean.valueOf((String) get(key));
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * If the object stored under key is a String then returns its value
   * otherwise returns null.
   * @param key key associated to the value to retrieve
   * @return the associated string
   */
  public String getString(Object key) {
    try {
      return (String) get(key);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * If the object stored under key is a Font then returns its value
   * otherwise returns null.
   * @param key key associated to the value to retrieve
   * @return the associated font
   */
  public Font getFont(Object key) {
    try {
      String stringValue = (String) get(key);
      if (stringValue == null) { return null; }
      StringTokenizer strTok = new StringTokenizer(stringValue, "#", false);
      String family = strTok.nextToken();
      int size = Integer.parseInt(strTok.nextToken());
      boolean italic = Boolean.valueOf(strTok.nextToken());
      boolean bold = Boolean.valueOf(strTok.nextToken());
      HashMap<TextAttribute, Serializable> fontAttrs =
        new HashMap<TextAttribute, Serializable>();
      fontAttrs.put(TextAttribute.FAMILY, family);
      fontAttrs.put(TextAttribute.SIZE, (float) size);
      if(bold) fontAttrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD);
      else fontAttrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
      if(italic) fontAttrs.put(
        TextAttribute.POSTURE, TextAttribute.POSTURE_OBLIQUE);
      else fontAttrs.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);
      return new Font(fontAttrs);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * If the object stored under key is a set then returns its value
   * otherwise returns an empty set.
   *
   * @param key key associated to the value to retrieve
   * @return the associated linked hash set
   */
  public LinkedHashSet<String> getSet(Object key) {
    return Strings.toSet((String) get(key), ", ");
  }


  /**
   * If the object stored under key is a map then returns its value
   * otherwise returns an empty map.
   *
   * @param key key associated to the value to retrieve
   * @return the associated map
   */
  public Map<String, String> getMap(Object key) {
      return Strings.toMap((String) get(key));
  }
}