/*
 * Copyright 2017 Grzegorz Skorupa <g.skorupa at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.signocom.signomix.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

import java.security.cert.X509Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * HttpClient will be better name
 *
 * @author greg
 */
public class OutboundHttpAdapter {

    private final String JSON = "application/json";
    private final String CSV = "text/csv";
    private final String HTML = "text/html";
    private final String TEXT = "text/plain";
    private final String XML = "text/xml"; 
    protected int timeout = 0;
    private final int SC_CONNECTION_REFUSED = 111;

    private boolean isRequestSuccessful(int code) {
        return code == HttpURLConnection.HTTP_OK || code == HttpURLConnection.HTTP_CREATED || code == HttpURLConnection.HTTP_ACCEPTED;
    }

    public Result send(String url, Request request, Object data, boolean transform, boolean ignoreCertificateCheck) {
        if (request == null) {
            request = new Request();
        }
        if (data != null) {
            request.setData(data);
        }
        String requestData = "";

        StandardResult result = new StandardResult();
        if (transform) {
            switch (request.properties.get("Content-Type")) {
                case JSON:
                    requestData = translateToJson(request.data);
                    break;
                case CSV:
                    requestData = translateToCsv(request.data);
                    break;
                case TEXT:
                    requestData = translateToText(request.data);
                    break;
                case HTML:
                    requestData = translateToHtml(request.data);
                    break;
                default:
            }
        } else {
            if (request.data != null) {
                if (requestData instanceof String) {
                    requestData = (String) request.data;
                } else {
                    requestData = request.data.toString();
                }
            }
        }
        result.setCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        try {
            long startPoint = System.currentTimeMillis();
            URL urlObj = new URL(url);
            HttpURLConnection con;
            HttpsURLConnection scon;
            if (url.toUpperCase().startsWith("HTTPS")) {
                if (ignoreCertificateCheck) {
                    HttpsURLConnection.setDefaultSSLSocketFactory(getTrustAllSocketFactory());
                }
                scon = (HttpsURLConnection) urlObj.openConnection();
                scon.setReadTimeout(timeout);
                scon.setConnectTimeout(timeout);
                scon.setRequestMethod(request.method);
                for (String key : request.properties.keySet()) {
                    scon.setRequestProperty(key, request.properties.get(key));
                }
                if (requestData.length() > 0) {
                    scon.setDoOutput(true);
                    scon.setFixedLengthStreamingMode(requestData.getBytes().length);
                    try (PrintWriter out = new PrintWriter(scon.getOutputStream())) {
                        out.print(requestData);
                        out.flush();
                        out.close();
                    }
                }
                scon.connect();
                result.setCode(scon.getResponseCode());
                result.setResponseTime(System.currentTimeMillis() - startPoint);
                if (isRequestSuccessful(result.getCode())) {
                    StringBuilder response;
                    try ( // success
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    scon.getInputStream()))) {
                        String inputLine;
                        response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }
                    result.setPayload(response.toString().getBytes());
                } else {
                    //result.setContent("");
                }
            } else {
                con = (HttpURLConnection) urlObj.openConnection();
                con.setReadTimeout(timeout);
                con.setConnectTimeout(timeout);
                con.setRequestMethod(request.method);
                for (String key : request.properties.keySet()) {
                    con.setRequestProperty(key, request.properties.get(key));
                }
                if (requestData.length() > 0 || "POST".equals(request.method) || "PUT".equals(request.method) || "DELETE".equals(request.method)) {
                    con.setDoOutput(true);
                    OutputStream os = con.getOutputStream(); 
                    OutputStreamWriter out = new OutputStreamWriter(os);
                    try{
                        out.write(requestData);
                        out.flush();
                        out.close();
                        os.close();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
                con.connect();
                result.setCode(con.getResponseCode());
                result.setResponseTime(System.currentTimeMillis() - startPoint);
                try{
                    StringBuilder response;
                    try ( // success
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    con.getInputStream()))) {
                        String inputLine;
                        response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }
                    result.setPayload(response.toString().getBytes());
                }catch(Exception e){
                    StringBuilder response;
                    try ( // success
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    con.getErrorStream()))) {
                        String inputLine;
                        response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                    }
                    result.setMessage(response.toString());
                }
            }
        } catch (IOException e) {
            String message = e.getMessage();
            result.setCode(SC_CONNECTION_REFUSED);
            result.setMessage(message);
        } 
        catch (NoSuchAlgorithmException | KeyManagementException e) {
            String message = e.getMessage();
            result.setCode(result.SC_UPGRADE_REQUIRED);
            result.setMessage(message);
        }
        return result;
    }

    protected String translateToHtml(Object data) {
        return translateToText(data);
    }

    protected String translateToText(Object data) {
        if (data != null) {
            return data.toString();
        }
        return "";
    }

    protected String translateToCsv(Object data) {
        return translateToCsv(data, null);
    }

    protected String translateToCsv(Object data, String header) {
        List list;
        if (data instanceof List) {
            list = (List) data;
        } else {
            list = new ArrayList();
            list.add(data);
        }
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isEmpty()) {
            sb.append(header).append("\r\n");
        }
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toString());
            sb.append("\r\n");
        }
        return sb.toString();
    }

    protected String translateToJson(Object data) {
        if (data instanceof String) {
            return (String) data;
        } else {
            //TODO: serialize to JSON?
            return "";
        }
    }

    private SSLSocketFactory getTrustAllSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc.getSocketFactory();
    }

    private void print_https_cert(HttpsURLConnection con) {
        if (con != null) {
            try {
                System.out.println("Response Code : " + con.getResponseCode());
                System.out.println("Cipher Suite : " + con.getCipherSuite());
                System.out.println("\n");
                Certificate[] certs = con.getServerCertificates();
                for (Certificate cert : certs) {
                    System.out.println("Cert Type : " + cert.getType());
                    System.out.println("Cert Hash Code : " + cert.hashCode());
                    System.out.println("Cert Public Key Algorithm : "
                            + cert.getPublicKey().getAlgorithm());
                    System.out.println("Cert Public Key Format : "
                            + cert.getPublicKey().getFormat());
                    System.out.println("\n");
                }
            } catch (SSLPeerUnverifiedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
