package de.wr.rxextensions;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.wr.libsimpledataclasses.DataClassFactory;

/**
 * Created by wolfgangreithmeier on 21.10.17.
 */
@DataClassFactory
public abstract class NullableTestDataFactory {
    abstract @Nullable Void nullableTestObject0(String s1, @NonNull String s2);
    abstract Void nullableTestObject2(String s1, @Nullable String s2);
    abstract Void nullableTestObject3(String s1, String s2);
    abstract @Nullable Void nullableTestObject4(String s1, String s2);
}
