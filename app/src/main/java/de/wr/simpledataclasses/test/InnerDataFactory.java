package de.wr.simpledataclasses.test;

import javax.annotation.Nullable;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.Gson;
import de.wr.libsimpledataclasses.Parcelable;

/**
 * Created by wolfgangreithmeier on 15/11/17.
 */

@DataClassFactory
@Gson
@Nullable
@Parcelable
public abstract class InnerDataFactory {
    abstract Void testObjectInner(String hallo);
}
