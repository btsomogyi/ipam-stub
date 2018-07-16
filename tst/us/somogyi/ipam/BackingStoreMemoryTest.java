package us.somogyi.ipam;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static us.somogyi.ipam.BackingStore.IpamRecord;

public class BackingStoreMemoryTest {

    public static final String V4CIDR1 = "192.168.24.0/24";
    public static final IpamSubnet validV4Subnet = IpamSubnet.fromCidr(V4CIDR1);

    public static final String V6CIDR1 = "1:2:3:4:5:6:7:8/64";
    public static final IpamSubnet validV6Subnet = IpamSubnet.fromCidr(V6CIDR1);

    @Test
    public void putSubnetBackingStoreMemory () {
        BackingStore memStore = new BackingStoreMemory();

        Optional<BackingStore.IpamRecord> result = Optional.ofNullable(null);
        Integer id = null;

        try {
            result = memStore.putSubnet(validV4Subnet);
            if (!result.isPresent())
                Assert.fail("putSubnet result is empty");
            id = result.get().getId();
            result = memStore.querySubnetById(id);
        } catch (BackingStoreException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals("Expected same IpamSubnet", validV4Subnet, result.get().getSubnet());
        Assert.assertEquals("Expected same IpamSubnet", id, result.get().getId());
    }

    @Test
    public void getSubnetBackingStoreMemory () {
        BackingStore memStore = new BackingStoreMemory();

        Optional<BackingStore.IpamRecord> result = Optional.ofNullable(null);
        Integer id = null;
        IpamRecord validV4IpamRecord = new IpamRecord(validV4Subnet, 1);

        try {
            result = memStore.putSubnet(validV4Subnet);
            id = result.get().getId();
            result = memStore.querySubnet(validV4Subnet);
        } catch (BackingStoreException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertTrue("Expected IpamSubnet", result.isPresent());
        Assert.assertEquals("Expected same IpamSubnet", validV4IpamRecord, result.get());
        Assert.assertEquals("Expected same IpamRecord ID", id, result.get().getId());
    }

    @Test
    public void getAllSubnetsBackingStoreMemory () {
        BackingStore memStore = new BackingStoreMemory();

        Optional<BackingStore.IpamRecord> result1 = Optional.ofNullable(null);
        Optional<BackingStore.IpamRecord> result2 = Optional.ofNullable(null);
        Integer id1 = null;
        Integer id2 = null;
        List<BackingStore.IpamRecord> queryResult = new ArrayList<>();

        IpamRecord validV4IpamRecord = new IpamRecord(validV4Subnet, 1);
        IpamRecord validV6IpamRecord = new IpamRecord(validV6Subnet, 2);

        try {
            result1 = memStore.putSubnet(validV4Subnet);
            id1 = result1.get().getId();
            result2 = memStore.putSubnet(validV6Subnet);
            id2 = result2.get().getId();

            queryResult = memStore.queryAllSubnets();
        } catch (BackingStoreException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals("Expected V4 result id equal to 1", new Integer(1), id1);
        Assert.assertEquals("Expected V6 result id equal to 2", new Integer(2), id2);
        Assert.assertEquals("Expected two results in result set", 2, queryResult.size() );
        Assert.assertTrue("Expected V4 IpamSubnet in result set", queryResult.contains(validV4IpamRecord));
        Assert.assertTrue("Expected V4 IpamSubnet in result set", queryResult.contains(validV6IpamRecord));
    }
}
