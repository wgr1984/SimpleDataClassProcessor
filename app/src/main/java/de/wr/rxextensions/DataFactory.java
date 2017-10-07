package de.wr.rxextensions;

import de.wr.libsimpledataclasses.DataClassFactory;

/**
 * Created by wolfgangreithmeier on 15/04/2017.
 */
@DataClassFactory
public abstract class DataFactory {

    abstract Object createDataObject1(String val1, int number, int number2);

}
