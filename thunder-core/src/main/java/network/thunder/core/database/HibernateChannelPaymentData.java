package network.thunder.core.database;

import network.thunder.core.communication.layer.high.payments.PaymentData;
import network.thunder.core.communication.layer.high.payments.PaymentSecret;
import network.thunder.core.communication.layer.high.payments.messages.OnionObject;

import javax.persistence.*;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Entity
@Table(name = "channel_payment_data")
public class HibernateChannelPaymentData {
    private Integer id;
    private boolean sending;
    private long amount;
    private byte[] secret;
    private byte[] secretHash;
    private int timestampOpen;
    private int timestampRefund;
    private OnionObject onionObject;

    public HibernateChannelPaymentData () {}

    public HibernateChannelPaymentData (PaymentData paymentData) {
        sending = paymentData.sending;
        amount = paymentData.amount;
        secret = paymentData.secret.secret;
        secret = paymentData.secret.hash;
        timestampOpen = paymentData.timestampOpen;
        timestampRefund = paymentData.timestampRefund;
        onionObject = paymentData.onionObject;
    }

    public PaymentData toPaymentData() {
        PaymentData paymentData = new PaymentData();
        paymentData.sending = sending;
        paymentData.amount = amount;
        paymentData.secret = new PaymentSecret(secret, secretHash);
        paymentData.timestampOpen = timestampOpen;
        paymentData.timestampRefund = timestampRefund;
        paymentData.onionObject = onionObject;
        return paymentData;
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public boolean isSending () {
        return sending;
    }

    public void setSending (boolean sending) {
        this.sending = sending;
    }

    public long getAmount () {
        return amount;
    }

    public void setAmount (long amount) {
        this.amount = amount;
    }

    public byte[] getSecret () {
        return secret;
    }

    public void setSecret (byte[] secret) {
        this.secret = secret;
    }

    public byte[] getSecretHash () {
        return secretHash;
    }

    public void setSecretHash (byte[] secretHash) {
        this.secretHash = secretHash;
    }

    public int getTimestampOpen () {
        return timestampOpen;
    }

    public void setTimestampOpen (int timestampOpen) {
        this.timestampOpen = timestampOpen;
    }

    public int getTimestampRefund () {
        return timestampRefund;
    }

    public void setTimestampRefund (int timestampRefund) {
        this.timestampRefund = timestampRefund;
    }

    @Column
    @Convert(converter = HibernateConverterOnionObject.class)
    public OnionObject getOnionObject () {
        return onionObject;
    }

    public void setOnionObject (OnionObject onionObject) {
        this.onionObject = onionObject;
    }
}
