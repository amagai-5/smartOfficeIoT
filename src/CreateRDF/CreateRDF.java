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
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileStoreAttributeView;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.xml.bind.DatatypeConverter;


import java.lang.Object;



import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeStampType;
import org.apache.jena.datatypes.xsd.impl.XSDDateTimeType;
import org.apache.jena.ext.xerces.impl.dv.xs.DateTimeDV;
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
import org.apache.jena.vocabulary.XSD;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Comment;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.Calendar.Events.CalendarImport;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;


public class CreateRDF implements MqttCallback {

MqttClient client;
String humString;
String co2String;
String tempString;
String powerString;
int i = 0;

java.util.Calendar calNow = null;
java.util.Calendar startCal = null;
java.util.Calendar endCal = null;


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
String topicAlarm = "java/alarm";
String topicPlan = "java/plan";
String wd = "https://www.wikidata.org/wiki/";
String sosa = "http://www.w3.org/ns/sosa/";
String ssn = "http://www.w3.org/ns/ssn/";
String rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
String data = "http://example.org/data/";
String rdfs = "http://www.w3.org/2000/01/rdf-schema#";
String roomStr = "RoomE208";
String xsd = "http://www.w3.org/2001/XMLSchema#";
String time = "https://www.w3.org/TR/owl-time/#";
String planStr = null;

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
Property now = m.createProperty(wd + "Q193168");
Property hasBegin = m.createProperty(time + "time:hasBegin");
Property hasEnd = m.createProperty(time + "time:hasEnd");
Property hasStatus = m.createProperty(data, "hasStatus");



Resource ESP32 = m.createResource(wd+"Q27921668");
Resource humidity = m.createResource(wd+"Q180600");
Resource co2 = m.createResource(wd+"Q1997");
Resource temperature = m.createResource(wd+"Q11466");
Resource Observation = m.createResource(ssn+"Observation");
Resource CurrentObservation = m.createResource(data+"CurrentObservation");
Resource room = m.createResource(data+ roomStr);
Resource electricPower = m.createResource(wd + "Q27137");
Resource plan = m.createResource(wd + "Q1371819");
Resource onOffStatus = m.createResource(data+"OnOffStatus");
Resource optimization = m.createResource(data+"Q24476018");

String eventStr = null;
String fileAsString = null;

private static final String APPLICATION_NAME = "Google Calendar API Java Quickstart";
private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR_READONLY);
Path path = Paths.get("resources").toAbsolutePath().normalize();
String pathJson = path.toFile().getAbsolutePath() + "/credential.json";



public CreateRDF() {
	System.out.println("<--------------------------->");
	System.out.println("----------start-------------");
	System.out.println("<--------------------------->");
	m.setNsPrefix("sosa", sosa);
	m.setNsPrefix("ssn", ssn);	
	m.setNsPrefix("wd", wd);
	m.setNsPrefix("data", data);
	m.setNsPrefix("rdf", rdf);
	m.setNsPrefix("rdfs", rdfs);
	m.setNsPrefix("xsd", xsd);
	m.setNsPrefix("time",time);
}

public static void main(String[] args) {
    new CreateRDF().doDemo();
//    ReasoningRDF.main(args);
}

