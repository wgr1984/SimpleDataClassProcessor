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
@DataClassFactory
public abstract class DataFactory {

    public final String test = "testValue";

    abstract void createDataObject1(@DefaultString(test) String val1, @DefaultInt(2) int number, @DefaultInt(3) int number2);

    abstract void createDataObject2(@Nullable String val1, @Nullable List<List<de.wr.rxextensions.DataObject1>> list, double number1);

}
