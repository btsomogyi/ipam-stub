package us.somogyi.ipam;

import com.google.common.net.InetAddresses;
import javafx.util.Pair;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.ws.spi.http.HttpExchange;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

import static us.somogyi.ipam.BackingStore.*;
import static us.somogyi.ipam.IpamServer.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpamServerTest {

    public static final String V4CIDR1 = "192.168.24.0/24";
    public static final String validV4SubnetInput = "{\"cidr\":\"" + V4CIDR1 + "\"}";
    public static final IpamSubnet validV4Subnet = IpamSubnet.fromCidr(V4CIDR1);
    public static final IpamRecord validV4IpamRecord = new IpamRecord(validV4Subnet, 1);

    public static final String V6CIDR1 = "1:2:3:4:5:6:7:8/64";
    public static final String validV6SubnetInput = "{\"cidr\":\"" + V6CIDR1 + "\"}";
    public static final IpamSubnet validV6Subnet = IpamSubnet.fromCidr(V6CIDR1);
    public static final IpamRecord validV6IpamRecord = new IpamRecord(validV6Subnet, 1);


    @Test
    public void ProcessGetRequestWithValidSingleV4RepoDataItem () {
        Map<String, String> parameters = new HashMap<>();
        JSONObject jo = new JSONObject();
        List<IpamRecord> expected =  new ArrayList<>();
        expected.add(validV4IpamRecord);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.GetAllSubnets()).thenReturn(expected);

        Pair<String, Integer> result = processGetRequest(parameters, jo, mockRepo);

        Assert.assertEquals("Expected empty JSON response", "[{\"cidr\":\"192.168.24.0/24\",\"id\":1,\"family\":\"4\"}]", result.getKey() );
        Assert.assertEquals("Expected valid response code", new Integer(200), result.getValue());
    }

    @Test
    public void ProcessGetRequestWithValidSingleV6RepoDataItem () {
        Map<String, String> parameters = new HashMap<>();
        JSONObject jo = new JSONObject();
        List<IpamRecord> expected =  new ArrayList<>();
        expected.add(validV6IpamRecord);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.GetAllSubnets()).thenReturn(expected);

        Pair<String, Integer> result = processGetRequest(parameters, jo, mockRepo);

        Assert.assertEquals("Expected empty JSON response", "[{\"cidr\":\"1:2:3:4:5:6:7:8/64\",\"id\":1,\"family\":\"6\"}]", result.getKey() );
        Assert.assertEquals("Expected valid response code", new Integer(200), result.getValue());
    }

    @Test
    public void ProcessGetRequestWithValidMixedV4V6RepoDataItem () {
        Map<String, String> parameters = new HashMap<>();
        JSONObject jo = new JSONObject();
        List<IpamRecord> expected =  new ArrayList<>();
        expected.add(validV4IpamRecord);
        expected.add(validV6IpamRecord);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.GetAllSubnets()).thenReturn(expected);

        Pair<String, Integer> result = processGetRequest(parameters, jo, mockRepo);

        Assert.assertEquals("Expected empty JSON response",
                "[{\"cidr\":\"192.168.24.0/24\",\"id\":1,\"family\":\"4\"}," +
                        "{\"cidr\":\"1:2:3:4:5:6:7:8/64\",\"id\":1,\"family\":\"6\"}]",
                result.getKey() );
        Assert.assertEquals("Expected valid response code", new Integer(200), result.getValue());
    }

    @Test
    public void ProcessGetRequestWithEmptyRepo () {
        Map<String, String> parameters = new HashMap<>();
        JSONObject jo = new JSONObject();
        Optional<IpamRecord> expected = Optional.ofNullable(null);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.AddSubnet(validV4Subnet)).thenReturn(expected);

        Pair<String, Integer> result = processGetRequest(parameters, jo, mockRepo);

        Assert.assertEquals("Expected empty JSON response", "[]", result.getKey());
        Assert.assertEquals("Expected valid response code", new Integer(200), result.getValue());
    }

    @Test
    public void ProcessPostRequestAcceptEmptyMapAndValidJson () {

        Map<String, String> parameters = new HashMap<>();
        JSONObject jo = new JSONObject(validV4SubnetInput);
        Optional<IpamRecord> expected = Optional.of(validV4IpamRecord);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.AddSubnet(validV4Subnet)).thenReturn(expected);

        Pair<String, Integer> result = processPostRequest(parameters, jo, mockRepo);

        Assert.assertEquals("Expected successful JSON response",
                "{\"cidr\":\"192.168.24.0/24\",\"id\":1,\"family\":\"4\"}",
                result.getKey() );
        Assert.assertEquals("Expected valid response code", new Integer(200), result.getValue());
    }

    @Ignore
    @Test
    public void HandlerWithEmptyParametersAndValidGetRequest () throws IOException {
        Optional<IpamRecord> expected = Optional.ofNullable(null);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.AddSubnet(validV4Subnet)).thenReturn(expected);

        IpamServer testIpamServer = new IpamServer();
        testIpamServer.repo = mockRepo;

        InetAddress localhost = InetAddresses.forString("0:0:0:0:0:0:0:1");
        int port = 51944;
        InputStream anyInputStream = new ByteArrayInputStream("".getBytes());

        HttpExchange mockExchange = org.mockito.Mockito.mock(HttpExchange.class);
        when(mockExchange.getRequestBody()).thenReturn(anyInputStream);
        when(mockExchange.getRequestMethod()).thenReturn("GET");
        when(mockExchange.getRemoteAddress()).thenReturn(new InetSocketAddress(localhost, port));

        //testIpamServer(mockExchange);
    }

    @Ignore
    @Test
    public void ProcessPostRequestRejectMapEntries() {
    }

    @Ignore
    @Test
    public void ProcessPostRequestRejectInvalidJson () {
    }
}
