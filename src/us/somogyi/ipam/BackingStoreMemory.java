package us.somogyi.ipam;

//import com.google.common.base.Optional;

import com.google.common.collect.*;

import java.util.*;

public class BackingStoreMemory implements BackingStore {

    private static BackingStoreMemory instance;
    private static Integer idCount = 1;

    private final BiMap<IpamSubnet, Integer> repo;
    private final BiMap<Integer, IpamSubnet> revrepo;

    // Singleton class
    private BackingStoreMemory() {
        repo = HashBiMap.create();
        revrepo = repo.inverse();
    }

    public static synchronized BackingStoreMemory getInstance() {
        if (instance == null)
            instance = new BackingStoreMemory();

        return instance;
    }

    @Override
    public Optional<IpamRecord> putSubnet(IpamSubnet net) throws BackingStoreException {
        IpamRecord response = null;
        if (!repo.containsKey(net)) {
            Integer newId;

            try {
                synchronized (repo) {
                    newId = allocateId();
                    repo.put(net, newId);
                }
                response = new IpamRecord(net, newId);
            } catch (Exception e) {
                throw new BackingStoreException("Failed to store new subnet"
                        + net.getSubnetId(), e.getCause());
            }
        }
        return Optional.ofNullable(response);
    }

    // Retrieve IpamRecord by IpamSubnet key
    @Override
    public Optional<IpamRecord> querySubnet(IpamSubnet net) {
        IpamRecord response = null;
        synchronized (repo) {
            if (repo.containsKey(net))
                response = new IpamRecord(net, repo.get(net));
        }
        return Optional.ofNullable(response);
    }

    @Override
    public Optional<IpamRecord> deleteSubnet(IpamSubnet net) throws BackingStoreException {
        IpamRecord response = null;
        Integer oldKey;
        try {
            synchronized (repo) {
                if (repo.containsKey(net)) {
                    oldKey = repo.remove(net);
                    response = new IpamRecord(net, oldKey);
                }
            }
        } catch (Exception e) {
            throw new BackingStoreException("Failed to remove subnet: " + net.getSubnetId(), e);
        }

        return Optional.ofNullable(response);
    }

    @Override
    public Optional<IpamRecord> querySubnetById(Integer id) throws BackingStoreException {
        IpamRecord response = null;

        try {
            synchronized (repo) {
                if (revrepo.containsKey(id)) {
                    response = new IpamRecord(revrepo.get(id), id);
                }
            }
        } catch (Exception e) {
            throw new BackingStoreException("Failed to query repo for id: "
                    + String.valueOf(id) + ", "
                    + e.getMessage(), e);
        }

        return Optional.ofNullable(response);
    }

    @Override
    public List<IpamRecord> queryAllSubnets() {
        List<IpamRecord> response = new ArrayList<>();
        synchronized (repo) {
            Set<IpamSubnet> keySet = repo.keySet();
            for (IpamSubnet key: keySet) {
                response.add(new IpamRecord(key, repo.get(key)));
            }
        }
        return response;
    }

    @Override
    public List<IpamRecord> queryAllSubnets(filterSpec spec) {
        List<IpamRecord> response = new ArrayList<>();
        synchronized (repo) {
            Set<IpamSubnet> keySet = repo.keySet();
            for (IpamSubnet key: keySet) {
                response.add(new IpamRecord(key, repo.get(key)));
            }
        }
        return response;
    }


    // Issue a monotonically incrementing unique ID for use in new record.
    private static Integer allocateId() {
        Integer allocated;
        synchronized (idCount) {
            allocated = idCount++;
        }
        return allocated;
    }

}
