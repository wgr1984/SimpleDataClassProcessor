package de.wr.rxextensions;

import java.util.List;

import de.wr.libsimpledataclasses.DataClassFactory;

@DataClassFactory
public abstract class SimpleTestDataFactory {
    abstract Void simpleObject(int value1, String value2, List<String> value3);
}
