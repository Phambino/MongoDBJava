package ca.utoronto.utm.mcs;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.json.*;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ComputeBaconPath implements HttpHandler {
	
	Driver driver;

	@Override
	public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("GET")) {
                handleGet(r);
            }else {
        		r.sendResponseHeaders(405, -1);
		        }
		    } catch (IOException e) {
		        r.sendResponseHeaders(500, -1);
		    } catch(JSONException e) {
		    		r.sendResponseHeaders(400, -1);
		    } catch(Exception e) {
		    		e.printStackTrace();
		    }
    }
	
	
    public void handleGet(HttpExchange r) throws IOException, JSONException {
    		try {
    			String body = Utils.convert(r.getRequestBody());	
    			JSONObject deserialized = new JSONObject(body);
    		    	
    		    	JSONObject return_json = new JSONObject();
    		    	String id = null;
    		
    		    	if (deserialized.has("actorId"))
    		    		id = deserialized.getString("actorId");
    		    	//Checks if there was a successful read if not its a bad request
    		    	if(id == null) {
    		    		r.sendResponseHeaders(400, -1);
    		    		return;
    		    	}

    		
    		    	driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j","secret"));
    		    Session session = driver.session();
    		    OutputStream os = r.getResponseBody();
    		    JSONArray bacon_path = new JSONArray();
    		    JSONObject paths = new JSONObject();
    		    	//Checks if we are going from kevin bacon to kevin bacon
    		    	if(id.equals("nm0000102")) {
    		    		//TO DO
    		    		return_json.put("baconNumber","0");
    		    		return_json.put("baconPath", bacon_path);
    		    	}else {//Go here if it is a normal input
    		    	    //Checks if the actor exists
    		    	    StatementResult actor_name = session.run("MATCH (s:actor)\n" + "WHERE s.id = '"+ id +"'\n" + "RETURN s.name");
    		    	    if(actor_name.list().isEmpty()) {
    		            		r.sendResponseHeaders(404, -1);
    		            		return;
    		            }
    		    	    
    		    	    StatementResult path = session.run("MATCH p=shortestPath(\n" + 
    		    	    		"  (bacon:actor {id: '"+ id +"'})-[*]-(meg:actor {id: 'nm0000102'})\n" + 
    		    	    		")\n" + 
    		    	    		"RETURN p");
    		    	    //Checking if there is no existing path
    		    	    if(!path.hasNext()) {
    		    	    		r.sendResponseHeaders(404, -1);
    		    	    		return;
    		    	    }
    		    	    
    		    	    Path shortest_path = path.single().get(0).asPath();
    		    	    String bacon_number = Integer.toString(shortest_path.length()/2);

    		    	    Iterable<Node> nodes = shortest_path.nodes();
    		    	  
    		    	    boolean start_iteration = false;
    		    	    int count = 0;
    		    	    String m_id = null;
    		    	    String a_id = null;

    		    	    for(Node node : nodes) {
    		    	    		if(start_iteration) {
    		    	    			count++;
    		    	    			
    		    		    		StatementResult node_SR = session.run("MATCH (s)\n" + 
    		    		    				"WHERE ID(s) = "+ node.id() +"\n" + 
    		    		    				"RETURN s.id");
    		    		    		String node_id = node_SR.single().get(0).asString();
    		    		    		if(count == 1) {//Movie acted in:
    		    		    			m_id = node_id;
    		    		    		}
    		    		    		if(count == 2) {
    		    		    			a_id = node_id;
    		    		    			paths.put("actorId",""+a_id+"");
    		    		    			paths.put("movieId",""+m_id+"");
    		    		    			bacon_path.put(paths);
    		    		    			//Resetting everything
    		    		    			paths = new JSONObject();
    		    		    			a_id = null;
    		    		    			m_id = null;
    		    		    			count = 0;
    		    		    		}
    		    	    		}
    		    	    		//So we skip the first iteration
    		    	    		start_iteration = true;

    		    	    }
    		    	    return_json.put("baconNumber",""+ bacon_number +"");	  
    		    	    return_json.put("baconPath", bacon_path);
    		    	}

    	        r.sendResponseHeaders(200, return_json.toString().length());
    	        os.write(return_json.toString().getBytes());
    	        os.close();
    	        
    		} catch (IOException e) {
    			r.sendResponseHeaders(500, -1);
		}

    }
}
