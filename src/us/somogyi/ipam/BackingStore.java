package us.somogyi.ipam;

import com.google.common.base.Objects;

import java.util.Optional;

import java.util.List;

public interface BackingStore {

    /* Persisted record of a subnet and its assigned id
     */
    class IpamRecord {
        final private IpamSubnet subnet;
        final private Integer id;

        public IpamRecord(IpamSubnet subnet, Integer id) {
            this.subnet = subnet;
            this.id = id;
        }

        public IpamSubnet getSubnet() {
            return this.subnet;
        }

        public Integer getId() {
            return this.id;
        }

        @Override
        public String toString() {
            return "[" + this.subnet.getCidr() + "," + this.id + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IpamRecord)) return false;
            IpamRecord that = (IpamRecord) o;
            return Objects.equal(getSubnet(), that.getSubnet()) &&
                    Objects.equal(getId(), that.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getSubnet(), getId());
        }
    }

    /* Passed to filter query to specify filter parameters
    *  includes a Builder pattern for simplicity of use (null values
    *  are not used for filtering)
     */
    public class filterSpec {
        final private String network;
        final private Integer maxid;
        final private Integer minid;
        final private IpamSubnet.Family family;
        final private Integer mask;

        public String getNetwork() {
            return network;
        }

        public Integer getMaxid() {
            return maxid;
        }

        public Integer getMinid() {
            return minid;
        }

        public IpamSubnet.Family getFamily() {
            return family;
        }

        public Integer getMask() {
            return mask;
        }

        public filterSpec (String network,
                           Integer maxid,
                           Integer minid,
                           IpamSubnet.Family family,
                           Integer mask) {
            this.network = network;
            this.maxid = maxid;
            this.minid = minid;
            this.family = family;
            this.mask = mask;
        }
    }

    /* filterSpec Builder e.g.:
    *  filterSpec newSpec = new filterSpecBuilder().network("192.168")
    *                           .family("IPV6").buildFilterSpec();
     */
    public class filterSpecBuilder {
        private String _network;
        private Integer _minid;
        private Integer _maxid;
        private IpamSubnet.Family _family;
        private Integer _mask;

        public filterSpecBuilder () {}

        public filterSpec buildFilterSpec () {
            return new filterSpec(_network, _maxid, _minid, _family, _mask);
        }

        public filterSpecBuilder network(String _network) {
            this._network = _network;
            return this;
        }

        public filterSpecBuilder minid(Integer _minid) {
            this._minid = _minid;
            return this;
        }

        public filterSpecBuilder maxid(Integer _maxid) {
            this._maxid = _maxid;
            return this;
        }

        public filterSpecBuilder family(IpamSubnet.Family _family) {
            this._family = _family;
            return this;
        }

        public filterSpecBuilder mask(Integer _mask) {
            this._mask = _mask;
            return this;
        }
    }

    public Optional<IpamRecord> putSubnet (IpamSubnet net) throws BackingStoreException;

    public Optional<IpamRecord> querySubnet (IpamSubnet net) throws BackingStoreException;

    public Optional<IpamRecord> querySubnetById (Integer id) throws BackingStoreException;

    public Optional<IpamRecord> deleteSubnet (IpamSubnet net) throws BackingStoreException;

    public List<IpamRecord> queryAllSubnets () throws BackingStoreException;

    public List<IpamRecord> queryAllSubnets (filterSpec spec) throws BackingStoreException;
}

