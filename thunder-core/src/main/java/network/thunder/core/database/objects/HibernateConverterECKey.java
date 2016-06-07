package network.thunder.core.database.objects;

import org.bitcoinj.core.ECKey;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */
public class HibernateConverterECKey implements AttributeConverter<ECKey, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (ECKey ecKey) {
        return ecKey.toASN1();
    }

    @Override
    public ECKey convertToEntityAttribute (byte[] bytes) {
        return ECKey.fromASN1(bytes);
    }
}
