package us.somogyi.ipam;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javafx.util.Pair;
import com.google.common.base.Preconditions;

import static us.somogyi.ipam.BackingStore.*;

public class IpamServer {

    static final int LISTENPORT = 9000;

    static final int SUCCESS = 200;
    static final int INVALIDPARAMTER = 422;

    private static IpamRepo repo;



    static class SubnetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {


            System.out.println("exchange.getRemoteAddress()" + exchange.getRemoteAddress());

            //Headers requestHeaders = exchange.getRequestHeaders();

            String requestMethod = exchange.getRequestMethod();
            Optional<String> requestQuery = Optional.ofNullable(exchange.getRequestURI().getQuery());
            Map<String, String> parameters;

            if (requestQuery.isPresent()) {
                parameters = IpamServer.UriQueryToMap(requestQuery.get());
            } else {
                parameters = new HashMap<>();
            }


            JSONObject inputJson = null;

            Pair<String, Integer> Response;

            /* Attempt to parse JSON body and send 400 response on illegal JSONException.
            *  Attempt to process request method if JSON body parses properly.
             */
            try {
                //System.out.println("SubnetHandler: enter try");
                inputJson = GetInputBody(exchange);
                System.out.println("inputJson.toString(): " + inputJson.toString());

                switch (requestMethod) {
                    case "GET":
                        Response = processGetRequest(parameters, inputJson, repo);
                        break;
                    case "POST":
                        Response = processPostRequest(parameters, inputJson, repo);
                        break;
                    default:
                        Response = new Pair("Invalid HTTP Method" + requestMethod.toString(), new Integer(400));
                }
            } catch (JSONException json) {
                System.out.println("SubnetHandler: caught JSONException: " + json.getMessage());
                Response = new Pair("Invalid JSON Body:" + json.getMessage(), new Integer(400));
            }

