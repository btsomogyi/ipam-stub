package us.somogyi.ipam;

import javafx.util.Pair;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static us.somogyi.ipam.BackingStore.*;
import static us.somogyi.ipam.IpamServer.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpamServerTest {

    public static final String V4CIDR1 = "192.168.24.0/24";
    public static final String validSubnetInput = "{\"cidr\":\"192.0.2.0/24\"}";

    public static final IpamSubnet validV4Subnet = new IpamSubnet(V4CIDR1);
    public static final IpamRecord validV4IpamRecord = new IpamRecord(validV4Subnet, 1);

    @Test
    public void ProcessPostRequestAcceptEmptyMapAndValidJson () {

        Map<String, String> parameters = new HashMap<>();
        JSONObject jo = new JSONObject(validSubnetInput);
        Optional<IpamRecord> expected = Optional.of(validV4IpamRecord);

        IpamRepo mockRepo = mock(IpamRepo.class);
        when(mockRepo.AddSubnet(validV4Subnet)).thenReturn(expected);

        Pair<String, Integer> result = processGetRequest(parameters, jo, mockRepo);

        /*
        Assert.assertEquals("Expected valid returned IpamRecord",
                expected.get().getSubnet().getSubnetId(),
                result.getKey());
                */
        Assert.assertEquals("Expected valid response code", result.getValue(), new Integer(200));
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
