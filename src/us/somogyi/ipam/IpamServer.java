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
import static us.somogyi.ipam.IpamSubnet.isValidCIDR;

public class IpamServer {

    static final int LISTENPORT = 9000;

    static final int CODE_SUCCESS = 200;
    static final int CODE_INVALIDMETHOD = 400;
    static final int CODE_INVALIDPARAMETER = 422;
    static final int CODE_SERVERERRROR = 500;

    static IpamRepo repo;

    /*  Primary handler class for HTTPServer.  Parses and validates input as correct JSON,
    *   then routes request to appropriate method handler (GET / POST).  Assembles response
    *   from method handler and sends response to client.
     */
    static class SubnetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {

                // Pretend server connection logging
                System.err.println("exchange.getRemoteAddress()" + exchange.getRemoteAddress());

                // Get exchange data (Method & query parameters)
                String requestMethod = exchange.getRequestMethod();
                Optional<String> requestQuery = Optional.ofNullable(exchange.getRequestURI().getQuery());
                Map<String, String> parameters;

                if (requestQuery.isPresent()) {
                    parameters = IpamServer.UriQueryToMap(requestQuery.get());
                } else {
                    parameters = new HashMap<>();
                }

                /* Prepare response object to carry response data (String) and code (Integer)
                *  Response.getKey() to retrieve string value, and
                *  Response.getValue() to retrieve Integer value
                 */
                Pair<String, Integer> Response;

                /* Attempt to parse JSON body and send 400 response on illegal JSONException.
                *  Attempt to process request method if JSON body parses properly.
                 */
                JSONObject inputJson;
                try {
                    inputJson = GetInputBody(exchange);

                    switch (requestMethod) {
                        case "GET":
                            Response = processGetRequest(parameters, inputJson, repo);
                            break;
                        case "POST":
                            Response = processPostRequest(parameters, inputJson, repo);
                            break;
                        default:
                            Response = new Pair("Invalid HTTP Method" + requestMethod.toString(), new Integer(CODE_INVALIDMETHOD));
                    }
                } catch (JSONException json) {
                    System.err.println("SubnetHandler: caught JSONException: " + json.getMessage());
                    Response = new Pair("Invalid JSON Body:" + json.getMessage(), new Integer(CODE_INVALIDMETHOD));
                }

