package network.thunder.core.database;

import network.thunder.core.communication.NodeKey;
import network.thunder.core.communication.layer.high.Channel;
import network.thunder.core.communication.layer.high.channel.ChannelSignatures;
import network.thunder.core.database.objects.HibernateConverterECKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.hibernate.Session;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jean-Pierre Rupp on 05/06/16.
 */

@Entity(name = "Channel")
class HibernateChannel {
    private int id;
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
    private Sha256Hash anchorTxHash;
    private Integer minConfirmationAnchor;
    private HibernateChannelStatus channelStatus;
    private Channel.Phase phase;
    private List<HibernateClosingSignature> closingSignatures = new ArrayList<>();
    private List<HibernateChannelSignature> channelSignatures = new ArrayList<>();
    private List<HibernatePaymentSignature> paymentSignatures = new ArrayList<>();

    public Channel toChannel () {
        List<TransactionSignature> channelTransactionSignatures = channelSignatures.stream()
                .map(HibernateChannelSignature::getTransactionSignature)
                .collect(Collectors.toList());
        List<TransactionSignature> paymentTransactionSignatures = paymentSignatures.stream()
                .map(HibernatePaymentSignature::getTransactionSignature)
                .collect(Collectors.toList());
        ChannelSignatures localChannelSignatures =
                new ChannelSignatures(channelTransactionSignatures, paymentTransactionSignatures);

        Channel channel = new Channel();
        channel.id = id;
        channel.nodeKeyClient = nodeKeyClient == null ? null : new NodeKey(nodeKeyClient);
        channel.keyClient = keyClient;
        channel.keyServer = keyServer;
        channel.masterPrivateKeyClient = masterPrivateKeyClient;
        channel.masterPrivateKeyServer = masterPrivateKeyServer;
        channel.shaChainDepthCurrent = shaChainDepthCurrent;
        channel.timestampOpen = timestampOpen;
        channel.timestampForceClose = timestampForceClose;
        channel.anchorTxHash = anchorTxHash;
        channel.anchorTx = anchorTx;
        channel.minConfirmationAnchor = minConfirmationAnchor;
        channel.channelStatus = channelStatus.toChannelStatus();
        channel.channelSignatures = localChannelSignatures;
        channel.phase = phase;
        channel.closingSignatures = closingSignatures.stream()
                .map(HibernateClosingSignature::getTransactionSignature)
                .collect(Collectors.toList());
        return channel;
    }

    public HibernateChannel () {
    }

    public HibernateChannel (Channel channel) {
        id = channel.id;
        hash = channel.getHash();
        nodeKeyClient = channel.nodeKeyClient == null ? null
                : channel.nodeKeyClient.getPubKey();
        keyClient = channel.keyClient;
        keyServer = channel.keyServer;
        masterPrivateKeyClient = channel.masterPrivateKeyClient;
        masterPrivateKeyServer = channel.masterPrivateKeyServer;
        shaChainDepthCurrent = channel.shaChainDepthCurrent;
        timestampOpen = channel.timestampOpen;
        timestampForceClose = channel.timestampForceClose;
        anchorTx = channel.anchorTx;
        anchorTxHash = channel.anchorTxHash;
        minConfirmationAnchor = channel.minConfirmationAnchor;
        if (channel.channelStatus != null) {
            channelStatus = new HibernateChannelStatus(channel.channelStatus);
        }
        phase = channel.phase;
    }

    public void saveChannelData (Session session, Channel channel) {
        if (channel.closingSignatures != null) {
            channel.closingSignatures.forEach(signature -> {
                HibernateClosingSignature closingSignature = new HibernateClosingSignature(signature);
                closingSignature.setChannel(this);
                this.getClosingSignatures().add(closingSignature);
                session.save(closingSignature);
            });
        }
        if (channel.channelSignatures != null) {
            if (channel.channelSignatures.paymentSignatures != null) {
                channel.channelSignatures.paymentSignatures.forEach(signature -> {
                    HibernatePaymentSignature paymentSignature = new HibernatePaymentSignature(signature);
                    paymentSignature.setChannel(this);
                    this.getPaymentSignatures().add(paymentSignature);
                    session.save(paymentSignature);
                });
            }
            if (channel.channelSignatures.channelSignatures != null) {
                channel.channelSignatures.channelSignatures.forEach(signature -> {
                    HibernateChannelSignature channelSignature = new HibernateChannelSignature(signature);
                    channelSignature.setChannel(this);
                    this.getChannelSignatures().add(channelSignature);
                    session.save(channelSignature);
                });
            }
        }
        if (channel.channelStatus != null && channel.channelStatus.paymentList != null) {
            channel.channelStatus.paymentList.forEach(payment -> {
                HibernatePaymentData paymentData = new HibernatePaymentData(payment);
                paymentData.setChannel(this);
                this.getChannelStatus().getPaymentList().add(paymentData);
                session.save(paymentData);
            });
        }
    }

    @Id
    public int getId () {
        return id;
    }

    public void setId (int id) {
        this.id = id;
    }

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

    @Embedded
    public HibernateChannelStatus getChannelStatus () {
        return channelStatus;
    }

    public void setChannelStatus (HibernateChannelStatus channelStatus) {
        this.channelStatus = channelStatus;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "channel")
    public List<HibernatePaymentSignature> getPaymentSignatures () {
        return paymentSignatures;
    }

    public void setPaymentSignatures (List<HibernatePaymentSignature> paymentSignatures) {
        this.paymentSignatures = paymentSignatures;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "channel")
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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "channel")
    public List<HibernateClosingSignature> getClosingSignatures () {
        return closingSignatures;
    }

    public void setClosingSignatures (List<HibernateClosingSignature> closingSignatures) {
        this.closingSignatures = closingSignatures;
    }

    public Sha256Hash getAnchorTxHash () {
        return anchorTxHash;
    }

    public void setAnchorTxHash (Sha256Hash anchorTxHash) {
        this.anchorTxHash = anchorTxHash;
    }
}
