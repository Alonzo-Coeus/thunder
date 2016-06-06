package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by Jean-Pierre Rupp on 05/06/16.
 */

public class HibernateChannel implements PersistentChannel {
    @Override
    public Optional<Channel> getChannel (PersistentSession session, Integer id) {
        return null;
    }

    @Override
    public Optional<Integer> insertChannel (PersistentSession session, Channel channel) {
        return null;
    }

    @Override
    public Iterator<Channel> getOpenChannels (PersistentSession session) {
        return null;
    }

    @Override
    public Iterator<Channel> getOpenChannelsForNode (PersistentSession session, NodeKey nodekey) {
        return null;
    }

    @Override
    public Iterator<Channel> getChannelsForNode (PersistentSession session, NodeKey nodekey) {
        return null;
    }

    @Override
    public Iterator<Channel> getIPObjectsWithActiveChannel (PersistentSession session) {
        return null;
    }

    @Override
    public void updateChannel (PersistentSession session, Channel channel) {

    }

    @Override
    public void deleteChannel (PersistentSession session, Channel channel) {

    }

    @Entity
    @Table(name = "channels")
    private class ChannelEntity {
        private Integer id;
        private NodeKey nodeKeyClient;
        private ECKey keyClient;
        private ECKey keyServer;
        private byte[] masterPrivateKeyClient;
        private byte[] masterPrivateKeyServer;
        private Integer shaChainDepthCurrent;
        private Integer timestampOpen;
        private Integer timestampForceClose;
        private Transaction anchorTx;
        private Integer minConfirmationAnchor;
        private ChannelStatus channelStatus;
        private Channel.Phase phase;
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ChannelSignatureEntity> channelSignatures = new ArrayList<>();
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        private List<PaymentSignatureEntity> paymentSignatures = new ArrayList<>();
        @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
        private List<ClosingSignatureEntity> closingSignatures = new ArrayList<>();

        public Channel toChannel() {
            List<TransactionSignature> channelTransactionSignatures = this.channelSignatures.stream()
                    .map(ChannelSignatureEntity::getTransactionSignature)
                    .collect(Collectors.toList());
            List<TransactionSignature> paymentTransactionSignatures = this.paymentSignatures.stream()
                    .map(PaymentSignatureEntity::getTransactionSignature)
                    .collect(Collectors.toList());
            ChannelSignatures channelSignatures =
                    new ChannelSignatures(channelTransactionSignatures, paymentTransactionSignatures);
            Channel channel = new Channel();
            channel.id = id;
            channel.nodeKeyClient = nodeKeyClient;
            channel.keyClient = keyClient;
            channel.keyServer = keyServer;
            channel.masterPrivateKeyClient = masterPrivateKeyClient;
            channel.masterPrivateKeyServer = masterPrivateKeyServer;
            channel.shaChainDepthCurrent = shaChainDepthCurrent;
            channel.timestampOpen = timestampOpen;
            channel.timestampForceClose = timestampForceClose;
            channel.anchorTxHash = anchorTx.getHash();
            channel.anchorTx = anchorTx;
            channel.minConfirmationAnchor = minConfirmationAnchor;
            channel.channelStatus = channelStatus;
            channel.channelSignatures = channelSignatures;
            channel.phase = phase;
            channel.closingSignatures = closingSignatures.stream()
                    .map(ClosingSignatureEntity::getTransactionSignature)
                    .collect(Collectors.toList());
            return channel;
        }

        public ChannelEntity () {
        }

        public ChannelEntity (Channel channel) {
            id = channel.id;
            nodeKeyClient = channel.nodeKeyClient;
            keyClient = channel.keyClient;
            keyServer = channel.keyServer;
            masterPrivateKeyClient = channel.masterPrivateKeyClient;
            masterPrivateKeyServer = channel.masterPrivateKeyServer;
            shaChainDepthCurrent = channel.shaChainDepthCurrent;
            timestampOpen = channel.timestampOpen;
            timestampForceClose = channel.timestampForceClose;
            anchorTx = channel.anchorTx;
            minConfirmationAnchor = channel.minConfirmationAnchor;
            channelStatus = channel.channelStatus;
            channelSignatures = channel.channelSignatures.channelSignatures.stream()
                    .map(ChannelSignatureEntity::new)
                    .collect(Collectors.toList());
            paymentSignatures = channel.channelSignatures.paymentSignatures.stream()
                    .map(PaymentSignatureEntity::new)
                    .collect(Collectors.toList());
            phase = channel.phase;
            closingSignatures = channel.closingSignatures.stream()
                    .map(ClosingSignatureEntity::new)
                    .collect(Collectors.toList());
        }

        @Id
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public NodeKey getNodeKeyClient() {
            return nodeKeyClient;
        }

        public void setNodeKeyClient(NodeKey nodeKeyClient) {
            this.nodeKeyClient = nodeKeyClient;
        }

