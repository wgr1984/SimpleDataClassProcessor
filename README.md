[![Build Status](https://travis-ci.org/wgr1984/SimpleDataClassProcessor.svg?branch=master)](https://travis-ci.org/wgr1984/SimpleDataClassProcessor)
[ ![Download](https://api.bintray.com/packages/wgr1984/SimpleDataClasses/SimpleDataClassProcessor/images/download.svg) ](https://bintray.com/wgr1984/SimpleDataClasses/SimpleDataClassProcessor/_latestVersion)

# SimpleDataClassProcessor
This projects provides an annotation processor to create Kotlin like, one-line data classes with the help of Google's AutoValue.

Furthermore it integrates the following autovalue extensions:
* [auto-value-parcel](https://github.com/rharter/auto-value-parcel)
* [auto-value-gson](https://github.com/rharter/auto-value-gson)

# How to use
As project currenlty not published to major maven repos please add:
```
repositories {
   maven { url "https://dl.bintray.com/wgr1984/SimpleDataClasses"}
}
```
and the following two dependecies:
```
annotationProcessor "de.wr.simpledataclasses:simpleDataClassesProcessor:0.1"
provided "de.wr.simpledataclasses:libSimpleDataClasses:0.1"
```
In order to the generate a data class can easily defined via a factory class
inside the package the data classes should be generated.

# Motivation #
Defining pojos in java involes quite some boiler plate code
```Java
public class SimpleObject {
   private int value1;
   private String value2;
   private List<String> value3;
   
   public int getValue1() {
      return value1;
   }
   
   public String getValue2() {
      return value2;
   }
   
   public List<String> getValue3() {
      return value3;
   }
   
   public void setValue1(int value) {
      value1 = value;
   }
   
   public void setValue2(String value) {
      value2 = value;
   }
   
   public void setValue3(List<String> value) {
      value3 = value;
   }
}
```
google autoValue provides some ease, as it also provides immutability for free as well as
automatically generated toString, equals and hashCode functions.
```Java
@AutoValue()
abstract class SimpleObject {

    @com.google.auto.value.AutoValue.Builder()
    abstract static class Builder {

        public abstract Builder value1(int value1);

        public abstract Builder value2(java.lang.String value2);

        public abstract Builder value3(java.util.List<java.lang.String> value3);

        public abstract SimpleObject build();
    }

    static Builder builder() {
        return new AutoValue_SimpleObject.Builder();
    }

    public abstract int value1();
    
    public abstract java.lang.String value2();

    public abstract java.util.List<java.lang.String> value3();
}
```
but in terms of lines of code it does not even get close to Kotlins data classes:
```Kotlin
data class SimpleObject(var value1: Int, var value2: String, var value3: List<String>)
```
## Challenge ##
So now the challenge is to combine the relayability of autoValue and simplicity
of kotlin like single-line data class definitions. 
Choosing the easiest way of java code generation - annotion processing - the following
idea involed. 
*Let the annotiation processor generate autoValue classes from one single line*
```
import java.util.List;

import de.wr.libsimpledataclasses.DataClassFactory;

@DataClassFactory
public abstract class SimpleTestDataFactory {
    abstract Void simpleObject(int value1, String value2, List<String> value3);
}
```
This simple factory will generate one data object per method declared inside itself.
Therby the return type will be ignored, the **method name** will be used as the **name of the data class**
and each **parameter** will be transformed into a **property**

It can then be used like any manually generated autoValue class:
```Java
SimpleObject simpleObject = SimpleObject.builder().value1(2).value2("String").value3(Collections.emptyList()).build();
int i = simpleObject.value1();
```

Furthermore it allows to define additional features such as default values, nullable/nonnull,
parcable and gson-adapter feature.

## Default Values ##
```Java
@DataClassFactory
public abstract class DefaultValuesTestDataFactory {
    abstract @Nullable Void defaultValueTestObject(
            @DefaultInt(1) int intV,
            @DefaultLong(2L) long longV,
            @DefaultShort(3) short shortV,
            @DefaultByte(4) byte byteV,
            @DefaultBool(true) boolean boolV,
            @DefaultFloat(5f) float floatV,
            @DefaultDouble(6d) double doubleV
    );
}
```
adds the following default values to the builder function of the generated autoValue class
```Java
static Builder builder() {
        return new AutoValue_DefaultValueTestObject.Builder()
                  .intV(1).longV(2).shortV((short) 3).byteV((byte)4)
                  .boolV(true).floatV(5.0f).doubleV(6.0).valueS("This is a test");
    }
```

Todo:
- [ ] Publish to jcenter
- [ ] Provide samples
- [ ] support auto-value-gson default values
