package com.pseudonymous.appmea.dataparse;

import android.content.Context;
import android.provider.DocumentsContract;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.pseudonymous.appmea.MainActivity;
import com.pseudonymous.appmea.network.ValuePair;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by David Smerkous on 10/8/16.
 *
 */

public class XMLParser {
    private Document document;

    XMLParser(Context context, String name) throws IOException {
        InputStream fileraw = context.getAssets().open(name);
        Reader fileis = new BufferedReader(new InputStreamReader(fileraw, "UTF-8"));

        char[] temp_buff = new char[(12 * 1024)];
        StringBuilder buffer = new StringBuilder();
        int numCharsRead;
        while ((numCharsRead = fileis.read(temp_buff, 0, temp_buff.length)) != -1) {
            buffer.append(temp_buff, 0, numCharsRead);
        }
        fileis.close();

        String targetString = buffer.toString();

        DocumentBuilderFactory docBuildFact = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder docBuild = docBuildFact.newDocumentBuilder();

            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(targetString));
            document = docBuild.parse(inputSource);
        } catch (ParserConfigurationException | SAXException err) {
            MainActivity.LogData("FAILED PARSING XML FILE", true);
            err.printStackTrace();
        }
    }

    private String getElementValue(Node elemenet) {
        Node child;
        if(elemenet == null) {
            MainActivity.LogData("XML DOCUMENT ELEMENT IS NULL", true);
            return "";
        }

        if(elemenet.hasChildNodes()) {
            for(child = elemenet.getFirstChild(); child != null; child = child.getNextSibling()) {
                if(child.getNodeType() == Node.TEXT_NODE) return child.getNodeValue();
                else MainActivity.LogData("NO NODE TEXT", true);
            }
        } else MainActivity.LogData("XML FILE HAS NO CHILD NODES", true);
        return "";
    }

    private String getValue(Element item, String load) {
        NodeList nodes = item.getElementsByTagName(load);
        return this.getElementValue(nodes.item(0));
    }

    public Document getDocument() {
        return this.document;
    }

    ArrayList<ConfigurationType> LoadConfig() {
        NodeList nodeList = this.document.getElementsByTagName(ConfigurationType.CONFIG_KEY);
        ArrayList<ConfigurationType> configs = new ArrayList<>();

        for(int ind = 0; ind < nodeList.getLength(); ind++) {
            Element configElement = (Element) nodeList.item(ind);

            ConfigurationType configurationType = new ConfigurationType();
            configurationType.name = this.getValue(configElement, "name");
            configurationType.method_name = this.getValue(configElement, "method");
            configurationType.first_arg = this.getValue(configElement, "value1");
            configs.add(configurationType);
        }

        return configs;
    }
}

class ConfigurationType {
    static final String CONFIG_KEY = "config";

    public String name = "";
    String method_name = "";
    String first_arg = null;
}