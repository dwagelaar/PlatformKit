// The MIT License
//
// Copyright (c) 2003 Ron Alford, Mike Grove, Bijan Parsia, Evren Sirin
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to
// deal in the Software without restriction, including without limitation the
// rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
// sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
// IN THE SOFTWARE.

package org.mindswap.pellet;


import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mindswap.pellet.exceptions.InternalReasonerException;
import org.mindswap.pellet.utils.ATermUtils;
import org.mindswap.pellet.utils.SetUtils;
import org.mindswap.pellet.utils.fsm.TransitionGraph;

import aterm.ATerm;
import aterm.ATermAppl;
import aterm.ATermList;

/*
 * Created on Aug 27, 2003
 *
 */

/**
 * @author Evren Sirin
 *
 */
public class Role {
	public final static String[] TYPES = {"Untyped", "Object", "Datatype", "Annotation", "Ontology"	};
    /**
     * @deprecated Use UNTYPED instead
     */
    final public static int UNDEFINED  = 0;
	final public static int UNTYPED    = 0;
	final public static int OBJECT     = 1;
	final public static int DATATYPE   = 2;
	final public static int ANNOTATION = 3;
	final public static int ONTOLOGY   = 4;
	//***************************************************
	
	private ATermAppl   name;
	
	private int  type = UNTYPED; 
	private Role inverse = null;

	private Set<Role> subRoles = Collections.emptySet();
	private Set<Role> superRoles = Collections.emptySet();
    private Set<Role> disjointRoles = Collections.emptySet();
    private Set subRoleChains = Collections.emptySet();
    
	private Set functionalSupers = Collections.emptySet();
	private Set transitiveSubRoles = Collections.emptySet();
	
	private ATermAppl domain = null;
	private ATermAppl range  = null;

	private Set domains = null;
	private Set ranges  = null;
    
    private TransitionGraph tg;

    public static int TRANSITIVE     = 0x01;
    public static int FUNCTIONAL     = 0x02;
    public static int INV_FUNCTIONAL = 0x04;
    public static int REFLEXIVE      = 0x08;
    public static int IRREFLEXIVE    = 0x10;
    public static int ANTI_SYM       = 0x20;

    public static int SIMPLE         = 0x40;    
    public static int COMPLEX_SUB    = 0x80;
    
    public static int FORCE_SIMPLE   = 0x100;
    
    private int flags = SIMPLE;
    
    
    /*
     * Explanation related 
     */
	private DependencySet explainAntisymmetric  = DependencySet.INDEPENDENT;
    private DependencySet explainDomain = DependencySet.INDEPENDENT;
	private DependencySet explainRange  = DependencySet.INDEPENDENT;
	private DependencySet explainFunctional = DependencySet.INDEPENDENT;
	private DependencySet explainIrreflexive  = DependencySet.INDEPENDENT;
	private DependencySet explainReflexive  = DependencySet.INDEPENDENT;
	private DependencySet explainSymmetric = DependencySet.INDEPENDENT;
	private DependencySet explainTransitive = DependencySet.INDEPENDENT;	
	private DependencySet explainInverseFunctional = DependencySet.INDEPENDENT;
	private DependencySet explainInverse = DependencySet.INDEPENDENT;
	//private Map explainInverse = new HashMap();
	private Map explainSub = new HashMap();
	private Map explainSup = new HashMap();
	private Map explainDomains = null;
	private Map explainRanges = null;
	
	
	
	public Role(ATermAppl name) {
		this(name, UNTYPED);
	}
   
	public Role(ATermAppl name, int type) {
		this.name = name;
		this.type = type;
		
		addSubRole(this, DependencySet.INDEPENDENT);
		addSuperRole(this, DependencySet.INDEPENDENT);
	}
	
	public boolean equals(Object o) {
		if(o instanceof Role)
			return name == ((Role)o).getName();		
		
		return false;
	}

	public String toString() {
		return name.getArity() == 0 ? name.getName() : name.toString();
	}
	
