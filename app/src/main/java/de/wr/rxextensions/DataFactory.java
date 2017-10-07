package de.wr.rxextensions;

import java.util.List;

import de.wr.libsimpledataclasses.DataClassFactory;

/**
 * Created by wolfgangreithmeier on 15/04/2017.
 */
@DataClassFactory
public abstract class DataFactory {

    abstract void createDataObject1(String val1, int number, int number2);

    abstract void createDataObject2(String val1, List<List<de.wr.rxextensions.DataObject1>> list, int number1);

}
