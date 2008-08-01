/*
 * Created on Oct 16, 2004
 */
package org.mindswap.pellet.jena;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.exceptions.UnsupportedQueryException;
import org.mindswap.pellet.query.QueryUtils;

import aterm.ATermAppl;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryException;
import com.hp.hpl.jena.query.QueryExecException;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.ResultBinding;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingMap;
import com.hp.hpl.jena.sparql.syntax.Template;
import com.hp.hpl.jena.sparql.util.Context;
import com.hp.hpl.jena.sparql.util.ModelUtils;
import com.hp.hpl.jena.util.FileManager;


/**
 * @author Evren Sirin
 */
public class PelletQueryExecution implements QueryExecution {
    public static Log log = LogFactory.getLog( PelletQueryExecution.class );
    
    private Query query;
    private Model source;

    public PelletQueryExecution( String query, Model source ) {        
        this( QueryFactory.create( query ), source );
    }

    public PelletQueryExecution( Query query, Model source ) {
        this.query = query;
        this.source = source;
        
        Graph graph = source.getGraph();
        if( !( graph instanceof PelletInfGraph ) )             
            throw new QueryException( "PelletQueryExecution can only be used with Pellet-backed models" );
        
        if( PelletOptions.FULL_SIZE_ESTIMATE )
        	((PelletInfGraph) graph).getKB().getSizeEstimate().computeAll();
    }
    
    public Model execDescribe() {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }

    public Model execDescribe( Model model ) {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }
    
    public Model execConstruct() {
       Model model = ModelFactory.createDefaultModel();
       
       execConstruct( model );
       
       return model;
    }
    
    public Model execConstruct( Model model ) {
        if ( !query.isConstructType() )
            throw new QueryExecException("Attempt to get a CONSTRUCT model from a "+labelForQuery(query)+" query") ; 
            
        try {
            // first try Pellet engine for ABox queries
            ResultSet results = exec();

            model.setNsPrefixes( source );
            model.setNsPrefixes( query.getPrefixMapping() );

            Set set = new HashSet();

            Template template = query.getConstructTemplate();

            while( results.hasNext() ) {
                Map bNodeMap = new HashMap();
                QuerySolution qs = results.nextSolution();
                Binding binding = toBinding( qs );
                template.subst( set, bNodeMap, binding );
            }

            for( Iterator iter = set.iterator(); iter.hasNext(); ) {
                Triple t = (Triple) iter.next();
                Statement stmt = ModelUtils.tripleToStatement( model, t );
                if( stmt != null ) model.add( stmt );
            }

            close();
            
            return model;
        } catch( UnsupportedQueryException e ) {
            log.info( "This is not an ABox query: " + e.getMessage() );
            
            // fall back to Jena engine for mixed queries
            return QueryExecutionFactory.create( query, source ).execConstruct();
        }        
    }

    public boolean execAsk() {
        if ( ! query.isAskType() )
            throw new QueryExecException("Attempt to have boolean from a "+labelForQuery(query)+" query") ; 

        try {
            // first try Pellet engine for ABox queries
            ResultSet results = exec();
            
            return results.hasNext();
        } catch( UnsupportedQueryException e ) {
            log.info( "This is not an ABox query: " + e.getMessage() );
            
            // fall back to Jena engine for mixed queries
            return QueryExecutionFactory.create( query, source ).execAsk();
        }
    }
    
    public ResultSet execSelect() {
        if ( !query.isSelectType() )
            throw new QueryExecException("Attempt to have ResultSet from a "+labelForQuery(query)+" query") ; 

        try {
            // first try Pellet engine for ABox queries
            return exec();
        } catch( UnsupportedQueryException e ) {
            log.info( "This is not an ABox query: " + e.getMessage() );
            
            // fall back to Jena engine for mixed queries
            return QueryExecutionFactory.create( query, source ).execSelect();
        }
    }
    
    private ResultSet exec() {
        PelletInfGraph pelletInfGraph = (PelletInfGraph) source.getGraph();
        if( !pelletInfGraph.isPrepared() )
            pelletInfGraph.prepare();
        OWLReasoner reasoner = pelletInfGraph.getOWLReasoner();
        
        ResultSet results = reasoner.execQuery( query );
        
        List sortConditions = query.getOrderBy();
        if( sortConditions != null && !sortConditions.isEmpty() ) {
        	results = ResultSetFactory.makeSorted( results, sortConditions );
        }
        
        return results;
    }

    public void abort() {
        // nothing to do here                
    }

    public void close() {
        // nothing to do here
    }

    public void setFileManager( FileManager manager ) {
        // TODO Auto-generated method stub        
    }

    public void setInitialBinding( QuerySolution arg0 ) {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }

    public Context getContext() {
        throw new UnsupportedOperationException( "Not supported yet!" );
    }

    public Dataset getDataset() {
		throw new UnsupportedOperationException( "Not supported yet!" );
	}    

	static private String labelForQuery(Query q)
    {
        if ( q.isSelectType() )     return "SELECT" ; 
        if ( q.isConstructType() )  return "CONSTRUCT" ; 
        if ( q.isDescribeType() )   return "DESCRIBE" ; 
        if ( q.isAskType() )        return "ASK" ;
        return "<<unknown>>" ;
    }
	
	private Binding toBinding(QuerySolution solution) {
        BindingMap result = new BindingMap(); 
		
		for ( Iterator i = solution.varNames(); i.hasNext(); ) {
			String varName = (String) i.next();
			
			result.add( Var.alloc(varName), solution.get( varName ).asNode() );
		}
		
        return result; 
	}
}