	public String debugString() {
		String str = "(" + TYPES[type] + "Role " + name;
		if(isTransitive()) str += " Transitive";
        if(isReflexive()) str += " Reflexive";
        if(isIrreflexive()) str += " Irreflexive";
		if(isSymmetric()) str += " Symmetric";
        if(isAntisymmetric()) str += " Antisymmetric";
		if(isFunctional()) str += " Functional";
		if(isInverseFunctional()) str += " InverseFunctional";
		if(hasComplexSubRole()) str += " ComplexSubRole";
		if(isSimple()) str += " Simple";
		if(type == OBJECT || type == DATATYPE) {
			if(domain != null) str += " domain=" + domain;
			if(range != null) str += " range=" + range;
			str += " superPropertyOf=" + subRoles;
			str += " subPropertyOf=" + superRoles;
            str += " hasSubPropertyChain=" + subRoleChains;
            str += " disjointWith=" + disjointRoles;
		}
		str += ")";
		
		return str; 
	}

	
	/**
	 * Add a sub role chain without dependency tracking information
	 * @param chain
	 */
    public void addSubRoleChain( ATermList chain ) {
    	addSubRoleChain(chain, DependencySet.INDEPENDENT);
    }
    
    /**
     * Add a sub role chain with dependency tracking.
     * 
     * @param chain List of role names of at least length 2.
     * @param ds
     */
    public void addSubRoleChain( ATermList chain, DependencySet ds) {
        if( chain.isEmpty() )
            throw new InternalReasonerException( "Adding a subproperty chain that is empty!" );
        else if( chain.getLength() == 1 )
            throw new InternalReasonerException( "Adding a subproperty chain that has a single element!" );

        subRoleChains = SetUtils.add( chain, subRoleChains );    
        explainSub.put(chain, ds);
        setSimple( false );
        
        boolean transitiveChain = ATermUtils.isTransitiveChain( chain, name );
        if( transitiveChain ) {
        	if( !isTransitive() ) 
        		setTransitive( true, ds );
        }
        else
        	setHasComplexSubRole( true );        	
    }

    public void removeSubRoleChain(ATermList chain) {
        subRoleChains = SetUtils.remove( chain, subRoleChains );
        explainSub.remove(chain);
        if( isTransitive() && ATermUtils.isTransitiveChain( chain, name ) ) {  
            setTransitive( false, null);
        }
    }
    
    public void removeSubRoleChains() {
        subRoleChains = Collections.emptySet();
        
        if( isTransitive() )         
            setTransitive( false, null);    
    }

	/**
	 * r is subrole of this role
     * 
	 * @param r
	 */
	
	public void addSubRole(Role r) {
		DependencySet ds = PelletOptions.USE_TRACING
		? new DependencySet(ATermUtils.makeSubProp(r.getName(), this.getName()))
		: DependencySet.INDEPENDENT;	
		addSubRole(r, ds);
	}
	
	/**
	 * Add sub role with depedency set.
     * 
	 * @param r subrole of this role
	 * @param ds
	 */
	public void addSubRole(Role r, DependencySet ds) {
		if (PelletOptions.USE_TRACING && explainSub.get(r.getName()) == null) 
			explainSub.put(r.getName(), ds);
		
		subRoles = SetUtils.add( r, subRoles );
		explainSub.put(r.getName(), ds);
	}
    
    public void removeSubRole(Role r) {
        subRoles = SetUtils.remove( r, subRoles );
    }
    
	/**
	 * r is superrole of this role
     * 
	 * @param r
	 */
	public void addSuperRole(Role r) {
		DependencySet ds = PelletOptions.USE_TRACING
		? new DependencySet(ATermUtils.makeSubProp(name, r.getName()))
		: DependencySet.INDEPENDENT;
		addSuperRole(r, ds);
	}
	
	public void addSuperRole(Role r, DependencySet ds) {
        superRoles = SetUtils.add( r, superRoles );
        explainSup.put(r.getName(), ds);
	}
    
    public void addDisjointRole(Role r) {        
        disjointRoles = SetUtils.add( r, disjointRoles );
    }
	
