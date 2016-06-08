package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.DIRECTION;
import network.thunder.core.communication.layer.MessageWrapper;
import network.thunder.core.communication.layer.high.AckableMessage;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.NumberedMessage;
import network.thunder.core.communication.layer.high.RevocationHash;
import network.thunder.core.communication.layer.high.payments.LNOnionHelper;
import network.thunder.core.communication.layer.high.payments.LNOnionHelperImpl;
import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.ChannelUpdate;
import network.thunder.core.communication.layer.high.payments.messages.PeeledOnion;
import network.thunder.core.communication.layer.middle.broadcasting.types.ChannelStatusObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.P2PDataObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyChannelObject;
import network.thunder.core.communication.layer.middle.broadcasting.types.PubkeyIPObject;
import network.thunder.core.database.objects.PaymentStatus;
import network.thunder.core.database.objects.PaymentWrapper;
import network.thunder.core.etc.Constants;
import network.thunder.core.etc.Tools;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static network.thunder.core.communication.layer.DIRECTION.RECEIVED;
import static network.thunder.core.communication.layer.DIRECTION.SENT;
import static network.thunder.core.communication.layer.high.Channel.Phase.OPEN;
import static network.thunder.core.database.objects.PaymentStatus.*;

public class HibernateMemoryDBHandler implements DBHandler {
    public Map<Integer, List<P2PDataObject>> fragmentToListMap = new HashMap<>();

    public final List<P2PDataObject> totalList = Collections.synchronizedList(new ArrayList<>());
    private final Collection<ByteBuffer> knownObjects = new HashSet<>();

    public Map<Sha256Hash, List<RevocationHash>> revocationHashListTheir = new ConcurrentHashMap<>();

    final List<PaymentWrapper> payments = Collections.synchronizedList(new ArrayList<>());

    List<PaymentSecret> secrets = new ArrayList<>();

    Map<NodeKey, List<MessageWrapper>> messageList = new ConcurrentHashMap<>();
    Map<NodeKey, Long> messageCountList = new ConcurrentHashMap<>();
    Map<NodeKey, Map<Long, NumberedMessage>> sentMessages = new ConcurrentHashMap<>();
    Map<NodeKey, Map<Long, Long>> linkedMessageMap = new ConcurrentHashMap<>();
    Map<NodeKey, List<AckableMessage>> unAckedMessageMap = new ConcurrentHashMap<>();

    LNOnionHelper onionHelper = new LNOnionHelperImpl();
    private SessionFactory sessionFactory;

    public HibernateMemoryDBHandler () {
        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure()
                .build();
        try {
            sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
        }
        catch (Exception e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }

        for (int i = 0; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
            fragmentToListMap.put(i, Collections.synchronizedList(new ArrayList<>()));
        }
    }

    @Override
    public void insertIPObjects (List<P2PDataObject> ipList) {
        syncDatalist(ipList);
    }

    @Override
    public List<PubkeyIPObject> getIPObjects () {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<PubkeyIPObject> pubkeyIPObjects = session
                .createQuery("from HibernatePubkeyIPObject", HibernatePubkeyIPObject.class)
                .stream()
                .map(HibernatePubkeyIPObject::toPubkeyIPObject)
                .collect(Collectors.toList());
        tx.commit();
        session.close();
        return pubkeyIPObjects;
    }

    @Override
    public P2PDataObject getP2PDataObjectByHash (byte[] hash) {
        synchronized (totalList) {
            for (P2PDataObject object : totalList) {
                if (Arrays.equals(object.getHash(), hash)) {
                    return object;
                }
            }
        }
        return null;
    }

