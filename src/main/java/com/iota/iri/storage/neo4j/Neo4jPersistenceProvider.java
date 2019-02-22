package com.iota.iri.storage.neo4j;

import com.iota.iri.model.Hash;
import com.iota.iri.model.HashFactory;
import com.iota.iri.model.StateDiff;
import com.iota.iri.model.TransactionHash;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.model.persistables.*;
import com.iota.iri.storage.Indexable;
import com.iota.iri.storage.Persistable;
import com.iota.iri.storage.PersistenceProvider;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.IotaIOUtils;
import com.iota.iri.utils.Pair;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.SecureRandom;
import java.util.*;


enum Neo4jLabel implements Label {
    TRANSACTION,
    MILESTONE,
    STATEDIFF,
    ADDRESS,
    APPROVEE,
    BUNDLE,
    OBSOLETETAG,
    TAG,
    METADATA
}

enum MyRelationshipTypes implements RelationshipType {
    IS_FRIEND_OF, HAS_SEEN
}

public class Neo4jPersistenceProvider implements AutoCloseable, PersistenceProvider
{
    private static final Logger log = LoggerFactory.getLogger(Neo4jPersistenceProvider.class);

    private final String dbPath;
    private final Map<String, Class<? extends Persistable>> columnFamilies;
    private final Map.Entry<String, Class<? extends Persistable>> metadataColumnFamily;

    private Map<Class<?>, Neo4jLabel> classTreeMap;
    private Map<Class<?>, Neo4jLabel> metadataReference = Collections.emptyMap();

    private final SecureRandom seed = new SecureRandom();

    GraphDatabaseService graphDb;

    private boolean available;
    private Map<Class<?>, Label> classLabelMap;

    public Neo4jPersistenceProvider(String dbPath, Map<String, Class<? extends Persistable>> columnFamilies,
                                    Map.Entry<String, Class<? extends Persistable>> metadataColumnFamily) {
        this.dbPath = dbPath;
        this.columnFamilies = columnFamilies;
        this.metadataColumnFamily = metadataColumnFamily;
    }

    @Override
    public void init() throws Exception {
        log.info("Initializing Database on " + dbPath);
        initDB(dbPath, columnFamilies);
        available = true;
        log.info("Neo4j persistence provider initialized.");
    }

    @Override
    public boolean isAvailable() {
        return this.available;
    }

    @Override
    public void shutdown() {
        graphDb.shutdown();
    }

    @Override
    public void close() throws Exception {
        shutdown();
    }

    @Override
    public boolean save(Persistable thing, Indexable index) throws Exception
    {
        try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
            Neo4jLabel label = classTreeMap.get(thing.getClass());
            Node node = graphDb.createNode(label);
            node.setProperty("key", Arrays.toString(index.bytes()));
            node.setProperty("value", Arrays.toString(thing.bytes()));

            Neo4jLabel referenceLabel = metadataReference.get(thing.getClass());
            if (referenceLabel != null) {
                Node refNode = graphDb.createNode(referenceLabel);
                refNode.setProperty("key", Arrays.toString(index.bytes()));
                refNode.setProperty("value", Arrays.toString(thing.metadata()));
            }

            tx.success();
        } catch (Exception e) {
            log.error("Error saving node {} - {}.", index, thing);
            throw e;
        }