                // Successfully parsed and processed request, not process Response object
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
                e.printStackTrace(System.err);
            }
        }
    }

    /* Returns a Pair object which includes the response body (String) and response code (Integer)
     *
     */
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
                            code = CODE_INVALIDPARAMETER;
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
                                    code = CODE_INVALIDPARAMETER;
                            }
                        }
                        break;
                    default:
                        response.append("Invalid query parameter:" + Param.toString() + "\n\r");
                        code = CODE_INVALIDPARAMETER;
                }
            }
        }

        // if code is null, no parameter errors have occurred, so continue processing
        List<IpamRecord> Result;

        if (code == null) {
            if (requestFilter != null) {
                Result = target.GetAllSubnets(requestFilter);
            } else {
                Result = target.GetAllSubnets();
            }

            JSONArray jsonresults = new JSONArray();

            for (IpamRecord net: Result) {
                JSONObject jo = getJsonObjectFromIpamRecord(net);
                jsonresults.put(jo);
            }

            response.append(jsonresults.toString());
            code = CODE_SUCCESS;
        }

        return new Pair(response.toString(), code);
    }

    /* Returns a Pair object which includes the response body (String) and response code (Integer)
    *
     */
    static Pair<String, Integer> processPostRequest(Map<String, String> parameters, JSONObject inputJson, IpamRepo target) {
        Preconditions.checkNotNull(parameters, "processPostRequest: Invalid null reference - parameters");
        Preconditions.checkNotNull(inputJson, "processPostRequest: Invalid null reference - inputJson");
        Preconditions.checkNotNull(target, "processPostRequest: Invalid null reference - target");

        StringBuilder response = new StringBuilder();
        Integer errorCode = null;

        // validate request parameters - no parameters expected, so any present is invalid
        for (String Param : parameters.keySet()) {
            switch (Param) {
                default:
                    response.append("Invalid query parameter:" + Param + "\n\r");
                    errorCode = CODE_INVALIDPARAMETER;
            }
        }

        Optional<IpamRecord> result;
        Pair<Boolean, String> validatedCidr;  // Pair<is valid, reason if invalid or cidr if valid>

        /* if code is null, no parameter errors have occurred, and JSON object valid, then
        *  continue processing
         */
        if (errorCode != null) {
            return new Pair(response.toString(), errorCode);
        } else if (!ValidateJsonSubnet(inputJson)) {
            response.append("Invalid subnet specification: " + inputJson.toString());
            errorCode = CODE_INVALIDPARAMETER;
        } else {

            validatedCidr = isValidCIDR(inputJson.get("cidr").toString());

            // If CIDR is valid data, attempt to add record to IPAM database
            if (validatedCidr.getKey()){
                try {
                    result = target.AddSubnet( IpamSubnet.fromCidr(validatedCidr.getValue()));
                    // Check that IPAM database update was complete, then parse results
                    if (result.isPresent()) {
                        JSONObject jsonResponse = getJsonObjectFromIpamRecord(result.get());
                        response.append(jsonResponse.toString());
                        errorCode = CODE_SUCCESS;
                    } else {
                        response.append("Failed to add subnet " + inputJson.toString());
                        errorCode = CODE_SERVERERRROR;
                    }
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Validated JSON failed to convert to IpamSubnet");
                }
            } else {
                response.append("Failed to add subnet " + inputJson.toString());
                errorCode = CODE_SERVERERRROR;
            }
        }

        return new Pair(response.toString(), errorCode);
    }

    private static JSONObject getJsonObjectFromIpamRecord(IpamRecord net) {
        JSONObject jo = new JSONObject();
        jo.put("id", net.getId());
        jo.put("family", net.getSubnet().getFamily().getAlias());
        jo.put("cidr", net.getSubnet().getCidr());
        return jo;
    }

    /*  Parse input body into JSON object.  Throws JSONException for invalid JSON data,
    *   and logs any other IOException errors
     */
    static JSONObject GetInputBody(HttpExchange exchange) throws JSONException {
        Preconditions.checkNotNull(exchange, "GetInputBody: Invalid null reference - exchange");

        StringBuilder inputbody = new StringBuilder();

        InputStream input = exchange.getRequestBody();

        JSONObject results;

        if (input != null) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
                String inputLine;

                while ((inputLine = br.readLine()) != null ) {

                    inputbody.append(inputLine);
                }

            } catch (IOException e) {
                System.err.println("GetInputBody IOException: " + e.getMessage());
            }



        }

        if (inputbody.length() > 0) {
            results = new JSONObject(inputbody.toString());
        } else {
            results = new JSONObject();
        }

        return results;
    }

    // Break apart URL Query parameters into keypair map
    static Map<String, String> UriQueryToMap(String query) {
        Preconditions.checkNotNull(query, "UriQueryToMap: Invalid null string reference - query");

        Map<String, String> params = new HashMap<>();
            for (String param : query.split("&")) {
                String pair[] = param.split("=");
                if (pair.length > 1)
                    params.put(pair[0], pair[1]);
                else
                    params.put(pair[0], "");
            }
        return params;
    }

    /*  Assemble response message
     */
    static private void SendResponse(HttpExchange exchange,
                                     int code,
                                     String type,
                                     String response) throws IOException {
        Preconditions.checkNotNull(exchange, "SendResponse: Invalid null reference - exchange");
        Preconditions.checkNotNull(type, "SendResponse: Invalid null reference - type");
        Preconditions.checkNotNull(response, "SendResponse: Invalid null reference - response");

        SetResponseHeaders(exchange, type);
        exchange.sendResponseHeaders(code, response.length());
        OutputStream out = exchange.getResponseBody();
        out.write(response.getBytes());
        out.close();
    }

    /*  Set appropriate response headers: TXT for error response, JSON for success response
     */
    static private void SetResponseHeaders(HttpExchange exchange, String type) {
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

    /*  Ensure input JSON contains both valid JSON format (eg no extraneous keys) as well
    *   valid well formatted CIDR data.
     */
    static private boolean ValidateJsonSubnet(JSONObject input) {
        Preconditions.checkNotNull(input, "ValidateJsonSubnet: Invalid null reference - input");

        int keycount = 0;
        for (String key : JSONObject.getNames(input)) {
            switch (key) {
                case "cidr":

                    if ( isValidCIDR(input.get(key).toString()).getKey() ) {
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

        BackingStore store = new BackingStoreMemory();
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