	void normalize() {
		if( domains != null ) {
		    if( domains.size() == 1 ) {
		        domain = (ATermAppl) domains.iterator().next();
		        explainDomain = PelletOptions.USE_TRACING
		        ? getExplainDomain(domain)
		        : DependencySet.INDEPENDENT;
		    } else {
		        domain = ATermUtils.makeSimplifiedAnd( domains );
		        explainDomain = DependencySet.INDEPENDENT;
		        if (PelletOptions.USE_TRACING) {
		        	for (Iterator i = domains.iterator(); i.hasNext();) {
		        		ATermAppl d = (ATermAppl) i.next();
		        		if (PelletOptions.USE_TRACING)
		        			explainDomain = explainDomain.union(getExplainDomain(d), true);
		        	}	
		        }
		    }
//			domains = null;
//			explainDomains = null;
		}
		if( ranges != null ) {
		    if( ranges.size() == 1 ) {
		        range = (ATermAppl) ranges.iterator().next();
		        explainRange = PelletOptions.USE_TRACING
		        ? getExplainRange(range)
		        : DependencySet.INDEPENDENT;
		    } else {
		        range = ATermUtils.makeSimplifiedAnd( ranges );
		        explainRange = DependencySet.INDEPENDENT;
		        if (PelletOptions.USE_TRACING) {
		        	for (Iterator i = ranges.iterator(); i.hasNext();) {
		        		ATermAppl r = (ATermAppl) i.next();
		        		if (PelletOptions.USE_TRACING)
		        			explainRange = explainRange.union(getExplainRange(r), true);
		        	}
		        }
		    }
//			ranges = null;
//			explainRanges = null;
		}
	}
	
	public void addDomain(ATermAppl a) {
		addDomain(a, DependencySet.INDEPENDENT);
	}
	
	public void addDomain(ATermAppl a, DependencySet ds) {
	    if( domains == null )
	        domains = new HashSet();
	    if ( explainDomains == null ) 
	    	explainDomains = new HashMap();
	    
	    ATermAppl normalized = ATermUtils.normalize(a);
	    domains.add( normalized );
	    explainDomains.put( normalized, ds );
	}
	
	public void addRange(ATermAppl a) {
		addRange(a, DependencySet.INDEPENDENT);
	}
	
	public void addRange(ATermAppl a, DependencySet ds) {
	    if( ranges == null )
	        ranges = new HashSet();
	    if (explainRanges == null) 
	    	explainRanges = new HashMap();
	    
	    ATermAppl normalized = ATermUtils.normalize(a);
	    ranges.add( normalized );
	    explainRanges.put( normalized, ds );
	}

	public void addDomains(Set a) {
	    if( domains == null )
	        domains = new HashSet();
	    if ( explainDomains == null)
	    	explainDomains = new HashMap();
	    	
	    domains.addAll( a );
	}
	
	public void addRanges(Set a) {
	    if( ranges == null )
	        ranges = new HashSet();
	    if (explainRanges == null)
	    	explainRanges = new HashMap();
	    ranges.addAll( a );
	}
	
	public boolean isObjectRole() {
		return type == OBJECT; 
	}		

	public boolean isDatatypeRole() {
		return type == DATATYPE; 
	}

	public boolean isOntologyRole() {
		return type == Role.ONTOLOGY; 
	}	
	/**
	 * check if a role is declared as datatype property
	 */
	public boolean isAnnotationRole() {
		return type == Role.ANNOTATION; 
	}
    
    public boolean isUntypedRole() {
        return type == UNTYPED;
    }

	/**
	 * @return
	 */
	public Role getInverse() {
		return inverse;
	}

	public boolean hasNamedInverse() {
		return inverse != null && !inverse.isAnon();
	}
	
	public boolean hasComplexSubRole() {
		return (flags & COMPLEX_SUB) != 0;
	}

	public boolean isFunctional() {
		return (flags & FUNCTIONAL) != 0;
	}	

	public boolean isInverseFunctional() {
		return (flags & INV_FUNCTIONAL) != 0; 
	}

	public boolean isSymmetric() {
		return inverse != null && inverse.equals(this); 
	}
    
    public boolean isAntisymmetric() {
        return (flags & ANTI_SYM) != 0; 
    }    

	public boolean isTransitive() {
		return (flags & TRANSITIVE) != 0;
	}
    
    public boolean isReflexive() {
        return (flags & REFLEXIVE) != 0;
    }   
	
    public boolean isIrreflexive() {
        return (flags & IRREFLEXIVE) != 0;
    }   
    
	public boolean isAnon() {
	    return name.getArity() != 0;
	}
	
	public ATermAppl getName() {
		return name;
	}

	public ATermAppl getDomain() {
		return domain;
	}

	public ATermAppl getRange() {
		return range;
	}

	public Set getDomains() {
		return domains;
	}

	public Set getRanges() {
		return ranges;
	}

