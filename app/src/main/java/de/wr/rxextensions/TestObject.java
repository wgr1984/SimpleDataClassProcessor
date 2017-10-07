package de.wr.rxextensions;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.Builder;

@AutoValue()
abstract class TestObject {

    public abstract java.lang.String val1();

    public abstract int number();

    public abstract int number2();

    static Builder builder() {
        return new AutoValue_TestObject.Builder();
    }
    @com.google.auto.value.AutoValue.Builder()
    abstract static class Builder {

        public abstract Builder val1(java.lang.String val1);

        public abstract Builder number(int number);

        public abstract Builder number2(int number2);

        public abstract TestObject build();
    }

}