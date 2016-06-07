package network.thunder.core.database;

import network.thunder.core.etc.Constants;
import org.bitcoinj.core.Transaction;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

public class HibernateConverterTransaction implements AttributeConverter<Transaction, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (Transaction transaction) {
        return transaction.bitcoinSerialize();
    }

    @Override
    public Transaction convertToEntityAttribute (byte[] bytes) {
        return new Transaction(Constants.getNetwork(), bytes);
    }
}
