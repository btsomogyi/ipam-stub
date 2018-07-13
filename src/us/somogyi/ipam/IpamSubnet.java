package us.somogyi.ipam;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;

import java.net.InetAddress;

import static us.somogyi.ipam.IpamSubnet.Family.IPV4;
import static us.somogyi.ipam.IpamSubnet.Family.IPV6;

public class IpamSubnet implements Comparable<IpamSubnet> {

    public static final int V4MASKMAX = 24;
    public static final int V6MASKMAX = 128;
    public static final int MASKMIN = 1;

    /* Begin compareTo implementation:
    *  Order on Subnet address first (IPV6 before IPV4), if equal
    *  sort on smaller subnet mask first.
     */
    /*
     * LGPL
     * https://thilosdevblog.wordpress.com/2010/09/15/sorting-ip-addresses-in-java/
     */
    @Override
    public int compareTo(IpamSubnet o) {
        byte[] ba1 = this.SubnetInfo.subnetId.getAddress();
        byte[] ba2 = o.SubnetInfo.subnetId.getAddress();

        // general ordering: ipv4 before ipv6
        if(ba1.length < ba2.length) return -1;
        if(ba1.length > ba2.length) return 1;

        // we have 2 ips of the same type, so we have to compare each byte
        for(int i = 0; i < ba1.length; i++) {
            int b1 = unsignedByteToInt(ba1[i]);
            int b2 = unsignedByteToInt(ba2[i]);
            if(b1 == b2)
                continue;
            if(b1 < b2)
                return -1;
            else
                return 1;
        }
        Integer mask1 = this.SubnetInfo.mask;
        Integer mask2 = o.SubnetInfo.mask;
        if ( mask1 < mask2 )
            return -1;
        else if (mask1 > mask2)
            return 1;

        return 0;

    }

    private static int unsignedByteToInt(byte b) {
        return (int) b & 0xFF;
    }

    // end compareTo implementation

    public enum Family {
        IPV4("V4"), IPV6("V6");
        private final String alias;
        Family(String alias) {
            this.alias = alias;
        }
        public String getAlias() {return alias;}
    }

    private final Subnet SubnetInfo;

    private class Subnet {
        final InetAddress subnetId;
        final int mask;
        final Family family;

        Subnet (String network, int mask) throws IllegalArgumentException {
            this.subnetId = InetAddresses.forString(network);
            this.mask = mask;
            if (this.subnetId.getAddress().length == 4) this.family = IPV4;
            else this.family = IPV6;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Subnet subnet = (Subnet) o;
            return mask == subnet.mask &&
                    Objects.equal(subnetId, subnet.subnetId) &&
                    family == subnet.family;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(subnetId, mask, family);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpamSubnet)) return false;
        IpamSubnet that = (IpamSubnet) o;
        return Objects.equal(SubnetInfo, that.SubnetInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(SubnetInfo);
    }

    public IpamSubnet (String cidr) throws IllegalArgumentException {
        String[] parts;
        String network;
        int mask;

        parts = cidr.split("/");
        if (parts == null || parts.length != 2) throw
                new IllegalArgumentException("Invalid CIDR Specified" + cidr);

        network = parts[0];
        mask = Integer.valueOf(parts[1]);


        if (!InetAddresses.isInetAddress(parts[0]) || mask >= V6MASKMAX || mask < MASKMIN ) {
            throw new IllegalArgumentException("Invalid CIDR Specified" + cidr);
        }

        this.SubnetInfo = new Subnet(network, mask);

    }

    public static boolean isValidIpamSubnet(String cidr) {
        Preconditions.checkNotNull(cidr, "isValidateSubnet: Invalid null reference - input");

        try {
            new IpamSubnet(cidr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }

    }

    public Family getFamily () {
        return this.SubnetInfo.family;
    }

    public String getSubnetId () {
        return this.SubnetInfo.subnetId.toString().substring(1); // remove leading slash
    }

    public String getCidr() {
        String CidrInfo = this.getSubnetId()
                + "/" + getMaskStr();
        return CidrInfo;
    }

    public int getMask() {
        return this.SubnetInfo.mask;
    }

    public String getMaskStr() {
        return Integer.toString(this.getMask());
    }
}
