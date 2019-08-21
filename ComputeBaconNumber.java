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

public class ComputeBaconNumber implements HttpHandler {
	
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
    		
    		    	String id = null;
    		
    		    	if (deserialized.has("actorId"))
    		    		id = deserialized.getString("actorId");
    		    	//Checks if there was a successful read if not its a bad request
    		    	if(id == null) {
    		    		r.sendResponseHeaders(400, -1);
    		    		return;
    		    	}
    	    		JSONObject resp = new JSONObject();
    	    		driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j","secret"));
    		    Session session = driver.session();
    		    OutputStream os = r.getResponseBody();	    	
    		    	//Checks if we are going from kevin bacon to kevin bacon
    		    	if(id.equals("nm0000102")) {
    		    		resp.put("baconNumber", "0");
    		    	}else {
    			    //Checking if the actor exists
    			    StatementResult actor_name = session.run("MATCH (s:actor)\n" + "WHERE s.id = '"+ id +"'\n" + "RETURN s.name");
    			    if(actor_name.list().isEmpty()) {
    		        		r.sendResponseHeaders(404, -1);
    		        		return;
    		        }
    			    
    			    StatementResult path = session.run("MATCH p=shortestPath(\n" + 
    			    		"  (root:actor {id: '"+ id +"'})-[*]-(bacon:actor {id:'nm0000102'})\n" + 
    			    		")\n" + 
    			    		"RETURN p");
    			    
    			    //Checks if the path does not exist, then we give an .
    			    if(!path.hasNext()) {
    			    		r.sendResponseHeaders(404, -1);
    			    		return;
    			    }
    			    
    			    //Extract the path
    			    Path shortest_path = path.single().get(0).asPath();
    			    //Gets the bacon number by dividing by 2
    			    String bacon_number = Integer.toString(shortest_path.length()/2);
    		
    		        resp.put("baconNumber", ""+ bacon_number +"");	    		
    		    	}
    		    
    	        r.sendResponseHeaders(200, resp.toString().length());
    	        os.write(resp.toString().getBytes());
    	        os.close();
    		} catch (IOException e) {
		        r.sendResponseHeaders(500, -1);
		}

    }
}
