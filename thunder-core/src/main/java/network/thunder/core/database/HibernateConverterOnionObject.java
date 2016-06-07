package network.thunder.core.database;

import network.thunder.core.communication.layer.high.payments.messages.OnionObject;

import javax.persistence.AttributeConverter;

/**
 * Created by Jean-Pierre Rupp on 07/06/16.
 */

public class HibernateConverterOnionObject implements AttributeConverter<OnionObject, byte[]> {
    @Override
    public byte[] convertToDatabaseColumn (OnionObject onionObject) {
        return onionObject.data;
    }

    @Override
    public OnionObject convertToEntityAttribute (byte[] bytes) {
        return new OnionObject(bytes);
    }
}
