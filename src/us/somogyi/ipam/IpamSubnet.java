package us.somogyi.ipam;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import javafx.util.Pair;


import java.net.InetAddress;

import static us.somogyi.ipam.IpamSubnet.Family.IPV4;
import static us.somogyi.ipam.IpamSubnet.Family.IPV6;

public class IpamSubnet implements Comparable<IpamSubnet> {

    public enum Family {
        IPV4("4"), IPV6("6");
        private final String alias;
        Family(String alias) {
            this.alias = alias;
        }
        public String getAlias() {return alias;}
    }

    public static final int V4MASKMAX = 32;
    public static final int V6MASKMAX = 128;
    public static final int MASKMIN = 1;

    private final SubnetHolder SubnetInfo;


    /*  IPV4 and IPV6 compatible representation of a subnet, using InetAddress
     *   as base storage for network number.
     */
    private class SubnetHolder {
        final InetAddress subnetId;
        final int mask;
        final Family family;

        private SubnetHolder (InetAddress network, int mask) {
            this.subnetId = network;
            this.mask = mask;
            if (this.subnetId.getAddress().length == 4) this.family = IPV4;
            else this.family = IPV6;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubnetHolder subnet = (SubnetHolder) o;
            return mask == subnet.mask &&
                    Objects.equal(subnetId, subnet.subnetId) &&
                    family == subnet.family;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(subnetId, mask, family);
        }
    }
    // End Subnet private subclass definition

    private IpamSubnet (InetAddress netAddr, int mask) {

        this.SubnetInfo = new SubnetHolder(netAddr, mask);

    }

    /* Protect constructor from invalid invocation (bad cidr)
    *
     */
    public static IpamSubnet fromCidr(String cidr) throws IllegalArgumentException {
        Pair<Boolean, String> valid = isValidCIDR(cidr);
        if(!valid.getKey())
            throw new IllegalArgumentException(valid.getValue());
        else {

            Pair<String, Integer> parsedCidr = parseCIDR(valid.getValue());
            InetAddress subnetId = InetAddresses.forString(parsedCidr.getKey());
            return new IpamSubnet(subnetId, parsedCidr.getValue());
        }
    }

    /*  Returns a Pair value of results and reason for failure (or original cidr string on success)
    *   To access results, use ReturnedPair.getKey() for Boolean of result, and ReturnedPair.getValue()
    *   for failure reason.
     */
    public static Pair<Boolean,String> isValidCIDR(String cidr) {
        Preconditions.checkNotNull(cidr, "isValidateSubnet: Invalid null reference - input");

        Pair<String, Integer> parsedCidr;

        try {
            parsedCidr = parseCIDR ( cidr);
        } catch (IllegalArgumentException e) {
            return new Pair(false, e.getMessage());
        }

        return new Pair(true, cidr);
    }

    /* Return network number (string) and network mask (int) as output provided a CIDR
    *  as input.  Validates and throws exception on invalid CIDR spec or format.
     */
    public static Pair<String, Integer> parseCIDR (String cidr) throws IllegalArgumentException {
        String[] parts;
        String network;
        int mask;

        parts = cidr.split("/");
        if (parts == null || parts.length != 2)
            throw new IllegalArgumentException("Invalid CIDR Specified" + cidr + ": invalid format");

        network = parts[0];
        mask = Integer.valueOf(parts[1]);


        if (!InetAddresses.isInetAddress(network))
            throw new IllegalArgumentException("Invalid CIDR Specified" + cidr + ": illegal network number");

        int family;
        switch (InetAddresses.forString(network).getAddress().length) {
            case 4:
                family = 4;
                break;
            case 16:
                family = 6;
                    break;
            default:
                throw new IllegalArgumentException("Invalid CIDR Specified" + cidr + ": illegal network number");
        }

        /* Check for mask size out of range for appropriate address family
         *  ie, 1 or greater, and <= 32 for V4 and <= 128 for V6
         */
        if (mask < MASKMIN ||
                (family == 4 && mask >= V4MASKMAX) ||
                (family == 6 && mask >= V6MASKMAX) ) {
            throw new IllegalArgumentException("Invalid CIDR Specified " + cidr + ": illegal mask length");
        }

        return new Pair(network, mask);
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

    /* Begin compareTo implementation:
     *  Order on Subnet address first (IPV6 before IPV4), if equal
     *  sort on smaller subnet mask first.  Allows use in ordered collections.
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
}
