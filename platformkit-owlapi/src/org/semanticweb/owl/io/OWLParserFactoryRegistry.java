package org.semanticweb.owl.io;

import org.semanticweb.owl.model.OWLException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/*
 * Copyright (C) 2006, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 15-Nov-2006<br><br>
 * <p/>
 * The <code>OWLParserFactoryRegistry</code> provides a central point for
 * the registration of parser factories that create parsers to parse OWL
 * ontologies.  The registry is typically used by at least one type of ontology
 * factory for loading ontologies whose concrete representations are contained
 * in some kind of document.
 */
public class OWLParserFactoryRegistry {

    private static OWLParserFactoryRegistry instance;

    private List<OWLParserFactory> parserFactories;


    private OWLParserFactoryRegistry() {
        parserFactories = new ArrayList<OWLParserFactory>();
    }


    public static synchronized OWLParserFactoryRegistry getInstance() {
        if (instance == null) {
            instance = new OWLParserFactoryRegistry();
        }
        return instance;
    }


    public void clearParserFactories() {
        parserFactories.clear();
    }


    public List<OWLParserFactory> getParserFactories() {
        return Collections.unmodifiableList(parserFactories);
    }


    public void registerParserFactory(OWLParserFactory parserFactory) {
        parserFactories.add(0, parserFactory);
    }


    public void unregisterParserFactory(OWLParserFactory parserFactory) {
        parserFactories.remove(parserFactory);
    }
}
