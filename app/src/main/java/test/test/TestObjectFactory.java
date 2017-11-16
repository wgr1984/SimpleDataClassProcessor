package test.test;

import javax.annotation.Nullable;

import de.wr.libsimpledataclasses.DataClassFactory;
import de.wr.libsimpledataclasses.Gson;
import de.wr.libsimpledataclasses.Parcelable;


@DataClassFactory
@Gson
@Nullable
@Parcelable
public abstract class TestObjectFactory {
    abstract Void testObjectPackage(String hallo);
}
