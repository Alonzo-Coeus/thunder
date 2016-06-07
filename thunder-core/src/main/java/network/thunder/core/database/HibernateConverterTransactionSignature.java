package network.thunder.core.database;

import org.bitcoinj.crypto.TransactionSignature;

import javax.persistence.AttributeConverter;

/**
 * Created by xeno on 07/06/16.
 */
public class HibernateConverterTransactionSignature implements AttributeConverter<TransactionSignature, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (TransactionSignature transactionSignature) {
        return transactionSignature.encodeToBitcoin();
    }

    @Override
    public TransactionSignature convertToEntityAttribute (byte[] bytes) {
        return TransactionSignature.decodeFromBitcoin(bytes, true, true);
    }
}
