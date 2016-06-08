package network.thunder.core.database;

import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.*;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Entity(name = "ChannelSignature")
class HibernateChannelClosingSignature {
    private TransactionSignature transactionSignature;
    private Integer id;
    private Integer channel;

    public HibernateChannelClosingSignature () {
    }

    public HibernateChannelClosingSignature (TransactionSignature transactionSignature) {
        this.transactionSignature = transactionSignature;
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    @Column
    @Convert(converter = HibernateConverterTransactionSignature.class)
    public TransactionSignature getTransactionSignature () {
        return transactionSignature;
    }

    public void setTransactionSignature (TransactionSignature transactionSignature) {
        this.transactionSignature = transactionSignature;
    }

    public Integer getChannel () {
        return channel;
    }

    public void setChannel (Integer channel) {
        this.channel = channel;
    }
}
