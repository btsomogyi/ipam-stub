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
            IpamSubnet.fromCidr("192.168.1.0/24"),
            IpamSubnet.fromCidr("10.0.0.0/8"),
            IpamSubnet.fromCidr("10.1.0.0/16"),
            IpamSubnet.fromCidr("10.2.1.0/24"),
            IpamSubnet.fromCidr("172.16.0.0/16"),
            IpamSubnet.fromCidr("172.17.0.0/24")
    );

    public static ArrayList<String[]> validSubnets = new ArrayList<String[]>();

    static {
        validSubnets.add(new String[] {"192.168.1.0","24","4"});
        validSubnets.add(new String[] {"10.0.0.0","8","4"});
        validSubnets.add(new String[] {"10.1.0.0","16","4"});
        validSubnets.add(new String[] {"10.2.1.0","24","4"});
        validSubnets.add(new String[] {"172.16.0.0","16","4"});
        validSubnets.add(new String[] {"172.17.0.0","24","4"});
        validSubnets.add(new String[] {"2001:0:0:4:0:0:0:8","32","6"});
        validSubnets.add(new String[] {"1:2:3:4:5:6:7:8","64","6"});
        validSubnets.add(new String[] {"1:0:0:4:0:0:7:8","64","6"});
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
    public void AcceptValidSubnetInputs() {
        IpamSubnet ipam = null;
        for (int i = 0; i < validSubnets.size(); i++) {
            try {
                ipam = IpamSubnet.fromCidr(validSubnets.get(i)[0] + "/" + validSubnets.get(i)[1]);
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
            IpamSubnet.fromCidr(invalidSubnet[i][0] + "/" + invalidSubnet[i][1]);
        }
    }

    @Test
    public void isValidIpamSubnetsPass() {
        for (int i = 0; i < validSubnets.size(); i++) {
            String subnet = validSubnets.get(i)[0] + "/" + validSubnets.get(i)[1];
            Assert.assertTrue("Expected valid IpamSubnet: " + subnet,
                    IpamSubnet.isValidCIDR(subnet).getKey()) ;
        }
    }

    @Test
    public void isValidIpamSubnetsFail() {
        for (int i = 0; i < invalidSubnet.length; i++) {
            String subnet = invalidSubnet[i][0] + "/" + invalidSubnet[i][1];
            Assert.assertFalse("Expected invalid IpamSubnet: " + subnet,
                    IpamSubnet.isValidCIDR(subnet).getKey()) ;
        }
    }

    @Test
    public void TwoIdenticalIpamSubnetAreEquals() {
        String subnet = validSubnets.get(0)[0] + "/" + validSubnets.get(0)[1];
        IpamSubnet ipam1 = IpamSubnet.fromCidr(subnet);
        IpamSubnet ipam2 = IpamSubnet.fromCidr(subnet);

        assertEquals(ipam1, ipam2);
    }


    @Test
    public void TwoDifferentIpamSubnetAreNotEquals() {
        String subnet = validSubnets.get(0)[0] + "/" + validSubnets.get(0)[1];
        IpamSubnet ipam1 = IpamSubnet.fromCidr(subnet);
        subnet = validSubnets.get(1)[0] + "/" + validSubnets.get(1)[1];
        IpamSubnet ipam2 = IpamSubnet.fromCidr(subnet);

        assertNotEquals(ipam1, ipam2);
    }

    // Additional Tests Needed
    // Reject Subnets with host bits beyond mask boundary (eg 192.168.1.1/24)
    //
}
