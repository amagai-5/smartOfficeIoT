package CreateRDF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.reasoner.rulesys.builtins.BaseBuiltin;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class CreateRDF implements MqttCallback {

MqttClient client;
String humString;
String co2String;
String tempString;
String powerString;
int i = 0;


boolean flagCo2 = false;
boolean flagHum = false;
boolean flagTem = false;
boolean flagPow = false;
String array[]= new String[5];
double results[] = new double[]{0,0,0,0,0};
String topicHumid = "esp32/humidity";
String topicCo2 = "esp32/co2";
String topicTemp = "esp32/temperature";
String topicPower = "esp32/power";
String topicExample = "java/output";
String wd = "https://www.wikidata.org/wiki/";
String sosa = "http://www.w3.org/ns/sosa/";
String ssn = "http://www.w3.org/ns/ssn/";
String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
String data = "http://example.org/data/";
String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
String roomStr = "RoomE208";

Model m = ModelFactory.createDefaultModel();//create RDF model
	
Property hosts = m.createProperty(sosa+"hosts");
Property hasSimpleResult = m.createProperty(sosa+"hasSimpleResult");
Property observedProperty = m.createProperty(sosa+"observedProperty");
Property hasFeatureOfInterest = m.createProperty(sosa+"hasFeatureOfInterest");
Property madeBySensor = m.createProperty(sosa+"madeBySensor");
Property resultTime = m.createProperty(sosa+"resultTime");
Property type = m.createProperty(rdf+"type");
Property hasValue = m.createProperty(rdf+"hasValue");
Property comments = m.createProperty(rdfs + "comments");

Resource ESP32 = m.createResource(wd+"Q27921668");
Resource humidity = m.createResource(wd+"Q180600");
Resource co2 = m.createResource(wd+"Q1997");
Resource temperature = m.createResource(wd+"Q11466");
Resource Observation = m.createResource(ssn+"Observation");
Resource CurrentObservation = m.createResource(data+"CurrentObservation");
Resource room = m.createResource(data+ roomStr);
Resource electricPower = m.createResource(wd + "Q27137");




public CreateRDF() {
	System.out.println("start");
	m.setNsPrefix("sosa", sosa);
	m.setNsPrefix("ssn", ssn);	
	m.setNsPrefix("wd", wd);
	m.setNsPrefix("data", data);
	m.setNsPrefix("rdf", rdf);
	m.setNsPrefix("rdfs", rdfs);
}

public static void main(String[] args) {
    new CreateRDF().doDemo();
//    ReasoningRDF.main(args);
}

public void doDemo() {
    try {
        client = new MqttClient("tcp://192.168.8.213:1883", "Sending");
        client.connect();
        client.setCallback(this);
        client.subscribe(topicHumid);
        client.subscribe(topicCo2);
        client.subscribe(topicTemp);
        client.subscribe(topicPower);

    	CurrentObservation.removeProperties();
    	room.removeAll(comments);
    }
    catch (MqttException e) {
        e.printStackTrace();
        
    }
}

@Override
public void connectionLost(Throwable cause){
    // TODO Auto-generated method stub++
}

@Override
public void messageArrived(String topic, MqttMessage message)
        throws Exception {
 System.out.println(message);
 System.out.println(topic);
 
 if (topic.equals(topicHumid)) {
	System.out.println("gethumid");	
	array[1] = message.toString();
	results[1] = Double.parseDouble(array[1]);
	flagHum = true;
	System.out.println("True");

 }
 else if(topic.equals(topicCo2)) {
	System.out.println("getCo2");
	array[2] = message.toString();
	results[2] = Double.parseDouble(array[2]);
	flagCo2 = true;
	System.out.println("True");

	
 }
else if(topic.equals(topicTemp)) {
	System.out.println("getTemp");
	array[3] = message.toString();
	results[3] = Double.parseDouble(array[3]);
	flagTem = true;
	System.out.println("True");

	
}

else if(topic.equals(topicPower)) {
	System.out.println("getPower");
	array[4] = message.toString();
	results[4] = Double.parseDouble(array[4]);
	flagPow = true;
	System.out.println("True");

	
}
if (flagTem && flagHum && flagCo2 && flagPow == true) {
	String time =getTimeStamp();
	array[0] = time;
	
	createModel(results,i);
	i++;
	
	m = inferModel(m);

	FileOutputStream fout = new FileOutputStream("test.nt"); // will be Changed to Notation 3 
	System.out.println("Write the file...");
		
	try {
		m.write(fout,"N-TRIPLE");
		fout.close();
	}
	catch(FileNotFoundException e) {
		e.printStackTrace();
	}



	
	flagCo2 = false;
	flagHum = false;
	flagTem = false;
	flagPow = false;
	
	NodeIterator statusOfRoom = m.listObjectsOfProperty(room, comments);
	while(statusOfRoom.hasNext()){   		
	    RDFNode object = statusOfRoom.next();
	    String statusStr = object.toString();
	    MqttMessage message1 = new MqttMessage();	
	    message1.setPayload(statusStr
              .getBytes());
	    client.publish(topicExample, message1);
	    System.out.println("pub");   	
	}
	
	CurrentObservation.removeProperties();
	m.removeAll(room, comments, null); 
	
	
}
}


@Override
public void deliveryComplete(IMqttDeliveryToken token) {
    // TODO Auto-generated method stub
}

public  void createModel(double[] results, int count) {
	Resource obCo2 = m.createResource(data+"Observation/co2/"+String.valueOf(count));
	Resource obTem = m.createResource(data+"Observation/temperature/"+String.valueOf(count));
	Resource obHum = m.createResource(data+"Observation/humidity/"+String.valueOf(count));
	Resource obPow = m.createResource(data+"Observation/power/"+String.valueOf(count));
	// adding property
	
	obHum.addLiteral(hasSimpleResult, results[1]);
	obCo2.addLiteral(hasSimpleResult, results[2]);
	obTem.addLiteral(hasSimpleResult, results[3]);
	obPow.addLiteral(hasSimpleResult, results[4]);
	
	obHum.addLiteral(resultTime, array[0]);
	obCo2.addLiteral(resultTime, array[0]);
	obTem.addLiteral(resultTime, array[0]);
	obPow.addLiteral(resultTime, array[0]);
	
	obHum.addProperty(hasFeatureOfInterest, room);
	obCo2.addProperty(hasFeatureOfInterest, room);
	obTem.addProperty(hasFeatureOfInterest, room);
	obPow.addProperty(hasFeatureOfInterest, room);
	
	obHum.addProperty(type, Observation);
	obCo2.addProperty(type, Observation);
	obTem.addProperty(type, Observation);
	obPow.addProperty(type, Observation);
	
	obHum.addProperty(observedProperty, humidity);
	obCo2.addProperty(observedProperty, co2);
	obTem.addProperty(observedProperty, temperature);	
	obPow.addProperty(observedProperty, electricPower);	
	
	CurrentObservation.addProperty(type, obHum);
	CurrentObservation.addProperty(type, obTem);
	CurrentObservation.addProperty(type, obCo2);
	CurrentObservation.addProperty(type, obPow);
}

public static String getTimeStamp() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
    return sdf.format(timestamp);
  }



public Model inferModel(Model m) {

	Path path = Paths.get("target").toAbsolutePath().normalize();
	String rulesStr = path.toFile().getAbsolutePath() + "/classes/myrules.rules";
	System.out.println(rulesStr);
	
	List <Rule> rules = Rule.rulesFromURL(rulesStr);
	System.out.println("Read the file...");
	GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(rules);
	// Bind the reasoner to the model
    InfModel infModel = ModelFactory.createInfModel(ruleReasoner, m);

	
    // Perform reasoning on the model
    infModel.prepare();
    // infModel.write(System.out, "N3"); 
	return(infModel);

}

}
