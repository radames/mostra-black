/**
 * Copyright (C) 2009 David Bouchard
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */


import java.awt.event.*;
import java.io.OutputStream;
import java.util.ArrayList;

import processing.core.*;
import processing.xml.*;

/**
 * This class manages the creation and calibration of keystoned surfaces.
 * 
 * To move and warp surfaces, place the Keystone object in calibrate mode. It catches mouse events and 
 * allows you to drag surfaces and control points with the mouse.
 *
 * The Keystone object also provides load/save functionality, once you've calibrated the layout to 
 * your liking. 
 * 
 * Version: 0.11
 */
public class Keystone {

  public final String VERSION = "001";

  PApplet parent;

  ArrayList<CornerPinSurface> surfaces;

  Draggable dragged;

  // calibration mode is application-wide, so I made this flag static
  // there should only be one Keystone object around anyway 
  static boolean calibrate;
  boolean drag;

  /**
   	 * @param parent applet
   	 */

  int surfaceSelected;

  public Keystone(PApplet parent) {
    this.parent = parent;
    this.parent.registerMouseEvent(this);
    this.parent.registerKeyEvent(this);
    surfaces = new ArrayList<CornerPinSurface>();
    dragged = null;

    // check the renderer type
    // issue a warning if its PGraphics2D
    PGraphics pg = (PGraphics)parent.g;
    if ((pg instanceof PGraphics2D) ) {
      PApplet.println("The keystone library will not work with PGraphics2D as the renderer because it relies on texture mapping. " +
        "Try P3D, OPENGL or GLGraphics.");
    }
  }

  /**
   	 * Creates and registers a new corner pin keystone surface. 
   	 * 
   	 * @param w width
   	 * @param h height
   	 * @param res resolution (number of tiles per axis)
   	 * @return
   	 */
  public CornerPinSurface createCornerPinSurface(int xx, int yy, int w, int h, int res, int offset) {
    CornerPinSurface s = new CornerPinSurface(parent, xx, yy, w, h, res, offset);
    surfaces.add(s);
    return s;
  }

  /**
   	 * Starts the calibration mode. Mouse events will be intercepted to drag surfaces 
   	 * and move control points around.
   	 */
  public void startCalibration() {
    calibrate = true;
  }

  /**
   	 * Stops the calibration mode
   	 */
  public void stopCalibration() {
    calibrate = false;
  }

  /**
   	 * Toggles the calibration mode
   	 */
  public void toggleCalibration() {
    calibrate = !calibrate;
  }

  /**
   	 * Returns the version of the library.
   	 * 
   	 * @return String
   	 */
  public String version() {
    return VERSION;
  }

  /**
   	 * Saves the layout to an XML file.
   	 */
  public void save(String filename) {


    XMLElement root = new XMLElement("keystone");

    // create XML elements for each surface containing the resolution
    // and control point data
    for (CornerPinSurface s : surfaces) {
      //String fmt = "surface res=\"%d\" x=\"%f\" y=\"%f\"";
      String fmt = "surface";
      String fmted = String.format(fmt, s.getRes(), s.x, s.y);


      XMLElement xml = new XMLElement(fmt); 

      for (int i=0; i < s.mesh.length; i++) {
        if (s.mesh[i].isControlPoint()) {
          fmt = "point i=\"%d\" x=\"%f\" y=\"%f\" u=\"%f\" v=\"%f\""; 
          // fmt = "point";
          fmted = String.format(fmt, i, s.mesh[i].x, s.mesh[i].y, s.mesh[i].u, s.mesh[i].v);
          xml.addChild(new XMLElement(fmted));
        }
      }
      root.addChild(xml);
    }

    // write the settings to keystone.xml in the sketch's data folder
    try {
      OutputStream stream = parent.createOutput(parent.dataPath(filename));
      XMLWriter writer = new XMLWriter(stream);
      writer.write(root, true);
    } 
    catch (Exception e) {
      PApplet.println(e.getStackTrace());
    }

    PApplet.println("Keystone: layou  t saved to " + filename);
  }

  /**
   	 * Saves the current layout into "keystone.xml"
   	 */
  public void save() {
    save("keystone.xml");
  }

