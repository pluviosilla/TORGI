package org.sofwerx.torgi.ogc.sos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.sofwerx.torgi.BuildConfig;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Currently a single class intended to handle all IPC with other SOS capable processes.
 * This may be refactored into multiple classes for a bit cleaner look. It is a single
 * class for now to facilitate ease of review by several parties while the concepts are
 * being fleshed-out.
 */
public class SosIpcTransceiver extends BroadcastReceiver {
    public final static String TAG = "SosIpc";
    public final static String SOFWERX_LINK_PLACEHOLDER = "http://www.sofwerx.org/placeholder"; //this is used as a placeholder where a URL should be provided for a new standard or feature
    private final static SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    public static final String ACTION_SOS = "org.sofwerx.ogc.ACTION_SOS";
    private static final String EXTRA_PAYLOAD = "SOS";
    private static final String EXTRA_ORIGIN = "src";
    private SosMessageListener listener;

    public SosIpcTransceiver(SosMessageListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if ((context != null) && (intent != null)) {
            if (ACTION_SOS.equals(intent.getAction()))
                onMessageReceived(context, intent.getStringExtra(EXTRA_ORIGIN), intent.getStringExtra(EXTRA_PAYLOAD));
            else
                Log.e(TAG, "Unexpected action message received: " + intent.getAction());
        }
    }

    /**
     * Handles the message received from the SOS Broadcast and pass the result to the listener
     * @param source the application that sent the request; mostly to prevent circular reporting
     * @param input the SOS operation received
     */
    public void onMessageReceived(Context context,String source,String input) {
        if (source == null) {
            Log.e(TAG,"SOS broadcasts from anonymous senders is not supported");
            return;
        }
        if (input == null)
            Log.e(TAG,"Null operation received from SOS broadcast IPC");
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new InputSource(new ByteArrayInputStream(input.getBytes("utf-8"))));
            if (doc != null) {
                AbstractSosOperation operation = AbstractSosOperation.newFromXML(doc);
                if (operation != null) {
                    if (listener != null)
                        listener.onSosOperationReceived(operation);
                }
                return;
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
        }
        Log.e(TAG,"SOS IPC broadcast was not XML: "+input);
    }

    /**
     * Broadcasts this SOS operation via IPC
     * @param context
     * @param operation
     */
    public void broadcast(Context context, AbstractSosOperation operation) {
        if (operation != null) {
            Document doc = null;
            try {
                doc = operation.toXML();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
            try {
                broadcast(context,toString(doc));
            } catch (Exception ex) {
                Log.d(TAG,"Unable to convert XML document to string");
            }
        }
    }

    public final static String toString(Document doc) throws TransformerException {
        if (doc == null)
            return null;
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    //FIXME just for testing
    /*public static void test() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            SosSensor sensor = new SosSensor("TORGI WISKEY 36","http://www.sofwerx.org/torgi/wiskey36","TORGI","Tactical Observation of RF and GNSS Interference sensor");
            SensorMeasurementTime sensorMeasurementTime = new SensorMeasurementTime();
            SensorMeasurementLocation sensorMeasurementLocation = new SensorMeasurementLocation();
            SensorMeasurement sensorMeasurementCn0 = new SensorMeasurement(new SensorResultTemplateField("cn0", SosIpcTransceiver.SOFWERX_LINK_PLACEHOLDER,"dB-Hz"));
            SensorMeasurement sensorMeasurementAgc = new SensorMeasurement(new SensorResultTemplateField("agc", SosIpcTransceiver.SOFWERX_LINK_PLACEHOLDER,"dB"));
            SensorMeasurement sensorMeasurementRisk = new SensorMeasurement(new SensorResultTemplateField("risk", SosIpcTransceiver.SOFWERX_LINK_PLACEHOLDER,"%"));
            sensor.addMeasurement(sensorMeasurementTime);
            sensor.addMeasurement(sensorMeasurementLocation);
            sensor.addMeasurement(sensorMeasurementCn0);
            sensor.addMeasurement(sensorMeasurementAgc);
            sensor.addMeasurement(sensorMeasurementRisk);
            OperationInsertSensor opInSen = new OperationInsertSensor(sensor);
            Log.d(TAG,toString(opInSen.toXML()));
            sensor.setAssignedProcedure("http://www.sofwerx.org/torgi/wiskey36");
            sensor.setAssignedOffering("http://www.sofwerx.org/torgi/wiskey36-sos");
            OperationInsertResultTemplate opInRsltT = new OperationInsertResultTemplate(sensor);
            Log.d(TAG,toString(opInRsltT.toXML()));
            sensor.setAssignedTemplate("http://www.sofwerx.org/torgi/wiskey36#output0");
            sensorMeasurementTime.setValue(System.currentTimeMillis());
            sensorMeasurementLocation.setLocation(27.956419,-82.4394057,17.0);
            sensorMeasurementCn0.setValue(26.5d);
            sensorMeasurementAgc.setValue(1d);
            sensorMeasurementRisk.setValue(11.25d);
            OperationInsertResult opInRes = new OperationInsertResult(sensor);
            Log.d(TAG,toString(opInRes.toXML()));
        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }
    }*/

    public void setListener(SosMessageListener listener) {
        this.listener = listener;
    }

    /**
     * Broadcast an SOS Operation
     * @param context
     * @param sosOperation
     */
    private void broadcast(Context context, String sosOperation) {
        if (context == null) {
            Log.d(TAG,"Context needed to broadcast an SOS Operation");
            return;
        }
        if (sosOperation == null) {
            Log.e(TAG, "Cannot broadcast an empty SOS Operation");
            return;
        }
        Intent intent = new Intent(ACTION_SOS);
        intent.putExtra(EXTRA_ORIGIN, BuildConfig.APPLICATION_ID);
        intent.putExtra(EXTRA_PAYLOAD,sosOperation);

        context.sendBroadcast(intent);
        Log.d(TAG,"Broadcast: "+sosOperation);
    }

    /**
     * Consumes ISO 8601 formatted text and translates into UNIX time
     * @param time unix time (or Long.MIN_VALUE if could not be parsed)
     * @return
     */
    public static long parseTime(String time) {
        if (time != null) {
            try {
                Date date = dateFormatISO8601.parse(time);
                return date.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return Long.MIN_VALUE;
    }

    public static String formatTime(long time) {
        return dateFormatISO8601.format(time);
    }
}