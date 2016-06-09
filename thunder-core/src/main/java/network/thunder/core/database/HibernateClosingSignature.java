package network.thunder.core.database;

import org.bitcoinj.crypto.TransactionSignature;
import org.hibernate.Hibernate;

import javax.persistence.*;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Entity(name = "ClosingSignature")
class HibernateClosingSignature {
    private TransactionSignature transactionSignature;
    private Integer id;
    private HibernateChannel channel;

    public HibernateClosingSignature () {
    }

    public HibernateClosingSignature (TransactionSignature transactionSignature) {
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

    @ManyToOne(fetch = FetchType.LAZY)
    public HibernateChannel getChannel () {
        return channel;
    }

    public void setChannel (HibernateChannel channel) {
        this.channel = channel;
    }
}
