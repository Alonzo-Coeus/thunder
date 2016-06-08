package network.thunder.core.database;

import network.thunder.core.communication.layer.high.ChannelStatus;
import network.thunder.core.communication.layer.high.RevocationHash;
import org.bitcoinj.core.Address;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Entity(name = "ChannelStatus")
public class HibernateChannelStatus {
    private Integer id;
    private long amountClient;
    private long amountServer;
    private List<HibernateChannelPaymentData> paymentList = new ArrayList<>();
    private int feePerByte;
    private int csvDelay;
    private HibernateChannelRevocationHash revoHashClientCurrent;
    private HibernateChannelRevocationHash revoHashServerCurrent;
    private HibernateChannelRevocationHash revoHashClientNext;
    private HibernateChannelRevocationHash revoHashServerNext;
    private Address addressClient;
    private Address addressServer;

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public long getAmountClient () {
        return amountClient;
    }

    public void setAmountClient (long amountClient) {
        this.amountClient = amountClient;
    }

    public long getAmountServer () {
        return amountServer;
    }

    public void setAmountServer (long amountServer) {
        this.amountServer = amountServer;
    }

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<HibernateChannelPaymentData> getPaymentList () {
        return paymentList;
    }

    public void setPaymentList (List<HibernateChannelPaymentData> paymentList) {
        this.paymentList = paymentList;
    }

    public int getFeePerByte () {
        return feePerByte;
    }

    public void setFeePerByte (int feePerByte) {
        this.feePerByte = feePerByte;
    }

    public int getCsvDelay () {
        return csvDelay;
    }

    public void setCsvDelay (int csvDelay) {
        this.csvDelay = csvDelay;
    }

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public HibernateChannelRevocationHash getRevoHashClientCurrent () {
        return revoHashClientCurrent;
    }

    public void setRevoHashClientCurrent (HibernateChannelRevocationHash revoHashClientCurrent) {
        this.revoHashClientCurrent = revoHashClientCurrent;
    }

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public HibernateChannelRevocationHash getRevoHashServerCurrent () {
        return revoHashServerCurrent;
    }

    public void setRevoHashServerCurrent (HibernateChannelRevocationHash revoHashServerCurrent) {
        this.revoHashServerCurrent = revoHashServerCurrent;
    }

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public HibernateChannelRevocationHash getRevoHashClientNext () {
        return revoHashClientNext;
    }

    public void setRevoHashClientNext (HibernateChannelRevocationHash revoHashClientNext) {
        this.revoHashClientNext = revoHashClientNext;
    }

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    public HibernateChannelRevocationHash getRevoHashServerNext () {
        return revoHashServerNext;
    }

    public void setRevoHashServerNext (HibernateChannelRevocationHash revoHashServerNext) {
        this.revoHashServerNext = revoHashServerNext;
    }

    public Address getAddressClient () {
        return addressClient;
    }

    public void setAddressClient (Address addressClient) {
        this.addressClient = addressClient;
    }

    public Address getAddressServer () {
        return addressServer;
    }

    public void setAddressServer (Address addressServer) {
        this.addressServer = addressServer;
    }

    public ChannelStatus toChannelStatus() {
        ChannelStatus channelStatus = new ChannelStatus();
        channelStatus.amountClient = amountClient;
        channelStatus.amountServer = amountServer;
        channelStatus.paymentList = paymentList.stream()
                .map(HibernateChannelPaymentData::toPaymentData)
                .collect(Collectors.toList());
        channelStatus.feePerByte = feePerByte;
        channelStatus.csvDelay = csvDelay;
        channelStatus.revoHashClientCurrent = revoHashClientCurrent.toRevocationHash();
        channelStatus.revoHashServerCurrent = revoHashServerCurrent.toRevocationHash();
        channelStatus.revoHashClientNext = revoHashClientNext.toRevocationHash();
        channelStatus.revoHashServerNext = revoHashServerNext.toRevocationHash();
        channelStatus.addressClient = addressClient;
        channelStatus.addressServer = addressServer;
        return channelStatus;
    }

    public HibernateChannelStatus() {}

    public HibernateChannelStatus(ChannelStatus channelStatus) {
        amountClient = channelStatus.amountClient;
        amountServer = channelStatus.amountServer;
        paymentList = channelStatus.paymentList.stream()
                .map(HibernateChannelPaymentData::new)
                .collect(Collectors.toList());
        feePerByte = channelStatus.feePerByte;
        csvDelay = channelStatus.csvDelay;
        revoHashClientCurrent = new HibernateChannelRevocationHash(channelStatus.revoHashClientCurrent);
        revoHashServerCurrent = new HibernateChannelRevocationHash(channelStatus.revoHashServerCurrent);
        revoHashClientNext = new HibernateChannelRevocationHash(channelStatus.revoHashClientNext);
        revoHashServerNext = new HibernateChannelRevocationHash(channelStatus.revoHashServerNext);
        addressClient = channelStatus.addressClient;
        addressServer = channelStatus.addressServer;
    }
}
