package us.somogyi.ipam;

import org.junit.Test;
import org.junit.Assert;
import us.somogyi.ipam.IpamSubnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class IpamSubnetTest {

    public static final List<IpamSubnet> exampleSubnets = Arrays.asList(
            new IpamSubnet("192.168.1.0/24"),
            new IpamSubnet("10.0.0.0/8"),
            new IpamSubnet("10.1.0.0/16"),
            new IpamSubnet("10.2.1.0/24"),
            new IpamSubnet("172.16.0.0/16"),
            new IpamSubnet("172.17.0.0/24")
    );

    public static ArrayList<String[]> validSubnets = new ArrayList<String[]>();

    static {
        validSubnets.add(new String[] {"192.168.1.0","24","V4"});
        validSubnets.add(new String[] {"10.0.0.0","8","V4"});
        validSubnets.add(new String[] {"10.1.0.0","16","V4"});
        validSubnets.add(new String[] {"10.2.1.0","24","V4"});
        validSubnets.add(new String[] {"172.16.0.0","16","V4"});
        validSubnets.add(new String[] {"172.17.0.0","24","V4"});
        validSubnets.add(new String[] {"2001:0:0:4:0:0:0:8","32","V6"});
        validSubnets.add(new String[] {"1:2:3:4:5:6:7:8","64","V6"});
        validSubnets.add(new String[] {"1:0:0:4:0:0:7:8","64","V6"});
    }


    public static final String[][] invalidSubnet = new String[][] {
            {"016.016.016.016","24"},
            {"016.016.016","24"},
            {"016.016","24"},
            {"016","24"},
            {"7:6:5:4:3:2:1:0::", "64"}, // too many parts
            {"9:8:7:6:5:4:3::2:1", "64"},  // too many parts
            {"1.2.3.4","64"},  // bad v4 mask
            {"192.168.1.0","0"}, // invalid mask
            {"1:0:0:4:0:0:7:8", "129"},
            {"1:0:0:4:0:0:7:8", "0"}
    };

    @Test
    public void TestForValidSubnetInputs() {
        IpamSubnet ipam = null;
        for (int i = 0; i < validSubnets.size(); i++) {
            try {
                ipam = new IpamSubnet(validSubnets.get(i)[0] + "/" + validSubnets.get(i)[1]);
            } catch (IllegalArgumentException e) {
            }
            assertEquals(validSubnets.get(i)[0], ipam.getSubnetId());
            assertEquals(validSubnets.get(i)[1], ipam.getMaskStr());
            assertEquals(validSubnets.get(i)[2], ipam.getFamily().getAlias());
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void RejectInvalidSubnets() {
        for (int i = 0; i < invalidSubnet.length; i++) {
            new IpamSubnet(invalidSubnet[i][0] + "/" + invalidSubnet[i][1]);
        }
    }

    @Test
    public void isValidIpamSubnetsPass() {
        IpamSubnet ipam = null;
        for (int i = 0; i < validSubnets.size(); i++) {
            Assert.assertTrue("Expected valid IpamSubnet", IpamSubnet.isValidIpamSubnet(validSubnets.get(i)[0] + "/" + validSubnets.get(i)[1])) ;
        }
    }

    @Test
    public void isValidIpamSubnetsFail() {
        IpamSubnet ipam = null;
        for (int i = 0; i < invalidSubnet.length; i++) {
            Assert.assertFalse("Expected invalid IpamSubnet", IpamSubnet.isValidIpamSubnet(invalidSubnet[i][0] + "/" + invalidSubnet[i][1])) ;
        }
    }
    // Additional Tests Needed
    // Reject Subnets with host bits beyond mask boundary (eg 192.168.1.1/24)
    //
}
