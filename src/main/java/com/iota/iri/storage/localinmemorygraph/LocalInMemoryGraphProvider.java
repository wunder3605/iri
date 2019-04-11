package com.iota.iri.storage.localinmemorygraph;

import com.iota.iri.conf.BaseIotaConfig;
import com.iota.iri.controllers.TransactionViewModel;
import com.iota.iri.model.Hash;
import com.iota.iri.model.TransactionHash;
import com.iota.iri.model.persistables.Transaction;
import com.iota.iri.service.tipselection.impl.CumWeightScore;
import com.iota.iri.service.tipselection.impl.KatzCentrality;
import com.iota.iri.storage.Indexable;
import com.iota.iri.storage.Persistable;
import com.iota.iri.storage.PersistenceProvider;
import com.iota.iri.storage.Tangle;
import com.iota.iri.utils.IotaUtils;
import com.iota.iri.utils.Pair;
import org.apache.commons.collections4.CollectionUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class LocalInMemoryGraphProvider implements AutoCloseable, PersistenceProvider {
    private HashMap<Hash, Double> score;
    private HashMap<Hash, Double> parentScore;
    private Map<Hash, Set<Hash>> graph;
    private Map<Hash, Hash> parentGraph;
    private Map<Hash, Set<Hash>> revGraph;
    private Map<Hash, Set<Hash>> parentRevGraph;
    private Map<Hash, Integer> degs;
    private HashMap<Integer, Set<Hash>> topOrder;
    private HashMap<Integer, Set<Hash>> topOrderStreaming;

    private Map<Hash, Integer> lvlMap;
    private HashMap<Hash, String> nameMap;
    private int totalDepth;
    private Tangle tangle;
    // to use
    private List<Hash> pivotChain;

    private Stack<Hash> ancestors;

    private Integer ancestorGeneration = 10;//72000;

    private boolean available;
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

    public LocalInMemoryGraphProvider(String dbDir, Tangle tangle) {
        this.tangle = tangle;
        initVariables();
    }

    //FIXME for debug
    public void setNameMap(HashMap<Hash, String> nameMap) {
        this.nameMap = nameMap;
    }

    @Override
    public void close() throws Exception {
        initVariables();
    }

    private void initVariables() {
        graph = new HashMap<>();
        revGraph = new HashMap<>();
        parentGraph = new ConcurrentHashMap<>();
        parentRevGraph = new HashMap<>();
        degs = new HashMap<>();
        topOrder = new HashMap<>();
        lvlMap = new HashMap<>();
        topOrderStreaming = new HashMap<>();
        score = new HashMap<>();
        parentScore = new HashMap<>();
        totalDepth = 0;
//        ancestors = new Stack<>();
    }

    public void init() throws Exception {
        try {
            buildGraph();
            service.scheduleAtFixedRate(new AncestorEngine(), 10, 30, TimeUnit.SECONDS);
        } catch (NullPointerException e) {
            ; // initialization failed because tangle has nothing
        }
    }

    public boolean isAvailable() {
        return this.available;
    }

    public void shutdown() {
        try {
            close();
            if (!service.isShutdown()) {
                service.shutdown();
            }
        } catch (Exception e) {
            ;
        }
    }

    public boolean save(Persistable model, Indexable index) throws Exception {
        return true;
    }

    public void delete(Class<?> model, Indexable index) throws Exception {
        // TODO implement this
    }

    public boolean update(Persistable model, Indexable index, String item) throws Exception {
        // TODO this function is not implemented or referenced
        return true;
    }

    public boolean exists(Class<?> model, Indexable key) throws Exception {
        // TODO implement this
        return false;
    }

    public Pair<Indexable, Persistable> latest(Class<?> model, Class<?> indexModel) throws Exception {
        // TODO implement this
        return new Pair<Indexable, Persistable>(new TransactionHash(), new Transaction());
    }

    public Set<Indexable> keysWithMissingReferences(Class<?> modelClass, Class<?> otherClass) throws Exception {
        // TODO implement this
        return new HashSet<Indexable>();
    }

    public Persistable get(Class<?> model, Indexable index) throws Exception {
        // TODO implement this
        return new Transaction();
    }

    public boolean mayExist(Class<?> model, Indexable index) throws Exception {
        // TODO implement this
        return false;
    }

    public long count(Class<?> model) throws Exception {
        // TODO implement this
        return (long) 0;
    }

    public Set<Indexable> keysStartingWith(Class<?> modelClass, byte[] value) {
        // TODO implement this
        return new HashSet<Indexable>();
    }

    public Persistable seek(Class<?> model, byte[] key) throws Exception {
        // TODO implement this
        return new Transaction();
    }

    public Pair<Indexable, Persistable> next(Class<?> model, Indexable index) throws Exception {
        // TODO implement this
        return new Pair<Indexable, Persistable>(new TransactionHash(), new Transaction());
    }

    public Pair<Indexable, Persistable> previous(Class<?> model, Indexable index) throws Exception {
        // TODO implement this
        return new Pair<Indexable, Persistable>(new TransactionHash(), new Transaction());
    }

    public Pair<Indexable, Persistable> first(Class<?> model, Class<?> indexModel) throws Exception {
        // TODO implement this
        return new Pair<Indexable, Persistable>(new TransactionHash(), new Transaction());
    }

    public boolean saveBatch(List<Pair<Indexable, Persistable>> models) throws Exception {
        for (Pair<Indexable, Persistable> entry : models) {
            if (entry.hi.getClass().equals(com.iota.iri.model.persistables.Transaction.class)) {

                Hash key = (Hash) entry.low;
                Transaction value = (Transaction) entry.hi;
                TransactionViewModel model = new TransactionViewModel(value, key);
                Hash trunk = model.getTrunkTransactionHash();
                Hash branch = model.getBranchTransactionHash();

                // Approve graph
                if (graph.get(key) == null) {
                    graph.put(key, new HashSet<>());
                }
                graph.get(key).add(trunk);
                graph.get(key).add(branch);

                //parentGraph
                parentGraph.put(key, trunk);

                // Approvee graph
                if (revGraph.get(trunk) == null) {
                    revGraph.put(trunk, new HashSet<>());
                }
                revGraph.get(trunk).add(key);
                if (revGraph.get(branch) == null) {
                    revGraph.put(branch, new HashSet<>());
                }
                revGraph.get(branch).add(key);

                if (parentRevGraph.get(trunk) == null) {
                    parentRevGraph.put(trunk, new HashSet<>());
                }
                parentRevGraph.get(trunk).add(key);

                // update degrees
                if (degs.get(model.getHash()) == null || degs.get(model.getHash()) == 0) {
                    degs.put(model.getHash(), 2);
                }
                if (degs.get(trunk) == null) {
                    degs.put(trunk, 0);
                }
                if (degs.get(branch) == null) {
                    degs.put(branch, 0);
                }
                updateTopologicalOrder(key, trunk, branch);
                updateScore(key);
                break;
            }
        }
        return true;
    }

    /**
     * 从ancestor开始build，所有的graph全部清空重来
     */
    // TODO for public  :: Get the graph using the BFS method
    public void buildGraph() {
        try {
            Pair<Indexable, Persistable> one = tangle.getFirst(Transaction.class, TransactionHash.class);
            Stack<Hash> ancestors = tangle.getAncestors();
            Hash ancestor = null;
            if (null != ancestors) {
                ancestor = ancestors.peek();
            }
            Boolean start = false;
            while (one != null && one.low != null) {
                TransactionViewModel model = new TransactionViewModel((Transaction) one.hi, (TransactionHash) one.low);
                Hash trunk = model.getTrunkTransactionHash();
                Hash branch = model.getBranchTransactionHash();

                //from last ancestor
                if (ancestor != null ) {
                    if (ancestor.equals(model.getHash()) && !start){
                        start = true;
                    }
                    if (!start) {
                        one = tangle.next(Transaction.class, one.low);
                        continue;
                    }
                }

                // approve direction
                if (graph.get(model.getHash()) == null) {
                    graph.put(model.getHash(), new HashSet<Hash>());
                }
                graph.get(model.getHash()).add(trunk);
                graph.get(model.getHash()).add(branch);

                // approved direction
                if (revGraph.get(trunk) == null) {
                    revGraph.put(trunk, new HashSet<Hash>());
                }
                if (revGraph.get(branch) == null) {
                    revGraph.put(branch, new HashSet<Hash>());
                }
                revGraph.get(trunk).add(model.getHash());
                revGraph.get(branch).add(model.getHash());

                //parentGraph
                parentGraph.put(model.getHash(), trunk);

                if (parentRevGraph.get(trunk) == null) {
                    parentRevGraph.put(trunk, new HashSet<>());
                }
                parentRevGraph.get(trunk).add(model.getHash());

                // update degrees
                if (degs.get(model.getHash()) == null || degs.get(model.getHash()) == 0) {
                    degs.put(model.getHash(), 2);
                }
                if (degs.get(trunk) == null) {
                    degs.put(trunk, 0);
                }
                if (degs.get(branch) == null) {
                    degs.put(branch, 0);
                }

                updateScore(model.getHash());
                one = tangle.next(Transaction.class, one.low);
            }
            computeToplogicalOrder();
            buildPivotChain();
        } catch (NullPointerException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(System.out));
        }
    }

    // base on graph
    public void buildPivotChain() {
        try {
            this.pivotChain = pivotChain(getGenesis());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTopologicalOrder(Hash vet, Hash trunk, Hash branch) {
        if (topOrderStreaming.isEmpty()) {
            topOrderStreaming.put(1, new HashSet<>());
            topOrderStreaming.get(1).add(vet);
            lvlMap.put(vet, 1);
            topOrderStreaming.put(0, new HashSet<>());
            topOrderStreaming.get(0).add(trunk);
            topOrderStreaming.get(0).add(branch);
            totalDepth = 1;
            return;
        } else {
            try {
                int trunkLevel = lvlMap.get(trunk);
                int branchLevel = lvlMap.get(branch);
                int lvl = Math.min(trunkLevel, branchLevel) + 1;
                if (topOrderStreaming.get(lvl) == null) {
                    topOrderStreaming.put(lvl, new HashSet<>());
                    totalDepth++;
                }
                topOrderStreaming.get(lvl).add(vet);
                lvlMap.put(vet, lvl);
            } catch (NullPointerException e) {
                ; // First block, do nothing here
            }
        }
    }

    private void updateScore(Hash vet) {
        try {
            if (BaseIotaConfig.getInstance().getStreamingGraphSupport()) {
                if (BaseIotaConfig.getInstance().getConfluxScoreAlgo().equals("CUM_WEIGHT")) {
                    score = CumWeightScore.update(graph, score, vet);
                    parentScore = CumWeightScore.updateParentScore(parentGraph, parentScore, vet);
                } else if (BaseIotaConfig.getInstance().getConfluxScoreAlgo().equals("KATZ")) {
                    score.put(vet, 1.0 / (score.size() + 1));
                    KatzCentrality centrality = new KatzCentrality(graph, revGraph, 0.5);
                    centrality.setScore(score);
                    score = centrality.compute();
                    parentScore = CumWeightScore.updateParentScore(parentGraph, parentScore, vet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(System.out));
        }
    }

    private void computeToplogicalOrder() {
        Deque<Hash> bfsQ = new ArrayDeque<>();
        Map<Hash, Integer> level = new HashMap<Hash, Integer>();
        Set<Hash> visited = new HashSet<Hash>();

        for (Hash h : degs.keySet()) {
            if (!degs.containsKey(h) || degs.get(h) == 0) {
                bfsQ.addLast(h);
                level.put(h, 0);
                break;
            }
        }

        while (!bfsQ.isEmpty()) {
            Hash h = bfsQ.pollFirst();
            int lvl = level.get(h);
            totalDepth = Math.max(totalDepth, lvl + 1);
            if (!topOrder.containsKey(lvl)) {
                topOrder.put(lvl, new HashSet<Hash>());
            }
            topOrder.get(lvl).add(h);

            Set<Hash> out = revGraph.get(h);
            if (out != null) {
                for (Hash o : out) {
                    if (!visited.contains(o)) {
                        bfsQ.addLast(o);
                        visited.add(o);
                        level.put(o, lvl + 1);
                    }
                }
            }
        }
        topOrderStreaming = topOrder;
    }

    public void computeScore() {
        try {
            if (BaseIotaConfig.getInstance().getStreamingGraphSupport()) {
                if (BaseIotaConfig.getInstance().getConfluxScoreAlgo().equals("CUM_WEIGHT")) {
                    score = CumWeightScore.compute(revGraph, graph, getGenesis());
                    // FIXME add parent score here
                } else if (BaseIotaConfig.getInstance().getConfluxScoreAlgo().equals("KATZ")) {
                    KatzCentrality centrality = new KatzCentrality(graph, revGraph, 0.5);
                    score = centrality.compute();
                    // FIXME add parent score here
                }
            }
        } catch (Exception e) {
            e.printStackTrace(new PrintStream(System.out));
        }
    }

    public Hash getPivotalHash(int depth) {
        Hash ret = null;
        buildPivotChain();
        if (depth == -1 || depth >= this.pivotChain.size()) {
            Set<Hash> set = topOrderStreaming.get(1);
            if (CollectionUtils.isEmpty(set)) {
                return null;
            }
            ret = set.iterator().next();
            return ret;
        }

        // TODO if the same score, choose randomly
        ret = this.pivotChain.get(this.pivotChain.size() - depth - 1);
        return ret;
    }

    //FIXME for debug :: for graphviz visualization
    public void printGraph(Map<Hash, Set<Hash>> graph, Hash k) {
        try {
            BufferedWriter writer = null;
            if (k != null) {
                writer = new BufferedWriter(new FileWriter(IotaUtils.abbrieviateHash(k, 4)));
            }
            for (Hash key : graph.keySet()) {
                for (Hash val : graph.get(key)) {
                    if (nameMap != null) {
                        if (k != null) {
                            writer.write("\"" + nameMap.get(key) + "\"->" +
                                    "\"" + nameMap.get(val) + "\"\n");
                        } else {
                            System.out.println("\"" + nameMap.get(key) + "\"->" +
                                    "\"" + nameMap.get(val) + "\"");
                        }
                    } else {
                        if (k != null) {
                            writer.write("\"" + IotaUtils.abbrieviateHash(key, 6) + "\"->" +
                                    "\"" + IotaUtils.abbrieviateHash(val, 6) + "\"\n");
                        } else {
                            System.out.println("\"" + IotaUtils.abbrieviateHash(key, 6) + "\"->" +
                                    "\"" + IotaUtils.abbrieviateHash(val, 6) + "\"");
                        }
                    }
                }
            }
            if (k != null) {
                writer.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //FIXME for debug :: for graphviz visualization
    void printRevGraph(Map<Hash, Set<Hash>> revGraph) {
        for (Hash key : revGraph.keySet()) {
            for (Hash val : revGraph.get(key)) {
                if (nameMap != null) {
                    System.out.println("\"" + nameMap.get(key) + "\"->" +
                            "\"" + nameMap.get(val) + "\"");
                } else {
                    System.out.println("\"" + IotaUtils.abbrieviateHash(key, 4) + "\"->" +
                            "\"" + IotaUtils.abbrieviateHash(val, 4) + "\"");
                }
            }
        }
    }

    //FIXME for debug :: for graphviz visualization
    void printTopOrder(HashMap<Integer, Set<Hash>> topOrder) {
        for (Integer key : topOrder.keySet()) {
            System.out.print(key + ": ");
            for (Hash val : topOrder.get(key)) {
                if (nameMap != null) {
                    System.out.print(nameMap.get(val) + " ");
                } else {
                    System.out.println(IotaUtils.abbrieviateHash(val, 4) + " ");
                }
            }
            System.out.println();
        }
    }

    public List<Hash> getSiblings(Hash block) {
        try {
            Persistable persistable = this.tangle.find(Transaction.class, block.bytes());
            if (persistable != null && persistable instanceof Transaction) {
                Set<Hash> children = new LinkedHashSet<>();
                children.addAll(revGraph.get(((Transaction) persistable).trunk));
                children.removeIf(t -> t.equals(block));
                return new LinkedList<>(children);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Hash> ret = new LinkedList<Hash>();
        return ret;
    }

    public List<Hash> getChain(HashMap<Integer, Set<Hash>> topOrder) {
        List<Hash> ret = new LinkedList<Hash>();
        Hash b = (Hash) topOrder.get(1).toArray()[0];
        ret.add(b);
        while (true) {
            Set<Hash> children = getChild(b);
            if (children.isEmpty()) {
                break;
            }
            double maxScore = 0;
            for (Hash h : children) {
                if (parentScore.get(h) > maxScore) {
                    maxScore = parentScore.get(h);
                    b = h;
                }
            }
            ret.add(b);
        }
        return ret;
    }

    public Set<Hash> getChild(Hash block) {
        if (revGraph.containsKey(block)) {
            return revGraph.get(block);
        }
        return new HashSet<>();
    }

    //TODO for debug
//    public static void printScore() {
//        for(Hash key : score.keySet()) {
//            if(nameMap != null) {
//                System.out.print(nameMap.get(key)+":"+score.get(key));
//            } else {
//                System.out.print(key+":"+score.get(key));
//            }
//            System.out.println();
//        }
//    }

    public void deleteBatch(Collection<Pair<Indexable, ? extends Class<? extends Persistable>>> models) throws Exception {
        // TODO implement this
    }

    public void clear(Class<?> column) throws Exception {
        // TODO implement this
    }

    public void clearMetadata(Class<?> column) throws Exception {
        // TODO implement this
    }

    public void addTxnCount(long count) {
        // TODO implement this
    }

    public long getTotalTxns() {
        return 0;
    }

    public List<Hash> totalTopOrder() {
        return confluxOrder(getPivot(getGenesis()));
    }

    public List<Hash> confluxOrder(Hash block) {
        LinkedList<Hash> list = new LinkedList<>();
        Set<Hash> covered = new HashSet<Hash>();
        if (block == null || !graph.keySet().contains(block)) {
            return list;
        }
        do {
            Hash parent = parentGraph.get(block);
            List<Hash> subTopOrder = new ArrayList<>();
            List<Hash> diff = getDiffSet(block, parent, covered);
            while (diff.size() != 0) {
                Map<Hash, Set<Hash>> subGraph = buildSubGraph(diff);
                List<Hash> noBeforeInTmpGraph = subGraph.entrySet().stream().filter(e -> CollectionUtils.isEmpty(e.getValue())).map(Map.Entry::getKey).collect(Collectors.toList());
                //TODO consider using SPECTR for sorting
                for (Hash s : noBeforeInTmpGraph) {
                    if (!lvlMap.containsKey(s)) {
                        lvlMap.put(s, Integer.MAX_VALUE); //FIXME this is a bug
                    }
                }
                noBeforeInTmpGraph.sort(Comparator.comparingInt((Hash o) -> lvlMap.get(o)).thenComparing(o -> o));
                subTopOrder.addAll(noBeforeInTmpGraph);
                diff.removeAll(noBeforeInTmpGraph);
            }
            list.addAll(0, subTopOrder);
            covered.addAll(subTopOrder);
            block = parentGraph.get(block);
        } while (parentGraph.get(block) != null && parentGraph.keySet().contains(block));
        return list;
    }

    public Map<Hash, Set<Hash>> buildSubGraph(List<Hash> blocks) {
        Map<Hash, Set<Hash>> subMap = new HashMap<>();
        for (Hash h : blocks) {
            Set<Hash> s = graph.get(h);
            Set<Hash> ss = new HashSet<>();

            for (Hash hh : s) {
                if (blocks.contains(hh)) {
                    ss.add(hh);
                }
            }
            subMap.put(h, ss);
        }
        return subMap;
    }

    public List<Hash> pivotChain(Hash start) {
        if (start == null || !graph.keySet().contains(start)) {
            return Collections.emptyList();
        }
        ArrayList<Hash> list = new ArrayList<>();
        list.add(start);
        while (!CollectionUtils.isEmpty(parentRevGraph.get(start))) {
            double tmpMaxScore = -1;
            Hash s = null;
            for (Hash block : parentRevGraph.get(start)) {
                //if (score.get(block) > tmpMaxScore || (score.get(block) == tmpMaxScore && block.compareTo(Objects.requireNonNull(s)) < 0)) {
                if (parentScore.containsKey(block) && parentScore.get(block) > tmpMaxScore) {
                    tmpMaxScore = parentScore.get(block);
                    s = block;
                }
            }
            if (s == null) {
                return list;
            }
            start = s;
            list.add(s);
        }
        return list;
    }

    public Hash getPivot(Hash start) {
        if (start == null || !graph.keySet().contains(start)) {
            return null;
        }
        while (!CollectionUtils.isEmpty(parentRevGraph.get(start))) {
            Set<Hash> children = parentRevGraph.get(start);
            double tmpMaxScore = -1;
            Hash s = null;
            for (Hash block : children) {
                //if (score.get(block) > tmpMaxScore || (score.get(block) == tmpMaxScore && block.compareTo(Objects.requireNonNull(s)) < 0)) {
                if (parentScore.containsKey(block) && parentScore.get(block) > tmpMaxScore) {
                    tmpMaxScore = parentScore.get(block);
                    s = block;
                }
            }
            if (s == null) {
                return start;
            }
            start = s;
        }
        return start;
    }

    public Hash getGenesis() {
        try {
            if (ancestors != null && !ancestors.empty()) {
                return ancestors.peek();
            }

            for (Hash key : parentGraph.keySet()) {
                if (!parentGraph.keySet().contains(parentGraph.get(key))) {
                    return key;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Set<Hash> past(Hash hash) {
        if (graph.get(hash) == null) {
            return Collections.emptySet();
        }
        Set<Hash> past = new HashSet<>();
        LinkedList<Hash> queue = new LinkedList<>();
        queue.add(hash);
        Hash h;
        while (!queue.isEmpty()) {
            h = queue.pop();
            for (Hash e : graph.get(h)) {
                if (graph.containsKey(e) && !past.contains(e)) {
                    queue.add(e);
                }
            }
            past.add(h);
        }
        return past;
    }

    public List<Hash> getDiffSet(Hash block, Hash parent, Set<Hash> covered) {
        if (graph.get(block) == null) {
            return Collections.emptyList();
        }

        Set<Hash> ret = new HashSet<Hash>();
        LinkedList<Hash> queue = new LinkedList<>();
        queue.add(block);
        Hash h;
        while (!queue.isEmpty()) {
            h = queue.pop();
            for (Hash e : graph.get(h)) {
                if (graph.containsKey(e) && !ret.contains(e) && !ifCovered(e, parent, covered)) {
                    queue.add(e);
                }
            }
            ret.add(h);
        }
        return new ArrayList<Hash>(ret);
    }

    private boolean ifCovered(Hash block, Hash ancestor, Set<Hash> covered) {
        if (revGraph.get(block) == null) {
            return false;
        }

        if (block.equals(ancestor)) {
            return true;
        }

        Set<Hash> visisted = new HashSet<>();
        LinkedList<Hash> queue = new LinkedList<>();
        queue.add(block);
        visisted.add(block);

        Hash h;
        while (!queue.isEmpty()) {
            h = queue.pop();
            for (Hash e : revGraph.get(h)) {
                if (e.equals(ancestor)) {
                    return true;
                } else {
                    if (revGraph.containsKey(e) && !visisted.contains(e) && !covered.contains(e)) {
                        queue.add(e);
                        visisted.add(e);
                    }
                }
            }
        }
        return false;
    }

    public int getNumOfTips() {
        int ret = 0;
        for (Hash h : graph.keySet()) {
            if (!revGraph.containsKey(h)) {
                ret++;
            }
        }
        return ret;
    }

    @Override
    public Stack<Hash> getAncestors() {
        return ancestors;
    }

    @Override
    public void storeAncestors(Stack<Hash> ancestors) {
        this.ancestors = ancestors;
    }

    public double getScore(Hash hash) {
        return score.get(hash);
    }

    public boolean containsKeyInGraph(Hash hash) {
        return graph.containsKey(hash);
    }

    class AncestorEngine implements Runnable {
        @Override
        public void run() {
            try {
                refreshGraph();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //入参，全图 + 上一个ancestor
        public Hash getAncestor() {
            if (null == parentGraph) {
                return null;
            }
            Iterator<Hash> iterator = parentGraph.keySet().iterator();
            Double maxScore = -1d;
            Hash maxScoreNode = null;
            while (iterator.hasNext()) {
                Hash node = iterator.next();
                if (pivotChain.contains(node)) {
                    continue;
                }
                Double score = parentScore.get(node);
                if (score > maxScore) {
                    maxScore = score;
                    maxScoreNode = node;
                }
            }
            if (maxScoreNode == null) {
                return null;
            }

            Double minScore = 1000000000000000d;
            Hash minScoreNode = null;

            for (Hash mainChainNode : pivotChain) {
                double mainChainNodeScore = parentScore.get(mainChainNode);
                if (mainChainNodeScore < (maxScore + new Double(ancestorGeneration))) {
                    continue;
                }
                if (minScore > mainChainNodeScore) {
                    minScore = mainChainNodeScore;
                    minScoreNode = mainChainNode;
                }
            }
            return minScoreNode;
        }

        void refreshGraph() {
            System.out.println("=========begin to reload ancestor node==========");
            long begin = System.currentTimeMillis();
            Stack<Hash> ancestors = tangle.getAncestors();
            Hash curAncestor = getAncestor();
            if (curAncestor == null) {
                curAncestor = getGenesis();
            }
            printAllGraph("before", curAncestor);
            if (curAncestor == null) {
                System.out.println("=========no ancestor node,cost:" + (System.currentTimeMillis() - begin) + "ms ==========");
                return;
            }
            if (CollectionUtils.isNotEmpty(ancestors) && ancestors.peek().equals(curAncestor)) {
                System.out.println("=========no ancestor node to reload,cost:" + (System.currentTimeMillis() - begin) + "ms ==========");
                return;
            }

            ancestors = reloadAncestor(ancestors, curAncestor);
            tangle.storeAncestors(ancestors);
//
            subGraph(curAncestor);

//            printAllGraph("after", curAncestor);
        }

        private void subGraph(Hash curAncestor) {
            graph = getSubConfirmGraph(graph, curAncestor);
            revGraph = getSubGraph(revGraph, curAncestor);

            parentGraph = getSubConfirmChain(parentGraph, curAncestor);
            parentRevGraph = getSubGraph(parentRevGraph, curAncestor);

            degs = subMap(degs, graph);
            degs = resetAncestor(degs,curAncestor);
            topOrder = new HashMap<>();
            computeToplogicalOrder();

            lvlMap = subMap(lvlMap, graph);

            score = getSubScore(score, revGraph) ;
            parentScore = getSubScore(parentScore, parentRevGraph);

            buildPivotChain();
        }

        /**
         * reset ancestor's parent level to 0
         * @param degs
         * @param curAncestor
         * @return
         */
        private Map<Hash, Integer> resetAncestor(Map<Hash, Integer> degs, Hash curAncestor) {
            totalDepth = 0;
            graph.get(curAncestor).forEach(e -> degs.put(e,0));
            return degs;
        }

        private HashMap<Hash, Double> getSubScore(HashMap<Hash, Double> score, Map<Hash, Set<Hash>> graph) {
            if (graph == null){
                return score;
            }
            HashMap<Hash, Double> subScore = new HashMap<>();
            graph.values().stream().flatMap(Collection::stream).forEach(
                    v -> subScore.put(v, score.get(v))
            );
            return subScore;
        }

        private Map<Hash, Integer> subMap(Map<Hash, Integer> degs, Map<Hash, Set<Hash>> graph) {
            if (null == graph) {
                return degs;
            }
            Map<Hash, Integer> subDegs = new HashMap<>();
            graph.values().stream().flatMap(Collection::stream).forEach(v -> subDegs.put(v, degs.get(v)));
            return subDegs;
        }

        private void printAllGraph(String tag, Hash ancestor) {
            System.out.println("======" + tag + "=======");
            printGraph(graph, null);
            System.out.println("-----------");
//            printRevGraph(revGraph);
//            System.out.println("-----------");
//            if (parentGraph != null) {
//                parentGraph.entrySet().forEach(e -> System.out.println(String.format("\"%s\"->\"%s\"", IotaUtils.abbrieviateHash(e.getKey(), 4), IotaUtils.abbrieviateHash(e.getValue(), 4))));
//            }
//            System.out.println("-----------");
//            printRevGraph(parentRevGraph);
//            System.out.println("-----------");
            CollectionUtils.emptyIfNull(ancestors).stream().forEach(a -> System.out.println(IotaUtils.abbrieviateHash(a, 4)));
//            System.out.println("-----------");
//            System.out.println(ancestor);
            System.out.println("======" + tag + "=======");
        }

        private Stack<Hash> reloadAncestor(Stack<Hash> ancestors, Hash curAncestor) {
            if (ancestors == null) {
                ancestors = new Stack<>();
            }
            ancestors.push(curAncestor);
            return ancestors;
        }

        private Map<Hash, Set<Hash>> getSubGraph(Map<Hash, Set<Hash>> g, Hash b) {
            Map<Hash, Set<Hash>> subGraph = new ConcurrentHashMap<>();
            Stack<Hash> stack = new Stack<>();
            stack.push(b);
            while (!stack.isEmpty()) {
                Hash h = stack.pop();
                Set<Hash> subNode = g.get(h);
                if (null != subNode) {
                    subGraph.put(h, g.get(h));
                    subNode.forEach(e -> stack.push(e));
                }
            }
            //放入触角
            g.entrySet().stream().filter(entry -> entry.getValue().contains(b)).forEach(entry -> {
                subGraph.put(entry.getKey(), new HashSet() {{
                    add(b);
                }});
            });
            return subGraph;
        }

        private Map<Hash, Set<Hash>> getSubConfirmGraph(Map<Hash, Set<Hash>> graph, Hash b) {
            Map<Hash, Set<Hash>> subGraph = new ConcurrentHashMap<>();
            Stack<Hash> stack = new Stack<>();
            stack.push(b);
            while (!stack.isEmpty()) {
                Hash h = stack.pop();
                graph.entrySet().stream().filter(entry -> entry.getValue().contains(h)).forEach(entry -> {
                    if (subGraph.get(entry.getKey()) == null) {
                        subGraph.put(entry.getKey(), new HashSet<>());
                    }
                    subGraph.get(entry.getKey()).add(h);
                    stack.push(entry.getKey());
                });
            }

            //放入触角
            graph.entrySet().stream().filter(entry -> entry.getKey().equals(b)).forEach(entry -> {
                subGraph.put(b, entry.getValue());
            });
            return subGraph;
        }

        private Map<Hash, Hash> getSubConfirmChain(Map<Hash, Hash> graph, Hash curAncestor) {
            //reverse map
            if (graph == null || graph.isEmpty()) {
                return null;
            }
            Map<Hash, Hash> subGraph = new ConcurrentHashMap<>();
            Stack<Hash> stack = new Stack<>();
            stack.push(curAncestor);
            while (!stack.empty()) {
                Hash h = stack.pop();
                graph.entrySet().stream().filter(e -> e.getValue().equals(h)).forEach(e -> {
                    subGraph.put(e.getKey(), h);
                    stack.push(e.getKey());
                });
            }
            //补充触角
            if (graph.get(curAncestor) != null) {
                subGraph.put(curAncestor, graph.get(curAncestor));
            }
            return subGraph;
        }
    }
}