	public Set<Role> getSubRoles() {
		return Collections.unmodifiableSet( subRoles );
	}
    
    public Set getSubRoleChains() {
        return subRoleChains;
    }    

	/**
	 * @return
	 */
	public Set<Role> getSuperRoles() {
		return Collections.unmodifiableSet( superRoles );
	}
    
    public Set<Role> getDisjointRoles() {
        return Collections.unmodifiableSet( disjointRoles );
    }

	/**
	 * @return
	 */
	public int getType() {
		return type;
	}

	public String getTypeName() {
		return TYPES[type];
	}
	
	public boolean isSubRoleOf(Role r) {
		return superRoles.contains(r);
	}

	public boolean isSuperRoleOf(Role r) {
		return subRoles.contains(r);
	}

	public void setInverse(Role term) {
		setInverse(term, DependencySet.INDEPENDENT);
	}
	
	public void setInverse(Role term, DependencySet ds) {
		inverse = term;
		explainInverse = ds;
	}

	public void setFunctional( boolean b ) {
		DependencySet ds = DependencySet.INDEPENDENT;
		setFunctional(b, ds);
	}
	
	public void setFunctional( boolean b, DependencySet ds) {
        if( b ) {
            flags |= FUNCTIONAL;
            explainFunctional = ds;
        } else {
            flags &= ~FUNCTIONAL;
            explainFunctional = DependencySet.INDEPENDENT;
        }
	}

    public void setInverseFunctional(boolean b) {
    	setInverseFunctional( b, DependencySet.INDEPENDENT );
    }
    
    public void setInverseFunctional(boolean b, DependencySet ds) {
        if( b ) {
            flags |= INV_FUNCTIONAL;
            explainInverseFunctional = ds;
        } else {
            flags &= ~INV_FUNCTIONAL;
            explainInverseFunctional = DependencySet.INDEPENDENT;
        }
    }
    
	public void setTransitive(boolean b) {
		DependencySet ds = PelletOptions.USE_TRACING
		? new DependencySet(ATermUtils.makeTransitive(name))
		: DependencySet.INDEPENDENT;
		
		setTransitive(b, ds);
	}
	
	public void setTransitive(boolean b, DependencySet ds) {
		
        ATermList roleChain = ATermUtils.makeList( new ATerm[] { name, name } );
        if( b ) {
            flags |= TRANSITIVE;
            explainTransitive = ds;
            addSubRoleChain( roleChain, ds );
        }
        else {
            flags &= ~TRANSITIVE;
            explainTransitive = ds;
            removeSubRoleChain( roleChain );
        }
	}
    
	public void setReflexive(boolean b) {
		setReflexive(b, DependencySet.INDEPENDENT);
	}
	
    public void setReflexive(boolean b, DependencySet ds) {
        if( b )
            flags |= REFLEXIVE;
        else
            flags &= ~REFLEXIVE;
        explainReflexive = ds;
    }        
	
    public void setIrreflexive(boolean b) {
    	setIrreflexive(b, DependencySet.INDEPENDENT);
    }
    
    public void setIrreflexive(boolean b, DependencySet ds) {
        if( b )
            flags |= IRREFLEXIVE;
        else
            flags &= ~IRREFLEXIVE;  
        explainIrreflexive = ds;
    }        
    
    public void setAntisymmetric(boolean b) {
    	setAntisymmetric(b, DependencySet.INDEPENDENT);
    }
    
    public void setAntisymmetric(boolean b, DependencySet ds) {
        if( b ) {
            flags |= ANTI_SYM;
        } else {
            flags &= ~ANTI_SYM;
        }
        explainAntisymmetric = ds;
    }        
    
    public void setHasComplexSubRole(boolean b) {
    	if( b == hasComplexSubRole() )
    		return;
    		
        if( b )
            flags |= COMPLEX_SUB;
        else
            flags &= ~COMPLEX_SUB;      	

        if( inverse != null )
        	inverse.setHasComplexSubRole( b );        
        
        if( b )
        	setSimple( false );
    } 

	public void setType(int type) {
		this.type = type;
	}
    
