package org.mindswap.pellet.rete;

import java.util.ArrayList;
import java.util.List;

import org.mindswap.pellet.utils.URIUtils;

public class Triple {
	protected Term subj, pred, obj;
	
	public Triple() {
		
	}
	
	public Triple(Term s, Term p, Term o) {
		if( s == null || p == null || o == null )
			throw new NullPointerException();

		this.subj = s;
		this.pred = p;
		this.obj = o;				
		
	}
	
	public List getList() {
		List pattern = new ArrayList();
		pattern.add(subj);
		pattern.add(pred);
		pattern.add(obj);
		return pattern;
	}

	/**
	 * @return Returns the obj.
	 */
	public Term getObj() {
		return obj;
	}

	/**
	 * @param obj The obj to set.
	 */
	public void setObj(Term obj) {
		this.obj = obj;
	}

	/**
	 * @return Returns the pred.
	 */
	public Term getPred() {
		return pred;
	}

	/**
	 * @param pred The pred to set.
	 */
	public void setPred(Term pred) {
		this.pred = pred;
	}

	/**
	 * @return Returns the subj.
	 */
	public Term getSubj() {
		return subj;
	}

	/**
	 * @param subj The subj to set.
	 */
	public void setSubj(Term subj) {
		this.subj = subj;
	}

	public List getVars() {
		List v = new ArrayList();
		if (subj instanceof Variable) v.add(subj);
		if (pred instanceof Variable) v.add(pred);
		if (obj instanceof Variable) v.add(obj);
		
		// TODO Auto-generated method stub
		return v;
	}
	
	public String toString() {
		if( pred.equals( Constant.TYPE ) ) {
			return format(obj) + "(" + format(subj) + ")";
		}
		else if( pred.equals( Constant.SAME_AS ) ) {
			return format(subj) + " = " + format(obj);
		}
		else if( pred.equals( Constant.SAME_AS ) ) {
			return format(subj) + " != " + format(obj);
		}
		else
			return format(pred) + "(" + format(subj) +"," + format(obj) + ")";
	}

   public static String format( Term term ) {
	   StringBuffer sb = new StringBuffer();
	   if( term == null )
	       sb.append("<null>");	   
	   else { 
		   if( term instanceof Variable )
	           sb.append("?");
	       
	       sb.append(URIUtils.getLocalName( term.toString() ) );
	   }
	   
	   return sb.toString(); 
	}

}
