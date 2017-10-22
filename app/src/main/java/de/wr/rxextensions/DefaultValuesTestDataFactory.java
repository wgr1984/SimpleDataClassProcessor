package de.wr.rxextensions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.DefaultBool;
import de.wr.libsimpledataclasses.DefaultByte;
import de.wr.libsimpledataclasses.DefaultDouble;
import de.wr.libsimpledataclasses.DefaultFloat;
import de.wr.libsimpledataclasses.DefaultInt;
import de.wr.libsimpledataclasses.DefaultLong;
import de.wr.libsimpledataclasses.DefaultShort;

/**
 * Created by wolfgangreithmeier on 21.10.17.
 */
@DataClassFactory
public abstract class DefaultValuesTestDataFactory {
    abstract @Nullable Void defaultValueTestObject(
            @DefaultInt(1) int intV,
            @DefaultLong(2L) long longV,
            @DefaultShort(3) short shortV,
            @DefaultByte(4) byte byteV,
            @DefaultBool(true) boolean boolV,
            @DefaultFloat(5f) float floatV,
            @DefaultDouble(6d) double doubleV
    );
}