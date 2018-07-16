package us.somogyi.ipam;

import com.google.common.base.Preconditions;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static us.somogyi.ipam.BackingStore.IpamRecord;
import static us.somogyi.ipam.BackingStore.filterSpec;

public class IpamRepo {
    private final BackingStore storage;

    public IpamRepo (BackingStore store) {
        this.storage = store;
    }

    public Optional<IpamRecord> AddSubnet(IpamSubnet add) {
        Optional<IpamRecord> result = null;

        try {
            if (!SubnetCollision(add)) {
                result = storage.putSubnet(add);
            } else {
                result = Optional.ofNullable(null);
            }
        } catch (BackingStoreException e) {
            System.err.println("Encountered BackingStoreException: " + e.getMessage());
            result = Optional.ofNullable(null);
        }

        return result;
    }

    public Optional<IpamRecord> DeleteSubnet(IpamSubnet delete) {
        Optional<IpamRecord> result = null;

        try {
            if (storage.querySubnet(delete).isPresent()) {
                result = storage.deleteSubnet(delete);
            } else {
                result = Optional.ofNullable(null);
            }
        } catch (BackingStoreException e) {
            System.err.println("Encountered BackingStoreException: " + e.getMessage());
        }

        return result;
    }

    public Optional<IpamRecord> GetSubnet(IpamSubnet query) {
        Optional<IpamRecord> result = null;

        try {
            result = storage.querySubnet(query);
        } catch (BackingStoreException e) {
            System.err.println("Encountered BackingStoreException: " + e.getMessage());
        }

        return result;
    }

    public List<IpamRecord> GetAllSubnets(filterSpec filter) {
        List<IpamRecord> result;

        try {
            result = storage.queryAllSubnets(filter);
        } catch (BackingStoreException e) {
            System.err.println("Encountered BackingStoreException: " + e.getMessage());
            result = new ArrayList<>();
        }

        return result;
    }

    public List<IpamRecord> GetAllSubnets() {
        List<IpamRecord> result;

        try {
            result = storage.queryAllSubnets();
        } catch (BackingStoreException e) {
            System.err.println("Encountered BackingStoreException: " + e.getMessage());
            result = new ArrayList<>();
        }

        return result;
    }

    private boolean SubnetCollision(IpamSubnet test)  {

        return false;
    }
}
