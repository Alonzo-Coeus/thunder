package network.thunder.core.database;

import network.thunder.core.communication.layer.high.RevocationHash;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Entity
@Table(name = "channel_revocation_hash")
public class HibernateChannelRevocationHash {
    private Integer id;
    private Integer index;
    private byte[] secret;
    private byte[] secretHash;

    public HibernateChannelRevocationHash() {}

    public HibernateChannelRevocationHash(RevocationHash revocationHash) {
        index = revocationHash.index;
        secret = revocationHash.secret;
        secretHash = revocationHash.secretHash;
    }

    public RevocationHash toRevocationHash() {
        return new RevocationHash(index, secret, secretHash);
    }

    @Id
    @GeneratedValue
    public Integer getId () {
        return id;
    }

    public void setId (Integer id) {
        this.id = id;
    }

    public Integer getIndex () {
        return index;
    }

    public void setIndex (Integer index) {
        this.index = index;
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
}
