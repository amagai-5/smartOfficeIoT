package CreateRDF;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
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

String tempThreshold = "30";// change threshold here

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

Resource ESP32 = m.createResource(wd+"Q27921668");
Resource humidity = m.createResource(wd+"Q180600");
Resource co2 = m.createResource(wd+"Q1997");
Resource temperature = m.createResource(wd+"Q11466");
Resource Observation = m.createResource(ssn+"Observation");
Resource CurrentObservation = m.createResource(data+"CurrentObservation");
Resource room = m.createResource(data+ roomStr);
Resource electricPower = m.createResource(wd + "Q27137");
Resource threshold = m.createResource(data + "threshold");
Resource TemperatureThreshold = m.createResource(data + "TemperatureThreshold");



public CreateRDF() {
	System.out.println("start");
	m.setNsPrefix("sosa", sosa);
	m.setNsPrefix("ssn", ssn);	
	m.setNsPrefix("wd", wd);
	m.setNsPrefix("data", data);
	m.setNsPrefix("rdf", rdf);
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
//        MqttMessage message = new MqttMessage();
//        message.setPayload(reasoning(results[1], results[2], results[3])
//                .getBytes());
//        client.publish(topicExample, message);
//        System.out.println("pub");
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
	
	createModel(array,i);
	i++;
	try {	
		FileOutputStream fout = new FileOutputStream("test.rdf");
		m.write(fout,"TTL");
	}
	catch(FileNotFoundException e) {
		e.printStackTrace();
	}
	//�z��ƃC���N�������g�������Ƃ���RDF�쐬�֐����쐬����
	
	CurrentObservation.removeProperties();
	
	flagCo2 = false;
	flagHum = false;
	flagTem = false;
	flagPow = false;
	
    MqttMessage message1 = new MqttMessage();
    message1.setPayload(reasoning(results[1], results[2], results[3])
            .getBytes());
    client.publish(topicExample, message1);
    System.out.println("pub");
}
}
@Override
public void deliveryComplete(IMqttDeliveryToken token) {
    // TODO Auto-generated method stub
}

public  void createModel(String[] results, int count) {
	Resource obCo2 = m.createResource(data+"Observation/co2/"+String.valueOf(count));
	Resource obTem = m.createResource(data+"Observation/temperature/"+String.valueOf(count));
	Resource obHum = m.createResource(data+"Observation/humidity/"+String.valueOf(count));
	Resource obPow = m.createResource(data+"Observation/power/"+String.valueOf(count));
	// adding property
	
	threshold.addProperty(type, TemperatureThreshold);
	threshold.addProperty(hasValue, tempThreshold);
	
	obHum.addProperty(hasSimpleResult, results[1]);
	obCo2.addProperty(hasSimpleResult, results[2]);
	obTem.addProperty(hasSimpleResult, results[3]);
	obPow.addProperty(hasSimpleResult, results[4]);
	
	obHum.addProperty(resultTime, results[0]);
	obCo2.addProperty(resultTime, results[0]);
	obTem.addProperty(resultTime, results[0]);
	obPow.addProperty(resultTime, results[0]);
	
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
	
	
	
	System.out.print("Writing RDF...");
}

public static String getTimeStamp() {
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
    return sdf.format(timestamp);
  }

public static String reasoning(double humidity, double co2, double temperature) {
	

	double humThreshold = 60;
	double temThreshold = 36;
	double co2Threshold = 1500;
	
	if (humThreshold < humidity || temThreshold < temperature || co2Threshold < co2) {
		System.out.println("UNCOMFROTABLE");
		return("UNCOMFORTABLE");
	}
	
	else {
		System.out.println("COMFROTABLE");
		return("COMFORTABLE");
	}
	
	
}


public static String reasoningSparqle() {
	
	return("COMFORTABLE");
	
}
}