package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.ws.spi.http.HttpExchange;

import org.json.JSONException;
import org.json.JSONObject;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;

import com.sun.net.httpserver.HttpHandler;

import static org.neo4j.driver.v1.Values.parameters;

public class HasRelationship implements HttpHandler {

	Driver driver;

	public void handle(com.sun.net.httpserver.HttpExchange r) throws IOException {
		try {
			if(r.getRequestMethod().equals("GET")) {
				handleAdd(r);
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
	
	public void handleAdd(com.sun.net.httpserver.HttpExchange r) throws IOException, JSONException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			
			String id1 = null;
			String id2 = null;
			StatementResult result;
			
			
			if (deserialized.has("actorId"))
				id1 = deserialized.getString("actorId"); 
			
			if (deserialized.has("movieId"))
				id2 = deserialized.getString("movieId"); 
			
			driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j","secret"));
			
			if(id1 == null || id2 == null) {
				r.sendResponseHeaders(400, -1);
				return;
			} 

			if(id1.equals("") || id2.equals("")) {
				r.sendResponseHeaders(404, -1);
				return;
			} 
			
	        Session session = driver.session();
	        
	        result = session.run("MATCH (n:actor {id:" + "'" +id1+ "'" + "})" + "\n" + "RETURN n.id");
	        if(result.list().isEmpty()) {
	        	r.sendResponseHeaders(404, -1);
	        	return;
	        }
	        result = session.run("MATCH (n:movie {id:" + "'" +id2+ "'" + "})" + "\n" + "RETURN n.id");
	        if(result.list().isEmpty()) {
	        	r.sendResponseHeaders(404, -1);
	        	return;
	        }
	        
	        
	        result = session.run("MATCH (n:actor {id:"+ "'" + id1+ "'" +"})-[r:ACTED_IN]->(c)" + "\n" + "RETURN c.id");
	     
	        Boolean b = false;
	        for(Record records : result.list()){
		    		//When I do get(0) it gives me the value but I cant use toString so I convert asInt and converted back to a string.
		    		String response = records.get(0).asString();
		    		if(response.equals(id2)) {
		    			b = true;
		    		}
	    		
	        }
	        JSONObject resp = new JSONObject();
	        resp.put("actorId", ""+id1+"");
	        resp.put("movieId", ""+id2+"");
	        resp.put("hasRelationship", b);
	        r.sendResponseHeaders(200, resp.toString().length());		
	        OutputStream os = r.getResponseBody();
	        os.write(resp.toString().getBytes());
	        os.close();
		} catch (IOException e) {
	        r.sendResponseHeaders(500, -1);
	    }
	}
}