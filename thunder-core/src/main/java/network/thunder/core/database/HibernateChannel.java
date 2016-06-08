package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.database.objects.HibernateConverterECKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jean-Pierre Rupp on 05/06/16.
 */

@Entity(name = "Channel")
class HibernateChannel {
    private Integer id;
    private Sha256Hash hash;
    private byte[] nodeKeyClient;
    private ECKey keyClient;
    private ECKey keyServer;
    private byte[] masterPrivateKeyClient;
    private byte[] masterPrivateKeyServer;
    private Integer shaChainDepthCurrent;
    private Integer timestampOpen;
    private Integer timestampForceClose;
    private Transaction anchorTx;
    private Integer minConfirmationAnchor;
    private HibernateChannelStatus channelStatus;
    private Channel.Phase phase;
    private List<HibernateChannelSignature> channelSignatures = new ArrayList<>();
    private List<HibernateChannelSignature> paymentSignatures = new ArrayList<>();
    private List<HibernateChannelSignature> closingSignatures = new ArrayList<>();

    public Channel toChannel () {
        List<TransactionSignature> channelTransactionSignatures = this.channelSignatures.stream()
                .map(HibernateChannelSignature::getTransactionSignature)
                .collect(Collectors.toList());
        List<TransactionSignature> paymentTransactionSignatures = this.paymentSignatures.stream()
                .map(HibernateChannelSignature::getTransactionSignature)
                .collect(Collectors.toList());
        ChannelSignatures channelSignatures =
                new ChannelSignatures(channelTransactionSignatures, paymentTransactionSignatures);
        Channel channel = new Channel();

        channel.id = id;
        channel.nodeKeyClient = new NodeKey(nodeKeyClient);
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
        channel.channelStatus = channelStatus.toChannelStatus();
        channel.channelSignatures = channelSignatures;
        channel.phase = phase;
        channel.closingSignatures = closingSignatures.stream()
                .map(HibernateChannelSignature::getTransactionSignature)
                .collect(Collectors.toList());
        return channel;
    }

    public HibernateChannel () {
    }

    public HibernateChannel (Channel channel) {
        id = channel.id;
        hash = channel.getHash();
        nodeKeyClient = channel.nodeKeyClient.getPubKey();
        keyClient = channel.keyClient;
        keyServer = channel.keyServer;
        masterPrivateKeyClient = channel.masterPrivateKeyClient;
        masterPrivateKeyServer = channel.masterPrivateKeyServer;
        shaChainDepthCurrent = channel.shaChainDepthCurrent;
        timestampOpen = channel.timestampOpen;
        timestampForceClose = channel.timestampForceClose;
        anchorTx = channel.anchorTx;
        minConfirmationAnchor = channel.minConfirmationAnchor;
        channelStatus = new HibernateChannelStatus(channel.channelStatus);
        channelSignatures = channel.channelSignatures.channelSignatures.stream()
                .map(HibernateChannelSignature::new)
                .collect(Collectors.toList());
        paymentSignatures = channel.channelSignatures.paymentSignatures.stream()
                .map(HibernateChannelSignature::new)
                .collect(Collectors.toList());
        phase = channel.phase;
        closingSignatures = channel.closingSignatures.stream()
                .map(HibernateChannelSignature::new)
                .collect(Collectors.toList());
    }

    @Id
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    @Column(unique = true)
    public Sha256Hash getHash () {
        return hash;
    }

    public void setHash (Sha256Hash hash) {
        this.hash = hash;
    }

    public byte[] getNodeKeyClient () {
        return nodeKeyClient;
    }

    public void setNodeKeyClient (byte[] nodeKeyClient) {
        this.nodeKeyClient = nodeKeyClient;
    }

    @Column
    @Convert(converter = HibernateConverterECKey.class)
    public ECKey getKeyClient () {
        return keyClient;
    }

    public void setKeyClient (ECKey keyClient) {
        this.keyClient = keyClient;
    }

    @Column
    @Convert(converter = HibernateConverterECKey.class)
    public ECKey getKeyServer () {
        return keyServer;
    }

    public void setKeyServer (ECKey keyServer) {
        this.keyServer = keyServer;
    }

    public byte[] getMasterPrivateKeyClient () {
        return masterPrivateKeyClient;
    }

    public void setMasterPrivateKeyClient (byte[] masterPrivateKeyClient) {
        this.masterPrivateKeyClient = masterPrivateKeyClient;
    }

    public byte[] getMasterPrivateKeyServer () {
        return masterPrivateKeyServer;
    }

    public void setMasterPrivateKeyServer (byte[] masterPrivateKeyServer) {
        this.masterPrivateKeyServer = masterPrivateKeyServer;
    }

    public Integer getShaChainDepthCurrent () {
        return shaChainDepthCurrent;
    }

    public void setShaChainDepthCurrent (Integer shaChainDepthCurrent) {
        this.shaChainDepthCurrent = shaChainDepthCurrent;
    }

    public Integer getTimestampOpen () {
        return timestampOpen;
    }

    public void setTimestampOpen (Integer timestampOpen) {
        this.timestampOpen = timestampOpen;
    }

    public Integer getTimestampForceClose () {
        return timestampForceClose;
    }

    public void setTimestampForceClose (Integer timestampForceClose) {
        this.timestampForceClose = timestampForceClose;
    }

    @Column
    @Convert(converter = HibernateConverterTransaction.class)
    public Transaction getAnchorTx () {
        return anchorTx;
    }

    public void setAnchorTx (Transaction anchorTx) {
        this.anchorTx = anchorTx;
    }

    public Integer getMinConfirmationAnchor () {
        return minConfirmationAnchor;
    }

    public void setMinConfirmationAnchor (Integer minConfirmationAnchor) {
        this.minConfirmationAnchor = minConfirmationAnchor;
    }

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public HibernateChannelStatus getChannelStatus () {
        return channelStatus;
    }

    public void setChannelStatus (HibernateChannelStatus channelStatus) {
        this.channelStatus = channelStatus;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<HibernateChannelSignature> getPaymentSignatures () {
        return paymentSignatures;
    }

    public void setPaymentSignatures (List<HibernateChannelSignature> paymentSignatures) {
        this.paymentSignatures = paymentSignatures;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<HibernateChannelSignature> getChannelSignatures () {
        return channelSignatures;
    }

    public void setChannelSignatures (List<HibernateChannelSignature> channelSignatures) {
        this.channelSignatures = channelSignatures;
    }

    public Channel.Phase getPhase () {
        return phase;
    }

    public void setPhase (Channel.Phase phase) {
        this.phase = phase;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<HibernateChannelSignature> getClosingSignatures () {
        return closingSignatures;
    }

    public void setClosingSignatures (List<HibernateChannelSignature> closingSignatures) {
        this.closingSignatures = closingSignatures;
    }

}