	/**
	 * 
	 * @param subRoleChains
	 * @param dependencies map from role names (or lists) to depedencies
	 */
    public void setSubRolesAndChains(Set subRoles, Set subRoleChains, Map dependencies) {
        this.subRoles = subRoles;
    	this.subRoleChains = subRoleChains;
        this.explainSub = dependencies;
    }
	
    
    /**
     * @deprecated Use setSubRolesAndChains now.
     * @param subRoles
     * @param subRoleChains
     * @param dependencies
     */
    public void setSubRoleChains(Set subRoleChains) {
        this.subRoleChains = subRoleChains;
        
    }
    
	/**
	 * @deprecated Use setSubRolesAndChains now.
	 * @param subRoles The subRoles to set.
	 */
	public void setSubRoles(Set subRoles) {
		this.subRoles = subRoles;
	}
	
	/**
	 * @param superRoles The superRoles to set.
	 * @param dependencies A map from role names (or role lists) to dependency sets.
	 */
	public void setSuperRoles(Set superRoles) {
		this.superRoles = superRoles;
	}

	/**
	 * @return Returns the functionalSuper.
	 */
	public Set getFunctionalSupers() {
		return functionalSupers;
	}
	
	/**
	 * @param functionalSuper The functionalSuper to set.
	 */
	public void addFunctionalSuper(Role r) {
        for(Iterator i = functionalSupers.iterator(); i.hasNext();) {
            Role fs = (Role) i.next();
            if( fs.isSubRoleOf( r ) ) {
                functionalSupers = SetUtils.remove( fs, functionalSupers );                
                break;
            }
            else if( r.isSubRoleOf( fs ) ) {
                return;
            }
        }
        functionalSupers = SetUtils.add( r, functionalSupers );    
	}

	public void setForceSimple( boolean b ) {
    	if( b == isForceSimple() )
    		return;
    	
        if( b )
            flags |= FORCE_SIMPLE;
        else
            flags &= ~FORCE_SIMPLE;  

        if( inverse != null )
        	inverse.setForceSimple( b );
	}
	
    public boolean isForceSimple() {
        return (flags & FORCE_SIMPLE) != 0;
    }
	
    public boolean isSimple() {
        return (flags & SIMPLE) != 0;
    }
    
    void setSimple( boolean b ) {
    	if( b == isSimple() )
    		return;
    	
        if( b )
            flags |= SIMPLE;
        else
            flags &= ~SIMPLE;  
        
        if( inverse != null )
        	inverse.setSimple( b );
    }

//	public boolean isSimple() {
//	    return !isTransitive() && transitiveSubRoles.isEmpty();
//	}
	
	/**
	 * @return Returns transitive sub roles.
	 */
	public Set getTransitiveSubRoles() {
		return transitiveSubRoles;
	}
	
	/**
	 * @param r The transtive sub role to add.
	 */
	public void addTransitiveSubRole( Role r ) {
		setSimple( false );
		
	    if( transitiveSubRoles.isEmpty() ) {
	        transitiveSubRoles = SetUtils.singleton( r );
	    }
	    else if( transitiveSubRoles.size() == 1 ) {
            Role tsr = (Role) transitiveSubRoles.iterator().next();
	        if( tsr.isSubRoleOf( r ) ) {
	            transitiveSubRoles = SetUtils.singleton( r );
	        }
	        else if( !r.isSubRoleOf( tsr ) ) {
	            transitiveSubRoles = new HashSet( 2 );
	            transitiveSubRoles.add( tsr );
	            transitiveSubRoles.add( r );
	        }	            
        }
        else {
	        for(Iterator i = transitiveSubRoles.iterator(); i.hasNext();) {
                Role tsr = (Role) i.next();
		        if( tsr.isSubRoleOf( r ) ) {
		            transitiveSubRoles.remove( tsr );
		            transitiveSubRoles.add( r );
		            return;
		        }
		        else if( r.isSubRoleOf( tsr ) ) {
		            return;
		        }
            }
	        transitiveSubRoles.add( r );
        }	    
	}

