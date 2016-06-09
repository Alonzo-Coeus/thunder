package network.thunder.core.database;

import network.thunder.core.communication.layer.high.RevocationHash;

import javax.persistence.*;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

@Embeddable
public class HibernateRevocationHash {
    private Integer index;
    private byte[] secret;
    private byte[] secretHash;

    public HibernateRevocationHash () {}

    public HibernateRevocationHash (RevocationHash revocationHash) {
        index = revocationHash.index;
        secret = revocationHash.secret;
        secretHash = revocationHash.secretHash;
    }

    public RevocationHash toRevocationHash() {
        return new RevocationHash(index, secret, secretHash);
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
