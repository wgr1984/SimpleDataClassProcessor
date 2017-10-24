package de.wr.simpledataclasses;

import java.util.List;

import javax.annotation.Nullable;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.DefaultInt;
import de.wr.libsimpledataclasses.DefaultString;
import de.wr.libsimpledataclasses.Gson;
import de.wr.libsimpledataclasses.Parcelable;

/**
 * Created by wolfgangreithmeier on 15/04/2017.
 */
@DataClassFactory
@Nullable
public abstract class DataFactory {

    public final String test = "testValue";

    @Gson abstract Void dataObject1(@DefaultString(test) String val1, @DefaultInt(2) int number, @DefaultInt(3) int number2);

    abstract Void dataObject2(@Nullable String val1, @Nullable List<List<de.wr.simpledataclasses.DataObject1>> list, double number1);

    @Gson @Parcelable abstract Void dataObject3(byte by, double d, float f, int i, short s, boolean b, long l, Number number);
}
