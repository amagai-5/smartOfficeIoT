package CreateRDF;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class ReasoningRDF implements MqttCallback {
	static String queryHum = 
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
			"PREFIX data: <http://example.org/data/> " + 
			"PREFIX sosa: <http://www.w3.org/ns/sosa/>"+
			"PREFIX wd: <https://www.wikidata.org/wiki/>"+
			"SELECT ?o "+ 
			"WHERE {?s sosa:hasSimpleResult ?o."+
		    "data:CurrentObservation a ?s."+
		    "?s sosa:observedProperty wd:Q180600}"
			;
	static String queryCo2 = 
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
			"PREFIX data: <http://example.org/data/> " + 
			"PREFIX sosa: <http://www.w3.org/ns/sosa/>"+
			"PREFIX wd: <https://www.wikidata.org/wiki/>"+
			"SELECT ?o "+ 
			"WHERE {?s sosa:hasSimpleResult ?o."+
		    "data:CurrentObservation a ?s."+
		    "?s sosa:observedProperty wd:Q1997}"
			;
	
	static String queryTem = 
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " + 
			"PREFIX data: <http://example.org/data/> " + 
			"PREFIX sosa: <http://www.w3.org/ns/sosa/>"+
			"PREFIX wd: <https://www.wikidata.org/wiki/>"+
			"SELECT ?o "+ 
			"WHERE {?s sosa:hasSimpleResult ?o."+
		    "data:CurrentObservation a ?s."+
		    "?s sosa:observedProperty wd:Q11466}"
			;
	
	static double humResult;
	static double temResult;
	static double co2Result;
	static MqttClient client;

	
	
	

	public static void main(String[] args) {
        try {
			client = new MqttClient("tcp://172.20.10.3:1883", "Sending");
	        client.connect();
	        client.setCallback(null);
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Timer time = new Timer();
		TimerTask task = new TimerTask()
		{
			public void run() {
			humResult = doQuery(queryHum);
			temResult = doQuery(queryTem);
			co2Result = doQuery(queryCo2);
			
			System.out.println(humResult);
			System.out.println(temResult);
			System.out.println(co2Result);
			
			String mes = doSimpleReasoning(humResult, temResult, co2Result);
			System.out.println(mes);
	        MqttMessage message = new MqttMessage();
	        message.setPayload(mes.getBytes());
			try {
				client.publish("java/output", message);
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
		time.scheduleAtFixedRate(task, 0, 10000);
	}
	

public static double doQuery(String queryString){
	double result = 0;
    Model model=ModelFactory.createDefaultModel();
    try {
		model.read(new FileInputStream("C:\\Users\\heilab.DESKTOP-5C885MS\\eclipse-workspace\\CreateRDF\\test.rdf"),null,"TTL");
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
	Query query = QueryFactory.create(queryString);
	QueryExecution qexec = QueryExecutionFactory.create(query, model);
	
	try {
		ResultSet results = qexec.execSelect();
		while(results.hasNext()) {
			QuerySolution soln = results.nextSolution();
			Literal num = soln.getLiteral("o");
			String numString = num.toString();
			result = Double.parseDouble(numString);  
		}

	}finally {			
	qexec.close();
	}
	
	return result;	
}

public static String doSimpleReasoning(double hum,double tem, double co2 ) {
	double humThreshold = 60;
	double temThreshold = 34;
	double co2Threshold = 1000;
	
	if (humThreshold < hum || temThreshold < tem || co2Threshold < co2) {
		return("UNCOMFORTABLE");
	}
	
	else {
		return("COMFORTABLE");
	}
	
}


@Override
public void connectionLost(Throwable arg0) {
	// TODO Auto-generated method stub
	
}


@Override
public void deliveryComplete(IMqttDeliveryToken arg0) {
	// TODO Auto-generated method stub
	
}


@Override
public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
	// TODO Auto-generated method stub
	
}

}

