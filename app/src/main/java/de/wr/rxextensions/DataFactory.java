package de.wr.rxextensions;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.util.List;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.DefaultInt;
import de.wr.libsimpledataclasses.DefaultString;

/**
 * Created by wolfgangreithmeier on 15/04/2017.
 */
@DataClassFactory(value = Nullable.class, nullableAsDefault = true)
public abstract class DataFactory {

    public final String test = "testValue";

    abstract Void createDataObject1(@DefaultString(test) String val1, @DefaultInt(2) int number, @DefaultInt(3) int number2);

    abstract Void createDataObject2(@Nullable String val1, @Nullable List<List<de.wr.rxextensions.DataObject1>> list, double number1);

    abstract Void createDataObject3(byte by, double d, float f, int i, short s, boolean b, long l, Number number);

//    abstract Void createDataObject3(byte by, double d, float f, int i, short s, boolean b, long l, Number number);
}