        return true;
    }

    @Override
    public void delete(Class<?> model, Indexable index) throws Exception
    {
        try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
            Neo4jLabel label = classTreeMap.get(model);

            Node node = graphDb.findNode(label, "key", Arrays.toString(index.bytes()));

            for (Relationship r : node.getRelationships()) {
                r.delete();
            }
            node.delete();

            tx.success();
        } catch (Exception e) {
            log.error("Error deleting node {}.", index);
            throw e;
        }
    }

    @Override
    public boolean update(Persistable thing, Indexable index, String item) throws Exception
    {
        Neo4jLabel label = metadataReference.get(thing.getClass());
        if (label != null) {
            try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
                Node node = graphDb.findNode(label, "key", Arrays.toString(index.bytes()));

                node.setProperty(Arrays.toString(index.bytes()), Arrays.toString(thing.metadata()));

                tx.success();
            } catch (Exception e) {
                log.error("Error deleting node {}.", index);
                throw e;
            }

        }
        return false;
    }

    @Override
    public boolean exists(Class<?> model, Indexable key) throws Exception
    {
        Neo4jLabel label = classTreeMap.get(model);
        return label != null && graphDb.findNode(label, "key", Arrays.toString(key.bytes())) != null;
    }

    public Set<Indexable> keysWithMissingReferences(Class<?> modelClass, Class<?> otherClass) throws Exception
    {
        Neo4jLabel label = classTreeMap.get(modelClass);
        Neo4jLabel otherLabel = classTreeMap.get(otherClass);

        Set<Indexable> indexables = null;

        try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
            ResourceIterator<Node> nodes = graphDb.findNodes(label);

            Node node = nodes.next();
            for (; node != null; node = nodes.next()) {
                if (graphDb.findNode(otherLabel, "key", node.getProperty("key")) == null) {
                    indexables = indexables == null ? new HashSet<>() : indexables;
                    indexables.add(HashFactory.GENERIC.create(modelClass, node.getProperty("key").toString().getBytes()));
                }
            }

            tx.success();
        } catch (Exception e) {
            log.error("Error finding node." + e);
            throw e;
        }

        return indexables == null ? Collections.emptySet() : Collections.unmodifiableSet(indexables);
    }

    public Persistable get(Class<?> model, Indexable index) throws Exception
    {
        Persistable object = (Persistable) model.newInstance();

        try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
            Node node = graphDb.findNode(classTreeMap.get(model), "key", index == null ? new byte[0] : index.bytes());
            object.read(node.getProperty("value").toString().getBytes());

            Neo4jLabel referenceHandle = metadataReference.get(model);
            if (referenceHandle != null) {
                Node refNode = graphDb.findNode(referenceHandle, "key", index == null ? new byte[0] : index.bytes());
                object.readMetadata(refNode.getProperty("value").toString().getBytes());
            }

            tx.success();
        } catch (Exception e) {
            log.error("Error finding node." + e);
            throw e;
        }

        return object;
    }

    public boolean mayExist(Class<?> model, Indexable index) throws Exception
    {
        return exists(model, index);
    }

    // TODO: 从count 这个接口到 saveBatch，除了 iteration 没有找到更好的方法。这个需要再查找一下。
    public long count(Class<?> model) throws Exception
    {
        Neo4jLabel label = classTreeMap.get(model);
        long count = 0;
        if (label != null) {
            ResourceIterator<Node> nodes = graphDb.findNodes(label);
            while (nodes.hasNext()) {
                Node node = nodes.next();
                count++;
            }
        }
        return count;
    }

    // TODO: remove iteration
    public Set<Indexable> keysStartingWith(Class<?> modelClass, byte[] value)
    {
        Objects.requireNonNull(value, "value byte[] cannot be null");
        Neo4jLabel label = classTreeMap.get(modelClass);
        Set<Indexable> keys = null;
        if (label != null) {
            try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
                ResourceIterator<Node> nodes = graphDb.findNodes(label);

                Node node = nodes.next();
                for (; node != null; node = nodes.next()) {
                    byte[] found = node.getProperty("key").toString().getBytes();
                    if (keyStartsWithValue(value, found)) {
                        keys = keys == null ? new HashSet<>() : keys;
                        keys.add(HashFactory.GENERIC.create(modelClass, found));
                    }
                }

                tx.success();
            } catch (Exception e) {
                log.error("Error finding node." + e);
                throw e;
            }
        }
        return keys == null ? Collections.emptySet() : Collections.unmodifiableSet(keys);
    }

    private static boolean keyStartsWithValue(byte[] value, byte[] key) {
        if (key == null || key.length < value.length) {
            return false;
        }
        for (int n = 0; n < value.length; n++) {
            if (value[n] != key[n]) {
                return false;
            }
        }
        return true;
    }

    // TODO: remove iteration
    public Persistable seek(Class<?> model, byte[] key) throws Exception
    {
        Set<Indexable> hashes = keysStartingWith(model, key);
        if (hashes.isEmpty()) {
            return get(model, null);
        }
        if (hashes.size() == 1) {
            return get(model, (Indexable) hashes.toArray()[0]);
        }
        return get(model, (Indexable) hashes.toArray()[seed.nextInt(hashes.size())]);
    }

    // TODO: remove iteration
    public Pair<Indexable, Persistable> next(Class<?> model, Indexable index) throws Exception
    {
        Neo4jLabel label = classTreeMap.get(model);
        try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
            Node node = graphDb.findNode(label, "key", index);
            if (node != null) {
                long id = node.getId();
                Node nextNode = graphDb.getNodeById(id + 1);
                return new Pair<>(nextNode.getProperty("key").toString(), nextNode.getProperty("value").toString());
            }
        } catch (Exception e) {
            log.error("Error finding node." + e);
        }
        return new Pair<>(null, null);
    }

    // TODO: remove iteration
    public Pair<Indexable, Persistable> previous(Class<?> model, Indexable index) throws Exception
    {
        Neo4jLabel label = classTreeMap.get(model);
        try (org.neo4j.graphdb.Transaction tx = graphDb.beginTx()) {
            Node node = graphDb.findNode(label, "key", index);
            if (node != null) {
                long id = node.getId();
                Node nextNode = graphDb.getNodeById(id - 1);
                return new Pair<>(nextNode.getProperty("key").toString(), nextNode.getProperty("value").toString());
            }
        } catch (Exception e) {
            log.error("Error finding node." + e);
        }
        return new Pair<>(null, null);
    }

    // TODO: remove iteration
    public Pair<Indexable, Persistable> latest(Class<?> model, Class<?> indexModel) throws Exception
    {
        // TODO implement this
        return new Pair<Indexable,Persistable>(new TransactionHash(), new Transaction());
    }

    // TODO: remove iteration
    public Pair<Indexable, Persistable> first(Class<?> model, Class<?> indexModel) throws Exception
    {
        // TODO implement this
        return new Pair<Indexable,Persistable>(new TransactionHash(), new Transaction());
    }

    public boolean saveBatch(List<Pair<Indexable, Persistable>> models) throws Exception
    {
        try ( org.neo4j.graphdb.Transaction tx = graphDb.beginTx() )
        {
            // find hash node first
            Node newNode = graphDb.createNode();
            for (Pair<Indexable, Persistable> entry : models) {
                if(entry.hi.getClass().equals(com.iota.iri.model.persistables.Transaction.class)) {
                    Indexable key = entry.low;
                    Persistable value = entry.hi;
                    Label label = classLabelMap.get(value.getClass());
                    newNode.addLabel(label);
                    String keyStr = Converter.trytes(((Hash)key).trits());
                    newNode.setProperty("key", keyStr);
                    break;
                }
            }
            // create the rest
            for (Pair<Indexable, Persistable> entry : models) {
                if(entry.hi.getClass().equals(Approvee.class)) {
                    Indexable key = entry.low;
                    String keyStr = Converter.trytes(((Hash)key).trits());
                    try {
                        Node prev = graphDb.findNode(DynamicLabel.label("Transaction"), "key", keyStr);
                        newNode.createRelationshipTo(prev, DynamicRelationshipType.withName( "APPROVES"));
                    } catch(MultipleFoundException e) {
                        ; // TODO should triage problem here
                    } catch(IllegalArgumentException e) {
                        ; // TODO How to handle null relationship?
                    }
                }
            }
            tx.success();
        }

        return true;
    }

    /**
     * Atomically delete all {@code models}.
     * @param models key value pairs that to be expunged from the db
     * @throws Exception
     */
    public void deleteBatch(Collection<Pair<Indexable, ? extends Class<? extends Persistable>>> models) throws Exception
    {
        // TODO implement this
    }

    public void clear(Class<?> column) throws Exception
    {
        // TODO implement this
    }

    public void clearMetadata(Class<?> column) throws Exception
    {
        // TODO implement this
    }

    private void initDB(String path, Map<String, Class<? extends Persistable>> columnFamilies) {
        try {
            // metadata descriptor is always last
            if (metadataColumnFamily != null) {

            }

            graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dbPath));

            initClassTreeMap();

        } catch (Exception e) {
            log.error("Error while initializing Neo4j", e);
            graphDb.shutdown();
        }
    }

    private void initClassTreeMap() throws Exception {
        //classTreeMap = MapUtils.unmodifiableMap(classMap);
        Map<Class<?>, Label> classMap = new LinkedHashMap<>();
        classMap.put(com.iota.iri.model.persistables.Transaction.class, DynamicLabel.label("Transaction"));
        classMap.put(Milestone.class,   DynamicLabel.label("Milestone"));
        classMap.put(StateDiff.class,   DynamicLabel.label("StateDiff"));
        classMap.put(Address.class,     DynamicLabel.label("Address"));
        classMap.put(Approvee.class,    DynamicLabel.label("Approvee"));
        classMap.put(Bundle.class,      DynamicLabel.label("Bundle"));
        classMap.put(ObsoleteTag.class, DynamicLabel.label("ObsoleteTag"));
        classMap.put(Tag.class,         DynamicLabel.label("Tag"));
        classLabelMap = classMap;
    }

    public long getTotalTxns() throws Exception
    {
        long ret = 0;
        Label label = classLabelMap.get(com.iota.iri.model.persistables.Transaction.class);
        try ( org.neo4j.graphdb.Transaction tx = graphDb.beginTx() )
        {
            ResourceIterator<Node> iter = graphDb.findNodes(label);
            while(iter.hasNext()) {
                iter.next();
                ret++;
            }
            tx.success();
        }

        return ret;
    }

    @Override
    public List<Hash> getSiblings(Hash block) {
        return Collections.emptyList();
    }

    @Override
    public void buildGraph() {
        //TODO
    }

    @Override
    public void computeScore() {
        //TODO
    }

    @Override
    public Hash getPivotalHash(int depth) {
        //TODO
        return null;
    }

    @Override
    public List<Hash> getChain(HashMap<Integer, Set<Hash>> topOrder) {
        //TODO
        return null;
    }

    @Override
    public Set<Hash> getChild(Hash block) {
        //TODO
        return null;
    }

    public int getNumOfTips() {
        // TODO
        return -1;
    }
}