  /**
   	 * Loads a saved layout from a given XML file
   	 */
  public void load(String filename) {
    XMLElement root = new XMLElement(parent, parent.dataPath(filename));
    for (int i=0; i < root.getChildCount(); i++) {
      surfaces.get(i).load(root.getChild(i));
    }
    PApplet.println("Keystone: layout loaded from " + filename);
  }


  /**
   	 * Loads a saved layout from "keystone.xml"
   	 */
  public void load() {
    load("keystone.xml");
  }



  public void resetMesh() {

    for (CornerPinSurface s : surfaces) {
      s.resetMesh();
    }
  }
  /**
   	 * @invisible
   	 */
  public void mouseEvent(MouseEvent e) {

    // ignore input events if the calibrate flag is not set
    if (!calibrate)
      return;

    int x = e.getX();
    int y = e.getY();

    switch (e.getID()) {

    case MouseEvent.MOUSE_PRESSED:
      CornerPinSurface top = null;
      // navigate the list backwards, as to select 
      for (int i=surfaces.size()-1; i >= 0; i--) {
        CornerPinSurface s = (CornerPinSurface)surfaces.get(i);
        dragged = s.select(x, y);
        if (dragged != null) {
          top = s;
          break;
        }
      }

      if (top != null) {
        // moved the dragged surface to the beginning of the list
        // this actually breaks the load/save order.
        // in the new version, add IDs to surfaces so we can just 
        // re-load in the right order (or create a separate list 
        // for selection/rendering)
        //int i = surfaces.indexOf(top);
        //surfaces.remove(i);
        //surfaces.add(0, top);
      }
      break;

    case MouseEvent.MOUSE_DRAGGED:
      drag = true;
      if (dragged != null)
        dragged.moveTo(x, y);
      break;

    case MouseEvent.MOUSE_RELEASED:
      drag = false;
      dragged = null;
      break;
    }
  }





  public void keyEvent(KeyEvent e) {
    float speed;
    CornerPinSurface s;
    //seeciona a superficie para fazer o ajuste fino
    if (e.isShiftDown()) {
      speed = 10;
    } 
    else {
      speed = 2;
    } 

    switch (e.getKeyCode()) {

    case KeyEvent.VK_1:
      surfaceSelected=0;
      parent.println("Superficie " + surfaceSelected + " Selecionada");
      break;

    case KeyEvent.VK_2:
      surfaceSelected=1;
      parent.println("Superficie " + surfaceSelected + " Selecionada");

      break;

    case KeyEvent.VK_LEFT : 
      if (calibrate ==true && drag == false) {
        parent.println("Esquerda...");

        s = (CornerPinSurface)surfaces.get(surfaceSelected);
        if (s.meshClicked!=-1) {

          dragged = s.mesh[s.meshClicked];
          float xx = s.mesh[s.meshClicked].x - 0.05F*speed;
          float yy = s.mesh[s.meshClicked].y;
          dragged.moveTo(xx, yy);
        }
      }

      break;
    case KeyEvent.VK_RIGHT: 

      if (calibrate ==true && drag == false) {
        parent.println("Direitra...");
        s = (CornerPinSurface)surfaces.get(surfaceSelected);
        if (s.meshClicked!=-1) {
          dragged = s.mesh[s.meshClicked];
          float xx = s.mesh[s.meshClicked].x + 0.05F*speed;
          float yy = s.mesh[s.meshClicked].y;
          dragged.moveTo(xx, yy);
        }
      }


      break;
    case KeyEvent.VK_UP   : 

      if (calibrate ==true && drag == false ) {
        parent.println("Cima...");
        s = (CornerPinSurface)surfaces.get(surfaceSelected);
        if (s.meshClicked!=-1) {

          dragged = s.mesh[s.meshClicked];
          float xx = s.mesh[s.meshClicked].x;
          float yy = s.mesh[s.meshClicked].y - 0.05F*speed;
          dragged.moveTo(xx, yy);
        }
      }


      break;
    case KeyEvent.VK_DOWN : 

      if (calibrate ==true  && drag == false) {
        parent.println("baixo...");
        s = (CornerPinSurface)surfaces.get(surfaceSelected);
        if (s.meshClicked!=-1) {

          dragged = s.mesh[s.meshClicked];
          float xx = s.mesh[s.meshClicked].x;
          float yy = s.mesh[s.meshClicked].y + 0.05F*speed;
          dragged.moveTo(xx, yy);
        }
      }


      break;
    default:
      dragged = null;
      break;
    }
  }
}