    protected Role copy(Map conversion) {
        Role newr;
        if(conversion.containsKey(this)) {
            newr = (Role)conversion.get(this);
        } else {
            newr = new Role(name);
            conversion.put(this, newr);
        }
        // first, we need to copy all the easy stuff: 
        // name, type, domains, ranges, and the assorted booleans
        // newr.name should be set already
        newr.type = type;
        if(domains != null) {
            newr.domains = (Set)((HashSet)domains).clone();
        } else {
            newr.domains = null;
        }
        if(ranges != null) {
            newr.ranges = (Set)((HashSet)ranges).clone();
        } else {
            newr.ranges = null;
        }
        newr.domain = domain;
        newr.range = range;

	    newr.flags = flags;

        // fix the inverse
        if(inverse != null) {
            if(!conversion.containsKey(inverse)) {
                conversion.put(inverse, new Role(inverse.name));
            }
            newr.inverse = (Role)conversion.get(inverse);
        } else {
            newr.inverse = null;
        }
        // fix the subRoles, superRoles, functionalSupers, and
        // transitiveSubRoles
        newr.subRoles = roleSetCopy(subRoles, conversion);
        newr.superRoles = roleSetCopy(superRoles, conversion);
        newr.disjointRoles = roleSetCopy(disjointRoles, conversion);
        newr.functionalSupers = roleSetCopy(functionalSupers, conversion);
//        newr.transitiveSubRoles = roleSetCopy(transitiveSubRoles, conversion);
        
        // Explanation related stuff
        newr.explainAntisymmetric = this.explainAntisymmetric;
        newr.explainDomain = this.explainDomain;
        newr.explainDomains = explainDomains == null ? null : new HashMap(explainDomains);
        newr.explainFunctional = this.explainFunctional;
        newr.explainInverse = this.explainInverse;
        newr.explainInverseFunctional = this.explainInverseFunctional;
        newr.explainIrreflexive = this.explainIrreflexive;
        newr.explainRange = this.explainRange;
        newr.explainRanges = explainRanges == null ? null : new HashMap(explainRanges);
        newr.explainReflexive = this.explainReflexive;
        newr.explainSub = new HashMap(explainSub);
        newr.explainSup = new HashMap(explainSup);
        newr.explainSymmetric = this.explainSymmetric;
        newr.explainTransitive = this.explainTransitive;
        
        return newr;
    }
    private Set roleSetCopy(Set roleset, Map conversion) {
        if(roleset == null) {
            return null;
        }
        Set newroles = new HashSet();
        Iterator j = roleset.iterator();
        while(j.hasNext()) {
            Role role = (Role)j.next();
            if(!conversion.containsKey(role)) {
                conversion.put(role, new Role(role.name));
            }
            newroles.add(conversion.get(role));
        }
        return newroles;
    }

    public void setFSM( TransitionGraph tg ) {
        this.tg = tg;        
    }

    public TransitionGraph getFSM() {
        return tg;        
    }
    
    /* Dependency Retreival */
    
    public DependencySet getExplainAntisymmetric() {
    	return explainAntisymmetric;
    }
    
    public DependencySet getExplainDomain() {
    	return explainDomain;
    }
    
    public DependencySet getExplainDomain(ATermAppl a) {
    	DependencySet ds = null;
    	if (explainDomains != null) 
    		ds = (DependencySet) explainDomains.get(a);
    	return ds;
    }
    
    public DependencySet getExplainFunctional() {
    	return explainFunctional;
    }
    
    public DependencySet getExplainInverse() {
    	//return (DependencySet) explainInverse.get(r);
    	return explainInverse;
    }
    
    public DependencySet getExplainInverseFunctional() {
    	return explainInverseFunctional;
    }
    
    public DependencySet getExplainIrreflexive() {
    	return explainIrreflexive;
    }
    
    public DependencySet getExplainRange() {
    	return explainRange;
    }
    
    public DependencySet getExplainRange(ATermAppl a) {
    	DependencySet ds = null;
    	if (explainRanges != null) 
    		ds = (DependencySet) explainRanges.get(a);
    	return ds;
    }
    
    public DependencySet getExplainReflexive() {
    	return explainReflexive;
    }
    
    public DependencySet getExplainSub(ATerm r) {
    	DependencySet ds = (DependencySet) explainSub.get(r);
    	if (ds == null)
    		return DependencySet.INDEPENDENT;
    	return ds;
    }
    
    public DependencySet getExplainSuper(ATerm r) {
    	DependencySet ds = (DependencySet) explainSup.get(r);
    	if (ds == null)
    		return DependencySet.INDEPENDENT;
    	return ds;
    }
    
    public DependencySet getExplainSymmetric() {
    	return explainSymmetric;
    }
    
    public DependencySet getExplainTransitive() {
    	return explainTransitive;
    }

}