    @Override
    public PubkeyIPObject getIPObject (byte[] nodeKey) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        PubkeyIPObject pubkeyIPObject = session
                .createQuery("from HibernatePubkeyIPObject where pubkey = :pubkey", HibernatePubkeyIPObject.class)
                .setParameter(":pubkey", nodeKey)
                .uniqueResultOptional()
                .map(HibernatePubkeyIPObject::toPubkeyIPObject)
                .orElse(null);
        tx.commit();
        session.close();
        return pubkeyIPObject;
    }

    @Override
    public void invalidateP2PObject (P2PDataObject ipObject) {
        //TODO with a real database, we rather want to invalidate them, rather then just deleting these..
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        synchronized (totalList) {
            totalList.remove(ipObject);
        }
        if (ipObject instanceof PubkeyIPObject) {
            session
                    .createQuery("delete HibernatePubkeyIPObject where pubkey = :pubkey")
                    .setParameter("pubkey", ((PubkeyIPObject) ipObject).pubkey)
                    .executeUpdate();
        }
        //TODO implement other objects
        tx.commit();
        session.close();
    }

    @Override
    public synchronized void syncDatalist (List<P2PDataObject> dataList) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        Iterator<P2PDataObject> iterator1 = dataList.iterator();

        while (iterator1.hasNext()) {
            boolean deleted = false;
            P2PDataObject object1 = iterator1.next();

            if (knownObjects.contains(ByteBuffer.wrap(object1.getHash()))) {
                iterator1.remove();
                continue;
            }

            synchronized (totalList) {
                Iterator<P2PDataObject> iterator2 = totalList.iterator();
                while (iterator2.hasNext() && !deleted) {
                    P2PDataObject object2 = iterator2.next();
                    if (object1.isSimilarObject(object2)) {

                        if (object1.getTimestamp() <= object2.getTimestamp()) {
                            iterator1.remove();
                        } else {
                            iterator2.remove();
                            for (int i = 0; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
                                fragmentToListMap.get(i).remove(object2);
                            }
                            if (object2 instanceof PubkeyIPObject) {
                                session.createQuery("delete HibernatePubkeyIPObject where pubkey = :pubkey")
                                        .setParameter("pubkey", ((PubkeyIPObject) object2).pubkey)
                                        .executeUpdate();
                            }
                            if (object2 instanceof PubkeyChannelObject) {
                                session.createQuery("delete HibernatePubKeyChannel where txidAnchor = :txid")
                                        .setParameter("txid", ((PubkeyChannelObject) object2).txidAnchor)
                                        .executeUpdate();
                            }
                            if (object2 instanceof ChannelStatusObject) {
                                session.createQuery("delete HibernateChannelStatusObject where pubkeyA = :pubkeyA and pubkeyB = :pubkeyB")
                                        .setParameter("pubkeyA", ((ChannelStatusObject) object2).pubkeyA)
                                        .setParameter("pubkeyB", ((ChannelStatusObject) object2).pubkeyB)
                                        .executeUpdate();
                            }
                        }
                        deleted = true;
                    }
                }
            }
        }

        synchronized (totalList) {
            Iterator<P2PDataObject> iterator2 = totalList.iterator();
            while (iterator2.hasNext()) {
                P2PDataObject object2 = iterator2.next();
                int timestamp = object2.getTimestamp();
                if (timestamp != 0 && Tools.currentTime() - timestamp > P2PDataObject.MAXIMUM_AGE_SYNC_DATA) {
                    iterator2.remove();
                    for (int i = 0; i < P2PDataObject.NUMBER_OF_FRAGMENTS + 1; i++) {
                        fragmentToListMap.get(i).remove(object2);
                    }
                    if (object2 instanceof PubkeyIPObject) {
                        session.createQuery("delete HibernatePubkeyIPObject where pubkey = :pubkey")
                                .setParameter("pubkey", ((PubkeyIPObject) object2).pubkey)
                                .executeUpdate();
                    }
                    if (object2 instanceof PubkeyChannelObject) {
                        session.createQuery("delete HibernatePubkeyChannelObject where txidAnchor = :txid")
                                .setParameter("txid", ((PubkeyChannelObject) object2).txidAnchor)
                                .executeUpdate();
                    }
                    if (object2 instanceof ChannelStatusObject) {
                        session.createQuery("delete HibernateChannelStatusObject where pubkeyA = :pubkeyA and pubkeyB = :pubkeyB")
                                .setParameter("pubkeyA", ((ChannelStatusObject) object2).pubkeyA)
                                .setParameter("pubkeyB", ((ChannelStatusObject) object2).pubkeyB)
                                .executeUpdate();
                    }
                }
            }
        }

        for (P2PDataObject obj : dataList) {
            fragmentToListMap.get(obj.getFragmentIndex()).add(obj);
            if (obj instanceof PubkeyIPObject) {
                Boolean found = session
                        .createQuery("from HibernatePubkeyIPObject where pubkey = :pubkey", HibernatePubkeyIPObject.class)
                        .setParameter("pubkey", ((PubkeyIPObject) obj).pubkey)
                        .uniqueResultOptional()
                        .isPresent();
                if (!found) {
                    session.save(new HibernatePubkeyIPObject((PubkeyIPObject) obj));
                }
            } else if (obj instanceof PubkeyChannelObject) {
                Boolean found = session
                        .createQuery("from HibernatePubkeyChannelObject where txidAnchor = :txid", HibernatePubkeyChannelObject.class)
                        .setParameter("txid", ((PubkeyChannelObject) obj).txidAnchor)
                        .uniqueResultOptional()
                        .isPresent();
                if (!found) {
                    session.save(new HibernatePubkeyChannelObject((PubkeyChannelObject) obj));
                }
            } else if (obj instanceof ChannelStatusObject) {
                ChannelStatusObject temp = (ChannelStatusObject) obj;
                Boolean found = session
                        .createQuery(
                                "from HibernateChannelStatusObject where (pubkeyA = :pubkeyA and pubkeyB = :pubkeyB) " +
                                        "or (pubkeyA = :pubkeyB and pubkeyB = :pubkeyA)",
                                HibernateChannelStatusObject.class)
                        .setParameter("pubkeyA", temp.pubkeyA)
                        .setParameter("pubkeyB", temp.pubkeyB)
                        .uniqueResultOptional()
                        .isPresent();
                if (found) {
                    continue;
                } else {
                    session.save(new HibernateChannelStatusObject(temp));
                }
            }
            List<P2PDataObject> list = getSyncDataByFragmentIndex(obj.getFragmentIndex());
            if (!list.contains(obj)) {
                list.add(obj);
            }
            synchronized (totalList) {
                if (!totalList.contains(obj)) {
                    totalList.add(obj);
                }
            }
            knownObjects.add(ByteBuffer.wrap(obj.getHash()));
        }

        tx.commit();
        session.close();
    }

    @Override
    public Channel getChannel (int id) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        HibernateChannel channel = session.get(HibernateChannel.class, id);
        tx.commit();
        session.close();
        if (channel != null) {
            return channel.toChannel();
        } else {
            throw new RuntimeException("Channel not found..");
        }
    }

    @Override
    public Channel getChannel (Sha256Hash hash) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        Optional<HibernateChannel> optional = session
                .createQuery("from HibernateChannel where hash = :hash", HibernateChannel.class)
                .setParameter("hash", hash)
                .uniqueResultOptional();
        tx.commit();
        session.close();
        if (optional.isPresent()) {
            return optional.get().toChannel();
        } else {
            throw new RuntimeException("Channel not found..");
        }
    }

    @Override
    public List<Channel> getChannel (NodeKey nodeKey) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Channel> channels = session
                .createQuery("from HibernateChannel where nodeKeyClient = :nodeKey", HibernateChannel.class)
                .setParameter("nodeKey", nodeKey)
                .stream()
                .map(HibernateChannel::toChannel)
                .collect(Collectors.toList());
        tx.commit();
        session.close();
        return channels;
    }

    @Override
    public List<Channel> getOpenChannel (NodeKey nodeKey) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Channel> channels = session
                .createQuery("from HibernateChannel where nodeKeyClient = :nodeKey and phase = :phase", HibernateChannel.class)
                .setParameter("nodeKey", nodeKey)
                .setParameter("phase", OPEN)
                .stream()
                .map(HibernateChannel::toChannel)
                .collect(Collectors.toList());
        tx.commit();
        session.close();
        return channels;
    }

    @Override
    public void insertChannel (Channel channel) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        session.persist(new HibernateChannel(channel));
        tx.commit();
        session.close();
    }

    @Override
    public void updateChannelStatus (@NotNull NodeKey nodeKey, @NotNull Sha256Hash channelHash, @NotNull ECKey keyServer,
                                     Channel channel, ChannelUpdate update, List<RevocationHash> revocationHash,
                                     NumberedMessage request, NumberedMessage response) {

        if (request != null) {
            setMessageProcessed(nodeKey, request);
        }
        if (response != null) {
            long messageId = saveMessage(nodeKey, response, SENT);
            response.setMessageNumber(messageId);
            if (request != null) {
                linkResponse(nodeKey, request.getMessageNumber(), response.getMessageNumber());
            }
        }
        if (revocationHash != null) {
            List<RevocationHash> revocationList = revocationHashListTheir.get(channelHash);
            if (revocationList == null) {
                revocationList = new ArrayList<>();
                revocationHashListTheir.put(channelHash, revocationList);
            }
            revocationList.addAll(revocationHash);
        }
        if (channel != null) {
            Session session = sessionFactory.openSession();
            Transaction tx = session.beginTransaction();
            int result = session
                    .createQuery("delete HibernateChannel where hash = :hash", HibernateChannel.class)
                    .setParameter("hash", channel.getHash())
                    .executeUpdate();
            if (result == 0) {
                tx.rollback();
                session.close();
                throw new RuntimeException("Not able to find channel in list, not updated..");
            } else {
                session.persist(new HibernateChannel(channel));
                tx.commit();
                session.close();
            }
        }
        synchronized (payments) {
            if (update != null) {
                for (PaymentData payment : update.newPayments) {
                    PaymentWrapper paymentWrapper;
                    if (!payment.sending) {
                        paymentWrapper = new PaymentWrapper();
                        payments.add(paymentWrapper);
                        paymentWrapper.paymentData = payment;
                        paymentWrapper.sender = nodeKey;
                        paymentWrapper.statusSender = EMBEDDED;

                        PeeledOnion onion = null;
                        onion = onionHelper.loadMessage(keyServer, payment.onionObject);

                        if (secrets.contains(payment.secret)) {
                            paymentWrapper.paymentData.secret = getPaymentSecret(payment.secret);
                            paymentWrapper.statusSender = TO_BE_REDEEMED;
                        } else if (onion.isLastHop) {
                            System.out.println("Don't have the payment secret - refund..");
                            paymentWrapper.statusSender = TO_BE_REFUNDED;
                        } else {

                            NodeKey nextHop = onion.nextHop;
                            if (getOpenChannel(nextHop).size() > 0) {
                                paymentWrapper.statusReceiver = TO_BE_EMBEDDED;
                                paymentWrapper.receiver = nextHop;
                                payment.onionObject = onion.onionObject;
                            } else {
                                System.out.println("InMemoryDBHandler.updateChannelStatus to be refunded?");
                                paymentWrapper.statusSender = TO_BE_REFUNDED;
                            }
                        }
                    } else {
                        paymentWrapper = getPayment(payment.secret);
                        paymentWrapper.statusReceiver = EMBEDDED;
                    }
                }

                for (PaymentData payment : update.redeemedPayments) {
                    addPaymentSecret(payment.secret);
                    PaymentWrapper paymentWrapper = getPayment(payment.secret);
                    if (paymentWrapper == null) {
                        throw new RuntimeException("Redeemed an unknown payment?");
                    }

                    paymentWrapper.paymentData.secret = payment.secret;
                    if (Objects.equals(paymentWrapper.receiver, nodeKey)) {
                        paymentWrapper.statusReceiver = REDEEMED;
                        paymentWrapper.statusSender = TO_BE_REDEEMED;
                    } else if (Objects.equals(paymentWrapper.sender, nodeKey)) {
                        paymentWrapper.statusSender = REDEEMED;
                    } else {
                        throw new RuntimeException("Neither of the parties involved in payment is the one who got here?");
                    }
                }

                for (PaymentData payment : update.refundedPayments) {
                    PaymentWrapper paymentWrapper = getPayment(payment.secret);
                    if (paymentWrapper == null) {
                        throw new RuntimeException("Refunded an unknown payment?");
                    }

                    if (Objects.equals(paymentWrapper.receiver, nodeKey)) {
                        paymentWrapper.statusReceiver = REFUNDED;
                        paymentWrapper.statusSender = TO_BE_REFUNDED;
                    } else if (Objects.equals(paymentWrapper.sender, nodeKey)) {
                        paymentWrapper.statusSender = REFUNDED;
                    } else {
                        throw new RuntimeException("Neither of the parties involved in payment is the one who got here?");
                    }
                }
                unlockPayments(nodeKey, payments.stream().map(p -> p.paymentData).collect(Collectors.toList()));
            }
        }
    }

    @Override
    public void checkPaymentsList () {
        synchronized (payments) {
            for (PaymentWrapper payment : payments) {
                if (payment.statusSender == EMBEDDED && payment.statusReceiver == TO_BE_EMBEDDED) {
                    if (Tools.currentTime() - payment.paymentData.timestampOpen > Constants.PAYMENT_TIMEOUT) {
                        payment.statusReceiver = UNKNOWN;
                        payment.statusSender = TO_BE_REFUNDED;
                    }
                }
            }
        }
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRefunded (NodeKey nodeKey) {
        synchronized (payments) {
            return getPaymentDatas(nodeKey, payments, true, TO_BE_REFUNDED, CURRENTLY_REFUNDING);
        }
    }

    @Override
    public List<PaymentData> lockPaymentsToBeMade (NodeKey nodeKey) {
        synchronized (payments) {
            return getPaymentDatas(nodeKey, payments, false, TO_BE_EMBEDDED, CURRENTLY_EMBEDDING);
        }
    }

    @Override
    public List<PaymentData> lockPaymentsToBeRedeemed (NodeKey nodeKey) {
        synchronized (payments) {
            return getPaymentDatas(nodeKey, payments, true, TO_BE_REDEEMED, CURRENTLY_REDEEMING);
        }
    }

    @NotNull
    private static List<PaymentData> getPaymentDatas (NodeKey nodeKey, List<PaymentWrapper> payments, boolean sender, PaymentStatus searchFor, PaymentStatus
            replaceWith) {
        List<PaymentData> paymentList = new ArrayList<>();
        for (PaymentWrapper p : payments) {
            if (sender) {
                if (nodeKey.equals(p.sender)) {
                    if (p.statusSender == searchFor) {
                        p.statusSender = replaceWith;
                        paymentList.add(p.paymentData);
                    }
                }
            } else {
                if (nodeKey.equals(p.receiver)) {
                    if (p.statusReceiver == searchFor) {
                        p.statusReceiver = replaceWith;
                        paymentList.add(p.paymentData);
                    }
                }
            }
        }
        return paymentList;
    }

    @Override
    public void unlockPayments (NodeKey nodeKey, List<PaymentData> paymentList) {
        synchronized (payments) {

            payments.stream()
                    .filter(p -> Objects.equals(p.receiver, nodeKey))
                    .filter(p -> paymentList.contains(p.paymentData))
                    .forEach(p -> {
                        if (p.statusReceiver == CURRENTLY_EMBEDDING) {
                            p.statusReceiver = TO_BE_EMBEDDED;
                        }
                        if (p.statusReceiver == CURRENTLY_REDEEMING) {
                            p.statusReceiver = TO_BE_REDEEMED;
                        }
                        if (p.statusReceiver == CURRENTLY_REFUNDING) {
                            p.statusReceiver = TO_BE_REFUNDED;
                        }
                    });

            payments.stream()
                    .filter(p -> Objects.equals(p.sender, nodeKey))
                    .filter(p -> paymentList.contains(p.paymentData))
                    .forEach(p -> {
                        if (p.statusSender == CURRENTLY_EMBEDDING) {
                            p.statusSender = TO_BE_EMBEDDED;
                        }
                        if (p.statusSender == CURRENTLY_REDEEMING) {
                            p.statusSender = TO_BE_REDEEMED;
                        }
                        if (p.statusSender == CURRENTLY_REFUNDING) {
                            p.statusSender = TO_BE_REFUNDED;
                        }
                    });
        }
    }

    @Override
    public List<Channel> getOpenChannel () {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Channel> channels = session
                .createQuery("from HibernateChannel where phase = :phase", HibernateChannel.class)
                .setParameter("phase", OPEN)
                .stream()
                .map(HibernateChannel::toChannel)
                .collect(Collectors.toList());
        tx.commit();
        session.close();
        return channels;
    }

    @Override
    public List<MessageWrapper> getMessageList (NodeKey nodeKey, Sha256Hash channelHash, Class c) {
        List<MessageWrapper> wrapper = messageList.get(nodeKey);
        if (wrapper == null) {
            return Collections.emptyList();
        }
        List<MessageWrapper> total =
                wrapper.stream()
                        .filter(message -> c.isAssignableFrom(message.getMessage().getClass()))
                        .collect(Collectors.toList());

        return total;
    }

    @Override
    public void setMessageProcessed (NodeKey nodeKey, NumberedMessage message) {
        saveMessage(nodeKey, message, RECEIVED);
    }

    @Override
    public long lastProcessedMessaged (NodeKey nodeKey) {
        List<MessageWrapper> messageWrappers = messageList.get(nodeKey);
        if (messageWrappers != null) {
            Optional<Long> o = messageWrappers.stream()
                    .filter(w -> w.getDirection() == RECEIVED)
                    .filter(w -> w.getMessage() instanceof NumberedMessage)
                    .map(MessageWrapper::getMessage)
                    .map(m -> (NumberedMessage) m)
                    .map(NumberedMessage::getMessageNumber)
                    .max(Long::compareTo);

            if (o.isPresent()) {
                return o.get();
            }
        }
        return 0;
    }

    @Override
    public synchronized long saveMessage (NodeKey nodeKey, NumberedMessage message, DIRECTION direction) {
        if (direction == SENT) {
            Long i = messageCountList.get(nodeKey);
            if (i == null) {
                i = 1L;
            } else {
                i++;
            }
            messageCountList.put(nodeKey, i);
            Map<Long, NumberedMessage> messageMap = sentMessages.get(nodeKey);
            if (messageMap == null) {
                messageMap = new ConcurrentHashMap<>();
                sentMessages.put(nodeKey, messageMap);
            }
            message.setMessageNumber(i);
            messageMap.put(i, message);

            if (message instanceof AckableMessage) {
                List<AckableMessage> unAckedMessageList = unAckedMessageMap.get(nodeKey);
                if (unAckedMessageList == null) {
                    unAckedMessageList = new ArrayList<>();
                    unAckedMessageMap.put(nodeKey, unAckedMessageList);
                }
                unAckedMessageList.add((AckableMessage) message);
            }

            List<MessageWrapper> messageList = this.messageList.get(nodeKey);
            if (messageList == null) {
                messageList = new ArrayList<>();
                this.messageList.put(nodeKey, messageList);
            }
            messageList.add(new MessageWrapper(message, Tools.currentTime(), direction));
            return i;
        } else {
            List<MessageWrapper> messageList = this.messageList.get(nodeKey);
            if (messageList == null) {
                messageList = new ArrayList<>();
                this.messageList.put(nodeKey, messageList);
            }
            messageList.add(new MessageWrapper(message, Tools.currentTime(), direction));
            return 0;
        }
    }

    @Override
    public void linkResponse (NodeKey nodeKey, long messageRequest, long messageResponse) {
        Map<Long, Long> linkedMessages = linkedMessageMap.get(nodeKey);
        if (linkedMessages == null) {
            linkedMessages = new ConcurrentHashMap<>();
            linkedMessageMap.put(nodeKey, linkedMessages);
        }
        linkedMessages.put(messageRequest, messageResponse);
    }

    @Override
    public List<AckableMessage> getUnackedMessageList (NodeKey nodeKey) {
        List<AckableMessage> unAckedMessageList = unAckedMessageMap.get(nodeKey);
        if (unAckedMessageList == null || unAckedMessageList.size() == 0) {
            return Collections.emptyList();
        } else {
            return unAckedMessageList;
        }
    }

    @Override
    public NumberedMessage getMessageResponse (NodeKey nodeKey, long messageIdReceived) {
        Map<Long, Long> linkMap = linkedMessageMap.get(nodeKey);
        if (linkMap != null) {
            Long response = linkMap.get(messageIdReceived);
            if (response != null) {
                Map<Long, NumberedMessage> messageMap = sentMessages.get(nodeKey);
                if (messageMap != null) {
                    NumberedMessage m = messageMap.get(response);
                    if (m != null) {
                        return m;
                    }
                }
                throw new RuntimeException("We wanted to resend an old message, but can't find it..");
            }
        }
        return null;
    }

    @Override
    public void setMessageAcked (NodeKey nodeKey, long messageId) {
        List<AckableMessage> unackedMessages = unAckedMessageMap.get(nodeKey);
        unackedMessages.removeIf(message -> message.getMessageNumber() == messageId);
    }

    @Override
    public List<P2PDataObject> getSyncDataByFragmentIndex (int fragmentIndex) {
//        cleanFragmentMap();
        fragmentToListMap.get(fragmentIndex).removeIf(
                p -> (Tools.currentTime() - p.getTimestamp() > P2PDataObject.MAXIMUM_AGE_SYNC_DATA));
        return fragmentToListMap.get(fragmentIndex);
    }

    @Override
    public List<P2PDataObject> getSyncDataIPObjects () {
        return null;
    }

    @Override
    public List<PubkeyIPObject> getIPObjectsWithActiveChannel () {
        return new ArrayList<>();
    }

    @Override
    public List<ChannelStatusObject> getTopology () {
        List<ChannelStatusObject> list = new ArrayList<>();
        synchronized (totalList) {
            for (P2PDataObject object : totalList) {
                if (object instanceof ChannelStatusObject) {
                    list.add((ChannelStatusObject) object);
                }
            }
        }
        return list;
    }

    @Override
    public List<PaymentWrapper> getAllPayments () {
        return new ArrayList<>(payments);
    }

    @Override
    public List<PaymentWrapper> getOpenPayments () {
        return new ArrayList<>();
    }

    @Override
    public List<PaymentWrapper> getRefundedPayments () {
        return new ArrayList<>();
    }

    @Override
    public List<PaymentWrapper> getRedeemedPayments () {
        return new ArrayList<>();
    }

    @Override
    public void addPayment (NodeKey nodeKey, PaymentData paymentData) {
        PaymentWrapper paymentWrapper = new PaymentWrapper();
        paymentWrapper.receiver = nodeKey;
        paymentWrapper.statusReceiver = TO_BE_EMBEDDED;
        paymentWrapper.paymentData = paymentData;
        synchronized (payments) {
            payments.add(paymentWrapper);
        }
    }

    @Override
    public void updatePayment (PaymentWrapper paymentWrapper) {
        synchronized (payments) {
            for (PaymentWrapper p : payments) {
                if (p.equals(paymentWrapper)) {
                    p.paymentData = paymentWrapper.paymentData;
                    p.receiver = paymentWrapper.receiver;
                    p.sender = paymentWrapper.sender;
                    p.statusReceiver = paymentWrapper.statusReceiver;
                    p.statusSender = paymentWrapper.statusSender;
                    return;
                }
            }
            payments.add(paymentWrapper);
        }
    }

    @Override
    public PaymentWrapper getPayment (PaymentSecret paymentSecret) {
        synchronized (payments) {
            Optional<PaymentWrapper> paymentWrapper = payments.stream()
                    .filter(p -> !isPaymentComplete(p))
                    .filter(p -> Objects.equals(p.paymentData.secret, paymentSecret))
                    .findAny();

            if (paymentWrapper.isPresent()) {
                return paymentWrapper.get();
            } else {
                return null;
            }
        }
    }

    private boolean isPaymentComplete (PaymentWrapper paymentWrapper) {
        return paymentWrapper.statusSender == REDEEMED && paymentWrapper.statusReceiver == REDEEMED ||
                paymentWrapper.statusSender == REFUNDED && paymentWrapper.statusReceiver == REFUNDED;
    }

    @Override
    public void addPaymentSecret (PaymentSecret secret) {
        if (secrets.contains(secret)) {
            PaymentSecret oldSecret = secrets.get(secrets.indexOf(secret));
            oldSecret.secret = secret.secret;
        } else {
            secrets.add(secret);
        }
    }

    @Override
    public PaymentSecret getPaymentSecret (PaymentSecret secret) {
        if (!secrets.contains(secret)) {
            return null;
        }
        return secrets.get(secrets.indexOf(secret));
    }

    @Override
    public NodeKey getSenderOfPayment (PaymentSecret paymentSecret) {
        synchronized (payments) {
            for (PaymentWrapper payment : payments) {
                if (payment.paymentData.secret.equals(paymentSecret)) {
                    return payment.sender;
                }
            }
            return null;
        }
    }
}
