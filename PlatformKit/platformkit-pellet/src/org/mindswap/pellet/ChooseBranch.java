/*
 * Created on Oct 11, 2006
 */
package org.mindswap.pellet;

import org.mindswap.pellet.utils.ATermUtils;

import aterm.ATermAppl;

public class ChooseBranch extends DisjunctionBranch {
    public ChooseBranch( ABox abox, CompletionStrategy completion, Node x, ATermAppl c, DependencySet ds ) {
        super( abox, completion, x, c, ds, new ATermAppl[] { c, ATermUtils.negate(c)} );    
    }
    
    protected String getDebugMsg() {
        return "CHOS: Branch (" + branch + ") try (" + (tryNext + 1) + "/" + tryCount
            + ") " + ind.getName() + " " +  disj[tryNext];
    }
}
