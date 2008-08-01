package org.mindswap.pellet.output;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.mindswap.pellet.taxonomy.Taxonomy;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.Comparators;

import aterm.ATermAppl;

/**
 * <p>
 * Title: Functional Taxonomy Printer
 * </p>
 * 
 * <p>
 * Description: The output of this printer is "functional" in the sense that any
 * taxonomy has only a single printed form. I.e., the output here is intended to
 * be unchanged by reorderings of sibling nodes in the classification algorithm.
 * It was developed as a way to compare the output of alternative classification
 * implementations. It is based on the format found in the DL benchmark test
 * data.
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2007
 * </p>
 * 
 * <p>
 * Company: Clark & Parsia, LLC. <http://www.clarkparsia.com>
 * </p>
 * 
 * @author Mike Smith
 */
public class FunctionalTaxonomyPrinter implements TaxonomyPrinter {

	private Taxonomy taxonomy;

	private OutputFormatter out;

	private Set<ATermAppl> bottomEquivalents;

	private Set<ATermAppl> printed;

	public FunctionalTaxonomyPrinter() {
	}

	public void print(Taxonomy taxonomy) {
		print(taxonomy, new OutputFormatter());
	}

	public void print(Taxonomy taxonomy, OutputFormatter out) {

		this.taxonomy = taxonomy;
		this.out = out;

		/*
		 * Note the bottom class (and equivalents) b/c it should only be output
		 * as a subclass if it is the *only* subclass.
		 */
		bottomEquivalents = new TreeSet<ATermAppl>(comparator);
		bottomEquivalents.addAll(taxonomy.getAllEquivalents(ATermUtils.BOTTOM));

		printed = new HashSet<ATermAppl>();

		out.println();

		Set<ATermAppl> sortedTop = new TreeSet<ATermAppl>(comparator);
		sortedTop.addAll(taxonomy.getAllEquivalents(ATermUtils.TOP));

		Set<Set<ATermAppl>> topGroup = Collections.singleton(sortedTop);
		printGroup(topGroup);

		this.taxonomy = null;
		this.out = null;
		this.bottomEquivalents = null;
		this.printed = null;

		out.println();
		out.flush();
	}

	private void printGroup(Collection<? extends Collection<ATermAppl>> concepts) {

		Set<Set<ATermAppl>> nextGroup = new LinkedHashSet<Set<ATermAppl>>();

		for (Iterator<? extends Collection<ATermAppl>> i = concepts.iterator(); i
				.hasNext();) {

			Collection<ATermAppl> eqC = i.next();
			ATermAppl firstC = eqC.iterator().next();

			// Use supers to determine if this has been printed before, if so
			// skip it
			Set<Set<ATermAppl>> supEqs = taxonomy.getSupers(firstC, true);
			if ((supEqs.size() > 1) && printed.contains(firstC)) {
				continue;
			} else {
				printed.add(firstC);
			}

			out.print("(");

			// Print equivalent class group passed in (assume sorted)
			printEqClass(eqC);

			out.print(" ");

			// Print any direct superclasses
			Set<Set<ATermAppl>> sortedSupEqs = new TreeSet<Set<ATermAppl>>(
					setComparator);
			for (Iterator<Set<ATermAppl>> j = supEqs.iterator(); j.hasNext();) {
				Set<ATermAppl> group = new TreeSet<ATermAppl>(comparator);
				group.addAll(j.next());
				sortedSupEqs.add(group);
			}
			printEqClassGroups(sortedSupEqs);

			out.print(" ");

			// Print any direct subclasses
			Set<Set<ATermAppl>> sortedSubEqs = new TreeSet<Set<ATermAppl>>(
					setComparator);
			Set<Set<ATermAppl>> subEqs = taxonomy.getSubs(firstC, true);
			for (Iterator<Set<ATermAppl>> j = subEqs.iterator(); j.hasNext();) {
				Set<ATermAppl> group = new TreeSet<ATermAppl>(comparator);
				group.addAll(j.next());
				sortedSubEqs.add(group);
			}
			printEqClassGroups(sortedSubEqs);
			nextGroup.addAll(sortedSubEqs);

			out.println(")");
		}

		switch (nextGroup.size()) {
		case 0:
			break;
		case 1:
			printGroup(nextGroup);
			break;
		default:
			nextGroup.remove(bottomEquivalents);
			printGroup(nextGroup);
			break;
		}
	}

	private void printEqClass(Collection<ATermAppl> concept) {
		int size = concept.size();
		ATermAppl c = null;

		switch (size) {
		case 0:
			out.print("NIL");
			break;

		case 1:
			c = concept.iterator().next();
			printURI(c);
			break;

		default:
			out.print("(");
			boolean first = true;
			for (Iterator<ATermAppl> i = concept.iterator(); i.hasNext();) {
				c = i.next();
				if (first) {
					first = false;
				} else {
					out.print(" ");
				}
				printURI(c);
			}
			out.print(")");
			break;
		}
	}

	private void printEqClassGroups(
			Collection<? extends Collection<ATermAppl>> concepts) {
		int size = concepts.size();
		Collection<ATermAppl> eqC = null;

		switch (size) {
		case 0:
			out.print("NIL");
			break;

		case 1:
			eqC = concepts.iterator().next();
			out.print("(");
			printEqClass(eqC);
			out.print(")");
			break;

		default:
			out.print("(");
			boolean first = true;
			for (Iterator<? extends Collection<ATermAppl>> i = concepts
					.iterator(); i.hasNext();) {
				eqC = i.next();
				if (first) {
					first = false;
				} else {
					out.print(" ");
				}
				printEqClass(eqC);
			}
			out.print(")");
			break;
		}
	}

	private void printURI(ATermAppl c) {
		String uri = c.getName();
		if (c.equals(ATermUtils.TOP))
			uri = "http://www.w3.org/2002/07/owl#Thing";
		else if (c.equals(ATermUtils.BOTTOM))
			uri = "http://www.w3.org/2002/07/owl#Nothing";

		out.printURI(uri);
	}

	private static final Comparator<ATermAppl> comparator = new Comparator<ATermAppl>() {
		public int compare(ATermAppl c, ATermAppl d) {
			return Comparators.stringComparator.compare(c.getName(), d
					.getName());
		}
	};

	private static final Comparator<Collection<ATermAppl>> setComparator = new Comparator<Collection<ATermAppl>>() {
		public int compare(Collection<ATermAppl> c, Collection<ATermAppl> d) {
			return comparator.compare(c.iterator().next(), d.iterator().next());
		}
	};
}