        public ECKey getKeyClient() {
            return keyClient;
        }

        public void setKeyClient(ECKey keyClient) {
            this.keyClient = keyClient;
        }

        public ECKey getKeyServer() {
            return keyServer;
        }

        public void setKeyServer(ECKey keyServer) {
            this.keyServer = keyServer;
        }

        public byte[] getMasterPrivateKeyClient() {
            return masterPrivateKeyClient;
        }

        public void setMasterPrivateKeyClient(byte[] masterPrivateKeyClient) {
            this.masterPrivateKeyClient = masterPrivateKeyClient;
        }

        public byte[] getMasterPrivateKeyServer() {
            return masterPrivateKeyServer;
        }

        public void setMasterPrivateKeyServer(byte[] masterPrivateKeyServer) {
            this.masterPrivateKeyServer = masterPrivateKeyServer;
        }

        public Integer getShaChainDepthCurrent() {
            return shaChainDepthCurrent;
        }

        public void setShaChainDepthCurrent(Integer shaChainDepthCurrent) {
            this.shaChainDepthCurrent = shaChainDepthCurrent;
        }

        public Integer getTimestampOpen() {
            return timestampOpen;
        }

        public void setTimestampOpen(Integer timestampOpen) {
            this.timestampOpen = timestampOpen;
        }

        public Integer getTimestampForceClose() {
            return timestampForceClose;
        }

        public void setTimestampForceClose(Integer timestampForceClose) {
            this.timestampForceClose = timestampForceClose;
        }

        public Transaction getAnchorTx() {
            return anchorTx;
        }

        public void setAnchorTx(Transaction anchorTx) {
            this.anchorTx = anchorTx;
        }

        public Integer getMinConfirmationAnchor() {
            return minConfirmationAnchor;
        }

        public void setMinConfirmationAnchor(Integer minConfirmationAnchor) {
            this.minConfirmationAnchor = minConfirmationAnchor;
        }

        public ChannelStatus getChannelStatus() {
            return channelStatus;
        }

        public void setChannelStatus(ChannelStatus channelStatus) {
            this.channelStatus = channelStatus;
        }

        public List<PaymentSignatureEntity> getPaymentSignatures() {
            return paymentSignatures;
        }

        public void setPaymentSignatures(List<PaymentSignatureEntity> paymentSignatures) {
            this.paymentSignatures = paymentSignatures;
        }

        public List<ChannelSignatureEntity> getChannelSignatures() {
            return channelSignatures;
        }

        public void setChannelSignatures(List<ChannelSignatureEntity> channelSignatures) {
            this.channelSignatures = channelSignatures;
        }

        public Channel.Phase getPhase() {
            return phase;
        }

        public void setPhase(Channel.Phase phase) {
            this.phase = phase;
        }

        public List<ClosingSignatureEntity> getClosingSignatures() {
            return closingSignatures;
        }

        public void setClosingSignatures(List<ClosingSignatureEntity> closingSignatures) {
            this.closingSignatures = closingSignatures;
        }

    }

    @Entity
    @Table(name = "channel_closing_signatures")
    private class ClosingSignatureEntity {
        private TransactionSignature transactionSignature;
        private Integer id;

        public ClosingSignatureEntity () {
        }

        public ClosingSignatureEntity (TransactionSignature transactionSignature) {
            this.transactionSignature = transactionSignature;
        }

        @Id
        @GeneratedValue
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public TransactionSignature getTransactionSignature() {
            return transactionSignature;
        }

        public void setTransactionSignature(TransactionSignature transactionSignature) {
            this.transactionSignature = transactionSignature;
        }
    }

    @Entity
    @Table(name = "channel_payment_signatures")
    private class PaymentSignatureEntity {
        private TransactionSignature transactionSignature;
        private Integer id;

        public PaymentSignatureEntity () {
        }

        public PaymentSignatureEntity (TransactionSignature transactionSignature) {
            this.transactionSignature = transactionSignature;
        }

        @Id
        @GeneratedValue
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public TransactionSignature getTransactionSignature() {
            return transactionSignature;
        }

        public void setTransactionSignature(TransactionSignature transactionSignature) {
            this.transactionSignature = transactionSignature;
        }
    }

    @Entity
    @Table(name = "channel_signatures")
    class ChannelSignatureEntity {
        private TransactionSignature transactionSignature;
        private Integer id;

        public ChannelSignatureEntity () {
        }

        public ChannelSignatureEntity (TransactionSignature transactionSignature) {
            this.transactionSignature = transactionSignature;
        }

        @Id
        @GeneratedValue
        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public TransactionSignature getTransactionSignature() {
            return transactionSignature;
        }

        public void setTransactionSignature(TransactionSignature transactionSignature) {
            this.transactionSignature = transactionSignature;
        }
    }
}

