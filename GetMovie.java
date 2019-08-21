package ca.utoronto.utm.mcs;
import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.exceptions.ClientException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
public class GetMovie implements HttpHandler{
	
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
    		
    		    	if (deserialized.has("movieId"))
    		    		id = deserialized.getString("movieId");
    		    	
    		
    		    	driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j","secret"));
    		    Session session = driver.session(); 
    		    OutputStream os = r.getResponseBody();
    		    StatementResult movie_name = session.run("MATCH (s:movie)\n" + "WHERE s.id = '"+ id +"'\n" + "RETURN s.name");
    	        if(id == null) {
    	        		r.sendResponseHeaders(400, -1);
    	        		return;
    	        }
    	        if(movie_name.list().isEmpty()) {
    		    		r.sendResponseHeaders(404, -1);
    		    		return;
    	        }
    	        //Following same format as my getActor
    	        movie_name = session.run("MATCH (s:movie)\n" + "WHERE s.id = '"+ id +"'\n" + "RETURN s.name");
    		    StatementResult movie_relationships = session.run("MATCH (x:movie { id: '"+ id +"' })<-[:ACTED_IN]-(n)\n" + " RETURN n.id");
    	        
    	        JSONObject resp = new JSONObject();
    	        JSONArray resp_arr = new JSONArray();
    	        for(Record records : movie_relationships.list()){
    	        		//When I do get(0) it gives me the value but I cant use toString so I convert asInt and converted back to a string.
    	        		resp_arr.put(records.get(0).asString());        
    	        	}
    	        resp.put("movieId", ""+id+"");
    	        resp.put("name", ""+movie_name.single().get(0).asString()+"");
    	        resp.put("actors", resp_arr);
    	        r.sendResponseHeaders(200, resp.toString().length());
    	        os.write(resp.toString().getBytes());
    	        os.close();   			
    		} catch (IOException e) {
		        r.sendResponseHeaders(500, -1);
		    } catch (ClientException e) {
		    	r.sendResponseHeaders(404, -1);
		    }

    }
}
