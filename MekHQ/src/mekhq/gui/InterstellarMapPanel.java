/*
 * InterstellarMapPanel
 *
 * Created on May 3, 2011
 */

package mekhq.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.SpaceLocation;
import mekhq.campaign.universe.Star;


/**
 * This is not functional yet. Just testing things out.
 * A lot of this code is borrowed from InterstellarMap.java in MekWars
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class InterstellarMapPanel extends javax.swing.JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = -1110105822399704646L;

	private List<Star> stars;
	private JumpPath jumpPath;
	private Campaign campaign;
	InnerStellarMapConfig conf = new InnerStellarMapConfig();
	CampaignGUI hqview;
	private Star selectedStar = null;
	private Planet selectedPlanet = null;
	Point lastMousePos = null;
    int mouseMod = 0;

	public InterstellarMapPanel(Campaign c, CampaignGUI view) {
		campaign = c;
		stars = campaign.getStars();
		hqview = view;
		jumpPath = new JumpPath();

		setBorder(BorderFactory.createLineBorder(Color.black));

		//TODO: get the key listener working
		addKeyListener(new KeyAdapter() {
			/** Handle the key pressed event from the text field. */
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();

				if (keyCode == 37)// left arrow
		        {
		        	conf.offset.y -= conf.scale;
		        } else if (keyCode == 38) // uparrow
		        {
		        	conf.offset.x -= conf.scale;
		        } else if (keyCode == 39)// right arrow
		        {
	     	      conf.offset.y += conf.scale;
		        } else if (keyCode == 40)// down arrow
		        {
		        	conf.offset.x += conf.scale;
		        } else {
		        	return;
		        }
		        repaint();
			}
		});

        addMouseListener(new MouseAdapter() {

        	public void mouseEntered(MouseEvent e) {
                lastMousePos = new Point(e.getX(), e.getY());
            }

            public void mouseExited(MouseEvent e) {
                lastMousePos = null;
            }

            public void mouseReleased(MouseEvent e) {
            	maybeShowPopup(e);
                mouseMod = 0;
            }

            public void mousePressed(MouseEvent e) {
            	maybeShowPopup(e);
            	mouseMod = e.getButton();
            }


            public void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                	JPopupMenu popup = new JPopupMenu();
                	JMenuItem item;
                	item = new JMenuItem("Zoom In");
                	item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            zoom(1.5);
                        }
                    });
                	popup.add(item);
                	item = new JMenuItem("Zoom Out");
                	item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            zoom(0.5);
                        }
                    });
                	popup.add(item);
                	JMenu centerM = new JMenu("Center Map");
                    item = new JMenuItem("On Selected Planet");
                    item.setEnabled(selectedStar != null);
                    if (selectedStar != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                center(selectedStar);
                            }
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Current Location");
                    item.setEnabled(campaign.getCurrentPlanet() != null);
                    if (campaign.getCurrentPlanet() != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                            	selectedStar = campaign.getCurrentPlanet().getStar();
                                center(campaign.getCurrentPlanet());
                            }
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Terra");
                    item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            conf.offset = new Point();
                            repaint();
                        }
                    });
                    centerM.add(item);
                    popup.add(centerM);
                	item = new JMenuItem("Cancel Current Trip");
                	item.setEnabled(null != campaign.getLocation().getJumpPath());
                	item.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            campaign.getLocation().setJumpPath(null);
                            repaint();
                        }
                    });
                	popup.add(item);
                    JMenu menuGM = new JMenu("GM Mode");
                    item = new JMenuItem("Move to selected planet");
                    item.setEnabled(selectedStar != null && campaign.isGM());
                    if (selectedStar != null) {// only add if there is a planet to center on
                        item.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent ae) {
                                campaign.getLocation().setCurrentLocation(selectedStar.getDefaultPlanet().getPointOnSurface());
                                campaign.getLocation().setTransitTime(0.0);
                                campaign.getLocation().setJumpPath(null);
                                jumpPath = new JumpPath();
                                center(selectedStar);
                                hqview.refreshLocation();
                            }
                        });
                    }
                    menuGM.add(item);
                    popup.add(menuGM);
                	popup.show(e.getComponent(), e.getX() + 10, e.getY() + 10);
                }
            }

            public void mouseClicked(MouseEvent e) {
            	if (e.getButton() == MouseEvent.BUTTON1) {

            		if (e.getClickCount() >= 2) {
            			//center and zoom
            			changeSelection(nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY())));
            			if(conf.scale < 4.0) {
            				conf.scale = 4.0;
            			}
            			center(selectedStar);
            		} else {
            			Planet target = nearestNeighbour(scr2mapX(e.getX()), scr2mapY(e.getY()));
                        if(null == target) {
                    		return;
                    	}
                        if(e.isAltDown()) {
                        	//calculate a new jump path from the current location
                        	jumpPath = campaign.calculateJumpPath(campaign.getCurrentPlanetName(), target.getName());
                        	selectedStar = target;
                    		repaint();
                    		hqview.refreshPlanetView();
                    		return;

                        }
                        else if(e.isShiftDown()) {
                        	//add to the existing jump path
                        	Planet lastPlanet = jumpPath.getLastPlanet();
                        	if(null == lastPlanet) {
                        		lastPlanet = campaign.getCurrentPlanet();
                        	}
                        	JumpPath addPath = campaign.calculateJumpPath(lastPlanet.getName(), target.getName());
                  			if(!jumpPath.isEmpty()) {
                  				addPath.removeFirstPlanet();
                  			}
                        	jumpPath.addPlanets(addPath.getPlanets());
                  			selectedStar = target;
                  			repaint();
                  			hqview.refreshPlanetView();
                  			return;
                        }
                    	changeSelection(target);
                    	repaint();
            		}
            	}
            }
        });

        addMouseMotionListener(new MouseAdapter() {

        	public void mouseDragged(MouseEvent e) {
                if (mouseMod != MouseEvent.BUTTON1) {
                   return;
                }
                if (lastMousePos != null) {
                    conf.offset.x -= lastMousePos.x - e.getX();
                    conf.offset.y -= lastMousePos.y - e.getY();
                    lastMousePos.x = e.getX();
                    lastMousePos.y = e.getY();
                }
                repaint();
            }

        	public void mouseMoved(MouseEvent e) {

                if (lastMousePos == null) {
                    lastMousePos = new Point(e.getX(), e.getY());
                } else {
                    lastMousePos.x = e.getX();
                    lastMousePos.y = e.getY();
                }
            }
        });

        addMouseWheelListener(new MouseAdapter() {
        	 public void mouseWheelMoved(MouseWheelEvent e) {
        		 zoom(Math.pow(1.5,-1 * e.getWheelRotation()));
        	 }
        });
	}

	public void setCampaign(Campaign c) {
		this.campaign = c;
		this.stars = campaign.getStars();
		repaint();
	}

	public void setJumpPath(JumpPath path) {
		jumpPath = path;
		repaint();
	}

	/**
     * Computes the map-coordinate from the screen koordinate system
     */
    private double scr2mapX(int x) {
        return Math.round((x - getWidth() / 2 - conf.offset.x) / conf.scale);
    }

    private double map2scrX(double x) {
        return Math.round(getWidth() / 2 + x * conf.scale) + conf.offset.x;
    }

    private double scr2mapY(int y) {
        return Math.round((getHeight() / 2 - (y - conf.offset.y)) / conf.scale);
    }

    private double map2scrY(double y) {
        return Math.round(getHeight() / 2 - y * conf.scale) + conf.offset.y;
    }

    public void setSelectedPlanet(Planet p) {
    	selectedStar = p.getStar();
    	selectedPlanet = p;
    	if(conf.scale < 4.0) {
			conf.scale = 4.0;
		}
		center(selectedStar);
    	repaint();
    }

	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.BLACK);
		g2.fillRect(0, 0, getWidth(), getHeight());
		double size = 1 + 5 * Math.log(conf.scale);
        size = Math.max(Math.min(size, conf.maxdotSize), conf.minDotSize);
        Arc2D.Double arc = new Arc2D.Double();
        //first get the jump diameter for selected planet
        if(null != selectedStar && conf.scale > conf.showPlanetNamesThreshold) {
        	double x = map2scrX(selectedStar.getX());
			double y = map2scrY(selectedStar.getY());
			double z = map2scrX(selectedStar.getX() + 30);
			double jumpRadius = (z - x);
			g2.setPaint(Color.DARK_GRAY);
			arc.setArcByCenter(x, y, jumpRadius, 0, 360, Arc2D.OPEN);
			g2.fill(arc);
        }

      //draw a jump path
		for(int i = 0; i < jumpPath.size(); i++) {
			Star planetB = jumpPath.get(i).getStar();
			double x = map2scrX(planetB.getX());
			double y = map2scrY(planetB.getY());
			//lest try rings
			g2.setPaint(Color.WHITE);
			arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
			g2.fill(arc);
			g2.setPaint(Color.BLACK);
			arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
			g2.fill(arc);
			g2.setPaint(Color.WHITE);
			arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
			g2.fill(arc);
			g2.setPaint(Color.BLACK);
			arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
			g2.fill(arc);
			if(i > 0) {
				Star planetA = jumpPath.get(i-1).getStar();
				g2.setPaint(Color.WHITE);
				g2.draw(new Line2D.Double(map2scrX(planetA.getX()), map2scrY(planetA.getY()), map2scrX(planetB.getX()), map2scrY(planetB.getY())));
			}
		}

		//check to see if the unit is traveling on a jump path currently and if so
		//draw this one too, in a different color
		if(null != campaign.getLocation().getJumpPath()) {
			for(int i = 0; i < campaign.getLocation().getJumpPath().size(); i++) {
				Star planetB = campaign.getLocation().getJumpPath().get(i).getStar();
				double x = map2scrX(planetB.getX());
				double y = map2scrY(planetB.getY());
				//lest try rings
				g2.setPaint(Color.YELLOW);
				arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.BLACK);
				arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.YELLOW);
				arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.BLACK);
				arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				if(i > 0) {
					Star planetA = campaign.getLocation().getJumpPath().get(i-1).getStar();
					g2.setPaint(Color.YELLOW);
					g2.draw(new Line2D.Double(map2scrX(planetA.getX()), map2scrY(planetA.getY()), map2scrX(planetB.getX()), map2scrY(planetB.getY())));
				}
			}
		}

		for(Planet planet : stars) {
			double x = map2scrX(planet.getX());
			double y = map2scrY(planet.getY());
			if(planet.equals(campaign.getCurrentPlanet())) {
				//lest try rings
				g2.setPaint(Color.ORANGE);
				arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.BLACK);
				arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.ORANGE);
				arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.BLACK);
				arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
			}
			if(null != selectedStar && selectedStar.equals(planet)) {
				//lest try rings
				g2.setPaint(Color.WHITE);
				arc.setArcByCenter(x, y, size * 1.8, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.BLACK);
				arc.setArcByCenter(x, y, size * 1.6, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.WHITE);
				arc.setArcByCenter(x, y, size * 1.4, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
				g2.setPaint(Color.BLACK);
				arc.setArcByCenter(x, y, size * 1.2, 0, 360, Arc2D.OPEN);
				g2.fill(arc);
			}
			ArrayList<Faction> factions = planet.getCurrentFactions(campaign.getCalendar().getTime());
			for(int i = 0; i < factions.size(); i++) {
				Faction faction = factions.get(i);
				g2.setPaint(faction.getColor());
				arc.setArcByCenter(x, y, size, 0, 360.0 * (1-((double)i)/factions.size()), Arc2D.PIE);
				g2.fill(arc);
			}


		}

		//cycle through planets again and assign names - to make sure names go on outside
		for(Star planet : stars) {
			double x = map2scrX(planet.getX());
			double y = map2scrY(planet.getY());
			if (conf.showPlanetNamesThreshold == 0 || conf.scale > conf.showPlanetNamesThreshold
					|| jumpPath.contains(planet)
					|| (null != campaign.getLocation().getJumpPath() && campaign.getLocation().getJumpPath().contains(planet))) {
				g2.setPaint(Color.WHITE);
	            g2.drawString(planet.getShortName(), (float)(x+size * 1.8), (float)y);
	        }
		}

	}

	 /**
     * Calculate the nearest neighbour for the given point If anyone has a better algorithm than this stupid kind of shit, please, feel free to exchange my brute force thing... An good idea would be an voronoi diagram and the sweep algorithm from Steven Fortune.
     */
    private Star nearestNeighbour(double x, double y) {
        double minDiff = Double.MAX_VALUE;
        double diff = 0.0;
        Star minPlanet = null;
        for(Star p : stars) {
            diff = Math.sqrt(Math.pow(x - p.getX(), 2) + Math.pow(y - p.getY(), 2));
            if (diff < minDiff) {
                minDiff = diff;
                minPlanet = p;
            }
        }
        return minPlanet;
    }

    private void center(Planet p, int x) {
    	center(p.getStar());
    }
    
    private void center(SpaceLocation l) {
    	center(l.getStar());
    }
    
    /**
     * Activate and Center
     */
    private void center(Star s) {

        if (s == null) {
            return;
        }
        conf.offset.setLocation(-s.getX() * conf.scale, s.getY() * conf.scale);
        repaint();
    }

    private void zoom(double percent) {
    	conf.scale *= percent;
        if (selectedStar != null) {
            conf.offset.setLocation(-selectedStar.getX() * conf.scale, selectedStar.getY() * conf.scale);
        }
        repaint();
    }

    public Star getSelectedStar() {
    	return selectedStar;
    }

    public JumpPath getJumpPath() {
    	return jumpPath;
    }

    private void changeSelection(Star s) {
    	selectedStar = s;
    	selectedPlanet = s.getDefaultPlanet();
    	jumpPath = new JumpPath();
    	hqview.refreshPlanetView();
    }
    
    private void changeSelection(Planet p) {
    	changeSelection(p.getStar());
    	selectedPlanet = p;
    }

    /**
     * All configuration behaviour of InterStellarMap are saved here.
     *
     * @author Imi (immanuel.scholz@gmx.de)
     */
    static public final class InnerStellarMapConfig {
        /**
         * Whether to scale planet dots on zoom or not
         */
        int minDotSize = 3;
        int maxdotSize = 25;
        /**
         * The scaling maximum dimension
         */
        int reverseScaleMax = 100;
        /**
         * The scaling minimum dimension
         */
        int reverseScaleMin = 2;
        /**
         * Threshold to not show planet names. 0 means show always
         */
        double showPlanetNamesThreshold = 3.0;
        /**
         * brightness correction for colors. This is no gamma correction! Gamma correction brightens medium level colors more than extreme ones. 0 means no brightening.
         */
        /**
         * The actual scale factor. 1.0 for default, higher means bigger.
         */
        double scale = 0.5;
        /**
         * The scrolling offset
         */
        Point offset = new Point();
        /**
         * The current selected Planet-id
         */
        int planetID;
    }
}
