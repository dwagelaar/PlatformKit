PlatformKit-Java build-ontology README
======================================

This document explains how to run the ontology building scripts:
- build-ontology/jre/build.xml
- build-ontology/swt/build.xml

Hardware requirements
---------------------

For the "jre" ontologies, have the following ready:
- minimum 4GB RAM
- 8 Intel Core2 class CPUs

Software requirements
---------------------

Make sure you have the following Eclipse plugins/features installed:
- Eclipse Modeling Tools (includes EMF/UML2/...)
- ATL version 3.1 or higher (http://soft.vub.ac.be/eclipse/update-3.5)
- IODT 1.1.2 (http://www.alphaworks.ibm.com/tech/semanticstk/download)

Prepare local webserver
-----------------------

- Set up a local webserver (accessible via http://localhost)
- Share the files "platform.owl", "isa.owl" and "java.owl" under http://localhost/~dennis/platformkit/2010_1/

Run the build.xml scripts
-------------------------

- Load the "empty.owl" file in the IODT ontology editor to initialise IODT
- Right-click the "build.xml" file you want to run and select "Run As -> Ant Build..."
- Under "Refresh", select "Refresh resources upon completion" and "The project containing the selected resource"
- Under "JRE", select "Run in the same JRE as the workspace"
- Under "Common", "Standard Input and Output", select "File:", and fill in "${workspace_loc:/platformkit-java/build-ontology/jre/build.log}" (replace "jre" by "swt" as needed)
- Click "Run"

The ontologies will now be generated. This takes around 20-30 minutes on the hardware configuration mentioned above.

Adapt generated ontologies
--------------------------

The generated ontologies need to be adapted and finalised by hand in the following way:
- Replace all occurrences of "http://localhost/~dennis/platformkit/2010_1/" by "http://soft.vub.ac.be/platformkit/2010/1/" (vi: ":%s/http:\/\/localhost\/\~dennis\/platformkit\/2010_1\//http:\/\/soft\.vub\.ac\.be\/platformkit\/2010\/1\//g")
- Remove unnecessary namespace declaration at the top (e.g. "ns_19")
- Insert the following attributes inside the "<rdf:RDF />" tag:
	 xmlns="http://soft.vub.ac.be/platformkit/2010/1/j2me-midp-1_0.owl#"
	 xml:base="http://soft.vub.ac.be/platformkit/2010/1/j2me-midp-1_0.owl"
  (where "j2me-midp-1_0.owl" is replaced by the actual generated ontology name)
- Remove the "j2me-midp-1_0" namespace declaration (replace "j2me-midp-1_0" by the actual ontology name)
- Change the "<owl:Ontology rdf:about="...">" URI to an empty string (vi: ":%s/<owl:Ontology rdf:about=".*">/<owl:Ontology rdf:about="">/")
- Remove any trailing "#" tokens from the "<owl:imports rdf:resource="..."/> tags
- Replace all occurrences of "&j2me-midp-1_0;" (replace by actual generated ontology name) by "#" (vi: ":%s/&j2me-midp-1_0;/#/g")
- Replace all occurrences of "j2me-midp-1_0:" by the empty string (vi: ":%s/j2me-midp-1_0://g")

The following can be done in Protégé:
- If the generated ontology does not represent a JRE, remove the generated JRE, JavaVM and JavaBytecode subclasses.
- If the generated ontology represents a JRE, add extra JavaBytecode data (major/minor version, preverified):
  - add JavaBytecode subclass equivalent expression: "java:majorVersionNumber some xsd:integer[>= 45] and java:preverified value false" (replace "45" by actual major version number, add "preverified" clause only when *false*)
  - add JavaBytecode subclass instance data slots: fill in values for "java:majorVersionNumber", "java:minorVersionNumber" and "java:preverified"

