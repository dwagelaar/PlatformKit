/* Generated By:JavaCC: Do not edit this line. KRSS2ParserConstants.java */
package de.uulm.ecs.ai.owl.krssparser;
          /*
 * Copyright (C) 2007, Ulm University
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
public interface KRSS2ParserConstants {

  int EOF = 0;
  int URI_START = 8;
  int URI_END = 9;
  int TOP = 11;
  int BOTTOM = 12;
  int NIL = 13;
  int TRUE = 14;
  int OPENPAR = 15;
  int CLOSEPAR = 16;
  int ENDTBOX = 17;
  int ENDABOX = 18;
  int PRIMITIVECONCEPT = 19;
  int DEFINEPRIMITIVECONCEPT = 20;
  int DEFINECONCEPT = 21;
  int DEFINEPRIMITIVEROLE = 22;
  int SUBROLE = 23;
  int TRANSITIVE = 24;
  int TRANSITIVE_ATTRIBUTE = 25;
  int SYMMETRIC_ATTRIBUTE = 26;
  int RANGE_ATTRIBUTE = 27;
  int DOMAIN_ATTRIBUTE = 28;
  int INVERSE_ATTRIBUTE = 29;
  int REFLEXIVE = 30;
  int DPOINT = 31;
  int PARENTS = 32;
  int PARENT = 33;
  int ENUM = 34;
  int RANGE = 35;
  int AND = 36;
  int OR = 37;
  int NOT = 38;
  int ALL = 39;
  int SOME = 40;
  int ATLEAST = 41;
  int ATMOST = 42;
  int EXACTLY = 43;
  int INSTANCE = 44;
  int RELATED = 45;
  int EQUAL = 46;
  int DISTINCT = 47;
  int IMPLIES = 48;
  int INT = 49;
  int NCNAME1 = 50;
  int NCCHAR_FULL = 51;
  int NCCHAR1 = 52;

  int DEFAULT = 0;
  int IN_URI = 1;

  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\r\"",
    "\"\\\"\"",
    "\"|\"",
    "\"\\\\\"",
    "\"\\n\"",
    "\"<\"",
    "\">\"",
    "<token of kind 10>",
    "<TOP>",
    "<BOTTOM>",
    "<NIL>",
    "<TRUE>",
    "\"(\"",
    "\")\"",
    "\"end-tbox\"",
    "\"end-abox\"",
    "\"primitive-concept\"",
    "\"define-primitive-concept\"",
    "\"define-concept\"",
    "\"define-primitive-role\"",
    "\"subrole\"",
    "<TRANSITIVE>",
    "<TRANSITIVE_ATTRIBUTE>",
    "\":symmetric\"",
    "\":range\"",
    "\":domain\"",
    "\":inverse\"",
    "\":reflexive\"",
    "\":\"",
    "\":parents\"",
    "\":parent\"",
    "\"enum\"",
    "\"range\"",
    "<AND>",
    "<OR>",
    "<NOT>",
    "<ALL>",
    "<SOME>",
    "<ATLEAST>",
    "<ATMOST>",
    "<EXACTLY>",
    "\"instance\"",
    "\"related\"",
    "\"equal\"",
    "\"distinct\"",
    "\"implies\"",
    "<INT>",
    "<NCNAME1>",
    "<NCCHAR_FULL>",
    "<NCCHAR1>",
  };

}
