package us.somogyi.ipam;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static us.somogyi.ipam.BackingStore.IpamRecord;

public class IpamRepoTest {

    public static final String V4CIDR1 = "192.168.24.0/24";
    public static final String V4CIDR2 = "172.16.0.0/16";
    public static final String V6CIDR = "1:2:3:4:5:6:7:8/64";

    public static final List<IpamRecord> exampleRepoRecords = Arrays.asList(
            new IpamRecord(IpamSubnet.fromCidr(V4CIDR1), 1),
            new IpamRecord(IpamSubnet.fromCidr(V4CIDR2), 2),
            new IpamRecord(IpamSubnet.fromCidr(V6CIDR), 3)
    );

    public static final Optional<IpamRecord> returnedIpamRecord =
            Optional.ofNullable(new IpamRecord(IpamSubnet.fromCidr(V4CIDR1), 1));

    @Test
    public void AddSubnetsToRepo () throws BackingStoreException {
        IpamSubnet subnet = IpamSubnet.fromCidr(V4CIDR1);
        BackingStore mockBackingStore = mock(BackingStore.class);
        IpamRepo repo = new IpamRepo(mockBackingStore);

        try {
            when(mockBackingStore.putSubnet(IpamSubnet.fromCidr(V4CIDR1))).thenReturn(returnedIpamRecord);

            Optional<IpamRecord> added = repo.AddSubnet(IpamSubnet.fromCidr(V4CIDR1));
            Assert.assertTrue("Added subnet expected to equal returned", subnet.equals(added.get().getSubnet()));
        } catch (BackingStoreException e) {
            fail("BackingStoreException:" + e.getMessage());
        }
    }


    @Test
    public void HandleAddSubnetsBackingStoreException () {
        IpamSubnet subnet = IpamSubnet.fromCidr(V4CIDR1);
        BackingStore mockBackingStore = mock(BackingStore.class);
        IpamRepo repo = new IpamRepo(mockBackingStore);



        try {
            when(mockBackingStore.putSubnet(subnet))
                    .thenThrow(new BackingStoreException("Failed to store new subnet", new IOException()));

            Optional<IpamRecord> added = repo.AddSubnet(subnet);
            Assert.assertFalse("Expected empty Optional<IpamRecord>", added.isPresent());
        } catch (BackingStoreException e) {
            fail("BackingStoreException uncaught by IpamRepo:" + e.getMessage());
        }

    }

    @Ignore
    @Test
    public void DeleteSubnetsFromRepo () {
    }

    @Ignore
    @Test
    public void QueryRepoForSubnets () {
    }

    @Ignore
    @Test
    public void QueryRepoForV4Subnets () {

    }

    @Ignore
    @Test
    public void QueryRepoForV6Subnets () {

    }

    @Ignore
    @Test
    public void QueryRepoForSpecificV4Subnet () {

    }

    @Ignore
    @Test
    public void QueryRepoForSpecificV6Subnet () {

    }

    @Ignore
    @Test
    public void QueryRepoForMissingV4Subnet () {

    }

    @Ignore
    @Test
    public void QueryRepoForMissingV6Subnet () {

    }

    @Ignore
    @Test
    public void AddConflictingSubnetToRepo () {

    }
}
