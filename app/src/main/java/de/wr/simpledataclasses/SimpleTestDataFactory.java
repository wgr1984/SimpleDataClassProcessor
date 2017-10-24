package de.wr.simpledataclasses;

import java.util.List;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.Gson;
import de.wr.libsimpledataclasses.Named;

@DataClassFactory
public abstract class SimpleTestDataFactory {
    abstract Void simpleObject(int value1, String value2, List<String> value3);
    abstract @Gson Void simpleObjectNamed(
            @Named("value_1") int value1,
            @Named("value_2") String value2,
            @Named("value_3") List<String> value3);
}