            try {
                String type;
                if (Response.getValue() >= 300)
                    type = "TXT";
                else
                    type = "JSON";

                SendResponse(exchange, Response.getValue(), type, Response.getKey());

            } catch (IOException io) {
                System.err.println("IOException sending response: " + exchange.toString());
            }

            }  catch (Exception e) {
                System.err.println("SubnetHandler caught exception: " + e.getMessage());
            }
        }
    }

    static Pair<String, Integer> processGetRequest(Map<String, String> parameters, JSONObject inputJson, IpamRepo target) {
        Preconditions.checkNotNull(parameters, "processGetRequest: Invalid null reference - parameters");
        Preconditions.checkNotNull(inputJson, "processGetRequest: Invalid null reference - inputJson");
        Preconditions.checkNotNull(target, "processGetRequest: Invalid null reference - target");

        StringBuilder response = new StringBuilder();
        Integer code = null;
        String filter;
        filterSpec requestFilter = null;

        if (!parameters.isEmpty()) {
            for (String Param : parameters.keySet()) {
                switch (Param) {
                    case "family":
                        filter = parameters.get(Param);
                        if (filter.isEmpty()) {
                            response.append("Empty family parameter");
                            code = INVALIDPARAMTER;
                        } else {
                            switch (filter) {
                                case "4":
                                    requestFilter = new filterSpecBuilder()
                                            .family(IpamSubnet.Family.valueOf("V4"))
                                            .buildFilterSpec();
                                    break;
                                case "6":
                                    requestFilter = new filterSpecBuilder()
                                            .family(IpamSubnet.Family.valueOf("V6"))
                                            .buildFilterSpec();
                                    break;
                                default:
                                    response.append("Invalid family parameter: " + filter.toString());
                                    response.append("\n\rExpected one of '4' or '6'");
                                    code = INVALIDPARAMTER;
                            }
                        }
                        break;
                    default:
                        response.append("Invalid query parameter:" + Param.toString() + "\n\r");
                        code = INVALIDPARAMTER;
                }
            }
        }

        // if code is null, no parameter errors have occurred, so continue processing
        List<IpamRecord> result;

        if (code == null) {
            if (requestFilter != null) {
                result = target.GetAllSubnets(requestFilter);
            } else {
                result = target.GetAllSubnets();
            }

            JSONArray jsonresults = new JSONArray();

            for (IpamRecord net: result) {
                JSONObject jo = new JSONObject();
                jo.put("id", net.getId());
                jo.put("family", net.getSubnet().getFamily());
                jo.put("cidr", net.getSubnet().getCidr());
                jsonresults.put(jo);
            }

            response.append(jsonresults.toString());
            code = SUCCESS;
        }

        return new Pair(response, code);
    }

    static Pair<String, Integer> processPostRequest(Map<String, String> parameters, JSONObject inputJson, IpamRepo target) {
        Preconditions.checkNotNull(parameters, "processPostRequest: Invalid null reference - parameters");
        Preconditions.checkNotNull(inputJson, "processPostRequest: Invalid null reference - inputJson");
        Preconditions.checkNotNull(target, "processPostRequest: Invalid null reference - target");

        StringBuilder response = new StringBuilder();
        Integer code = null;

        for (String Param : parameters.keySet()) {
            switch (Param) {
                default:
                    response.append("Invalid query parameter:" + Param + "\n\r");
                    code = INVALIDPARAMTER;
            }
        }

        // if code is null, no parameter errors have occurred, so continue processing
        Optional<IpamRecord> result;

        if (code == null && ValidateJsonSubnet(inputJson)) {

            try {
                result = target.AddSubnet( new IpamSubnet(inputJson.get("cidr").toString()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Validated JSON failed to convert to IpamSubnet");
            }

            if (result.isPresent()) {
                JSONObject jsonresponse = new JSONObject();

                jsonresponse.put("id", result.get().getId());
                jsonresponse.put("family", result.get().getSubnet().getFamily());
                jsonresponse.put("cidr", result.get().getSubnet().getCidr());

                response.append(jsonresponse.toString());
                code = SUCCESS;
            } else {
                response.append("Failed to add subnet " + inputJson.toString());
                code = 500;
            }

        }
        System.out.println("processPostRequest: response.toString()" + response.toString());
        System.out.println("processPostRequest: code " + code);

        return new Pair(response.toString(), code);
    }


    static JSONObject GetInputBody(HttpExchange exchange) throws JSONException {
        Preconditions.checkNotNull(exchange, "GetInputBody: Invalid null reference - exchange");

        System.out.println("GetInputBody: entered ");
        StringBuilder inputbody = new StringBuilder();

        InputStream input = exchange.getRequestBody();

        JSONObject results;

        if (input != null) {
            //System.out.println("GetInputBody InputStream input: not null" );
            try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
                String inputLine;

                while ((inputLine = br.readLine()) != null ) {
                    //System.out.println("GetInputBody inputLine: " + inputLine);
                    inputbody.append(inputLine);
                }

            } catch (IOException e) {
                System.err.println("GetInputBody IOException: " + e.getMessage());
            }

            results = new JSONObject(inputbody.toString());

        } else {
            //System.out.println("GetInputBody InputStream input: null" );
            results = new JSONObject("");
        }

        System.out.println("GetInputBody: leaving ");
        return results;
    }

    // Break apart URL Query parameters into keypair map
    static Map<String, String> UriQueryToMap(String query) {
        Preconditions.checkNotNull(query, "UriQueryToMap: Invalid null string reference - query");

        System.out.println("UriQueryToMap");
        Map<String, String> params = new HashMap<>();
            for (String param : query.split("&")) {
                System.out.println("UriQueryToMap param[0]: " + param);
                String pair[] = param.split("=");
                if (pair.length > 1)
                    params.put(pair[0], pair[1]);
                else
                    params.put(pair[0], "");
            }
        return params;
    }

    static private void SendResponse(HttpExchange exchange,
                                     int code,
                                     String type,
                                     String response) throws IOException {
        Preconditions.checkNotNull(exchange, "SendResponse: Invalid null reference - exchange");
        Preconditions.checkNotNull(type, "SendResponse: Invalid null reference - type");
        Preconditions.checkNotNull(response, "SendResponse: Invalid null reference - response");

        SetHeaders(exchange, type);
        exchange.sendResponseHeaders(code, response.length());
        OutputStream out = exchange.getResponseBody();
        out.write(response.getBytes());
        out.close();
    }

    static private void SetHeaders(HttpExchange exchange, String type) {
        Preconditions.checkNotNull(exchange, "SetHeaders: Invalid null reference - exchange");
        Preconditions.checkNotNull(type, "SetHeaders: Invalid null reference - type");

        Headers responseHeaders = exchange.getResponseHeaders();
        switch (type) {
            case "TXT":
                responseHeaders.set("Content-Type", "text/plain");
                break;
            case "JSON":
                responseHeaders.set("Content-Type", "text/json");
                break;
            default:
                responseHeaders.set("Content-Type", type);
        }

        responseHeaders.set("Server", "JavaHTTPServer/1.0");
    }

    static private boolean ValidateJsonSubnet(JSONObject input) {
        Preconditions.checkNotNull(input, "ValidateJsonSubnet: Invalid null reference - input");

        int keycount = 0;
        for (String key : JSONObject.getNames(input)) {
            switch (key) {
                case "cidr":

                    if ( IpamSubnet.isValidIpamSubnet(input.get(key).toString()) ) {
                    keycount++;
                    } else
                        return false;
                    break;
                default:
                    return false;
            }
        }
        if (keycount == 1) return true;
        else return false;
    }

    public static void main(String[] args) {

        System.out.println("Starting HTTP Server...");

        BackingStore store = BackingStoreMemory.getInstance();
        repo = new IpamRepo(store);

        final Executor multi = Executors.newCachedThreadPool();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(LISTENPORT), 0);
            server.createContext("/subnets", new IpamServer.SubnetHandler());
            server.start();
            System.out.println("HTTP Server Started");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
