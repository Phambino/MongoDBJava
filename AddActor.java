package ca.utoronto.utm.mcs;
import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class AddActor implements HttpHandler {
	Driver driver;

	@Override
	public void handle(HttpExchange r) throws IOException {
        try {
            if (r.getRequestMethod().equals("PUT")) {
                handlePut(r);
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
	
    public void handlePut(HttpExchange r) throws IOException, JSONException {
    		try {
    			String body = Utils.convert(r.getRequestBody());
    			JSONObject deserialized = new JSONObject(body);
    			
    			String name = null;
    			String id = null;
    			StatementResult result;
    			StatementResult result2;
    			
    			if (deserialized.has("name"))
    				name = deserialized.getString("name"); 
    			
    			if (deserialized.has("actorId"))
    				id = deserialized.getString("actorId");

    			driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j","secret"));
    			
    			//Error checking starts
    			//Empty strings are provided
    			if(name == null || id == null || name.equals("") || id.equals("")) {
    				r.sendResponseHeaders(400, -1);
    				return;
    			} 

    			
    			Session session = driver.session();
    				
    			result = session.run("MATCH (n:actor {id:" + "'" +id+ "'" + "})" + "\n" + "RETURN n.name");
    			result2 = session.run("MATCH (n:actor {id:"+ "'" +id+ "'" + "})" + "\n" + "RETURN n.id");
    				
    			if(result.list().isEmpty() && result2.list().isEmpty()) {
    				session.run("Create (:actor {name:\"" +name+ "\", id:" + "'" + id+ "'" + "})");
    				r.sendResponseHeaders(200, -1);	
    				return;
    			}  
    			
    			//Reset changes.
    			result = session.run("MATCH (n:actor {id:" + "'" +id+ "'" + "})" + "\n" + "RETURN n.name");
    			result2 = session.run("MATCH (n:actor {id:" + "'" +id+ "'" + "})" + "\n" + "RETURN n.id");
    			
    			String node_name = result.single().get(0).asString();
    			String str_id = result2.single().get(0).asString();
    			//Checks if the name is not the same but the id is so we must OVERWRITE.
    			if(!(node_name.equals(name)) && str_id.equals(id)) {
    				//Query for overwriting the the name.
    				session.run("MATCH (n:actor {name:\"" +node_name+ "\", id:" + "'" + str_id+ "'" + "})" + "\n" + "SET n.name =" + "'" + name + "'"+ "\n" + "RETURN n.name");	
    				r.sendResponseHeaders(200, -1);
    				return;
    			} 
    			//Checks if they are the same then we leave it UNCHANGED.
    			if (node_name.equals(name) && str_id.equals(id)) {
    				r.sendResponseHeaders(200, -1);
    				return;
    			}    			
    		} catch (IOException e) {
    			r.sendResponseHeaders(405, -1);
    		}

    }

}


