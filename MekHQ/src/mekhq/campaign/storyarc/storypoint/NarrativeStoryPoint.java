/*
 * StoryPoint.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.storyarc.storypoint;

import megamek.Version;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.storyarc.StoryPoint;
import mekhq.gui.dialog.StoryNarrativeDialog;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;

/**
 * This story point creates a {@link StoryNarrativeDialog StoryNarrativeDialog} with a simple narrative description.
 */
public class NarrativeStoryPoint extends StoryPoint {

    String title;
    String narrative;

    public NarrativeStoryPoint() {
        super();
    }

    @Override
    public String getTitle() {
        return title;
    }

    public String getNarrative() {
        return narrative;
    }

    @Override
    public void start() {
        super.start();
        final StoryNarrativeDialog narrativeDialog = new StoryNarrativeDialog(null, this);
        narrativeDialog.setVisible(true);
        complete();
    }

    @Override
    public String getResult() {
        //this one has no variation
        return "";
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent++);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "title", title);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "narrative", narrative);
        writeToXmlEnd(pw1, --indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node wn, Campaign c, Version version) throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("title")) {
                    title =wn2.getTextContent().trim();
                } else if (wn2.getNodeName().equalsIgnoreCase("narrative")) {
                    narrative =wn2.getTextContent().trim();
                }
            } catch (Exception e) {
                LogManager.getLogger().error(e);
            }
        }
    }
}
