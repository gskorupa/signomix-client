/*
 * Copyright 2017 Grzegorz Skorupa <g.skorupa at gmail.com>.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.signocom.signomix.client;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Grzegorz Skorupa &lt;g.skorupa at gmail.com&gt;
 */
public class SignomixClient {

    private String serviceUrl = null;
    private boolean trustAllCertificates = false;
    private String token = null;
    private String credentials = null;
    private int retries = 0;
    private String loggingFilter = null;

    private final String authAPI = "/api/auth";
    private final String userAPI = "/api/user";
    private final String iotAPI = "/api/iot";
    private final String integrationAPI = "/api/integration";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        String url;
        url = "https://signomix.signocom.com";
        //url = "http://localhost:8080";
        SignomixClient client = new SignomixClient(url, true, 1, "main");

        // login
        try {
            client.getSessionToken("tester1", "signocom");
            client.logInfo("main", "token: " + client.token);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        // request user data
        try {
            String userData = client.getUser("tester1");
            client.logInfo("main", userData);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        // register device
        /*
        try {
            String deviceEUI = client.registerDevice("/api/iot", "12345", "longitude,latitude");
            client.logInfo("main", deviceEUI);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, null, ex);
        }
         */
        // request device data
        String deviceKey = null;
        try {
            String deviceConfig = client.getDevice("iot-emulator");
            client.logInfo("main", deviceConfig);
            deviceKey = client.getDeviceKey(deviceConfig);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        // get data
        try {
            String channelData = client.getDeviceData("iot-emulator", "temperature", 2);
            client.logInfo("main", channelData);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
        // logout
        try {
            boolean sessionClosed = client.closeSession();
            client.logInfo("main", "session closed: " + sessionClosed);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

        // send measured data from the device
        HashMap<String, String> params = new HashMap<>();
        /*
        params.put("longitude", "12.3456");
        params.put("latitude", "7.890");
        params.put("battery","100");
         */
        params.put("temperature", "28");
        params.put("humidity", "33.33");
        try {
            String result = client.sendData("iot-emulator", deviceKey, params);
            client.logInfo("main", "SendData: " + result);
        } catch (ClientException ex) {
            Logger.getLogger(SignomixClient.class.getName()).log(Level.SEVERE, ex.getMessage());
        }

    }

    public SignomixClient() {

    }

    /**
     * Constructor
     *
     * @param url Signomix platform base address
     * @param trustAllCertificates omit to check SSL certificates
     * @param retries number of request retries in case of errors (eg session
     * timeout). When greater than 0 the client will try to re-login
     * automatically
     * @param loggingFilter
     */
    public SignomixClient(String url, boolean trustAllCertificates, int retries, String loggingFilter) {
        this.serviceUrl = url;
        this.trustAllCertificates = trustAllCertificates;
        this.retries = retries;
        this.loggingFilter = loggingFilter;
    }

    /**
     * Log in to Signomix and get session token. The token and credentials used
     * (login, password) are stored internally for for future use.
     *
     * @param endpoint authentication API endpoint
     * @param login user login
     * @param password user password
     */
    public void getSessionToken(String login, String password) throws ClientException {
        setCredentials(login, password);
        refreshToken();
    }

    /**
     * Log out from Signomix
     *
     * @return logout result (true if session is closed)
     */
    public boolean closeSession() throws ClientException {
        OutboundHttpAdapter client = new OutboundHttpAdapter();
        Request req = new Request();
        req.setProperty("Accept", "text/plain");
        req.setProperty("Authentication", token);
        req.setData("p=ignotethis");
        req.setMethod("DELETE");
        String requestUrl = this.serviceUrl + authAPI + "/" + token;
        StandardResult res = (StandardResult) client.send(requestUrl, req, null, false, trustAllCertificates);
        logInfo("closeSession", "" + res.getCode() + " " + res.getMessage() + " " + res.getResponseTime());
        if (res.getCode() != StandardResult.SC_OK) {
            throw new ClientException(res.getCode());
        }
        return res.getCode() == res.SC_OK;
    }

    /**
     * Get user profile data
     *
     * @param endpoint user API endpoint
     *
     * @param userLogin user ID (login)
     * @return user profile parameters
     */
    public String getUser(String userLogin) throws ClientException {
        OutboundHttpAdapter client = new OutboundHttpAdapter();
        Request req = new Request();
        req.setMethod("GET");
        req.setProperty("Accept", "application/json");
        req.setProperty("Authentication", token);
        String requestUrl = this.serviceUrl + userAPI + "/" + userLogin;
        int counter = 0;
        while (counter <= retries) {
            StandardResult res = (StandardResult) client.send(requestUrl, req, null, false, trustAllCertificates);
            logInfo("getUser", "" + res.getCode() + " " + res.getMessage() + " " + res.getResponseTime());
            switch (res.getCode()) {
                case StandardResult.SC_OK:
                    try {
                        return new String(res.getPayload(), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        logError("getUser", ex.getMessage());
                    }
                case StandardResult.SC_SESSION_EXPIRED:
                    logInfo("getUser", "" + res.getCode() + " " + res.getMessage() + " " + res.getData());
                    counter++;
                    if (counter <= retries) {
                        refreshToken();
                    }
                    break;
                default:
                    throw new ClientException(res.getCode());
            }
        }
        return null;
    }

    /**
     * Modify user profile
     *
     * @param endpoint user API endpoint
     * @param userLogin user login (user ID)
     * @param parameters profile parameters to change
     * @return user profile parameters after modification
     */
    public String modifyUser(String endpoint, String userLogin, HashMap parameters) throws ClientException {
        throw new ClientException(StandardResult.SC_NOT_IMPLEMENTED);
    }

    /**
     * Get device parameters
     *
     * @param endpoint
     * @param deviceEUI
     * @return
     */
    public String getDevice(String deviceEUI) throws ClientException {
        OutboundHttpAdapter client = new OutboundHttpAdapter();
        Request req = new Request();
        req.setMethod("GET");
        req.setProperty("Accept", "application/json");
        req.setProperty("Authentication", token);
        String requestUrl = this.serviceUrl + iotAPI + "/" + deviceEUI;
        int counter = 0;
        while (counter <= retries) {
            StandardResult res = (StandardResult) client.send(requestUrl, req, null, false, trustAllCertificates);
            logInfo("getDevice", "" + res.getCode() + " " + res.getMessage() + " " + res.getResponseTime());
            switch (res.getCode()) {
                case StandardResult.SC_OK:
                    try {
                        return new String(res.getPayload(), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        logError("getDevice", ex.getMessage());
                    }
                case StandardResult.SC_SESSION_EXPIRED:
                    logInfo("getDevice", "" + res.getCode() + " " + res.getMessage() + " " + res.getData());
                    counter++;
                    if (counter <= retries) {
                        refreshToken();
                    }
                    break;
                default:
                    throw new ClientException(res.getCode());
            }
        }
        return null;
    }

    /**
     * Get device parameters
     *
     * @param deviceEUI
     * @return
     */
    public String getDeviceData(String deviceEUI, String channelName, int limit) throws ClientException {
        int counter = 0;
        while (counter <= retries) {
            OutboundHttpAdapter client = new OutboundHttpAdapter();
            Request req = new Request();
            req.setMethod("GET");
            req.setProperty("Accept", "application/json");
            req.setProperty("Authentication", token);
            String requestUri = this.serviceUrl + iotAPI + "/" + deviceEUI + "/" + channelName + "?query=last";
            if (limit > 1) {
                requestUri = requestUri.concat("%20" + limit);
            }
            StandardResult res = (StandardResult) client.send(requestUri, req, null, false, trustAllCertificates);
            logInfo("getDeviceData", "" + res.getCode() + " " + res.getMessage() + " " + res.getResponseTime());
            switch (res.getCode()) {
                case StandardResult.SC_OK:
                    try {
                        return new String(res.getPayload(), "UTF-8");
                    } catch (UnsupportedEncodingException ex) {
                        logError("getDevice", ex.getMessage());
                    }
                case StandardResult.SC_SESSION_EXPIRED:
                    logInfo("getDevice", "" + res.getCode() + " " + res.getMessage() + " " + res.getData());
                    counter++;
                    if (counter <= retries) {
                        refreshToken();
                    }
                    break;
                default:
                    throw new ClientException(res.getCode());
            }
        }
        return null;
    }

    /**
     * Send measured data from the device. The data will be stored in the
     * device's data channels.
     *
     * @param deviceEUI device EUI
     * @param deviceKey secret key of the device
     * @param data data map
     * @return Signomix response ("OK" or error message)
     */
    public String sendData(String deviceEUI, String deviceKey, HashMap data) throws ClientException {
        String dataAsJson;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"dev_eui\":\"")
                .append(deviceEUI)
                .append("\",\"timestamp\":")
                .append(System.currentTimeMillis())
                .append(",\"payload_fields\":[");
        Iterator it = data.keySet().iterator();
        String key;
        while (it.hasNext()) {
            key = (String) it.next();
            sb.append("{")
                    .append("\"name\":\"")
                    .append((String) key)
                    .append("\",\"value\":")
                    .append("\"")
                    .append((String) data.get(key))
                    .append("\"")
                    .append("}");
            if (it.hasNext()) {
                sb.append(",");
            }
        }
        sb.append("]}");
        dataAsJson = sb.toString();

        OutboundHttpAdapter client = new OutboundHttpAdapter();
        Request req = new Request();
        req.setMethod("POST");
        req.setProperty("Accept", "text/plain");
        req.setProperty("Content-Type", "text/plain");
        req.setProperty("Authorization", deviceKey);
        req.setData(dataAsJson); // data must be added to POST or PUT requests
        String requestUrl = this.serviceUrl + integrationAPI;
        StandardResult res = (StandardResult) client.send(requestUrl, req, null, false, trustAllCertificates);
        if (!(res.getCode() == StandardResult.SC_OK || res.getCode() == StandardResult.SC_CREATED)) {
            throw new ClientException(res.getCode());
        }
        String content = "";
        try {
            content = new String(res.getPayload(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            logError("sendData", ex.getMessage());
            content = ex.getMessage();
        }
        logInfo("sendData", "" + res.getCode() + " " + content + " " + res.getResponseTime());
        return content;
    }

    public String registerDevice(String deviceEUI, String channels) throws ClientException {
        String data;
        StringBuilder sb = new StringBuilder();
        sb.append("eui=")
                .append(deviceEUI)
                .append("&")
                .append("channels=")
                .append(channels); //TODO: encode
        data = sb.toString();

        OutboundHttpAdapter client = new OutboundHttpAdapter();
        Request req = new Request();
        req.setMethod("POST");
        req.setProperty("Accept", "text/plain");
        req.setProperty("Authentication", token);
        req.setData(data); // data must be added to POST or PUT requests
        String requestUrl = this.serviceUrl + iotAPI;
        StandardResult res = (StandardResult) client.send(requestUrl, req, null, false, trustAllCertificates);
        if (res.getCode() != StandardResult.SC_CREATED) {
            throw new ClientException(res.getCode(), res.getMessage());
        }
        String content = "";
        try {
            content = new String(res.getPayload(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            logError("registerDevice", ex.getMessage());
            content = ex.getMessage();
        }
        logInfo("registerDevice", "" + res.getCode() + " " + content + " " + res.getResponseTime());
        return content;
    }

    /**
     * *************************************************************************
     * PRIVATE METHODS
     * *************************************************************************
     */
    private void refreshToken() throws ClientException {
        OutboundHttpAdapter client = new OutboundHttpAdapter();
        Request req = new Request();
        req.setMethod("POST");
        req.setProperty("Accept", "text/plain");
        req.setProperty("Authentication", "Basic " + credentials);
        req.setData("p=ignotethis"); // data must be added to POST or PUT requests
        String requestUrl = this.serviceUrl + authAPI;
        StandardResult res = (StandardResult) client.send(requestUrl, req, null, false, trustAllCertificates);
        if (res.getCode() != StandardResult.SC_OK) {
            throw new ClientException(res.getCode());
        }
        logInfo("refreshToken", "" + res.getCode() + " " + res.getMessage() + " " + res.getResponseTime());
        String content = "";
        try {
            content = new String(res.getPayload(), "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            logError("refreshToken", ex.getMessage());
        }
        token = content;
    }

    private void setCredentials(String login, String password) {
        credentials = Base64.getEncoder().encodeToString((login + ":" + password).getBytes());
    }

    private void logInfo(String source, String message) {
        if (loggingFilter != null && (loggingFilter.equals("*") || source.startsWith(loggingFilter))) {
            System.out.println(getDate() + " " + getClass().getName() + " " + source);
            System.out.println(message);
        }
    }

    private void logError(String source, String message) {
        if (loggingFilter != null && (loggingFilter.equals("*") || source.startsWith(loggingFilter))) {
            System.out.println(getDate() + " " + getClass().getName() + " " + source);
            System.err.println(message);
        }
    }

    private String getDate() {
        return getDate("yyyy/MM/dd HH:mm:ss");
    }

    private String getDate(String format) {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date date = new Date();
        return dateFormat.format(date);
    }

    private String getDeviceKey(String deviceDefinition) {
        String key;
        int pos = deviceDefinition.indexOf("\"key\":");
        key = deviceDefinition.substring(pos + 7, deviceDefinition.indexOf("\"", pos + 7));
        return key;
    }
}
