package de.wr.simpledataclasses;

import android.support.annotation.Nullable;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.DefaultBool;
import de.wr.libsimpledataclasses.DefaultByte;
import de.wr.libsimpledataclasses.DefaultDouble;
import de.wr.libsimpledataclasses.DefaultFloat;
import de.wr.libsimpledataclasses.DefaultInt;
import de.wr.libsimpledataclasses.DefaultLong;
import de.wr.libsimpledataclasses.DefaultShort;
import de.wr.libsimpledataclasses.DefaultString;
import de.wr.libsimpledataclasses.Gson;

/**
 * Created by wolfgangreithmeier on 21.10.17.
 */
@DataClassFactory
public abstract class DefaultValuesTestDataFactory {
    abstract @Gson(true) @Nullable Void defaultValueTestObject(
            @DefaultInt(1) int intV,
            @DefaultLong(2L) long longV,
            @DefaultShort(3) short shortV,
            @DefaultByte(4) byte byteV,
            @DefaultBool(true) boolean boolV,
            @DefaultFloat(5f) float floatV,
            @DefaultDouble(6d) double doubleV,
            @DefaultString("This is a test") String valueS
    );
}