public void doDemo() {
    try {
        client = new MqttClient("tcp://172.20.10.3:1883", "Sending");
        client.connect();
        client.setCallback(this);
        client.subscribe(topicHumid);
        client.subscribe(topicCo2);
        client.subscribe(topicTemp);
        client.subscribe(topicPower);

    	CurrentObservation.removeProperties();
    	room.removeAll(comments);
		room.removeAll(hasBegin);
		room.removeAll(hasEnd);
		onOffStatus.removeAll(comments);
		plan.removeAll(type);

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
public void messageArrived(String topic, MqttMessage message)throws Exception {
	System.out.println("<--------------------------->");
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
	System.out.println("<--------------------------->");

	if (flagTem && flagHum && flagCo2 && flagPow == true) {
		calNow =getTimeStamp();
		calNow.getTimeInMillis();;
		// array[0] = time;
		System.out.println("<--------------------------->");
		System.out.println(getCalendar());
		System.out.println("<--------------------------->");
		createModel(results,i);
		i++;
		
		m = inferModel(m);

		FileOutputStream fout = new FileOutputStream("resources/test.nt"); // will be Changed to Notation 3 
		System.out.println("<--------------------------->");
		System.out.println("--------Write the file-------");
		System.out.println("<--------------------------->");
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
		
		NodeIterator onOffIterator = m.listObjectsOfProperty(onOffStatus, comments);
		while(onOffIterator.hasNext()){   		
			RDFNode object1 = onOffIterator.next();
			String statusStr1 = object1.toString();
			System.out.println(statusStr1);
			MqttMessage message1 = new MqttMessage();	
			message1.setPayload(statusStr1.getBytes());
			client.publish(topicPlan, message1);
			System.out.println("<--------------------------->");
			System.out.println("--------------pub------------");   	
			System.out.println("<--------------------------->");
		}

		NodeIterator statusOfRoom = m.listObjectsOfProperty(room, comments);
		while(statusOfRoom.hasNext()){   		
			RDFNode object2 = statusOfRoom.next();
			String statusStr2 = object2.toString();
			MqttMessage message2 = new MqttMessage();	
			message2.setPayload(statusStr2.getBytes());
			client.publish(topicAlarm, message2);
			System.out.println("<--------------------------->");
			System.out.println("--------------pub------------");   	
			System.out.println("<--------------------------->");
		}
		
		CurrentObservation.removeProperties();
		m.removeAll(room, comments, null); 
		m.removeAll(room,hasEnd,null);
		m.removeAll(room,hasBegin,null);
		m.removeAll(onOffStatus,comments,null);
		m.removeAll(plan,type,null);
		
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
	
	obHum.addLiteral(resultTime, calNow);
	obCo2.addLiteral(resultTime, calNow);
	obTem.addLiteral(resultTime, calNow);
	obPow.addLiteral(resultTime, calNow);
	

	room.addLiteral(now, calNow);
	room.addProperty(hasFeatureOfInterest, plan); //another property should be better
	room.addProperty(hasFeatureOfInterest, optimization);
	if(startCal != null){
		plan.addLiteral(hasBegin,startCal);
		plan.addLiteral(hasEnd,endCal);
		plan.addLiteral(type,eventStr);
		startCal.add(java.util.Calendar.MINUTE, -30);
		endCal.add(java.util.Calendar.MINUTE, -15);

		optimization.addLiteral(hasBegin, startCal);
		optimization.addLiteral(hasEnd, endCal);
	}

	room.addProperty(hasStatus,onOffStatus);
	
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

public  java.util.Calendar getTimeStamp() throws ParseException {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	sdf.setTimeZone(TimeZone.getTimeZone("CET"));
	String ttime = sdf.format(timestamp);
	System.out.print(ttime);
	java.util.Calendar cal = java.util.Calendar.getInstance(TimeZone.getTimeZone("JST"));
	cal = DatatypeConverter.parseDateTime(ttime);
	return cal;
}



public Model inferModel(Model m) {

	Path path = Paths.get("resources").toAbsolutePath().normalize();
	String rulesStr = path.toFile().getAbsolutePath() + "/myrules.rules";
	System.out.println(rulesStr);
	
	List <Rule> rules = Rule.rulesFromURL(rulesStr);
	System.out.println("<--------------------------->");
	System.out.println("---------Read the file------");
	System.out.println("<--------------------------->");
	GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(rules);
	// Bind the reasoner to the model
    InfModel infModel = ModelFactory.createInfModel(ruleReasoner, m);

	
    // Perform reasoning on the model
    infModel.prepare();
    // infModel.write(System.out, "N3"); 
	return(infModel);

}
public String getCalendar() throws IOException, GeneralSecurityException {
	DateTime now = new DateTime(System.currentTimeMillis());
	GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(pathJson.toString()))
.createScoped(Collections.singleton(CalendarScopes.CALENDAR_EVENTS));
	final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	Events events = service.events().list("c_55e22b7f5550fb20f7926339fb7d0b1ff34168810bc72446a6fc2e4c9918958b@group.calendar.google.com").setMaxResults(1)
	.setTimeMin(now).setOrderBy("startTime").setSingleEvents(true)
	.execute();
	System.out.println("calendarAPI is working");
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	sdf.setTimeZone(TimeZone.getTimeZone("CET"));
	List<Event> items = events.getItems();
	for (Event event : items) {
		 DateTime start = event.getStart().getDateTime();
		 String startTime = start.toString();
		 startCal = DatatypeConverter.parseDateTime(startTime);
		 DateTime end = event.getEnd().getDateTime();
		 String endTime = end.toString();
		 endCal = DatatypeConverter.parseDateTime(endTime);
		 System.out.printf(event.getSummary() + " (" + start + " - " + end + ")");
		 eventStr = event.getSummary();
	}	
	return eventStr;

}


}
